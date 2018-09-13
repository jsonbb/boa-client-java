package com.boa.client.registry.zookeeper;

import com.boa.client.Constant;
import com.boa.client.URL;
import com.boa.client.exception.RpcException;
import com.boa.client.protocol.ExchangeClient;
import com.boa.client.protocol.ExchangeClientFactory;
import com.boa.client.registry.Registry;
import com.boa.client.utils.NetUtils;
import org.apache.curator.framework.api.CuratorWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;


public class ZookeeperRegistry extends Registry {
    private final static Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);


    private final ConcurrentMap<String, CuratorWatcher> zkListeners = new ConcurrentHashMap<String, CuratorWatcher>();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
    private final CuratorZookeeperClient zkClient;
    public ZookeeperRegistry(String zkAddress) {
        this.zkClient = new CuratorZookeeperClient(zkAddress,null);
        this.zkClient.addStateListener(new StateListener() {
            public void stateChanged(int state) {
                if (state == RECONNECTED) {
                    try {
                        ZookeeperRegistry.this.recover();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        //定时检测本来缓存服务列表是否可用
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {

                for(String appName:endpointTableCache.keySet()){
                    List<ExchangeClient> clients = endpointTableCache.get(appName);
                    System.out.println(String.format("[%s] providers count : %d",appName,clients.size()));
                    if (clients != null && clients.size() > 0){
                        for (ExchangeClient client:clients){
                            try {
                                client.clearPool();
                            } catch (Exception e) {
//                                clients.remove(client);
                               logger.error(e.getMessage(),e);
                            }
                            System.out.println(String.format("%s connect pool size: %d",client.getURL(),client.getConnectPool().size()));
                        }

                    }else {
                        endpointTableCache.remove(appName);
                    }
                }

            }
        }, 2,1,TimeUnit.MINUTES);

    }


    /**
     * 重连的时候，恢复相关信息
     */
    public  void recover(){
        for (String appName:localConsumerMap.keySet()) {
            zkClient.createEphemeral(String.format(Constant.CONSUMER_REGISTER_PATH, appName, NetUtils.getLocalHostIP()));
        }
        for (String appName:zkListeners.keySet()){
            String appProviderPath = String.format(Constant.PROVIDER_PATH, appName);
            zkClient.addChildListener(appProviderPath,zkListeners.get(appName));
        }
    }

    /**
     * 注册consumer信息
     * @param url
     */
    public void register(URL url) {
        if (localConsumerMap.get(url.getAppName()) == null){
            localConsumerMap.putIfAbsent(url.getAppName(),url);
        }
        zkClient.createEphemeral(String.format(Constant.CONSUMER_REGISTER_PATH, url.getAppName(), NetUtils.getLocalHostIP()));
    }

    public void unregister(URL url) {
        zkClient.delete(String.format(Constant.CONSUMER_REGISTER_PATH, url.getAppName(), NetUtils.getLocalHostIP()));
    }

    public void subscribe(String appName ) {
        final String fAppName = appName;
        String appProviderPath = String.format(Constant.PROVIDER_PATH, appName);
        CuratorWatcher watcher = zkClient.createChildListener(new ChildListener() {
            @Override
            public void childChanged(String path, List<String> children) {
                System.out.println("change ------>:"+path);
                if (children == null || children.size() == 0){
                    //如果provider为空，则清除相关连接
                    ZookeeperRegistry.this.clearProviders(fAppName);
                }else{
                    //在本地服务缓存表中删除停止的服务，加入新增的服务
                    List<ExchangeClient> clients = new ArrayList<ExchangeClient>(ZookeeperRegistry.this.endpointTableCache.get(fAppName));
                    int maxConn = ZookeeperRegistry.this.maxConn(children.size());
                    int minConn = ZookeeperRegistry.this.minConn(children.size());

                    HashSet<String> childrenSet = new HashSet<String>(children);
                    List<ExchangeClient> reservedClients = new CopyOnWriteArrayList<ExchangeClient>();
                    for (ExchangeClient c: clients){
                       String providerStr =  c.getURL().getPHP();
                       if (childrenSet.contains(providerStr)){
                           //更新最大与最小连接数
                           URL url = c.getURL();
                           url.setMaxConns(maxConn);
                           url.setMinConns(minConn);
                           if (c.isConnect()){
                              reservedClients.add(c);
                           }
                           children.remove(providerStr);
                       }else{
                           //清除需删除的服务
                           c.clearAllConn();
                           c = null;
                       }
                    }
                    //初始化新加入的providers
                    List<ExchangeClient> newClients = ZookeeperRegistry.this.toClient(fAppName,children,minConn,maxConn);
                    newClients.addAll(reservedClients);
                    //更新本地缓存表
                    ZookeeperRegistry.this.endpointTableCache.put(fAppName,newClients);
                }

            }
        });
        if(zkListeners.putIfAbsent(appName,watcher) == null){
            zkClient.addChildListener(appProviderPath,watcher);
        }
    }

    public void unsubscribe(String path) {
        CuratorWatcher watcher = zkListeners.get(path);
        zkListeners.remove(path);
        zkClient.removeChildListener(watcher);
    }

    /**
     * 初始化本地endpoint缓存表
     * @param appName
     * @return
     */
    @Override
    public List<ExchangeClient> initEndpointTable(String appName) {
        List<String> providers = this.zkClient.getChildren(String.format(Constant.PROVIDER_PATH, appName));
        List<ExchangeClient> clients = toClient(appName,providers);
        endpointTableCache.putIfAbsent(appName,clients);
        return clients;
    }

    /**
     * 清理appName 下的所有provider
     * @param appName
     */
    private void clearProviders(String appName){
        List<ExchangeClient> clients = endpointTableCache.get(appName);
        if (clients != null){
            for (ExchangeClient client: clients){
                client.clearAllConn();
                client = null;
            }
            endpointTableCache.remove(appName);
        }
    }


    /**
     * 每个provider最小连接数
     * @param totalProviders
     * @return
     */
    private int minConn(int totalProviders){
        if (Constant.MIN_CONNECTS < totalProviders){
            return 2;
        }else {
            return Constant.MIN_CONNECTS /totalProviders + 1;
        }

    }

    /**
     * 每个provider最大连接数
     * @param totalProviders
     * @return
     */
    private int maxConn(int totalProviders){
        if (Constant.MAX_CONNECTS < totalProviders){
            return 2;
        }else {
            return Constant.MAX_CONNECTS /totalProviders + 1;
        }

    }

    /**
     * providers字符串（eg:thrift:127.0.0.1:9889）转换成ExchangeClient实例
     * @param appName
     * @param providers
     * @return
     */
    private List<ExchangeClient> toClient(String appName,List<String> providers ){
        List<ExchangeClient> clients = new CopyOnWriteArrayList<>();
        int len = providers.size();
        if (providers != null){
            for (String c:providers){
                String[] cs = c.split(":");
                if (cs.length == 3){
                    URL url =new URL(appName,cs[0],null,null,cs[1],cs[2],
                            c,maxConn(len),minConn(len));
                    try{
                        clients.add(ExchangeClientFactory.createClient(url));
                    }catch (RpcException e){
                        logger.error("create client{}  fail",url);
                       logger.error(e.getMessage(),e);
                    }

                }
            }
        }
        return clients;
    }

    /**
     * providers字符串（eg:thrift:127.0.0.1:9889）转换成ExchangeClient实例
     * @param appName
     * @param providers
     * @param minConn
     * @param maxConn
     * @return
     */
    private List<ExchangeClient> toClient(String appName,List<String> providers,int minConn,int maxConn ){
        List<ExchangeClient> clients = new CopyOnWriteArrayList<>();
        if (providers != null){
            for (String c:providers){
                String[] cs = c.split(":");
                if (cs.length == 3){
                    URL url =new URL(appName,cs[0],null,null,cs[1],cs[2],
                            c,maxConn,minConn);
                    try{
                        clients.add(ExchangeClientFactory.createClient(url));
                    }catch (RpcException e){
                        e.printStackTrace();
                        logger.error("create client{}  fail",url);
                        logger.error(e.getMessage(),e);
                    }

                }
            }
        }
        return clients;
    }

    public List<URL> lookup(String appName) {
        return null;
    }
}
