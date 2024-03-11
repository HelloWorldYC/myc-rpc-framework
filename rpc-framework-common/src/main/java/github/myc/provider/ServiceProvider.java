package github.myc.provider;


import github.myc.config.RpcServiceConfig;

/**
 * 保存和提供服务对象
 */
public interface ServiceProvider {

    /**
     * @param rpcServiceConfig 服务的相关属性
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * @param rpcServiceName rpc 服务名称
     * @return 服务的对象
     */
    Object getService(String rpcServiceName);

    /**
     * @param rpcServiceConfig 服务的相关属性
     */
    void publishService(RpcServiceConfig rpcServiceConfig);
}
