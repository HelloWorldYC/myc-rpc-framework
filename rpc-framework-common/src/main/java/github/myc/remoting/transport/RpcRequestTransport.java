package github.myc.remoting.transport;

import github.myc.annotation.SPI;
import github.myc.remoting.dto.RpcRequest;

/**
 * 传输请求的接口
 */
@SPI
public interface RpcRequestTransport {
    /**
     * 发送 Rpc 请求给服务端并获取结果
     * @param request 发送的请求
     * @return        响应数据
     */
    Object sendRpcRequest(RpcRequest request);
}
