package org.twtvfhpfm.live666.mediasource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class MediaSource {
    public abstract String buildSDP();
    public abstract ByteBuffer getNextFrame();
    public abstract List<byte[]> frame2RTPPackets(ByteBuffer frame);

    public byte[] buildRTPHeader(int version, int padding, int extension, int csrcCount, int mark, int pt,
                                 int seqNum, int timeStamp, int ssrc, byte[] csrc){
        byte[] header = new byte[12 + 4 * csrcCount];
        header[0] = (byte)((version << 6) | (padding << 5) | (extension << 4) | csrcCount);
        header[1] = (byte)((mark << 7) | pt);
        header[2] = (byte)(seqNum >> 8);
        header[3] = (byte)(seqNum & 0xff);
        header[4] = (byte)(timeStamp >> 24);
        header[5] = (byte)((timeStamp >> 16) & 0xff);
     header[6] = (byte)((timeStamp >> 8) & 0xff);
        header[7] = (byte)(timeStamp & 0xff);
        header[8] = (byte)(ssrc >> 24);
        header[9] = (byte)((ssrc >> 16) & 0xff);
        header[10] = (byte)((ssrc >> 8) & 0xff);
        header[11] = (byte)(ssrc & 0xff);
        if (csrcCount > 0 && csrc != null) {
            for (int i = 0; i < csrcCount * 4; i++) {
                header[12 + i] = csrc[i];
            }
        }
        return header;
    }
}
