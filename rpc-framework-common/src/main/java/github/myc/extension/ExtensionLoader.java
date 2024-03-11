package github.myc.extension;


import github.myc.annotation.SPI;
import lombok.extern.slf4j.Slf4j;
import github.myc.utils.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加载和管理扩展点
 * 参考 Dubbo SPI：https://dubbo.apache.org/zh-cn/docs/source_code_guide/dubbo-spi.html
 */
@Slf4j
public final class ExtensionLoader<T> {

    // 扩展点配置文件所在目录
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    // 缓存不同接口的扩展点加载类 -> <不同接口的 Class 对象，接口对应的扩展点加载类实例>
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    // 缓存已创建的扩展点实例 -> <扩展点接口的 Class 对象，接口对应的实例对象>
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    // 扩展点接口的 Class 对象
    private final Class<?> type;
    // 缓存已创建的扩展点实例 -> <扩展点实现类的名称，对应的实例对象的 Holder 对象>
    // 跟 EXTENSION_INSTANCES 不同的是获取实例对象的方式不一样，一个通过接口类型，一个通过实现类名
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    // 缓存扩展点实现类 -> <扩展点实现类名称，对应的 Class 对象>
    // 对于不同的接口，就有不同的 cachedClass，存储对应扩展点实现类
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();


    private ExtensionLoader(Class<?> type){
        this.type = type;
    }

    // 获取扩展点加载类实例
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type){
        // 进行输入参数的判断
        if(type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        if(!type.isInterface()) {
            throw new IllegalArgumentException("Extension type should be an interface.");
        }
        if(type.getAnnotation(SPI.class) == null){
            throw new IllegalArgumentException("Extension type must be annotated by @SPI.");
        }

        // 先从缓存中获取，如果没有再创建
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if(extensionLoader == null){
            // 创建一个该接口对应的扩展点加载类，并添加入缓存中
            // putIfAbsent()方法如果不存在时添加进入，返回 null；若已存在，则返回已存在的值
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            // 再从缓存中获取该扩展点加载类
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    // 通过扩展点实现类的名称获取对应的扩展点实例
    public T getExtension(String name){
        if(StringUtil.isBlank(name)){
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }
        // 首先从缓存中获取扩展点实现类对应的 Holder 对象，如果没有，再创建一个
        Holder<Object> holder = cachedInstances.get(name);
        if(holder == null){
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        // 再从 Holder 对象中获取实例，如果没有实例，则创建一个
        Object instance = holder.get();
        if(instance == null) {
            // 由于 Holder 中变量是 volatile 修饰的，所以需要搭配 Synchronized 加锁保证实例是单例的
            synchronized (holder) {
                // 再获取一边是为了避免在加锁之前实例已经被创建的情况
                instance = holder.get();
                if(instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    // 创建扩展点实现类的实例
    public T createExtension(String name){
        // 从文件中加载接口 T 的所有扩展实现类，并按名称获取特定的扩展实现类
        Class<?> clazz = getExtensionClasses().get(name);
        if(clazz == null){
            throw new RuntimeException("No such github.myc.extension of name " + name);
        }
        // 获取特定扩展点实现类的实例
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if(instance == null){
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.getDeclaredConstructor().newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e){
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    // 获取接口的所有扩展实现类，
    private Map<String, Class<?>> getExtensionClasses(){
        // 从缓存中加载扩展点实现类，如果为空，再创建该接口对应的扩展点实现类
        Map<String, Class<?>> classes = cachedClasses.get();
        if(classes == null) {
            // 由于 Holder 中变量是 volatile 修饰的，所以需要搭配 Synchronized 加锁保证实例是单例的
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if(classes == null) {
                    classes = new HashMap<>();
                    // 从扩展点配置文件加载该接口所有的扩展点实现类
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    // 加载一个接口的所有扩展点实现类
    public void loadDirectory(Map<String, Class<?>> extensionClasses){
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            // Enumeration<> 是一个枚举类，URL 是枚举的类型
            Enumeration<URL> urls;
            // 获取类加载器
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            // 加载类路径下的资源文件，获取与文件名匹配的所有资源的 URL，定位标识符
            urls = classLoader.getResources(fileName);
            if(urls != null){
                // 对于每一个 url，加载对应的资源，用类加载器加载类
                while(urls.hasMoreElements()){
                    URL resourceUrl = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    // 通过传入的类加载器加载扩展点类
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl){
        // resourceUrl 是文件的 url，即定位标识符，如 META-INF/github.myc.serialize/Serializer
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {
            String line;
            // 读取资源文件的每一行
            while((line = reader.readLine()) != null){
                // 该行中 '#' 后面的为注释，忽略它们
                final int ci = line.indexOf('#');
                if(ci >= 0){
                    line = line.substring(0, ci);
                }
                // 去掉前后空格
                line = line.trim();
                if(line.length() > 0){
                    try {
                        // '=' 左边是 key，代表名称，右边是 value，代表该接口的一个扩展实现类
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();
                        // 由于 SPI 规范要求 key-value 对，所以 key value 不能为空
                        if(name.length() > 0 && clazzName.length() > 0){
                            // 通过传入的类加载器加载这个扩展实现类
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            // 将扩展点实现类的名称和字节码对象存入缓存中
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
