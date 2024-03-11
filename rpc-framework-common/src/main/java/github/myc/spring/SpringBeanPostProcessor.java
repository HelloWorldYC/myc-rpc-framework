package github.myc.spring;

import github.myc.annotation.RpcReference;
import github.myc.annotation.RpcService;
import github.myc.config.RpcServiceConfig;
import github.myc.enums.RpcRequestTransportEnum;
import github.myc.extension.ExtensionLoader;
import github.myc.factory.SingletonFactory;
import github.myc.provider.impl.ZkServiceProviderImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import github.myc.provider.ServiceProvider;
import github.myc.proxy.RpcClientProxy;
import github.myc.remoting.transport.RpcRequestTransport;

import java.lang.reflect.Field;

/**
 * 在 Bean 初始化前后执行一些处理操作
 * 在创建 Bean 之前调用这个方法，看看类是否被注释了
 */
@Slf4j
@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension(RpcRequestTransportEnum.NETTY.getName());
    }

    /**
     * 服务端上的
     * 在服务的 bean 提供服务之前先将 bean 注册到 zookeeper 上
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if(bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annoted with [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
            // 获取 RpcService 注解
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            // 构造 RpcServiceConfig
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean).build();
            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }

    /**
     * 用在客户端的，将要远程调用方法的对象替换为其代理对象，屏蔽远程调用底层细节（包括网络传输和代理对象的细节）
     * 在 bean 初始化之后自动将该 bean 注入到使用该 bean 的地方，自动依赖注入
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        // 获取 bean 定义的所有字段
        Field[] declaredFields = targetClass.getDeclaredFields();
        // 遍历每个字段，查看是否有 @RpcReference 注解
        for(Field declaredField : declaredFields) {
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                // 若有该注解注释的字段，表明该对象属于要远程方法的对象，将其替换为其代理对象
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .group(rpcReference.group())
                        .version(rpcReference.version()).build();
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
                // 得到代理对象
                Object clientProxy = rpcClientProxy.getProxy(declaredField.getType());
                // 设置字段的可访问性，即使字段是私有的或受限制的，也可以通过设置可访问性来绕过访问限制
                declaredField.setAccessible(true);
                try {
                    // 设置这个 bean 中这个字段的值，从而将原来要调用远程方法的对象替换为其代理对象
                    declaredField.set(bean, clientProxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
