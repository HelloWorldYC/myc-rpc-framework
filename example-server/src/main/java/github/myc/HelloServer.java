package github.myc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import github.myc.remoting.dto.RpcMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HelloServer {

    private static final Logger logger = LoggerFactory.getLogger(HelloServer.class);

    public void start(int port){
        // 1.创建 ServerSocket 对象并绑定一个端口，表明服务器在这个端口上进行监听请求
        try(ServerSocket server = new ServerSocket(port)){
            Socket socket = null;
            logger.info("服务器即将启动，等待客户的连接...");
            // 2.监听客户端的请求，等待客户端连接
            socket = server.accept();
            try(ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());){
                // 3.通过输入流读取客户端发送的请求信息
                RpcMessage rpcMessage = (RpcMessage) ois.readObject();
                System.out.println("服务端接收到的数据为：" + rpcMessage.getData());
                // 4.通过输出流向客户端发送响应信息
                rpcMessage.setData(new String("请求已处理！"));
                oos.writeObject(rpcMessage);
                oos.flush();
                logger.info("响应数据已返回给客户端");
            } catch (IOException | ClassNotFoundException e){
                logger.error("出现异常：", e);
            }
        } catch (IOException e) {
            logger.error("出现IO异常", e);
        }
    }

    public static void main(String[] args){
        HelloServer helloServer = new HelloServer();
        helloServer.start(6666);
    }
}
