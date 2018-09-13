package com.boa.client.utils;

import java.net.InetAddress;


public class NetUtils {

    public static String getLocalHostIP() {
        String ip;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
        } catch(Exception ex) {
            ip = "";
        }
        return ip;
    }
}
