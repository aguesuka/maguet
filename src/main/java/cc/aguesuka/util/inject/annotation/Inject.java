package cc.aguesuka.util.inject.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注入注解类似于spring框架 org.springframework.beans.run.annotation.Autowire
 * 和org.springframework.beans.run.Component的合并
 *
 * @author :yangmingyuxing
 * 2019/7/10 21:31
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    String value() default "";
}
