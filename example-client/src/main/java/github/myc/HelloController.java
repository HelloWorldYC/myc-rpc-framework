package github.myc;

import github.myc.annotation.RpcReference;
import github.myc.helloservice.Hello;
import github.myc.helloservice.HelloService;
import org.springframework.stereotype.Component;

@Component
public class HelloController {

    /**
     * 用 @RpcReference 注解自动注入，不用再手动创建目标对象的代理对象
     */
    @RpcReference(version = "version1", group = "test1")
    private HelloService helloService;

    public void test() throws InterruptedException {
        String hello = this.helloService.hello(new Hello("111", "222"));
        //如需使用 assert 断言，需要在 VM options 添加参数：-ea
        assert "Hello description is 222".equals(hello);
        Thread.sleep(8000);
        for(int i = 0; i < 10; i++) {
            System.out.println(helloService.hello(new Hello("111", "222")));
        }
    }
}
