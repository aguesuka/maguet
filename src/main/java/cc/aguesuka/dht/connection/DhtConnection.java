package cc.aguesuka.dht.connection;

import cc.aguesuka.util.stop.Timeout;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author :yangmingyuxing
 * 2019/7/7 13:26
 */
public interface DhtConnection<A, M> extends Closeable {
    /**
     * 开始
     */
    void start();

    /**
     * 发送消息
     * @param address 地址
     * @param message 消息
     * @throws IOException io异常
     */
    void send(A address, M message) throws IOException;

    /**
     * 发送消息等待回复
     * @param address 地址
     * @param message 消息
     * @param timeout 超时时间
     * @return 消息回复
     * @throws IOException io异常
     */
    M sendAndRecv(A address, M message, Timeout timeout) throws IOException;

}
