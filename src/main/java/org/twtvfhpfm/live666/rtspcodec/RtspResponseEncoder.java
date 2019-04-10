package org.twtvfhpfm.live666.rtspcodec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.Map;

public class RtspResponseEncoder extends MessageToByteEncoder<RtspResponse> {
    protected void encode(ChannelHandlerContext ctx, RtspResponse response, ByteBuf byteBuf) throws Exception {
        String version = RtspConsts.versionMap.get(response.getVersion());
        int code = response.getCode();
        String desc = RtspConsts.respCodeMap.get(code);
        StringBuilder sb = new StringBuilder();
        sb.append(version + " ").append(code + " ").append(desc + "\r\n");
        for (Map.Entry e: response.getHeaders().entrySet()){
            sb.append(e.getKey() + ": " + e.getValue() + "\r\n");
        }
        sb.append("\r\n");
        if (response.getBody() != null){
            sb.append(response.getBody());
        }
        System.out.println("debug: response:\n" + sb.toString());
        byteBuf.writeBytes(sb.toString().getBytes());
    }
}
