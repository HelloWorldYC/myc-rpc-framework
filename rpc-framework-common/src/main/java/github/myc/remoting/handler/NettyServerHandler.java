package github.myc.remoting.handler;

import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.dto.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
    // AtomicInteger 是原子整数类，提供原子操作，可以在多线程环境下进行线程安全的整数操作，而无需使用显式的锁，特别适用于计数器、序号生成器、并发控制等场景。
    private static final AtomicInteger atomicInteger = new AtomicInteger(1);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            RpcRequest rpcRequest = (RpcRequest) msg;
            logger.info("server receive msg: [{}], times:[{}]", rpcRequest, atomicInteger.getAndIncrement());
            // 构造响应数据
            RpcResponse messageFromServer = RpcResponse.builder().message("message from server").build();
            // cf 代表着数据写出的状态和结果
            ChannelFuture cf = ctx.writeAndFlush(messageFromServer);
            // ChannelFutureListener.CLOSE 是一个常用的 ChannelFutureListener，它在操作完成后会关闭相关的 Channel。
            // 具体地，当操作完成时，ChannelFutureListener.CLOSE 会调用 Channel 的 close 方法来关闭该 Channel。
            cf.addListener(ChannelFutureListener.CLOSE);
            // ctx.channel().close();
        } finally {
            // 将对象的引用计数减 1。如果引用计数变为 0，该方法将负责释放对象，并确保资源得到正确地释放。
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("server catch github.myc.exception:", cause);
        ctx.close();
    }
}
