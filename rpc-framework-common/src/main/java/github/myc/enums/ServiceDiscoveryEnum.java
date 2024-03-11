package github.myc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ServiceDiscoveryEnum {
    ZK("zk");

    private final String name;
}
