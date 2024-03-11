package github.myc.provider.impl;

import github.myc.config.RpcServiceConfig;
import github.myc.enums.RpcErrorMessageEnum;
import github.myc.enums.ServiceRegistryEnum;
import github.myc.exception.RpcException;
import github.myc.extension.ExtensionLoader;
import lombok.extern.slf4j.Slf4j;
import github.myc.provider.ServiceProvider;
import github.myc.registry.ServiceRegistry;
import github.myc.remoting.transport.netty.server.NettyRpcServer;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zookeeper 服务提供方的实现
 */
@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    /**
     * 保存服务的 map
     * key：rpc 服务的名称 (interface name + group + version)
     * value：服务的对象
     */
    private final Map<String, Object> serviceMap;
    private final Set<String> registeredService;
    // ServiceRegistry 接口定义了服务注册和服务获取的方法。
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension(ServiceRegistryEnum.ZK.getName());
    }

    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if(registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces: {}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if(service == null){
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            // 因为是发布服务，所以服务的地址就是本台服务器的地址
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("occur github.myc.exception when getHostAddress", e);
        }
    }
}
