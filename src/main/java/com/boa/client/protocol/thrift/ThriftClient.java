package com.boa.client.protocol.thrift;

import com.boa.client.Constant;
import com.boa.client.URL;
import com.boa.client.exception.RpcException;
import com.boa.client.protocol.ClientWrapper;
import com.boa.client.protocol.ExchangeClient;
import com.boa.client.utils.JsonMapper;
import com.boa.client.utils.NetUtils;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class ThriftClient implements ExchangeClient<BoarpcService.Client>{

    private static Logger logger = LoggerFactory.getLogger(ThriftClient.class);
    private volatile ConcurrentLinkedQueue<ClientWrapper>  connectPool = new ConcurrentLinkedQueue<>();

    //记录connection 数量
    private volatile AtomicInteger counter = new AtomicInteger(0);

    private volatile boolean isRecycle = true;

    private URL url = null;

    public ThriftClient(URL url) throws RpcException {
        this.url = url;
        for (int i=0; i < url.getMinConns();i++){
            connectPool.add(createClient());
            counter.incrementAndGet();
        }
    }

    /**
     * 清理连接池，离最近使用超过6分钟的conn释放掉
     */
    @Override
    public void clearPool() throws Exception{
        int poolSize = connectPool.size();
        if ( poolSize > url.getMinConns()){
            for (ClientWrapper conn:connectPool){
                if (poolSize > url.getMinConns() && conn.isClear()){
                    if(connectPool.remove(conn)){
                        conn.close();
                        poolSize --;
                        counter.decrementAndGet();
                    }
                    continue;
                }
            }
        }
    }

    public boolean isConnect(){
        //检测provider是否存在
        ClientWrapper pqconn = null;
        for (int i = 0; i < 3; i++){
            try {
                pqconn = createClient();
                pqconn.request(String.format(Constant.HEART_ENDPOINT,url.getAppName()),new HashMap<>());
                return true;
            }catch (RpcException e){
                logger.error("provider :{} unavailable !!!!!",url.getPHP());
                logger.error(e.getMessage(),e);
                if (i == 2){
                    isRecycle = false;
                    for (ClientWrapper c:connectPool){
                        c.close();
                    }
                   return false;
                }
            }finally {
                if (pqconn != null)
                    pqconn.close();
            }
        }
        return false;
    }

    @Override
    public void clearAllConn() {
        isRecycle = false;
        for (ClientWrapper conn:connectPool){
            if (!conn.isUse()){
                conn.close();
                conn = null;
            }
        }
    }

    @Override
    public boolean isRecycle() {
        return isRecycle;
    }

    @Override
    public void recycleConnect(ClientWrapper conn) {
        if (isRecycle) {
            counter.incrementAndGet();
            connectPool.add(conn);
        }else {
            conn.close();
            conn = null;
        }
    }

    /**
     * 获取provider的连接，如果超过最大连接数，则返回null
     * @return
     * @throws RpcException
     */
    @Override
    public ClientWrapper getConnect()  throws RpcException {
        ClientWrapper conn = connectPool.poll();
        if (conn != null){
            counter.decrementAndGet();
            return conn;
        }else if (counter.get() < url.getMaxConns()){
            return createClient();
        }
        return null;
    }

    private  ClientWrapper createClient() throws RpcException {
        TTransport transport = null;
        try{
            TSocket tSocket = new TSocket(url.getHost(), Integer.parseInt(url.getPort()));
            tSocket.setSocketTimeout(Constant.NETWORK_TIMEOUT);
            transport = tSocket;
            transport.open();
            TProtocol protocol = new TCompactProtocol(transport);
            return new ThriftClientWrapper(protocol,transport);
        }catch (TTransportException e){
            throw new RpcException(e);
        }
    }

    @Override
    public  ConcurrentLinkedQueue<ClientWrapper> getConnectPool() {
        return this.connectPool;
    }


    @Override
    public URL getURL() {
        return this.url;
    }

    private class ThriftClientWrapper extends BoarpcService.Client implements ClientWrapper{
        private Date recentUseTime;
        private TTransport transport = null;
        private volatile  boolean isUse = false;
        public ThriftClientWrapper(TProtocol prot,TTransport transport) {
            super(prot);
            recentUseTime = new Date();
            this.transport = transport;
        }

        public ThriftClientWrapper(TProtocol iprot, TProtocol oprot,TTransport transport) {
            super(iprot, oprot);
            recentUseTime = new Date();
            this.transport = transport;
        }

        public void setRecentUseTime(Date recentUseTime) {
            this.recentUseTime = recentUseTime;
        }

        @Override
        public Map request(String endpoint,Map param)throws RpcException {
            isUse = true;
            try{
                String[] endpoints = endpoint.split("\\.");
                List<String> paramList = new ArrayList<>();
                paramList.add(JsonMapper.toJsonString(param));
                String result = dispatcher(NetUtils.getLocalHostIP(),endpoints[1],endpoints[2],endpoints[3],paramList);
                this.recentUseTime = new Date();
                return (Map) JsonMapper.fromJsonString(result,Map.class);
            }catch (TException e){
                throw new RpcException(e);
            }finally {
                isUse = false;
            }
        }

        @Override
        public boolean isClear() {
            return !this.isUse &&(new Date().getTime() - recentUseTime.getTime() > 120000);
        }

        public boolean isUse(){
            return this.isUse;
        }
        @Override
        public Date recentUseTime() {
            return this.recentUseTime;
        }

        @Override
        public void close() {
            try{
                if (this.transport != null && this.transport.isOpen()){
                    this.transport.close();
                }
            }catch (Exception e){

            }
        }
    }
}
