package cc.aguesuka.maguet.util.net;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.function.Consumer;

public class TcpClientImpl<T extends TcpClient.Setting> implements TcpClient<T> {

    private static final int EOF = -1;
    private final EventLoop eventLoop;
    private final T setting;
    private SelectionKey key;
    private SocketChannel channel;
    private State state;
    private Consumer<T> connectCallback;
    private Consumer<T> writeCompleteCallback;
    private Consumer<T> readCallback;
    private ByteBuffer writeBuffer = EMPTY_BUFFER;
    private ByteBuffer readBuffer;
    private int targetPosition;

    TcpClientImpl(EventLoop eventLoop, T setting) {
        this.eventLoop = Objects.requireNonNull(eventLoop);
        this.setting = Objects.requireNonNull(setting);
    }

    @Override
    public void connect(SocketAddress address, Consumer<T> callback) {
        try {
            // do connect
            this.channel = SocketChannel.open();
            channel.configureBlocking(false);
            key = eventLoop.register(channel, SelectionKey.OP_CONNECT, this::onSelected);
            channel.connect(address);

            // set callback and state
            this.connectCallback = callback;
            changeState(State.CONNECT);
        } catch (Throwable e) {
            setting.handleThrowable(e);
        }
    }

    @Override
    public void read(ByteBuffer buffer, int targetPosition, Consumer<T> callback) {
        // check args and state
        if (targetPosition > buffer.limit() || targetPosition <= 0) {
            throw new IllegalArgumentException();
        }
        state.checkStateForReadWrite();

        // set args
        this.readBuffer = Objects.requireNonNull(buffer);
        this.targetPosition = targetPosition;
        this.readCallback = Objects.requireNonNull(callback);

        changeState(State.READ_OR_WRITE);
    }

    @Override
    public void setWriteBuffer(ByteBuffer buffer) {
        state.checkStateForReadWrite();
        this.writeBuffer = Objects.requireNonNull(buffer);
        changeState(State.READ_OR_WRITE);
    }

