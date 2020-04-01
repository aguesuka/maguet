package cc.aguesuka.btfind.socket;

/**
 * @author :yangmingyuxing
 * 2020/2/19 14:54
 */
public class AsyncTcpException extends RuntimeException{
    public AsyncTcpException(String message) {
        super(message);
    }

    public AsyncTcpException(Throwable cause) {
        super(cause);
    }
}
