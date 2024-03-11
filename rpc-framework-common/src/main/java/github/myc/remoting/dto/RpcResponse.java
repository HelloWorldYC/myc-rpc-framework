package github.myc.remoting.dto;

import github.myc.enums.RpcResponseCodeEnum;
import lombok.*;

import java.io.Serializable;

/**
 * 响应实体类
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcResponse<T> implements Serializable {
    private static final long serialVersionUID = 715745410605631233L;
    private String requestId;   // 对应哪一个请求
    private Integer code;       // 响应码
    private String message;     // 响应消息
    private T data;             // 响应体

    // 如果请求处理成功就调用这个方法
    public static <T> RpcResponse<T> success(T data, String requestId){
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(RpcResponseCodeEnum.SUCCESS.getCode());
        response.setMessage(RpcResponseCodeEnum.SUCCESS.getMessage());
        response.setRequestId(requestId);
        if(data != null){
            response.setData(data);
        }
        return response;
    }

    // 如果请求处理失败就调用这个方法
    public static <T> RpcResponse<T> fail(RpcResponseCodeEnum rpcResponseCodeEnum){
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(rpcResponseCodeEnum.getCode());
        response.setMessage(rpcResponseCodeEnum.getMessage());
        return response;
    }

}
