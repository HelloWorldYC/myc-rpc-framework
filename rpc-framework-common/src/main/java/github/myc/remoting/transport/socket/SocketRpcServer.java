package github.myc.remoting.transport.socket;

import github.myc.config.CustomShutdownHook;
import github.myc.config.RpcServiceConfig;
import github.myc.factory.SingletonFactory;
import lombok.extern.slf4j.Slf4j;
import github.myc.provider.ServiceProvider;
import github.myc.provider.impl.ZkServiceProviderImpl;
import github.myc.remoting.transport.netty.server.NettyRpcServer;
import github.myc.utils.concurrent.threadpool.ThreadPoolFactoryUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * 基于 Socket 传输的服务端
 */
@Slf4j
public class SocketRpcServer {

    private final ExecutorService threadPool;
    private final ServiceProvider serviceProvider;

    public SocketRpcServer() {
        threadPool = ThreadPoolFactoryUtil.createCustomThreadPoolIfAbsent("socket-server-rpc-pool");
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    public void registerService(RpcServiceConfig rpcServiceConfig){
        serviceProvider.publishService(rpcServiceConfig);
    }

    /**
     * 本台服务器开启服务
     */
    public void start() {
        try (ServerSocket server = new ServerSocket()) {
            String host = InetAddress.getLocalHost().getHostAddress();
            // bind() 方法将服务器绑定到指定的主机和端口，用于标识服务器在哪个网络接口上进行监听，以侦听传入的连接请求，
            server.bind(new InetSocketAddress(host, NettyRpcServer.PORT));
            CustomShutdownHook.getCustomShutdownHook().clearAll();
            Socket socket;
            while((socket = server.accept()) != null){
                log.info("client connected [{}]", socket.getInetAddress());
                threadPool.execute(new SocketRpcRequestHandlerRunnable(socket));
            }
            threadPool.shutdown();
        } catch (IOException e) {
            log.error("occur IOException: ", e);
        }
    }
}
