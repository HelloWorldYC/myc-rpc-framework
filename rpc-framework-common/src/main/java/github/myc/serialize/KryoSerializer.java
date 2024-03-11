package github.myc.serialize;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import github.myc.remoting.dto.RpcRequest;
import github.myc.remoting.dto.RpcResponse;
import github.myc.exception.SerializeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo 序列化类，Kryo 序列化效率很高，但只兼容 Java 语言。
 */
@Slf4j
public class KryoSerializer implements Serializer {
    /**
     * 由于 kryo 不是线程安全的，每个线程都应该有自己的 kryo，Input 和 Output 实例。
     * 所以，使用 ThreadLocal 存放 kryo 对象
     */
    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 类的注册顺序会影响到反序列化时类的选择，后注册的会先判断。
        kryo.register(RpcResponse.class);
        kryo.register(RpcRequest.class);
        // 是否启用对象引用跟踪，默认情况下关闭，但启用了可以解决循环引用的问题
        kryo.setReferences(true);
        // 用于指定是否要求显式注册所有需要序列化和反序列化的类，默认是true，当设置为 false 时，Kryo 可以在遇到未显式注册的类时进行自动注册。
        kryo.setRegistrationRequired(false);
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream)){
            Kryo kryo = kryoThreadLocal.get();
            // 将对象序列化为 byte 数组，保存到字节数组输出流中，Object -> byte[]
            kryo.writeObject(output, obj);
            kryoThreadLocal.remove();
            return output.toBytes();
        } catch (Exception e){
            throw new SerializeException("Serialization failed");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes); // 将 byte[] 保存到字节数组输入流中
            Input input = new Input(byteArrayInputStream)){
            Kryo kryo = kryoThreadLocal.get();
            // 将字节数组输入流中的字节数组反序列化为对象，byte[] -> Object
            Object obj = kryo.readObject(input, clazz);
            kryoThreadLocal.remove();
            return clazz.cast(obj);
        } catch (Exception e){
            throw new SerializeException("Deserialization failed");
        }
    }
}
