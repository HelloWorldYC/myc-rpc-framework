package github.myc;

import github.myc.annotation.RpcScan;
import github.myc.config.RpcServiceConfig;
import github.myc.helloservice.HelloService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import github.myc.remoting.transport.netty.server.NettyRpcServer;
import github.myc.serviceimpl.HelloServiceImpl;

/**
 * 通过 @RpcScan 加载包下的 Bean，再通过 AnnotationConfigApplicationContext 获取对应的 Bean：NettyRpcServer
 */
@RpcScan(basePackage = {"github.myc"})
public class NettyServerMain {
    public static void main(String[] args) {
        // 通过注释注册服务
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        HelloService helloService = new HelloServiceImpl();
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group("test2").version("version2").service(helloService).build();
        nettyRpcServer.registerService(rpcServiceConfig);
        nettyRpcServer.start();
    }
}
