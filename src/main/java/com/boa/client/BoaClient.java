package com.boa.client;


import com.boa.client.exception.HighLoadException;
import com.boa.client.exception.RpcException;
import com.boa.client.loadBalance.LoadBalance;
import com.boa.client.loadBalance.impl.RandomLoadBalance;
import com.boa.client.protocol.ClientWrapper;
import com.boa.client.protocol.ExchangeClient;
import com.boa.client.registry.Registry;
import com.boa.client.registry.zookeeper.ZookeeperRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoaClient {
    private static Logger logger = LoggerFactory.getLogger(BoaClient.class);
    private final String protocol = "thrift";
    private Registry registry;
    private LoadBalance loadBalance;

    public BoaClient(String zkHosts) {
        this.registry = new ZookeeperRegistry(zkHosts);
        this.loadBalance = new RandomLoadBalance();
    }
    public BoaClient(BoaConf conf) {
        this.registry = new ZookeeperRegistry(conf.getZkHosts());
        this.loadBalance = new RandomLoadBalance();
        Constant.MAX_CONNECTS = conf.getMaxConnect();
        Constant.MIN_CONNECTS = conf.getMinConnect();
        Constant.RETRIES = conf.getRetries();
    }

    public BoaClient(String zkAddress,int networkTimeout) {
        this(zkAddress);
        Constant.NETWORK_TIMEOUT = networkTimeout;
    }

    /**
     * 请求远程endpoint
     * @param endpoint
     * @param param
     * @return
     */
    public Map request(String endpoint,Map param){
        String appName = endpoint.substring(0,endpoint.indexOf("."));
        List<ExchangeClient> clients =  registry.endpointTableCache.get(appName);
        try {
            if (clients == null){
                if (registry.localConsumerMap.get(appName) == null){
                    URL url = new URL(appName);
                    registry.register(url);
                    registry.localConsumerMap.put(appName,url);
                    clients =  registry.initEndpointTable(appName);
                    registry.subscribe(appName);
                }
            }
            return  invoke(endpoint,param,clients);
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return null;
    }

    /**
     * 通过loadbalance调用远程服务
     * @param endpoint
     * @param param
     * @param clients
     * @return
     * @throws Exception
     */
    private  Map invoke(String endpoint,Map param,List<ExchangeClient> clients)throws Exception{
        List<ExchangeClient> invoked = new ArrayList<>(clients.size()); // 已经调用过的providers.
        for (int i = 0; i < Constant.RETRIES; i ++){
            ExchangeClient client = loadBalance.select(clients,invoked);//选择一个provider
            ClientWrapper conn = null;
            try {
                conn = client.getConnect();//获取该provider的一个connect
                if (conn == null){
                    if (i > 1) {
                        Thread.sleep(2000);
                    }
                    continue;
                }
                Map result = conn.request(endpoint,param);
                client.recycleConnect(conn);
                return result;
            }catch (RpcException e){
                invoked.add(client);
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
                continue;
            }catch (Throwable e){
                if (conn != null){
                    client.recycleConnect(conn);
                }
                throw e;
            }
        }
        throw new HighLoadException();
    }


}
