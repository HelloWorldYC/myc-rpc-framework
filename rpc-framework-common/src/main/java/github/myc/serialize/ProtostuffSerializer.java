package github.myc.serialize;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Protostuff 是一个基于 Java 的序列化框架，用于将对象转换为字节流以进行存储或传输，并且可以将字节流反序列化回对象。
 * Protostuff 使用紧凑的二进制格式进行序列化，因此序列化后的数据通常更小，占用更少的存储空间。
 */
public class ProtostuffSerializer implements Serializer{

    /**
     * 避免每次序列化时重新应用缓冲区空间
     */
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

    @Override
    public byte[] serialize(Object obj) {
        Class<?> clazz = obj.getClass();
        // 创建一个 Schema 来描述 obj 类的结构
        Schema schema = RuntimeSchema.getSchema(clazz);
        byte[] bytes;
        try {
            bytes = ProtostuffIOUtil.toByteArray(obj, schema, BUFFER);
        } finally {
            BUFFER.clear();
        }
        return bytes;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }
}
