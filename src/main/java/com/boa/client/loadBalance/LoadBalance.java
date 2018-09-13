package com.boa.client.loadBalance;


import com.boa.client.protocol.ExchangeClient;

import java.util.List;

public interface LoadBalance {

    ExchangeClient select(List<ExchangeClient> clients,List<ExchangeClient> invoked);

}
