package github.myc.remoting.transport.netty.server;

import github.myc.config.CustomShutdownHook;
import github.myc.config.RpcServiceConfig;
import github.myc.factory.SingletonFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import github.myc.provider.ServiceProvider;
import github.myc.provider.impl.ZkServiceProviderImpl;
import github.myc.remoting.transport.netty.codec.RpcMessageDecoder;
import github.myc.remoting.transport.netty.codec.RpcMessageEncoder;
import github.myc.utils.RuntimeUtil;
import github.myc.utils.concurrent.threadpool.ThreadPoolFactoryUtil;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Netty 服务端，接收客户端的信息，根据请求的信息调用相关的方法，并返回结果给客户端
 */
@Slf4j
@Component
public class NettyRpcServer {

    public static final int PORT = 8889;

    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    /**
     * Netty 服务端开启服务
     */
    @SneakyThrows
    public void start() {
        // 清除本台服务器在 zookeeper 上注册的服务
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress();
        // bossGroup 是一个单线程的 EventLoopGroup，负责分发，而 workerGroup 则是相当于一个线程池，由next()选择一个eventLoop进行注册及处理
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGoup = new NioEventLoopGroup();
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGoup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            // TCP 默认开启了 Nagle 算法，该算法的作用就是尽可能地发送大数据块，减少网络传输。TCP_NODELAY 参数的作用就是控制是否开启 Nagle 算法
            serverBootstrap.option(ChannelOption.TCP_NODELAY, true);
            // 是否开启 TCP 底层心跳机制，心跳机制主要用于检测连接是否存活，帮助维持长时间的连接，以及超时处理。
            serverBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            // 在 TCP 服务器端，当有新的客户端连接请求到达时，服务器会将其放入一个等待处理的连接队列中，等待服务器处理。
            // 当服务器的连接队列已满时，新的连接请求将会被拒绝或者丢弃。SO_BACKLOG 参数用来配置服务器的连接队列的最大长度。
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);
            serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
            // 当客户端第一次进行请求的时候才会进行初始化
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    // 30 秒内没有收到客户端请求的话就关闭连接
                    p.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                    p.addLast(new RpcMessageEncoder());
                    p.addLast(new RpcMessageDecoder());
                    // 自定义服务端处理器处理请求，这些处理器用的是 serviceHandlerGroup 线程池
                    p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                }
            });

            // 绑定服务器侦听的端口，同步等待绑定成功
            ChannelFuture cf = serverBootstrap.bind(host, PORT).sync();
            // 等待服务端监听端口关闭，也就是服务端服务提供结束，在这之前由 bossGroup 和 workerGroup 不断接收处理请求
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e){
            log.error("occur github.myc.exception when start server:", e);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            // 将三个线程池关闭
            bossGroup.shutdownGracefully();
            workerGoup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
