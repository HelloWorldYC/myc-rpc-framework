package github.myc.remoting.transport.netty.server;

import github.myc.enums.CompressTypeEnum;
import github.myc.enums.RpcResponseCodeEnum;
import github.myc.enums.SerializationTypeEnum;
import github.myc.factory.SingletonFactory;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.constants.RpcConstants;
import github.myc.remoting.dto.RpcMessage;
import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.dto.RpcResponse;
import github.myc.remoting.handler.RpcRequestHandler;

/**
 * 自定义服务端的 channelHandler 来处理客户端的请求
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，SimpleChannelInboundHandler 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof RpcMessage) {
                log.info("server receive msg: [{}]", msg);
                byte messageType = ((RpcMessage) msg).getMessageType();
                // 构造响应的数据
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.KYRO.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    // 执行请求的目标方法并将方法执行结果返回给客户端
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("server get result； %s", result.toString()));
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        rpcMessage.setData(rpcResponse);
                    } else {
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(rpcResponse);
                        log.error("not writable now, message dropped");
                    }
                }
                // 当写出 rpcMessage 操作失败时，会触发监听器回调方法，关闭关联的 channel
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            // 将对象的引用计数减 1。如果引用计数变为 0，该方法将负责释放对象，并确保资源得到正确地释放。
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断事件的类型，如果是 IdleStateEvent 事件再进行处理，该事件在连接的读操作或写操作在一定时间内没有活动（即空闲）时触发
        if (evt instanceof IdleStateEvent) {
            // IdleState 枚举定义了三个常量：READER_IDLE、WRITER_IDLE 和 ALL_IDLE，分别对应读空闲、写空闲和读写都空闲的状态。
            IdleState state = ((IdleStateEvent) evt).state();
            // 服务端要判断的是读空闲，也就是没有请求过来时，关闭通道
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server catch github.myc.exception");
        cause.printStackTrace();
        ctx.close();
    }
}
