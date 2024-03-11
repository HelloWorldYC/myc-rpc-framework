package github.myc.loadbalance.loadbalancer;

import github.myc.loadbalance.AbstractLoadBalance;
import github.myc.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡策略
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    @Override
    protected String doSelect(List<String> serviceAddress, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddress.get(random.nextInt(serviceAddress.size()));
    }
}
