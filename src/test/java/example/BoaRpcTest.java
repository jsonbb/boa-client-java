package example;


import com.boa.client.BoaClient;
import com.boa.client.BoaConf;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BoaRpcTest {

    @Test
    public void tetstRequest(){
        BoaConf conf = new BoaConf("10.28.102.136:2181");
        BoaClient client = new BoaClient(conf);
        Map paramMap = new HashMap<>();
        paramMap.put("id","`123456yt");
        Map re = client.request("test_name.demoEndpoint.Demo.test",paramMap);
        System.out.println(re);
        ExecutorService pool = Executors.newFixedThreadPool(50);
        for(int i = 0; i < 50; i++){
            pool.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    BoaConf conf = new BoaConf("10.28.102.136:2181");
                    BoaClient client = new BoaClient(conf);
                    Map paramMap = new HashMap<>();
                    paramMap.put("id","`123456yt");

                    Map re = client.request("test_name.demoEndpoint.Demo.test",paramMap);
                    System.out.println(re);

                    while (true){
                        try {
                            Thread.sleep(10000);
                            re = client.request("test_name.demoEndpoint.Demo.test",paramMap);
                            System.out.println(re);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
//                    return null;
                }
            });
        }


        while (true){
            try {
                Thread.sleep(1000);
//                re = client.request("test_name.demoEndpoint.Demo.test",paramMap);
//                System.out.println(re);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
