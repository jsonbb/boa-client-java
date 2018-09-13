package com.boa.client;


import java.io.Serializable;
import java.util.Map;

public class URL implements Serializable{

    private  String appName;
    private  String protocol;
    private  String username;
    private  String password;
    private  String host;
    private  String port;
    private  String path;
    private volatile int maxConns;
    private volatile int minConns;
    private  Map<String, String> parameters;



    public URL(String appName) {
        this.appName = appName;
    }

    public URL(String appName, String protocol, String username, String password, String host, String port, String path, Map<String, String> parameters) {
        this.appName = appName;
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters;
    }

    public URL(String appName, String protocol, String username, String password, String host, String port, String path, int maxConns, int minConns) {
        this.appName = appName;
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.maxConns = maxConns;
        this.minConns = minConns;
    }

    @Override
    public String toString() {
        return "{ appName:" + appName + " protocol:" + protocol + "  host:"
                + host + "  port:" + port + "   maxConns:" + maxConns + "  minConns:" + minConns + " }" ;
    }

    public String getPHP(){
        return String.format("%s:%s:%s", this.protocol,this.host,this.port);
    }
    public int getMaxConns() {
        return maxConns;
    }

    public void setMaxConns(int maxConns) {
        this.maxConns = maxConns;
    }

    public int getMinConns() {
        return minConns;
    }

    public void setMinConns(int minConns) {
        this.minConns = minConns;
    }

    public String getAppName() {
        return appName;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
