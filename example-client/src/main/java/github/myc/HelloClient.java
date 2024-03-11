package github.myc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import github.myc.remoting.dto.RpcMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HelloClient {

    private static final Logger logger = LoggerFactory.getLogger(HelloClient.class);

    public Object send(RpcMessage rpcMessage, String host, int port){
        // 1.创建Socket对象并且指定服务器的地址和端口号
        try(Socket socket = new Socket(host, port)){
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            // 2.通过输出流向服务端发送请求信息
            oos.writeObject(rpcMessage);
            logger.info("请求已发送");
            // 3.通过输入流获取服务端响应的信息
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            logger.info("已接收到服务端的响应");
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e){
            logger.error("出现异常：", e);
        }
        return null;
    }

    public static void main(String[] args){
        HelloClient helloClient = new HelloClient();
        RpcMessage rpcMessage = (RpcMessage) helloClient.send(RpcMessage.builder().data(new String("请求数据！")).build(), "127.0.0.1", 6666);
        System.out.println("服务端的响应数据为：" + rpcMessage.getData());
    }
}
