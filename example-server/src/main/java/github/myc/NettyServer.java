package github.myc;

import github.myc.remoting.transport.netty.codec.NettyKryoDecoder;
import github.myc.remoting.transport.netty.codec.NettyKryoEncoder;
import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.dto.RpcResponse;
import github.myc.remoting.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import github.myc.serialize.KryoSerializer;

public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    private final int port;
    private NettyServer(int port){
        this.port = port;
    }

    private void run() {
        // bossGroup 是一个单线程的 EventLoopGroup，负责分发，而 workerGroup 则是相当于一个线程池，由next()选择一个eventLoop进行注册及处理
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGoup = new NioEventLoopGroup();
        KryoSerializer kryoSerializer = new KryoSerializer();
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
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    // 将客户端发过来的请求解码
                    ch.pipeline().addLast(new NettyKryoDecoder(kryoSerializer, RpcRequest.class));
                    // 将请求的处理结果即响应编码
                    ch.pipeline().addLast(new NettyKryoEncoder(kryoSerializer, RpcResponse.class));
                    // 自定义服务端处理器处理请求
                    ch.pipeline().addLast(new NettyServerHandler());
                }
            });

            // 绑定端口，同步等待绑定成功
            ChannelFuture cf = serverBootstrap.bind(port).sync();
            // 等待服务端监听端口关闭
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e){
            logger.error("occur github.myc.exception when start server:", e);
        } finally {
            // 将两个线程池关闭
            bossGroup.shutdownGracefully();
            workerGoup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new NettyServer(8889).run();
    }
}
