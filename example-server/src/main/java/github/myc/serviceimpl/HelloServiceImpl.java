package github.myc.serviceimpl;

import github.myc.annotation.RpcService;
import github.myc.helloservice.Hello;
import github.myc.helloservice.HelloService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RpcService(group = "test1", version = "version1")
public class HelloServiceImpl implements HelloService {

    static {
        System.out.println("HelloServiceImpl 被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl 收到：{}", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl 返回：{}", result);
        return result;
    }
}
