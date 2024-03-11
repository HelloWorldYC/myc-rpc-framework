package github.myc.config;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcServiceConfig {

    // 服务版本，实际是接口的一个实现类的不同版本
    private String version = "";
    // 当接口有多个实现类时，按组区分
    private String group = "";
    // 目标服务对象
    private Object service;

    /**
     * @return rpc 服务的名称
     */
    public String getRpcServiceName(){
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    /**
     * @return service 对象所实现的第一个接口的规范化名称
     */
    public String getServiceName() {
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
