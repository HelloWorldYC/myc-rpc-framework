package github.myc.serialize;


import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import github.myc.exception.SerializeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Hessian 是一种动态类型、二进制序列化和 Web 服务协议，专为面向对象的传输而设计。
 * 它通常用于构建基于 RPC 的分布式系统，用于实现不同服务之间的远程调用和数据传输。
 */
public class HessianSerializer implements Serializer{

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(obj);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new SerializeException("Serialization failed");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            HessianInput hessianInput = new HessianInput(byteArrayInputStream);
            Object obj = hessianInput.readObject();
            return clazz.cast(obj);
        } catch (Exception e) {
            throw new SerializeException("Deserialization failed");
        }
    }
}
