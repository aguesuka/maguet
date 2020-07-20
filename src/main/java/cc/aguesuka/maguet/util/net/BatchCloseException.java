package cc.aguesuka.maguet.util.net;

import java.util.Collections;
import java.util.List;

/**
 * Throw when close {@link java.io.Closeable} batch has exceptions
 *
 * @author :aguesuka
 * 2020/6/6 21:04
 */
public class BatchCloseException extends RuntimeException {
    private final List<Throwable> closeExceptions;

    public BatchCloseException(List<Throwable> closeExceptions) {
        super();
        this.closeExceptions = Collections.unmodifiableList(closeExceptions);
    }

    public List<Throwable> getCloseExceptions() {
        return closeExceptions;
    }
}
