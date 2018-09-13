package com.boa.client.registry;


import com.boa.client.URL;
import com.boa.client.protocol.ExchangeClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class Registry {

    public  static  volatile Map<String,List<ExchangeClient>> endpointTableCache = new ConcurrentHashMap<String, List<ExchangeClient>>();
    public  static  volatile Map<String,List<URL>> consumerMap = new ConcurrentHashMap<String, List<URL>>();
    public  static  volatile Map<String,URL> localConsumerMap = new ConcurrentHashMap<String, URL>();

    public abstract void register(URL url);


    public abstract void unregister(URL url);


    public abstract void subscribe(String appName);


    public abstract void unsubscribe(String appName);


    public abstract List<URL> lookup(String appName);

    public abstract List<ExchangeClient> initEndpointTable(String appName);

    public abstract void recover();
}
