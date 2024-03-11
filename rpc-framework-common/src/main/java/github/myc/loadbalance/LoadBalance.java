package github.myc.loadbalance;

import github.myc.annotation.SPI;
import github.myc.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡策略接口
 */
@SPI
public interface LoadBalance {
    /**
     * 从现有服务地址列表中选择一个地址
     * @param serviceList   现有服务地址列表
     * @param rpcRequest    rpc 请求
     * @return              选择出来的目标服务地址
     */
    String selectServiceAddress(List<String> serviceList, RpcRequest rpcRequest);
}
