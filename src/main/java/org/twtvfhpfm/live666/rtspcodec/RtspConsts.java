package org.twtvfhpfm.live666.rtspcodec;

import java.util.HashMap;
import java.util.Map;

public class RtspConsts {
    public enum Version{
        V1_0, V2_0,
    }

    public static Map<Version, String> versionMap = new HashMap<Version, String> ();
    static{
        versionMap.put(Version.V1_0, "RTSP/1.0");
        versionMap.put(Version.V2_0, "RTSP/2.0");
    }

    public static Map<Integer, String> respCodeMap = new HashMap<Integer, String>();
    static{
        respCodeMap.put(200, "OK");
    }
}
