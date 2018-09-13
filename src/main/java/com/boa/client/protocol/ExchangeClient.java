package com.boa.client.protocol;

import com.boa.client.URL;
import com.boa.client.exception.RpcException;

import java.util.concurrent.ConcurrentLinkedQueue;


public interface ExchangeClient<T> {
    /**
     * 获取连接池
     * @return
     */
    ConcurrentLinkedQueue<ClientWrapper> getConnectPool();

    /**
     * 获取URL
     * @return
     */
    URL getURL();

    /**
     * 清理连接池
     */
    void clearPool() throws Exception;

    /**
     * 清空所有连接
     */
    void clearAllConn();

    /**
     * 是否需要回收连接，否则关闭连接
     * @return
     */
    boolean isRecycle();

    /**
     * 回收连接
     * @param conn
     */
    void recycleConnect(ClientWrapper conn);

    /**
     * 获取一个连接
     * @return
     * @throws RpcException
     */
    ClientWrapper getConnect() throws RpcException;

    /**
     * provider是否可连接
     * @return
     */
    public boolean isConnect();
}
