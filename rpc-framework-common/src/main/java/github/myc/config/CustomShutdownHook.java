package github.myc.config;

import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.transport.netty.server.NettyRpcServer;
import github.myc.utils.CuratorUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * 自定义关闭钩子
 * 当服务器关闭的时候，执行一些操作，例如注销所有服务
 */
@Slf4j
public class CustomShutdownHook {

    /**
     * 通过使用私有静态常量 CUSTOM_SHUTDOWN_HOOK，可以确保在整个程序的生命周期内只有一个 CustomShutdownHook 实例，
     * 以便在多个地方共享同一个实例。
     */
    private static final CustomShutdownHook CUSTOM_SHUTDOWN_HOOK = new CustomShutdownHook();

    /**
     * 私有构造函数，确保只能在类内部实例化
     */
    private CustomShutdownHook() {
    }

    public static CustomShutdownHook getCustomShutdownHook() {
        return CUSTOM_SHUTDOWN_HOOK;
    }

    /**
     * 清除本台服务器在 zookeeper 上注册的服务
     */
    public void clearAll() {
        log.info("addShutdownHook for clearAll");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), NettyRpcServer.PORT);
                CuratorUtils.clearRegistry(CuratorUtils.getZkClient(), inetSocketAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }));
    }

}
