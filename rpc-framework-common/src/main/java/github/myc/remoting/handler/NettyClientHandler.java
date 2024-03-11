package github.myc.remoting.handler;

import github.myc.remoting.dto.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            RpcResponse rpcResponse = (RpcResponse) msg;
            logger.info("client receive msg: [{}]", rpcResponse.toString());
            // 将响应数据保存在 Attribute 中，首先需声明一个 AttributeKey 对象
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse");
            // 将服务端的返回结果保存到 AttributeMap 上，AttributeMap 可以看作是一个 channel 的共享数据源
            ctx.channel().attr(key).set(rpcResponse);
            // 关闭 channel
            ctx.channel().close();
        } finally {
            // 将对象的引用计数减 1。如果引用计数变为 0，该方法将负责释放对象，并确保资源得到正确地释放。
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("client caught github.myc.exception", cause);
        ctx.close();
    }
}
