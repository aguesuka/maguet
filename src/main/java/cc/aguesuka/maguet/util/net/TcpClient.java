package cc.aguesuka.maguet.util.net;


import java.io.Closeable;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Tcp client
 *
 * @param <T> type of setting
 * @author agueuska
 */
public interface TcpClient<T extends TcpClient.Setting> extends Closeable {
    /**
     * Empty byte buffer
     */
    ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0).flip();

    /**
     * Create instance of TcpClient
     *
     * @param eventLoop EventLoop
     * @param setting   setting
     * @param <T>       type of setting
     * @return instance of TcpClient
     */
    static <T extends TcpClient.Setting> TcpClient<T> of(EventLoop eventLoop, T setting) {
        return new TcpClientImpl<>(eventLoop, setting);
    }

    /**
     * Connect to remote server
     *
     * @param address  remote address
     * @param callback callback on connect success
     * @implNote redirect exception to {@link Setting#handleThrowable(Throwable)}
     */
    void connect(SocketAddress address, Consumer<T> callback);

    /**
     * On connect is readable, read by the buffer, invoke callback unit the
     * {@code buffer.position >= requireSize}
     *
     * @param buffer         nonnull buffer
     * @param targetPosition {@code 0 < requireSize <= buffer.limit()}
     * @param callback       callback not null
     * @throws IllegalArgumentException when {@code  requireSize <=0 || requireSize > buffer.limit()}
     * @throws IllegalStateException    not connect or is closed
     * @throws NullPointerException     any null argument
     */
    void read(ByteBuffer buffer, int targetPosition, Consumer<T> callback);

    /**
     * Bind buffer to this TcpClient, when connect is writeable and the buffer has remaining,
     * write it.
     *
     * @param buffer nonnull buffer
     * @throws NullPointerException  any null argument
     * @throws IllegalStateException not connect or is closed
     */
    void setWriteBuffer(ByteBuffer buffer);

    /**
     * Set the callback, it will invoke at {@link #setWriteBuffer(ByteBuffer) writeBuffer} has not remaining
     *
     * @param callback nullable callback
     * @throws IllegalStateException not connect or is closed
     */
    void onWriteComplete(Consumer<T> callback);

    /**
     * Return true if this closed
     *
     * @return true if this closed
     */
    boolean isClosed();

    /**
     * Close this client
     *
     * @implNote redirect exception to {@link Setting#handleThrowable(Throwable)}
     */
    @Override
    void close();

    /**
     * The setting of client
     */
    interface Setting {
        /**
         * Invoke on client closed
         *
         * @implNote don't throw exception
         */
        void onClose();

        /**
         * Async throwable handler
         *
         * @param throwable throwable
         */
        void handleThrowable(Throwable throwable);

        /**
         * If return true, client will auto close when not set read or write buffer
         *
         * @return is auto close on idle
         */
        default boolean autoCloseOnIdle() {
            return true;
        }

        /**
         * Invoke on read or write result is EOF
         *
         * @see #autoCloseOnEof()
         */
        void onEOF();

        /**
         * If return true, client will close before {@link #onEOF()}
         *
         * @return is auto close on EOF
         * @see #onEOF()
         */
        default boolean autoCloseOnEof() {
            return true;
        }
    }
}
