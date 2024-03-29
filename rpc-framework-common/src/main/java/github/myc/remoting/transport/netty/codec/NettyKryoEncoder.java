package github.myc.remoting.transport.netty.codec;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import github.myc.serialize.Serializer;

/**
 * 编码器，负责处理“出站”消息，将消息格式转换为字节数组然后写入到字节数据的容器中
 * 网络传输需要通过字节流来实现，ByteBuf 可以看作是 Netty 提供的字节数据的容器，使用它会让我们更加方便地处理字节数据
 * MeMessageToByteEncoder 继承了 ChannelOutboundHandlerAdapter，也就是要写出去的
 */
@AllArgsConstructor
public class NettyKryoEncoder extends MessageToByteEncoder<Object> {
    private final Serializer serializer;
    private final Class<?> genericClass;

    /**
     * 将对象转换为字节码然后写入到 ByteBuf 对象中
     * @param channelHandlerContext 连接通道的处理器的上下文环境信息
     * @param o 要进行传输的对象
     * @param byteBuf 字节数据的容器，存储序列化后的对象
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        if(genericClass.isInstance(o)){
            // 1. 将对象转换为 byte，即序列化
            byte[] body = serializer.serialize(o);
            // 2. 读取消息的长度
            int dataLength = body.length;
            // 3. 写入消息对应的字节数组长度，writeIndex 加 4
            byteBuf.writeInt(dataLength);
            // 4. 将字节数组写入 ByteBuf 对象中，所以 byteBuf 中的数据格式：字节数组长度 + 字节数组
            byteBuf.writeBytes(body);
        }
    }
}
