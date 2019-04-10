package org.twtvfhpfm.live666.rtspserver;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.twtvfhpfm.live666.mediasource.H264MediaSource;
import org.twtvfhpfm.live666.mediasource.MediaSource;
import org.twtvfhpfm.live666.rtspcodec.RtspRequest;
import org.twtvfhpfm.live666.rtspcodec.RtspResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.ByteBuffer;

public class RtspServerHandler extends SimpleChannelInboundHandler<RtspRequest> {

    private Set<String> sessionSet = new HashSet<String>();
    private MediaSource source = null;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RtspRequest request) throws Exception {
        System.out.println("channelRead0");
        System.out.println(request);

        RtspResponse response = new RtspResponse();
        response.setVersion(request.getVersion());
        response.setCode(200);
        String seqNo = request.getHeader("CSeq");
        if (seqNo != null){
            response.setHeader("CSeq", seqNo);
        }
        Date nowDate = new Date();
        boolean startStream = false;
        SimpleDateFormat sf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss 'GMT'");
        response.setHeader("Date", sf.format(nowDate));
        switch(request.getMethod()){
            case OPTIONS:
                handleOptions(response);
                break;
            case DESCRIBE:
                handleDescribe(response);
                break;
            case SETUP:
                handleSetup(request, response);
                break;
            case PLAY:
                handlePlay(request, response);
                startStream = true;
                break;
        }
        ctx.writeAndFlush(response);
        if (startStream){
            sendStream(ctx, source);
        }
    }

    private void sendStream(final ChannelHandlerContext ctx, final MediaSource source){
        new Thread(){
            @Override
            public void run(){
                ByteBuffer frame = null;
                while ((frame = source.getNextFrame()) != null){
                    List<byte[]> pktList = source.frame2RTPPackets(frame);
                    for(byte[] data : pktList){
                        ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
                        buffer.put((byte)'$');
                        buffer.put((byte)0);
                        buffer.putShort((short)data.length);
                        buffer.put(data);
                        ctx.writeAndFlush(Unpooled.copiedBuffer(buffer.array()));
                        System.out.print("send data " + String.format("%5d", data.length) + " bytes:\t\t");
                        for (int i = 0; i< Math.min(20, data.length);i++) System.out.print(String.format("%2x ", data[i]));
                        System.out.println();
                    }
                }
            }
        }.start();
    }

    private void handleDescribe(RtspResponse response) {
        response.setHeader("Content-Base", "rtsp://127.0.0.1:8888/");
        response.setHeader("Content-Type", "application/sdp");
        try {
            source = new H264MediaSource("4.mp4").Initialize();
            String sdp = source.buildSDP();
            response.setHeader("Content-Length", Integer.toString(sdp.length()));
            response.setBody(sdp);
        }catch (IOException e){
            response.setCode(300);
        }
    }

    private void handleSetup(RtspRequest request, RtspResponse response){
        String transport = request.getHeader("Transport");
        response.setHeader("Transport", transport);
        Random ra = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++){
            sb.append(String.format("%02x", ra.nextInt(255)));
        }
        sessionSet.add(sb.toString());
        response.setHeader("Session", sb.toString());
    }

    private void handlePlay(RtspRequest request, RtspResponse response){
        String session = request.getHeader("Session");
        if (sessionSet.contains(session)){
            response.setHeader("Session", session);
        }else{
            response.setCode(404);
            return;
        }
        String range = request.getHeader("Range");
        response.setHeader("Range", range);
        response.setHeader("RTP-Info", "url=rtsp://127.0.0.1:8888/trackID=1;seq=1;rtptime=0");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
        cause.printStackTrace();
    }

    private void handleOptions(RtspResponse response){
        response.setHeader("Public", "DESCRIBE,SETUP,PLAY,TEARDOWN");
    }
}
