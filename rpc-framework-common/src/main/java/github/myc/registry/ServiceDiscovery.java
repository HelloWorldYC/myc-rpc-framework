package github.myc.registry;

import github.myc.annotation.SPI;
import github.myc.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

/**
 * 服务查询
 */
@SPI
public interface ServiceDiscovery {
    /**
     * 通过 rpc 服务的名称查询服务的地址
     * @param rpcRequest rpc 服务类
     * @return  服务地址
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
