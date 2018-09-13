package com.boa.client.protocol;


import com.boa.client.exception.RpcException;

import java.util.Date;
import java.util.Map;

public interface ClientWrapper {

    public Map request(String endpoint, Map param)throws RpcException;
    public void close();
    public Date recentUseTime();
    public boolean isClear();
    public boolean isUse();
}
