package github.myc;

import github.myc.config.RpcServiceConfig;
import github.myc.helloservice.Hello;
import github.myc.helloservice.HelloService;
import github.myc.proxy.RpcClientProxy;
import github.myc.remoting.transport.RpcRequestTransport;
import github.myc.remoting.transport.socket.SocketRpcClient;

public class SocketClientMain {
    public static void main(String[] args) {
        RpcRequestTransport rpcRequestTransport = new SocketRpcClient();
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcRequestTransport, rpcServiceConfig);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String hello = helloService.hello(new Hello("111", "222"));
        System.out.println(hello);
    }
}
