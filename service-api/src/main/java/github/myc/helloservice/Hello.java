package github.myc.helloservice;

import lombok.*;

import java.io.Serializable;

/**
 * HelloService 要传输的对象
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Hello implements Serializable {

    private String message;
    private String description;

}
