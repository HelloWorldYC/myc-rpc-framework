package github.myc;

import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.transport.netty.codec.NettyKryoDecoder;
import github.myc.remoting.transport.netty.codec.NettyKryoEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.dto.RpcResponse;
import github.myc.remoting.handler.NettyClientHandler;
import github.myc.serialize.KryoSerializer;

@Slf4j
public class NettyClient {

    private final String host;
    private final int port;
    private static final Bootstrap bootstrap;

    public NettyClient(String host, int port){
        this.host = host;
        this.port = port;
    }

    static {
        bootstrap = new Bootstrap();
        KryoSerializer kryoSerializer = new KryoSerializer();
        bootstrap.channel(NioSocketChannel.class);
        // 一组 EventLoop 的容器，管理 EventLoop，也可以看作是一个线程池，将 channel 分配到 EventLoop 上
        bootstrap.group(new NioEventLoopGroup());
        // 指定连接的超时时间，超过这个时间还是建立不上的话则表示连接失败
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LoggingHandler());
                // 自定义序列化解码器，ByteBuf -> RpcResponse，只有在 ChannelInbound 类型即入站时执行
                ch.pipeline().addLast(new NettyKryoDecoder(kryoSerializer, RpcResponse.class));
                // 自定义序列化编码器，RpcRequest -> ByteBuf，只有在 ChannelOutbound 类型即出站时执行
                ch.pipeline().addLast(new NettyKryoEncoder(kryoSerializer, RpcRequest.class));
                // 自定义客户端处理器，只有在 ChannelInbound 类型即入站时执行
                ch.pipeline().addLast(new NettyClientHandler());
            }
        });
    }

    /**
     * 发送消息到服务端
     * @param rpcRequest 消息体
     * @return 服务端的响应数据
     */
    public RpcResponse sendMessage(RpcRequest rpcRequest){
        try{
            // 这里的 ChannelFuture 对象代表着连接的建立和异步操作的结果
            // 这里用 sync() 表示同步的意思，也就是要阻塞，等待连接成功
            ChannelFuture cf = bootstrap.connect(host, port).sync();
            log.info("client connect {}", host + ":" + port);
            // 获取当前 future 的 channel
            Channel futureChannel = cf.channel();
            log.info("send message");
            if(futureChannel != null) {
                // 将 rpcRequest 通过通道写出去，经过 Handler 处理后进行网络传输
                // 这里通过 writeAndFlush 方法返回的 ChannelFuture 是一个新的对象，代表着数据写入操作的结果，跟上面的 ChannelFuture 对象不同。
                ChannelFuture cf2 = futureChannel.writeAndFlush(rpcRequest);
                cf2.addListener(future -> {
                    // 监听的回调函数，写出操作结束后执行该方法
                    if(future.isSuccess()) {
                        log.info("client send message: [{}]", rpcRequest.toString());
                    } else {
                        log.error("Send failed:", future.cause());
                    }
                });
                // 阻塞等待，直到 channel 关闭，注意这里只是监听通道的关闭，futureChannel 对象不会销毁
                // closeFuture()方法返回一个 future 对象，用于监听通道的关闭
                futureChannel.closeFuture().sync();
                // 将服务端的响应数据即 RpcResponse 对象取出
                // Attribute 是一种用于在 Channel 中存储自定义数据的机制。通过 AttributeKey 可以唯一标识和获取 Channel 中的 Attribute，存储有点像 Map。
                // <RpcResponse> 表示 AttributeKey 存储的 key 是这个类型，名称是 valueOf 中的字符串。
                // 这里是获取 key 的名称，而不是设置 key 的值，key 的值在服务端响应后会在 NettyClientHandler 中设置
                AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse");
                return futureChannel.attr(key).get();

            }
        } catch (Exception e){
            log.error("occur github.myc.exception when connect server:", e);
        }
        return null;
    }

    public static void main(String[] args) {
        // 构造一个客户端请求
        RpcRequest rpcRequest = RpcRequest.builder().interfaceName("interface").methodName("hello").build();
        NettyClient nettyClient = new NettyClient("127.0.0.1", 8889);
        for(int i = 0; i < 3; i++){
            nettyClient.sendMessage(rpcRequest);
        }
        RpcResponse rpcResponse = nettyClient.sendMessage(rpcRequest);
        System.out.println(rpcResponse.toString());
    }
}
