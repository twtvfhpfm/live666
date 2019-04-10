package org.twtvfhpfm.live666.rtspcodec;

import java.util.HashMap;
import java.util.Map;

public class RtspResponse {
    private RtspConsts.Version version;
    private int code;
    private Map<String, String> headerMap = new HashMap<String, String>();
    private String body = null;

    public RtspResponse(){

    }

    public RtspConsts.Version getVersion() {
        return version;
    }

    public void setVersion(RtspConsts.Version version) {
        this.version = version;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setHeader(String key, String value){
        this.headerMap.put(key, value);
    }

    public Map<String, String> getHeaders(){
        return headerMap;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
