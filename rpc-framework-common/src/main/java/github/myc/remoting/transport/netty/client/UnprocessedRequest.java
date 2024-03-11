package github.myc.remoting.transport.netty.client;


import github.myc.remoting.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存 客户端发送出去、但服务端还未返回处理结果 的请求
 */
public class UnprocessedRequest {
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        // CompletableFuture 的 complete() 方法用于完成一个 CompletableFuture 实例，并设置其结果值。
        // complete(T value): 将给定的值作为结果设置到 CompletableFuture 中，并将其标记为已完成。
        if (null != future) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
