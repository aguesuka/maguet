package cc.aguesuka.util.inject.help;

/**
 * 注入时出现的异常
 *
 * @author :yangmingyuxing
 * @see cc.aguesuka.util.inject.Injector
 * 2019/7/10 21:44
 */
public class InjectorException extends RuntimeException {
    public InjectorException() {
    }

    public InjectorException(Throwable cause) {
        super(cause);
    }

    public InjectorException(String message) {
        super(message);
    }

    public InjectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
