package github.myc.remoting.transport.netty.codec;


import github.myc.compress.Compress;
import github.myc.enums.CompressTypeEnum;
import github.myc.enums.SerializationTypeEnum;
import github.myc.extension.ExtensionLoader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.constants.RpcConstants;
import github.myc.remoting.dto.RpcMessage;
import github.myc.serialize.Serializer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <pre>  自定义的协议
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+--------+-----+-----+---+---+
 *   |   magic   code        |version | full length         |messageType| codec |github.myc.compress|    RequestId      |
 *   +-----------------------+--------+---------------------+-----------+-------+--------+-----+-----+---+---+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4 Byte  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B codec（序列化类型）    1B github.myc.compress（压缩类型）   4B  requestId（请求的Id）
 * body（object类型数据）
 * 注：magic code 是为了筛选数据包的，用来匹配识别数据包是否是遵循上述自定义协议的
 * </pre>
 *
 * <p>
 * 编码器，负责处理“出站”消息，将消息格式转换为字节数组然后写入到字节数据的容器中
 * 网络传输需要通过字节流来实现，ByteBuf 可以看作是 Netty 提供的字节数据的容器，使用它会让我们更加方便地处理字节数据
 * MessageToByteEncoder 继承了 ChannelOutboundHandlerAdapter，也就是要写出去的
 * <p/>
 */
@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) throws Exception {
        try {
            out.writeBytes(RpcConstants.MAGIC_NUMBER);
            out.writeByte(RpcConstants.VERSION);
            // 预留出消息长度的 4 byte，等计算完全部再设置
            out.writerIndex(out.writerIndex() + 4);
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            out.writeByte(rpcMessage.getCodec());
            out.writeByte(rpcMessage.getCompress());
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());
            // 构建 full length
             byte[] bodyBytes = null;
             int fullLength = RpcConstants.HEAD_LENGTH;
             // 如果消息类型不是心跳信息，full length = head length + body length.
            if(messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // 序列化对象
                String codeName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("codec name : [{}]", codeName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codeName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                // 压缩字节数组
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                fullLength += bodyBytes.length;
            }

            if(bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }
            // 记录写完的位置
            int writeIndex = out.writerIndex();
            // 将写指针回退到 full length 区
            out.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            out.writeInt(fullLength);
            // 将写指针重新跳到写完的位置
            out.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }
}
