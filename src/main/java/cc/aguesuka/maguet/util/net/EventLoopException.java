package cc.aguesuka.maguet.util.net;

/**
 * Exception to {@link EventLoop}
 *
 * @author :aguesuka
 */
public class EventLoopException extends RuntimeException {
    public EventLoopException(String message) {
        super(message);
    }

    public EventLoopException(String message, Throwable cause) {
        super(message, cause);
    }
}
