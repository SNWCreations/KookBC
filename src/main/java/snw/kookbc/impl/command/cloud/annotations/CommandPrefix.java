package snw.kookbc.impl.command.cloud.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 2023/6/10<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CommandPrefix {
    String[] value() default {};
}
