package org.twtvfhpfm.live666;

import org.twtvfhpfm.live666.mediasource.H264MediaSource;
import org.twtvfhpfm.live666.rtspserver.RtspServer;


/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        RtspServer server = new RtspServer();
        server.run(8888);
        //H264MediaSource s = new H264MediaSource("av.mp4");
        //s.main2();
    }
}
