package com.boa.client.loadBalance.impl;

import com.boa.client.loadBalance.LoadBalance;
import com.boa.client.protocol.ExchangeClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class RandomLoadBalance implements LoadBalance {

    private final Random random = new Random();
    @Override
    public ExchangeClient select(List<ExchangeClient> clients,List<ExchangeClient> selected) {
        int len = clients.size();

        if (len == 2 && selected != null && !selected.isEmpty()) {
            return selected.get(0) == clients.get(0) ? clients.get(1) : clients.get(0);
        }
        ExchangeClient client = clients.get(random.nextInt(len));
        if(selected != null && !selected.isEmpty()&&selected.contains(client)){
            List<ExchangeClient> reclients = new ArrayList<>();
            for (ExchangeClient c : clients){
                if (!selected.contains(c)){
                    reclients.add(c);
                }
            }
            return reclients.get(random.nextInt(reclients.size()));
        }
        return client;

    }
}
