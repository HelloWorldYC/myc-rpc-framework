package github.myc.remoting.handler;

import github.myc.exception.RpcException;
import github.myc.factory.SingletonFactory;
import lombok.extern.slf4j.Slf4j;
import github.myc.provider.ServiceProvider;
import github.myc.provider.impl.ZkServiceProviderImpl;
import github.myc.remoting.dto.RpcRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * rpc 请求处理器
 */
@Slf4j
public class RpcRequestHandler {

    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    /**
     * 处理 rpc 请求：调用对应的方法并返回执行结果
     * @param rpcRequest rpc 请求
     * @return 方法执行结果
     */
    public Object handle(RpcRequest rpcRequest) {
        // 从服务端本地获得服务对象，以用来操作方法
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * 获取方法执行结果
     * @param rpcRequest    客户端请求
     * @param service       保存在服务端本地的服务对象
     * @return              目标方法的执行结果
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            // getClass() 方法返回的是对象的实际运行时类对象，而不是编译时的类对象。这意味着，在多态的情况下，如果对象是一个子类的实例，那么 getClass() 方法将返回子类的 Class 对象。
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            // service 是要调用方法的服务对象或实例
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service [{}] successfully invoke method: [{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }

}
