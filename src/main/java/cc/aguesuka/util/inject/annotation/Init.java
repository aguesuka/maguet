package cc.aguesuka.util.inject.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 初始化注解,类似javax.annotation.PostConstruct
 *
 * @author :yangmingyuxing
 * 2019/7/10 21:34
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Init {
}
