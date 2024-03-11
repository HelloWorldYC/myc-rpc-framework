package github.myc.utils;


import github.myc.enums.RpcConfigEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Curator 工具类，Curator 是 Zookeeper 客户端工具
 */
@Slf4j
public final class CuratorUtils {

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    // 该服务对应的节点下的所有子节点
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    private static CuratorFramework zkClient;
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2281";

    private CuratorUtils(){
    }

    /**
     * 在 zookeeper 中建立持久化节点。持久化节点与临时节点不同，当客户端断开连接时，不会删除持久节点。
     * @param path      节点路径
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path){
        try {
            if(REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exits. The node is: [{}]", path);
            } else {
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is: [{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 获取一个节点下的所有子节点
     * @param rpcServiceName rpc 服务的名称
     * @return 该节点下的所有子节点
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName){
        if(SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result= zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            registerWathcer(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children node for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * 注册监听特定节点的变动
     * @param rpcServiceName rpc 服务名称
     */
    public static void registerWathcer(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        // 监听器，监听指定路径下子节点的变化情况
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        // 监听器回调，当节点变动时，要更新 SERVICE_ADDRESS_MAP
        pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
                SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
            }
        });
        pathChildrenCache.start();
    }

    /**
     * 某个服务端清除在 zookeeper 上注册的服务
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress){
        REGISTERED_PATH_SET.parallelStream().forEach(p -> {
            try {
                if(p.endsWith(inetSocketAddress.toString())){
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("clear github.myc.registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared: [{}]", REGISTERED_PATH_SET.toString());
    }


    public static CuratorFramework getZkClient() {
        // 检查用户是否有设置 zookeeper 的地址，即注册地址
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;
        // 如果 zkClient 已经启动，直接返回
        if(zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        // 重试策略：重试 3 次，每次重试之间的睡眠时间递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 要连接的服务器，也可以是服务器列表
        zkClient = CuratorFrameworkFactory.builder().connectString(zookeeperAddress).retryPolicy(retryPolicy).build();
        zkClient.start();

        // 等待 30s 直到连接上 zookeeper，若成功连接，则返回 true，若超出指定时间还没连接上则抛出异常
        try {
            zkClient.blockUntilConnected(30, TimeUnit.SECONDS);
            log.info("Connected to Zookeeper successfully.");
        } catch (InterruptedException e) {
            log.error("Time out waiting to connect to Zookeeper!");
        }
        return zkClient;
    }
}
