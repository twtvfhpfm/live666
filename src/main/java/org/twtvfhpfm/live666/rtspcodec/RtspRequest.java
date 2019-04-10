package org.twtvfhpfm.live666.rtspcodec;

import java.util.*;

public class RtspRequest {
    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public RtspConsts.Version getVersion() {
        return version;
    }

    public void setVersion(RtspConsts.Version version) {
        this.version = version;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public enum Method {
        OPTIONS, DESCRIBE, SETUP, PLAY, TEARDOWN, PAUSE,
        GET_PARAMETER, SET_PARAMETER,
    }
    private Method method;
    private RtspConsts.Version version;
    private String uri;
    private Map<String, String> headers = new HashMap<String, String>();
    public RtspRequest(Method m, String u, RtspConsts.Version v){
        setMethod(m);
        setVersion(v);
        setUri(u);
    }
    public RtspRequest(RtspRequest req){
        setMethod(req.getMethod());
        setVersion(req.getVersion());
        setUri(req.getUri());
        headers.putAll(req.headers);
    }
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Request: " + this.hashCode());
        sb.append("\nMethod: ");
        for (String key: methodMap.keySet()){
            if (methodMap.get(key).equals(getMethod())){
                sb.append(key + "\n");
                break;
            }
        }
        sb.append("Version: ");
        for (String key: versionMap.keySet()){
            if (versionMap.get(key).equals(getVersion())){
                sb.append(key + "\n");
                break;
            }
        }
        sb.append("URI: " + getUri());
        sb.append("\nHEADERS: " + headers.size());
        for (String key: headers.keySet()){
            sb.append("\nKEY: " + key + " VALUE: " + headers.get(key));
        }
        return sb.toString();
    }
    
    public void setHeader(String key, String value){
        headers.put(key, value);
    }

    public String getHeader(String key){
        return headers.get(key);
    }

    private static Map<String, Method> methodMap = new HashMap<String, Method>();
    static {
        methodMap.put("OPTIONS", Method.OPTIONS);
        methodMap.put("DESCRIBE", Method.DESCRIBE);
        methodMap.put("SETUP", Method.SETUP);
        methodMap.put("PLAY", Method.PLAY);
        methodMap.put("TEARDOWN", Method.TEARDOWN);
        methodMap.put("PAUSE", Method.PAUSE);
        methodMap.put("GET_PARAMETER", Method.GET_PARAMETER);
        methodMap.put("SET_PARAMETER", Method.SET_PARAMETER);
    }

    public static Method methodOf(String name){
        if (null == name){
            throw new NullPointerException("name");
        }
        name = name.trim().toUpperCase();
        if (name.isEmpty()){
            throw new  IllegalArgumentException("empty name");
        }
        Method result = methodMap.get(name);
        if (result == null){
            throw new  IllegalArgumentException("invalid name");
        }
        return result;
    }

    private static Map<String, RtspConsts.Version> versionMap = new HashMap<String, RtspConsts.Version>();
    static {
        versionMap.put("RTSP/1.0", RtspConsts.Version.V1_0);
        versionMap.put("RTSP/2.0", RtspConsts.Version.V2_0);
    }

    public static RtspConsts.Version versionOf(String name){
        if (null == name){
            throw new NullPointerException("name");
        }
        name = name.trim().toUpperCase();
        if (name.isEmpty()){
            throw new  IllegalArgumentException("empty name");
        }
        RtspConsts.Version result = versionMap.get(name);
        if (result == null){
            throw new  IllegalArgumentException("invalid name");
        }
        return result;
    }
}

