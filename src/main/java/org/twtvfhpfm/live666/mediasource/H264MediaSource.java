package org.twtvfhpfm.live666.mediasource;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part15.AvcConfigurationBox;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Sample;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.tools.Path;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class H264MediaSource extends MediaSource {
    private String fileName;
    private int seqNum = 0;
    private int timeStamp = 0;
    private long timeScale = 0; //90000
    private long duration = 0; //timeScale ＊ secs
    private double width = 0;
    private double height = 0;
    private List<Sample> sampleList = null;
    private List<ByteBuffer> spsPpsList = null;
    private int frameIdx = 0;
    public H264MediaSource(String fileName){
        this.fileName = fileName;
    }

    public H264MediaSource Initialize() throws IOException{
        Movie m = MovieCreator.build(fileName);
        for (Track track: m.getTracks()) {
            if (track.getHandler().endsWith("vide")) {
                System.out.println(track.getHandler());
                duration = track.getDuration();
                width = track.getTrackMetaData().getWidth();
                height = track.getTrackMetaData().getHeight();
                timeScale = track.getTrackMetaData().getTimescale();
                sampleList = track.getSamples();
                break;
            }
        }
        spsPpsList = getSpsPps();
        return this;
    }

    public List<ByteBuffer> getSpsPps() throws IOException
    {
        IsoFile isoFile = new IsoFile(fileName);
        AvcConfigurationBox avcCBox = Path.getPath(isoFile, "moov/trak/mdia/minf/stbl/stsd/avc1/avcC");
        if (avcCBox == null){
            return null;
        }
        List<ByteBuffer> bbList = new ArrayList<ByteBuffer>();
        bbList.add(avcCBox.getSequenceParameterSets().get(0));
        bbList.add(avcCBox.getPictureParameterSets().get(0));
        isoFile.close();
        return bbList;
    }

    public ByteBuffer getNextFrame(){
        if (frameIdx >= spsPpsList.size() + sampleList.size()){
            return null;
        }
        ByteBuffer buf = null;
        if (frameIdx < spsPpsList.size()){
            buf = spsPpsList.get(frameIdx++);
        } else {
            buf = sampleList.get(frameIdx++ - spsPpsList.size()).asByteBuffer();
            buf.getInt(); //skip length;
            timeStamp += duration / sampleList.size();
            //System.out.println("timeStamp: " + timeStamp + " duration: " + duration + " frames: " + sampleList.size());
        }
        return buf.slice();
    }

    public String buildSDP() {
        List<ByteBuffer> spsPps = null;
        try{
            spsPps = getSpsPps();
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
        byte[] sps = spsPps.get(0).array();
        byte[] pps = spsPps.get(1).array();
        String prof_lvl_id = String.format("%02x%02x%02x", sps[1], sps[2], sps[3]);
        String sprop = Base64.encode(sps) + "," + Base64.encode(pps);
        StringBuilder sb = new StringBuilder();
        sb.append("v=0\r\n");
        sb.append("t=0 0\r\n");
        sb.append("c=IN IP4 0.0.0.0\r\n");
        sb.append("a=control:*\r\n");
        sb.append("a=range:npt=0-\r\n");
        sb.append("m=video 0 RTP/AVP 96\r\n");
        sb.append("a=rtpmap:96 H264/90000\r\n");
        sb.append("a=fmtp:96 packetization-mode=1;profile-level-id=" + prof_lvl_id
                + ";sprop-parameter-sets=" + sprop + "\r\n");
        sb.append("a=control:trackID=1\r\n");
        //sb.append("m=audio 8004 RTP/AVP 3\r\n");
        //sb.append("a=control:trackID=2\r\n");
        return sb.toString();
    }

    public List<byte[]> frame2RTPPackets(ByteBuffer frame){
        List<byte[]> packets = new ArrayList<byte[]>();
        int length = frame.remaining();
        if (length <= 1024){
            byte[] rtpHeader = buildRTPHeader(2, 0, 0, 0, 1, 96,
                    seqNum, timeStamp, 0, null);
            byte[] packet = new byte[rtpHeader.length + length];
            System.arraycopy(rtpHeader, 0, packet, 0, rtpHeader.length);
            System.arraycopy(frame.array(), frame.arrayOffset(), packet, rtpHeader.length, length);
            packets.add(packet);
            seqNum++;
        } else{ //fragment
            for(int i = 1; i < length; i += 1024){
                int mark, dataLen;
                byte fuHeader;
                if (i == 1){ //first
                    mark = 0;
                    dataLen = 1024;
                    fuHeader = (byte)((1<<7) | (0<<6) | (0<<5) | (frame.get(0) & 0x1F));//start
                }else if (i >= length - 1024){ //end
                    mark = 1;
                    dataLen = (length - 1) % 1024;
                    fuHeader = (byte)((0<<7) | (1<<6) | (0<<5) | (frame.get(0) & 0x1F));//end
                }else{ //middle
                    mark = 0;
                    dataLen = 1024;
                    fuHeader = (byte)((0<<7) | (0<<6) | (0<<5) | (frame.get(0) & 0x1F));//middle
                }
                byte[] rtpHeader = buildRTPHeader(2, 0, 0, 0, mark, 96,
                        seqNum, timeStamp, 0, null);
                byte[] packet = new byte[rtpHeader.length + 2 + dataLen];
                System.arraycopy(rtpHeader, 0, packet, 0, rtpHeader.length);
                byte fuIndicator = (byte)((frame.get(0) & 0xE0) | 28);
                packet[rtpHeader.length] = fuIndicator;
                packet[rtpHeader.length + 1] = fuHeader;
                System.arraycopy(frame.array(), frame.arrayOffset() + i, packet, rtpHeader.length + 2, dataLen);
                packets.add(packet);
                seqNum++;
            }
        }
        return packets;
    }

    public void main2() throws IOException{
        String outFileName = "test_out.h264";
        byte[] startCode = {0,0,0,1};
        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(
                        new File(outFileName)));
        List<ByteBuffer> bbList = getSpsPps();
        if (bbList != null){
            for (ByteBuffer bb: bbList){
                System.out.println("sps(pps) size: " + bb.remaining());
                out.write(startCode);
                out.write(bb.array());
            }
        }
        Movie m = MovieCreator.build(fileName);
        for (Track track: m.getTracks()){
            if (track.getHandler().endsWith("soun")) continue;
            System.out.println(track.getHandler());
            System.out.println("duration: " + track.getDuration());
            System.out.println("width: " + track.getTrackMetaData().getWidth());
            System.out.println("heigh: " + track.getTrackMetaData().getHeight());
            System.out.println("ts: " + track.getTrackMetaData().getTimescale());
            List<Sample> sl = track.getSamples();
            System.out.println("total samples: " + sl.size());
            int i = 0;
            for (Sample sample: sl){
                i++;
                ByteBuffer bb = sample.asByteBuffer();
                while(bb.remaining() > 0){
                    int length = bb.getInt();
                    int size = (int)sample.getSize();
                    ByteBuffer frame = bb.slice();
                    out.write(startCode);
                    out.write(frame.array(), frame.arrayOffset(), length);
                    StringBuilder sb=new StringBuilder();
                    for(int j=0;j<length;j++){
                        sb.append(String.format(" %x " ,frame.get(j)));
                        if (j == 7) break;
                    }

                    System.out.println(String.format("frame(%d): ", length) + sb.toString());
                    bb.position(bb.position() + length);
                }
            }
        }
        out.close();
    }
}
