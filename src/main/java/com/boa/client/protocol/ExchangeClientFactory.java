package com.boa.client.protocol;

import com.boa.client.URL;
import com.boa.client.exception.RpcException;
import com.boa.client.protocol.thrift.ThriftClient;


public class ExchangeClientFactory {

    public static ExchangeClient createClient(URL url) throws RpcException {
        if ("thrift".equals(url.getProtocol())){
            return new ThriftClient(url);
        }
        return null;
    }
}
