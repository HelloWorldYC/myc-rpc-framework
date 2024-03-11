package github.myc.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取单例对象的工厂类
 */
public final class SingletonFactory {
    /**
     * 保存单例对象
     * key：单例对象对应的 Class 对象
     * value：该单例对象
     */
    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    private SingletonFactory(){

    }

    /**
     * 获取单例对象
     * 如果 OBJECT_MAP 中有该单例对象则返回该单例对象，若没有则根据 Class 对象创建对应的单例对象并存取 OBJECT_MAP
     * @param c     所需单例对象的 Class 对象
     * @return      单例对象
     */
    public static <T> T getInstance(Class<T> c){
        if(c == null) {
            throw new IllegalArgumentException();
        }
        String key = c.toString();
        if(OBJECT_MAP.containsKey(key)){
            return c.cast(OBJECT_MAP.get(key));
        } else {
            return c.cast(OBJECT_MAP.computeIfAbsent(key, k -> {
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException |NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }
}
