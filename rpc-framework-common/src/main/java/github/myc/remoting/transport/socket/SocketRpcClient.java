package github.myc.remoting.transport.socket;

import github.myc.enums.ServiceDiscoveryEnum;
import github.myc.exception.RpcException;
import github.myc.extension.ExtensionLoader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import github.myc.registry.ServiceDiscovery;
import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.transport.RpcRequestTransport;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于 Socket 传输 RpcRequest
 */
@AllArgsConstructor
@Slf4j
public class SocketRpcClient implements RpcRequestTransport {

    private final ServiceDiscovery serviceDiscovery;

    public SocketRpcClient(){
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.ZK.getName());
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // 1.从 zookeeper 查询服务端地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        log.info("服务查询结果：" + inetSocketAddress.toString());
        try(Socket socket = new Socket()){
            // 2.通过查询的地址连接服务端
            socket.connect(inetSocketAddress);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            // 3.通过输出流向服务端发送请求信息
            oos.writeObject(rpcRequest);
            log.info("rpc 请求已发送");
            // 4.通过输入流获取服务端响应的信息
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            log.info("已接收到服务端的响应");
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e){
            throw new RpcException("调用服务失败：", e);
        }
    }


}
