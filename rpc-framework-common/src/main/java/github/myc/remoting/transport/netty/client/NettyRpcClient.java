package github.myc.remoting.transport.netty.client;

import github.myc.enums.CompressTypeEnum;
import github.myc.enums.SerializationTypeEnum;
import github.myc.enums.ServiceDiscoveryEnum;
import github.myc.extension.ExtensionLoader;
import github.myc.factory.SingletonFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import github.myc.registry.ServiceDiscovery;
import github.myc.remoting.constants.RpcConstants;
import github.myc.remoting.dto.RpcMessage;
import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.dto.RpcResponse;
import github.myc.remoting.transport.RpcRequestTransport;
import github.myc.remoting.transport.netty.codec.RpcMessageDecoder;
import github.myc.remoting.transport.netty.codec.RpcMessageEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 初始化以及关闭 Bootstrap 对象
 */
@Slf4j
public class NettyRpcClient implements RpcRequestTransport {

    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequest unprocessedRequest;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    public NettyRpcClient() {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        // 一组 EventLoop 的容器，管理 EventLoop，也可以看作是一个线程池，将 channel 分配到 EventLoop 上
        bootstrap.group(eventLoopGroup);
        // 指定连接的超时时间，超过这个时间还是建立不上的话则表示连接失败
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        bootstrap.handler(new LoggingHandler(LogLevel.INFO));
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                // 如果15秒内没有发送数据给服务器，则发送心跳请求，三个参数分别对应读空闲，写空闲，读写都空闲
                p.addLast(new IdleStateHandler(0, 60, 0, TimeUnit.SECONDS));
                p.addLast(new RpcMessageEncoder());
                p.addLast(new RpcMessageDecoder());
                p.addLast(new NettyRpcClientHandler());
            }
        });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.ZK.getName());
        this.unprocessedRequest = SingletonFactory.getInstance(UnprocessedRequest.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * 连接服务器并且获取 channel，通过 channel 可以发送 rpc 请求到服务端
     * @param inetSocketAddress
     * @return
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        // connect 方法返回的是一个 channelFuture 对象，代表的是连接操作的结果，添加监听器监听操作结果
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener)future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successfully!", inetSocketAddress.toString());
                // 这一步不要忘了，否则发送不出去请求！！！将连接的结果 future.channel 作为 completableFuture 的结果返回。
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        log.info("执行到了这一步");
        return completableFuture.get();
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // 构造返回值
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // 从 zookeeper 查询服务端地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // 获取与服务端连接的 channel
        Channel channel = getChannel(inetSocketAddress);
        log.info("The state of channel is : [{}]", channel.isActive());
        if(channel != null && channel.isActive()) {
            // 将请求放入 unprocessedRequest 中
            unprocessedRequest.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                    .codec(SerializationTypeEnum.KYRO.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE).build();
            // 将 rpcMessage 写出去并监听写出是否成功
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    // 写出失败则要关闭通道并清除掉 unprocessedRequest 中的对应请求
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed: ", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if(channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        log.info("channel is established.");
        return channel;
    }

    public void close() {
        // 优雅地关闭线程池，释放线程池拥有的资源
        eventLoopGroup.shutdownGracefully();
    }
}
