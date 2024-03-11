package github.myc.loadbalance;

import github.myc.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡策略的抽象类
 */
public abstract class AbstractLoadBalance implements LoadBalance{

    @Override
    public String selectServiceAddress(List<String> serviceAddress, RpcRequest rpcRequest) {
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            return null;
        }
        // 只有一个地址，只能选择这个
        if (serviceAddress.size() == 1) {
            return serviceAddress.get(0);
        }
        return doSelect(serviceAddress, rpcRequest);
    }

    /**
     * 有多个（两个及两个以上）服务地址可选择，从中选择一个
     */
    protected abstract String doSelect(List<String> serviceAddress, RpcRequest rpcRequest);
}
