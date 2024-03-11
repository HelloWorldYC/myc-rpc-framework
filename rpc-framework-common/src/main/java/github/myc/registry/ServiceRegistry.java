package github.myc.registry;


import github.myc.annotation.SPI;

import java.net.InetSocketAddress;

/**
 * 服务注册
 */
@SPI
public interface ServiceRegistry {
    /**
     * 注册服务
     * @param rpcServiceName rpc 服务名称
     * @param inetSocketAddress 服务所在服务端地址
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);
}
