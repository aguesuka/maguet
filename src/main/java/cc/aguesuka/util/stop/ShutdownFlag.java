package cc.aguesuka.util.stop;

/**
 * @author :yangmingyuxing
 * 2019/7/29 21:31
 */
public class ShutdownFlag {
    private volatile boolean isShutdown = false;

    public void shutdown() {
        isShutdown = true;
    }

    public void check() {
        if (isShutdown) {
            throw new ShutdownException();
        }
    }
}
