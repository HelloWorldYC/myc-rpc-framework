package github.myc.remoting.transport.netty.client;

import github.myc.enums.CompressTypeEnum;
import github.myc.enums.SerializationTypeEnum;
import github.myc.factory.SingletonFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.constants.RpcConstants;
import github.myc.remoting.dto.RpcMessage;
import github.myc.remoting.dto.RpcResponse;

import java.net.InetSocketAddress;

/**
 * 自定义客户端 ChannelHandler，处理服务端返回的数据
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放，SimpleChannelInboundHandler 内部的
 * ChannelRead 方法会替你释放 ByteBuf，避免可能导致的内存泄露问题。
 */
@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {

    private final UnprocessedRequest unprocessedRequest;
    private final NettyRpcClient nettyRpcClient;

    public NettyRpcClientHandler() {
        this.unprocessedRequest = SingletonFactory.getInstance(UnprocessedRequest.class);
        this.nettyRpcClient = SingletonFactory.getInstance(NettyRpcClient.class);
    }

    /**
     * 读取服务器发送的信息
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            log.info("client receive msg: [{}]", msg);
            /* 这里为什么是 instanceof RpcMessage，在前一个处理器已经转换为 RpcMessage 了吗？
               是的，在反序列化后就是返回 RpcMessage。*/
            if (msg instanceof RpcMessage) {
                // 将 msg 强制类型转换为 RpcMessage 类型，以便访问其特定的成员。
                RpcMessage temp = (RpcMessage) msg;
                byte messageType = temp.getMessageType();
                if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                    log.info("heart [{}]", temp.getData());
                } else if (messageType == RpcConstants.RESPONSE_TYPE) {
                    RpcResponse<Object> rpcResponse = (RpcResponse<Object>) temp.getData();
                    unprocessedRequest.complete(rpcResponse);
                }
            }
        } finally {
            // 将对象的引用计数减 1。如果引用计数变为 0，该方法将负责释放对象，并确保资源得到正确地释放。
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 连接空闲状态时处理
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断事件的类型，如果是 IdleStateEvent 事件再进行处理，该事件在连接的读操作或写操作在一定时间内没有活动（即空闲）时触发
        if (evt instanceof IdleStateEvent) {
            // IdleState 枚举定义了三个常量：READER_IDLE、WRITER_IDLE 和 ALL_IDLE，分别对应读空闲、写空闲和读写都空闲的状态。
            IdleState state = ((IdleStateEvent) evt).state();
            // 因为是客户端，所以判断的是写空闲，也就是当没有请求要发送时，发送心跳信息
            if (state == IdleState.WRITER_IDLE) {
                log.info("write idle happen [{}]", ctx.channel().remoteAddress());
                Channel channel = nettyRpcClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.KYRO.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_REQUEST_TYPE);
                rpcMessage.setData(RpcConstants.PING);
                channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 在处理客户端消息异常时调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch github.myc.exception: ", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