    @Override
    public void onWriteComplete(Consumer<T> callback) {
        state.checkStateForReadWrite();
        this.writeCompleteCallback = Objects.requireNonNull(callback);
        changeState(State.READ_OR_WRITE);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec If this not closed but {@link #channel} closed or {@link #key} canceled, closes the client and returns
     * true
     */
    @Override
    public boolean isClosed() {
        if (state == State.NOT_CONNECT) {
            // not connect, key and channel is null
            return false;
        }
        if (state == State.CLOSED) {
            return true;
        }

        if (!key.isValid() || !channel.isOpen()) {
            setting.onClose();
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        State state = this.state;
        if (state == State.CLOSED) {
            return;
        }

        changeState(State.CLOSED);

        if (state == State.NOT_CONNECT) {
            return;
        }
        try {
            channel.close();
            setting.onClose();
        } catch (Throwable e) {
            setting.handleThrowable(e);
        }
    }


    private void onSelected() {
        try {
            assert key.isValid() : "key not valid";
            setting.onSelected();
            if (isClosed()) {
                return;
            }
            Consumer<T> callback = state.handleSelectedEvent(this);
            while (callback != null) {
                callback.accept(setting);
                // maybe closed by callback function
                if (isClosed()) {
                    return;
                }
                // invokes callback outside to avoid stack overflow
                callback = state.moreCallback(this);
            }
            state.updateSelectionKeyOps(this);
        } catch (Throwable throwable) {
            // redirect throwable
            handleThrowable(throwable);
        }
    }

    private void onEOF() {
        if (setting.autoCloseOnEof()) {
            close();
        }
        setting.onEOF();
    }

    private Consumer<T> getReadCallbackIfComplete() {
        Consumer<T> readCallback = this.readCallback;
        ByteBuffer readBuffer = this.readBuffer;
        if (readBuffer == null || readBuffer.position() < this.targetPosition) {
            return null;
        }
        this.readCallback = null;
        this.readBuffer = null;
        this.targetPosition = 0;
        return readCallback;
    }

    private Consumer<T> getWriteCallbackIfComplete() {
        Consumer<T> writeCompleteCallback = this.writeCompleteCallback;
        if (writeCompleteCallback == null || this.writeBuffer.hasRemaining()) {
            return null;
        }
        this.writeCompleteCallback = null;
        return writeCompleteCallback;
    }

    private Consumer<T> handleReadOrWriteSelectedEvent() throws Exception {
        if (key.isReadable() && readBuffer != null) {
            int result = channel.read(readBuffer);
            if (result == EOF) {
                onEOF();
                return null;
            }
            return getReadCallbackIfComplete();
        }

        if (key.isWritable() && writeBuffer.hasRemaining()) {
            int result = channel.write(writeBuffer);
            if (result == EOF) {
                onEOF();
                return null;
            }
            return getWriteCallbackIfComplete();
        }

        throw new IllegalStateException("on selected but not readable or writeable");
    }

    private void handleThrowable(Throwable throwable) {
        close();
        setting.handleThrowable(throwable);
        setting.onClose();
    }

    private void changeState(State state) {
        this.state = state;
    }

    /**
     * State of client
     * <pre><b>State change graph</b>
     * (NOT_CONNECT -> CONNECT -> IDLE <-> READ_OR_WRITE ) -> CLOSED
     * </pre>
     */
    private enum State {
        NOT_CONNECT,

        CONNECT {
            @Override
            <T extends Setting> Consumer<T> handleSelectedEvent(TcpClientImpl<T> client) throws Exception {
                assert client.key.isValid() : "key not valid";
                assert client.key.isConnectable() : "key not connectable";
                assert client.channel.isOpen() : "channel not open";


                boolean connectSuccess = client.channel.finishConnect();
                assert connectSuccess : "connect fail";

                Consumer<T> callback = client.connectCallback;
                assert callback != null;

                client.connectCallback = null;
                client.changeState(IDLE);

                return callback;
            }
        },

        IDLE {
            @Override
            <T extends Setting> void updateSelectionKeyOps(TcpClientImpl<T> client) {
                if (client.setting.autoCloseOnIdle()) {
                    client.close();
                } else {
                    client.key.interestOps(0);
                }
            }

            @Override
            <T extends Setting> Consumer<T> moreCallback(TcpClientImpl<T> client) {
                return null;
            }

            @Override
            void checkStateForReadWrite() {
            }
        },

        READ_OR_WRITE {
            @Override
            <T extends Setting> void updateSelectionKeyOps(TcpClientImpl<T> client) {
                int ops = 0;
                if (client.readCallback != null) {
                    ops |= SelectionKey.OP_READ;
                }
                if (client.writeBuffer != null && client.writeBuffer.hasRemaining()) {
                    ops |= SelectionKey.OP_WRITE;
                }
                if (ops == 0) {
                    client.changeState(IDLE);
                    IDLE.updateSelectionKeyOps(client);
                } else {
                    client.key.interestOps(ops);
                }
            }

            @Override
            <T extends Setting> Consumer<T> handleSelectedEvent(TcpClientImpl<T> client) throws Exception {
                return client.handleReadOrWriteSelectedEvent();
            }

            @Override
            <T extends Setting> Consumer<T> moreCallback(TcpClientImpl<T> client) {
                Consumer<T> readCallback = client.getReadCallbackIfComplete();
                if (readCallback != null) {
                    return readCallback;
                }
                return client.getWriteCallbackIfComplete();
            }

            @Override
            void checkStateForReadWrite() {

            }
        },

        CLOSED;

        /**
         * Update {@link SelectionKey#interestOps()} after handle SelectedEvent
         */
        <T extends Setting> void updateSelectionKeyOps(TcpClientImpl<T> client) {
            throw new IllegalStateException(this.name());
        }

        /**
         * Whenever a selected event occurs, and handles the event, then returns the callback function or null if not
         * complete.
         *
         * @param <T>    type of client setting
         * @param client client instance
         * @return callback should invoke, or {@code null} if not have
         */
        <T extends Setting> Consumer<T> handleSelectedEvent(TcpClientImpl<T> client) throws Exception {
            throw new IllegalStateException(this.name());
        }

        /**
         * Get a callback function that does not need to wait for the next selection
         *
         * @return callback should invoke, or {@code null} if not have
         */
        <T extends Setting> Consumer<T> moreCallback(TcpClientImpl<T> client) {
            throw new IllegalStateException(this.name());
        }

        void checkStateForReadWrite() {
            throw new IllegalStateException(this.name());
        }
    }
}
