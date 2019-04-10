package org.twtvfhpfm.live666.rtspcodec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.ByteProcessor;
import java.util.*;

import org.twtvfhpfm.live666.rtspcodec.RtspRequest;
import org.twtvfhpfm.live666.util.ByteBufUtils;

public class RtspRequestDecoder extends ByteToMessageDecoder {
    private enum State {
        INITIAL, FIRST_LINE, HEADER_LINE,
    }
    private RtspRequest request;
    private State decoderState = State.INITIAL;

    private void parseLine(ByteBuf lineBuf){
        if (decoderState == State.INITIAL){
            return;
        }
        byte[] lineBytes = new byte[lineBuf.readableBytes()];
        lineBuf.getBytes(0, lineBytes);
        String lineStr = new String(lineBytes);
        if (decoderState == State.FIRST_LINE){
            //decode method, uri and version
            String[] strList = lineStr.split("\\t| ");
            if (strList.length != 3) return;
            try{
                RtspRequest.Method m = RtspRequest.methodOf(strList[0].trim());
                String uri = strList[1];
                RtspConsts.Version v = RtspRequest.versionOf(strList[2].trim());
                request = new RtspRequest(m, uri, v);
                decoderState = State.HEADER_LINE;
            } catch (Exception e){
                e.printStackTrace();
            }
        } else if (decoderState == State.HEADER_LINE){
            //decode header
            int idx = lineStr.indexOf(':');
            if (idx != -1){
                String key = lineStr.substring(0, idx).trim();
                String value = lineStr.substring(idx + 1).trim();
                System.out.println("debug: setHeader " + key + "=" + value);
                request.setHeader(key, value);
            }
        }
    }

    private void decodeRTCP(ChannelHandlerContext ctx, ByteBuf buffer,
                            List<Object> out){
        if (buffer.readableBytes() < 4){
            return;
        }
        int frameLen = buffer.getShort(buffer.readerIndex() + 2);
        if (buffer.readableBytes() < 4 + frameLen){
            return;
        }
        int channel = buffer.getByte(buffer.readerIndex() + 1);
        System.out.println("===>RTCP: chn " + channel + " len: " + frameLen);
        buffer.readerIndex(buffer.readerIndex() + 4 + frameLen);
    }

    private void decodeRTSP(ChannelHandlerContext ctx, ByteBuf buffer,
                            List<Object> out){
        ByteBuf sep = Unpooled.copiedBuffer("\r\n\r\n".getBytes());
        int idx = ByteBufUtil.indexOf(sep, buffer);
        if (idx != -1) {
            decoderState = State.FIRST_LINE;
            ByteBuf lineSep = Unpooled.copiedBuffer("\r\n".getBytes());
            int lineStart = 0, lineEnd = idx;
            ByteBuf reqBuf = buffer.slice(0, idx + lineSep.readableBytes());
            System.out.println("debug: " + ByteBufUtils.ByteBuf2String(reqBuf));
            while ((lineEnd = ByteBufUtil.indexOf(lineSep, reqBuf)) != -1) {
                ByteBuf lineBuf = reqBuf.slice(0, lineEnd);
                parseLine(lineBuf);
                lineStart = lineEnd + lineSep.readableBytes();
                reqBuf = reqBuf.slice(lineStart, reqBuf.readableBytes() - lineStart);
            }
            buffer.readerIndex(idx + sep.readableBytes());
            out.add(new RtspRequest(request));
            decoderState = State.INITIAL;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer,
            List<Object> out) throws Exception {
        System.out.println("buffer: " + buffer);
        System.out.println("out: " + out);
        if (buffer.getByte(buffer.readerIndex()) == '$'){
            decodeRTCP(ctx, buffer, out);
        }else{
            decodeRTSP(ctx, buffer, out);
        }
    }

    @Override
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf buffer,
            List<Object> out) throws Exception {
        System.out.println("in: " + buffer);
        System.out.println("out: " + out);
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        System.out.println(new String(data));
        super.decodeLast(ctx, buffer, out);
    }
}
