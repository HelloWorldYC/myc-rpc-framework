package github.myc.remoting.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import github.myc.serialize.Serializer;

import java.util.List;

/**
 * 自定义解码器，负责处理“入站”消息，它会从 ByteBuf 中读取到业务对象对应的字节序列，然后再将字节序列反序列化为对象
 * ByteToMessageDecoder 继承了 ChannelInboundHandlerAdapter，也就是处理接收到的。
 */
@AllArgsConstructor
@Slf4j
public class NettyKryoDecoder extends ByteToMessageDecoder {
    private final Serializer serializer;
    private final Class<?> genericClass;
    // Netty 传输的消息长度也就是对象序列化后对应的字节数组的大小，存储在 ByteBuf 头部
    private static final int BODY_LENGTH = 4;

    /**
     * 解码 ByteBuf 对象
     * @param ctx 解码器关联的 channelHandlerContext 对象
     * @param in   “入站”数据，也就是 ByteBuf 对象
     * @param out  解码之后的数据对象
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 1. byteBuf 中写入的消息长度所占的字节数已经是 4 了，所以 byteBuf 的可读字节必须大于 4
        if(in.readableBytes() >= BODY_LENGTH){
            // 2. 标记当前 readIndex 的位置，以便后面重置 readIndex 的时候使用
            in.markReaderIndex();
            // 3. 读取消息的长度，该消息长度是 encoder 的时候我们自己写入的
            int dataLength = in.readInt();
            // 4. 遇到不合理的情况直接 return
            // 这里的 readIndex 已经往后移了，所以此时的 readableBytes() 跟 1. 中判断时的不一样了
            if(dataLength < 0 || in.readableBytes() < 0){
                log.error("data length or byteBuf readableBytes is not valid");
                return;
            }
            // 5. 如果可读字节数小于消息长度的话，说明是不完整的消息，重置 readIndex
            if(in.readableBytes() < dataLength){
                in.resetReaderIndex();
                return;
            }
            // 6. 走到这里说明没什么问题，可以读取字节数组了
            byte[] body = new byte[dataLength];
            in.readBytes(body);
            // 7. 将 bytes 数组反序列化为对象
            Object obj = serializer.deserialize(body, genericClass);
            out.add(obj);
            log.info("successfully decode ByteBuf to Object");
        }
    }
}
