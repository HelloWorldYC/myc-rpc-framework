package github.myc.loadbalance.loadbalancer;

import github.myc.loadbalance.AbstractLoadBalance;
import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希负载均衡策略
 * 参考 Dubbo 的一致性哈希负载均衡策略：https://github.com/apache/dubbo/blob/2d9583adf26a2d8bd6fb646243a9fe80a77e65d5/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance/ConsistentHashLoadBalance.java
 */
@Slf4j
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    /**
     * 对于不同的 service，存储它的对应的 ConsistentHashSelector，作一个缓存，避免重复计算
     */
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceAddress, RpcRequest rpcRequest) {
        // identityHashCode() 是 System 类的静态方法，用于返回对象的标识哈希码。标识哈希码是根据对象的内存地址计算得出的一个整数值，用于唯一标识对象。
        int identityHashCode = System.identityHashCode(serviceAddress);
        // 根据 rpcService 构建 rpcServiceName
        String rpcServiceName = rpcRequest.getRpcServiceName();
        // 获取 service 对应的 ConsistentHashSelector
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        // 先验证 identityHashCode 是不是一致，若不一致，说明服务地址有变动，需重新映射再进行选择
        if(selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddress, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        // 用 rpcServiceName + 参数作为请求映射的 key
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    /**
     * 一致性哈希选择器
     */
    static class ConsistentHashSelector {
        /**
         * 存储 Hash 值与节点映射关系的 TreeMap
         */
        private final TreeMap<Long, String> virtualInvokers;

        /**
         * 用来识别 Invoker列表是否发生变更的Hash码
         */
        private final int identityHashCode;

        /**
         * @param invokers 就是服务地址列表
         * @param replicaNumber 配置的节点数目，也就是服务地址副本数，对于每一个服务地址都有这么多的虚拟节点
         */
        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for(String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i);
                    // 将加密后的消息摘要（hash 值）分为四部分，都对应同一个 invoker，也就是服务地址
                    for (int h = 0; h < 4; h++) {
                        // 计算虚拟节点对应的部分消息摘要
                        long m = hash(digest, h);
                        // 将每个虚拟节点的消息摘要以及对应的服务地址都放进 TreeMap 中
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        /**
         * 用 md5 加密服务地址（IP+端口+编号）
         * @param key  服务地址（IP+端口+编号）
         * @return     加密后的字节数组
         */
        static byte[] md5(String key) {
            // MessageDigest 是 Java 提供的一个加密算法工具类，用于计算哈希值（摘要）的消息摘要算法
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                // 把 key 转为二进制字节数组
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                // 用 md5 算法加密这个字节数组
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return md.digest();
        }

        /**
         * 将服务地址（IP+端口+编号）加密后的消息摘要（哈希值）进一步散列，按顺序分为四部分，分给四个虚拟节点
         * @param digest 服务地址（IP+端口+编号）加密后的消息摘要，消息摘要长 16 字节 128 位
         * @param idx  四个虚拟节点中的第 idx 个
         * @return   散列后的第 idx 个虚拟节点对应的消息摘要（原来的四分之一）
         */
        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24
                    | (long) (digest[2 + idx * 4] & 255) << 16
                    | (long) (digest[1 + idx * 4] & 255) << 8
                    | (long) (digest[idx * 4] & 255))
                    & 4294967295L;
        }

        /**
         * 根据请求选择对应的虚拟节点（服务地址）
         */
        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            // 仅取消息摘要（Hash Code）的 0-31 位来进行选择
            return selectForKey(hash(digest, 0));
        }

        /**
         * 根据请求计算出来的消息摘要（仅取 0-31 位）来选择虚拟节点
         */
        public String selectForKey(long hashCode) {
            // tailMap(hashCode, true) 返回 virtualInvokers 中的一个子映射，其键大于等于 hashCode 的部分。
            // firstEntry() 方法返回子映射的第一个键值对，即最小的键值对。
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();
            // 若没有对应的键值对，说明在 hashCode 在环形队列哈希映射模型的 2^32 - 1 到 0 之间的区域，直接返回第一个虚拟节点
            if(entry == null) {
                entry = virtualInvokers.firstEntry();
            }

            return entry.getValue();
        }
    }
}
