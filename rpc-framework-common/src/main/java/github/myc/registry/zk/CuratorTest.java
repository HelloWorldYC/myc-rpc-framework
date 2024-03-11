package github.myc.registry.zk;


import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;


public class CuratorTest {
    // 连接之间的睡眠时间
    private static final int BASE_SLEEP_TIME = 1000;
    // 最大重试次数
    private static final int MAX_RETRIES = 3;

    // 测试连接
    public static void testConnect(){
        // 重试策略：重试 3 次，每次重试之间的睡眠时间递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 要连接的服务器，也可以是服务器列表
        CuratorFramework zkClient = CuratorFrameworkFactory.newClient("127.0.0.1:2281", retryPolicy);
        zkClient.start();

        try {
            // 检查是否与 ZooKeeper 建立了连接
            // STARTED 表示 CuratorFramework 已经启动并且与 ZooKeeper 服务器建立了连接。
            if (zkClient.getState().equals(CuratorFrameworkState.STARTED)) {
                System.out.println("连接成功!");
            } else {
                System.out.println("连接失败");
            }
        } finally {
            // 关闭 CuratorFramework
            zkClient.close();
        }
    }

    // 测试创建节点
    public static void testCreate() throws Exception {
        // 重试策略：重试 3 次，每次重试之间的睡眠时间递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 要连接的服务器，也可以是服务器列表
        CuratorFramework zkClient = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2281").retryPolicy(retryPolicy).build();
        // 创建节点之前需要先连接
        zkClient.start();
        System.out.println("连接创建：" + zkClient);

        // 操作结束之后关闭节点
        if(zkClient != null){
            // 创建节点
            String path = zkClient.create().forPath("/zhangsan","/zhangsan".getBytes());
            System.out.println("节点创建成功：" + path);
            zkClient.close();
        }
        System.out.println("节点已关闭");
    }

    // 测试获取节点数据
    public static void testGet() throws Exception {
        // 重试策略：重试 3 次，每次重试之间的睡眠时间递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 要连接的服务器，也可以是服务器列表
        CuratorFramework zkClient = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2281").retryPolicy(retryPolicy).build();
        // 创建节点之前需要先连接
        zkClient.start();

        byte[] bytes = zkClient.getData().forPath("/zhangsan");
        System.out.println(new String(bytes));
        zkClient.close();
        /*try {
            // 检查是否与 ZooKeeper 建立了连接
            boolean isConnected = zkClient.getZookeeperClient().isConnected();
            if (isConnected) {
                byte[] bytes = zkClient.getData().forPath("/zhangsan");
                System.out.println(new String(bytes));
            } else {
                System.out.println("获取节点数据失败");
            }
        } finally {
            // 关闭 CuratorFramework
            zkClient.close();
        }*/
    }

    // 测试修改节点
    public static void testSet() throws Exception {
        // 重试策略：重试 3 次，每次重试之间的睡眠时间递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 要连接的服务器，也可以是服务器列表
        CuratorFramework zkClient = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2281").retryPolicy(retryPolicy).build();
        // 创建节点之前需要先连接
        zkClient.start();
        zkClient.setData().forPath("/zhangsan", "zhangsanjinhua".getBytes());
        zkClient.close();
    }

    // 测试删除节点
    public static void testDelete() throws Exception {
        // 重试策略：重试 3 次，每次重试之间的睡眠时间递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 要连接的服务器，也可以是服务器列表
        CuratorFramework zkClient = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2281").retryPolicy(retryPolicy).build();
        // 创建节点之前需要先连接
        zkClient.start();
        zkClient.delete().forPath("/zhangsan");
        zkClient.close();
    }

    // 测试监听单一节点
    public static void testNodeListener() throws Exception {
        // 重试策略：重试 3 次，每次重试之间的睡眠时间递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 要连接的服务器，也可以是服务器列表
        CuratorFramework zkClient = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2281").retryPolicy(retryPolicy).build();
        // 创建节点之前需要先连接
        zkClient.start();

        // 1.创建 NodeCache 对象
        NodeCache nodeCache = new NodeCache(zkClient, "/zhangsan");
        // 2.注册监听
        nodeCache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                System.out.println("节点变化了");
                // 获取修改后节点的数据
                byte[] bytes = nodeCache.getCurrentData().getData();
                System.out.println(new String(bytes));
            }
        });
        // 3.开启监听，如果设置为 true，则开启监听，加载缓冲数据
        nodeCache.start();
        while(true){

        }
    }

    public static void main(String[] args) throws Exception {
        testConnect();
        // testCreate();
        testGet();
        // testSet();
        // testDelete();
        // testNodeListener();
    }
}
