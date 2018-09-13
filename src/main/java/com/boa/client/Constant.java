package com.boa.client;


public class Constant {

    public static String CONSUMER_REGISTER_PATH = "/boa_rpc/%s/consumers/%s";
    public static String PROVIDER_PATH = "/boa_rpc/%s/providers";
    public static String HEART_ENDPOINT = "%s.heart.ping.pong";
    public static volatile int NETWORK_TIMEOUT = 600000;
    public static volatile  int MIN_CONNECTS = 10;
    public static volatile int MAX_CONNECTS = 100;
    public static volatile int RETRIES = 3;
}
