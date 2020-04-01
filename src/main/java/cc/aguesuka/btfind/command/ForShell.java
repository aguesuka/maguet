package cc.aguesuka.btfind.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author :yangmingyuxing
 * 2020/2/22 13:39
 */
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ForShell {
}
