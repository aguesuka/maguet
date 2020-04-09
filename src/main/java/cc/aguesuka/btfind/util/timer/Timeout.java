package cc.aguesuka.btfind.util.timer;

/**
 * timeout create by {@link Timer}
 *
 * @author :aguesuka
 * 2020/4/2 18:00
 */
public interface Timeout {
    /**
     * cancel this
     *
     * @return true if success
     */
    boolean cancel();
}
