package github.myc.registry.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import github.myc.registry.ServiceRegistry;
import github.myc.utils.CuratorUtils;

import java.net.InetSocketAddress;

/**
 * 基于 Zookeeper 注册服务
 */
@Slf4j
public class ZkServiceRegistryImpl implements ServiceRegistry {

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        /**
         * 这里 rpcServiceName 后面应该是要再加上一个 "/" ？
         * 答案：不用，inetSocketAddress.toString() 返回的就有斜杠了
         */
        String ServicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        log.info("The address of service [{}] is [{}]", rpcServiceName, inetSocketAddress.toString());
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, ServicePath);
    }
}
