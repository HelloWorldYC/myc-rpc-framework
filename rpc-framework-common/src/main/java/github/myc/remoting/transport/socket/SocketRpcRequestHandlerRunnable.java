package github.myc.remoting.transport.socket;


import github.myc.factory.SingletonFactory;
import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.dto.RpcResponse;
import github.myc.remoting.handler.RpcRequestHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Slf4j
public class SocketRpcRequestHandlerRunnable implements Runnable{

    private final Socket socket;
    private final RpcRequestHandler rpcRequestHandler;

    public SocketRpcRequestHandlerRunnable(Socket socket) {
        this.socket = socket;
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void run() {
        log.info("server handle message from client by thread: [{}]", Thread.currentThread().getName());
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) ois.readObject();
            Object result = rpcRequestHandler.handle(rpcRequest);
            oos.writeObject(RpcResponse.success(result, rpcRequest.getRequestId()));
            oos.flush();
            log.info("request from client has been handled and response has been sent.");
        } catch (IOException | ClassNotFoundException e) {
            log.error("occur github.myc.exception: ", e);
        }
    }
}
