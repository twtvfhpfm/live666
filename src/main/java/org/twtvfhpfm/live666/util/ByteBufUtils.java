package org.twtvfhpfm.live666.util;

import io.netty.util.ByteProcessor;
import io.netty.buffer.ByteBuf;

public class ByteBufUtils {
    public static String ByteBuf2String(ByteBuf buf){
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(0, bytes);
        return new String(bytes);
    }
    
}
