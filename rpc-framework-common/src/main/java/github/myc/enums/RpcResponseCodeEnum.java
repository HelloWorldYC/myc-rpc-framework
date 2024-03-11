package github.myc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public enum RpcResponseCodeEnum {
    SUCCESS(200, "The remote call is successful"),  // 注意这里是用逗号隔开
    FAIL(500, "The remote call is fail");

    // 枚举类常量中的参数，对应上面括号中的数据
    private final int code;
    private final String message;
}
