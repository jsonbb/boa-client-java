package com.boa.client;


public class BoaConf {

    private int minConnect = 10;
    private int maxConnect = 100;
    private int retries = 3;
    private String zkHosts = "127.0.0.1:2181";


    public BoaConf(String zkHosts) {
        this.zkHosts = zkHosts;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getZkHosts() {
        return zkHosts;
    }

    public void setZkHosts(String zkHosts) {
        this.zkHosts = zkHosts;
    }

    public int getMinConnect() {
        return minConnect;
    }

    public void setMinConnect(int minConnect) {
        this.minConnect = minConnect;
    }

    public int getMaxConnect() {
        return maxConnect;
    }

    public void setMaxConnect(int maxConnect) {
        this.maxConnect = maxConnect;
    }
}
