package cc.aguesuka.util.stop;

import java.net.SocketTimeoutException;

/**
 * 超时工具
 *
 * @author :yangmingyuxing
 * 2019/7/5 17:43
 */
public interface Timeout {
    /**
     * 通过毫秒设置超时
     *
     * @param timeout 超时时间ms 0 永不超时
     * @return 超时定时器
     */
    static Timeout getMilliSecond(int timeout) {
        return new TimeoutImpl(timeout);
    }


    /**
     * 是否超时
     *
     * @return 剩余时间毫秒, 如果永不超时返回0
     * @throws SocketTimeoutException 超时则抛出异常
     */
    int checkTimeout() throws SocketTimeoutException;

    /**
     * 是否为永不超时
     *
     * @return 是否为永不超时
     */
    boolean isNever();

    class TimeoutImpl implements Timeout {
        private long startTime;
        private int timeout;

        TimeoutImpl(int timeout) {
            if (timeout < 0) {
                throw new IllegalArgumentException();
            }
            this.timeout = timeout;
            startTime = System.currentTimeMillis();
        }

        @Override
        public int checkTimeout() throws SocketTimeoutException {
            if (isNever()) {
                return 0;
            } else {
                int ram = timeout - ((int) (System.currentTimeMillis() - startTime));
                if (ram <= 0) {
                    throw new SocketTimeoutException(Integer.toString(ram));
                }
                return ram;
            }
        }

        @Override
        public boolean isNever() {
            return timeout == 0;
        }
    }

}
