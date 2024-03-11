package github.myc.remoting.dto;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcMessage implements Serializable {

    // rpc 信息类型
    private byte messageType;
    // 序列化方式
    private byte codec;
    // 压缩方式
    private byte compress;
    // rpc 请求 Id
    private int requestId;
    // 请求的数据
    private Object data;

}
