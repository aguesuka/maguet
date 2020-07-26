package cc.aguesuka.maguet.util.timer;

/**
 * Result creates by {@link Timer}
 *
 * @author aguesuka
 */
public interface Timeout {
    /**
     * cancel this
     *
     * @return true if success
     */
    boolean cancel();
}
