package cc.aguesuka.maguet.util.net;

/**
 * Exception of {@link EventLoop}
 *
 * @author :aguesuka
 * 2020/6/6 22:44
 */
public class EventLoopException extends RuntimeException {
    public EventLoopException(String message) {
        super(message);
    }

    public EventLoopException(String message, Throwable cause) {
        super(message, cause);
    }
}
