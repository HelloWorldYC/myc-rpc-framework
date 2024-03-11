package github.myc.remoting.dto;

import lombok.*;

import java.io.Serializable;

/**
 * 请求实体类
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;     // version 字段主要是为后续不兼容升级提供可能
    private String group;       // group 字段主要用于处理一个接口有多个实现类的情况

    public String getRpcServiceName(){
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
