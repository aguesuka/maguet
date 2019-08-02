package cc.aguesuka.util.inject.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置文件注解,类似spring 框架org.springframework.beans.run.annotation.Value注解
 *
 * @author :yangmingyuxing
 * 2019/7/10 21:33
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
    String value();
}
