package github.myc;

import github.myc.config.RpcServiceConfig;
import github.myc.helloservice.HelloService;
import github.myc.remoting.transport.socket.SocketRpcServer;
import github.myc.serviceimpl.HelloServiceImpl;

public class SocketServerMain {
    public static void main(String[] args) {
        // helloService 就是服务对象
        HelloService helloService = new HelloServiceImpl();
        SocketRpcServer socketRpcServer = new SocketRpcServer();
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        rpcServiceConfig.setService(helloService);
        socketRpcServer.registerService(rpcServiceConfig);
        socketRpcServer.start();
    }
}
