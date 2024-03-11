package github.myc.remoting.transport.netty.codec;

import github.myc.compress.Compress;
import github.myc.enums.CompressTypeEnum;
import github.myc.enums.SerializationTypeEnum;
import github.myc.extension.ExtensionLoader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.constants.RpcConstants;
import github.myc.remoting.dto.RpcMessage;
import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.dto.RpcResponse;
import github.myc.serialize.Serializer;

import java.util.Arrays;

/**
 * <pre>  自定义的协议
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+--------+-----+-----+---+---+
 *   |   magic   code        |version | full length         |messageType| codec |github.myc.compress|RequestId|
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
 * 自定义解码器，负责处理“入站”消息，即处理接收到的数据。
 * LengthFieldBasedFrameDecoder 继承自 ByteToMessageDecoder，ByteToMessageDecoder 继承自 ChannelInboundHandlerAdapter.
 * LengthFieldBasedFrameDecoder 是一个基于长度的解码器，用于解决TCP拆包和粘接问题。
 * 参考链接：https://zhuanlan.zhihu.com/p/95621344
 */
@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    /**
     * 根据自定义的协议，LengthFieldBasedFrameDecoder 的构造函数的各项参数默认设置应该如下：
     * lengthFieldOffset：长度字段对应 full length，那么在数据包中的偏移应该为 magic code + version 的字节数，为 5
     * lengthFieldLength：对应 full length 长度，为 4
     * lengthAdjustment：full length 包括所有的数据，并且前面已经读了 9 个字节，所以剩下的字节数应该为 (full length - 9)，所以设为 -9
     * initialBytesToStrip：我们要检查 magic code 和 version，所以不需要跳过任何字节，设为 0
     */
    public RpcMessageDecoder () {
        this(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }

    /**
     * LengthFieldBasedFrameDecoder 的构造函数参数
     * @param maxFrameLength 数据包的最大长度，超过此长度的数据包将被丢弃。
     * @param lengthFieldOffset 长度字段的偏移量，表示长度字段在数据包中的起始位置，长度字段是跳过指定字节长度的字段。
     * @param lengthFieldLength 长度字段的长度，表示长度字段占用的字节数。
     * @param lengthAdjustment 长度调整值，要添加到长度字段值上的补偿值，用于调整解码时计算的数据包长度。
     * @param initialBytesToStrip 需要跳过的字节数。如果需要接收所有 head+body 的数据，则此值为 0
     *                            如果仅需接收 body 数据，则需要跳过 head 所占字节数
     */
    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                             int lengthAdjustment, int initialBytesToStrip){
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // LengthFieldBasedFrameDecoder 的 decode 方法是用来解出帧，最后得到的是一个 ByteBuf 对象，它包含了解码后的帧数据。
        Object decoded = super.decode(ctx, in);
        if(decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            // 如果该帧的可读数据大于 head 的数据，才是正常的请求，这里是否应该是 HEAD_LENGTH ？
            if(frame.readableBytes() >= RpcConstants.TOTAL_LENGTH) {
                try {
                    return decodeFrame(frame);
                } catch (Exception e) {
                    log.error("Decode frame error!", e);
                    throw e;
                } finally {
                    frame.release();
                }
            }
        }
        return decoded;
    }

    /**
     * 解码帧数据，注意：必须按顺序读 ByteBuf
     * @param in 帧数据
     * @return RpcMessage 对象
     */
    private Object decodeFrame(ByteBuf in) {
        checkMagicCode(in);
        checkVersion(in);
        int fullLength = in.readInt();
        // 构造 rpcMessage 对象
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();
        RpcMessage rpcMessage = RpcMessage.builder()
                                    .messageType(messageType)
                                    .codec(codecType)
                                    .compress(compressType)
                                    .requestId(requestId).build();
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if(messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if(bodyLength > 0) {
            byte[] bytes = new byte[bodyLength];
            // 把数据帧剩下的字节全部读出
            in.readBytes(bytes);
            // 解压缩
            String compressName = CompressTypeEnum.getName(compressType);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            bytes = compress.decompress(bytes);
            // 反序列化
            String codecName = SerializationTypeEnum.getName(codecType);
            log.info("codec name : [{}]", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            if (messageType == RpcConstants.REQUEST_TYPE) {
                RpcRequest tmpValue = serializer.deserialize(bytes, RpcRequest.class);
                rpcMessage.setData(tmpValue);
            } else {
                RpcResponse tmpValue = serializer.deserialize(bytes, RpcResponse.class);
                rpcMessage.setData(tmpValue);
            }
        }
        return rpcMessage;
    }

    /**
     * 检查 magic code 是否匹配
     * @param in 帧数据
     */
    private void checkVersion(ByteBuf in) {
        byte version = in.readByte();
        if(version != RpcConstants.VERSION) {
            throw new RuntimeException("version isn't compatible " + version);
        }
    }

    /**
     * 检查 magic code 是否匹配
     * @param in 帧数据
     */
    private void checkMagicCode(ByteBuf in) {
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[len];
        in.readBytes(tmp);
        for(int i = 0; i < len; i++) {
            if(tmp[i] != RpcConstants.MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException("Unknow magic code: " + Arrays.toString(tmp));
            }
        }
    }
}
