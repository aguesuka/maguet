package cc.aguesuka.btfind.socket;

import cc.aguesuka.btfind.dispatch.EventLoop;
import cc.aguesuka.btfind.metadata.MetadataDownloadException;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Async tcp client connection,dispatch with {@link EventLoop}. auto close if not has async task.
 *
 * @author :aguesuka
 * 2020/2/8 11:10
 */
public class AsyncTcpConnection implements Closeable {
    private boolean isWorking;
    private SocketChannel channel;
    private SelectionKey key;
    private Runnable eventLoopEventHandler;
    private Runnable callback;
    private Consumer<Throwable> throwableHandler;
    private Runnable closeCallback;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private BooleanSupplier beforeHandleCheck;

    private AsyncTcpConnection(Consumer<Throwable> throwableHandler,
                               Runnable closeCallback,
                               BooleanSupplier beforeHandleCheck) {
        Objects.requireNonNull(throwableHandler);
        Objects.requireNonNull(closeCallback);
        Objects.requireNonNull(beforeHandleCheck);
        this.throwableHandler = throwableHandler;
        this.closeCallback = closeCallback;
        this.beforeHandleCheck = beforeHandleCheck;
    }

    /**
     * Create {@link AsyncTcpConnection} instance with listen function
     *
     * @param throwableConsumer when exception on {@link AsyncTcpConnection#execute(Runnable)} and
     *                          use {@link AsyncTcpConnection#channel},it maybe call more than once.
     * @param closeCallback     after {@link AsyncTcpConnection#close()}. it called only once
     * @param beforeHandle      before execute callback ,if return false,close connection; {@link Handler#handle()}
     * @return AsyncTcpConnection
     */
    public static AsyncTcpConnection of(Consumer<Throwable> throwableConsumer,
                                        Runnable closeCallback,
                                        BooleanSupplier beforeHandle) {
        return new AsyncTcpConnection(throwableConsumer, closeCallback, beforeHandle);
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            close();
            throwableHandler.accept(e);
        }
    }

    /**
     * Connect to server.
     *
     * @param eventLoop event loop.
     * @param address   server address
     * @param callback  execute after connect
     */
    public void connect(EventLoop eventLoop, InetSocketAddress address, Runnable callback) {
        try {
            isWorking = true;
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            key = eventLoop.register(channel, SelectionKey.OP_CONNECT, new Handler());
            channel.connect(address);
            this.eventLoopEventHandler = () -> finishConnect(callback);
        } catch (Exception e) {
            throwableHandler.accept(e);
        }
    }

    private void finishConnect(Runnable callback) {
        try {
            boolean success = channel.finishConnect();
            if (success) {
                readBuffer = ByteBuffer.allocate(1024 * 32).flip();
                writeBuffer = ByteBuffer.allocate(1024 * 16);
                setCallback(callback);
            } else {
                close();
            }
        } catch (IOException e) {
            throw new AsyncTcpException(e);
        }
    }

    /**
     * Read bytes async. callback will execute when {@code buffer remaining > length}
     *
     * @param length   length for bytes
     * @param callback Consumer of bytes
     */
    public void recvBytes(int length, Consumer<byte[]> callback) {
         if(length < 0) {
            throw new MetadataDownloadException("msg length < 0");
        }
        readForLength(length, () -> {
            byte[] bytes = new byte[length];
            readBuffer.get(bytes);
            callback.accept(bytes);
        });
    }

    /**
     * recv async 4 bytes as int
     *
     * @param callback int consumer
     */
    public void recvInt(IntConsumer callback) {
        readForLength(4, () -> callback.accept(readBuffer.getInt()));
    }

    /**
     * write bytes to writeBuffer
     *
     * @param bytes byte array
     * @return this
     */
    public AsyncTcpConnection write(byte[] bytes) {
        writeBuffer.put(bytes);
        return this;
    }

    /**
     * write byte to writeBuffer
     *
     * @param b byte
     * @return this
     */
    public AsyncTcpConnection write(byte b) {
        writeBuffer.put(b);
        return this;
    }

    /**
     * write int to writeBuffer
     *
     * @param i int
     * @return this
     */
    public AsyncTcpConnection writeInt(int i) {
        writeBuffer.putInt(i);
        return this;
    }

    /**
     * Async send writeBuffer bytes to server.
     *
     * @param callback when bytes send over
     */
    public void send(Runnable callback) {
        writeBuffer.flip();
        sendAll(callback);
    }

    private void sendAll(Runnable callback) {
        if (writeBuffer.hasRemaining()) {
            key.interestOps(SelectionKey.OP_WRITE);
            this.eventLoopEventHandler = () -> {
                try {
                    int len = channel.write(writeBuffer);
                    if (len == -1) {
                        throw new AsyncTcpException("EOF");
                    }
                } catch (IOException e) {
                    throw new AsyncTcpException(e);
                }
                sendAll(callback);
            };
        } else {
            writeBuffer.clear();
            setCallback(callback);
        }
    }

    private void setCallback(Runnable callback) {
        this.callback = callback;
    }

    private void readForLength(int length, Runnable callback) {
        if (readBuffer.remaining() >= length) {
            setCallback(callback);
            return;
        }
        key.interestOps(SelectionKey.OP_READ);
        this.eventLoopEventHandler = () -> {
            readBuffer.compact();
            try {
                int len = channel.read(readBuffer);
                if (len == -1) {
                    throw new AsyncTcpException("EOF");
                }
            } catch (IOException e) {
                throw new AsyncTcpException(e);
            }
            readBuffer.flip();
            readForLength(length, callback);
        };
    }

    /**
     * close connection, not throw exception
     */
    @Override
    public void close() {
        try {
            if (isWorking) {
                isWorking = false;
                channel.close();
                closeCallback.run();
            }
        } catch (Exception e) {
            throwableHandler.accept(e);
        }
    }

    private class Handler implements EventLoop.Handler {
        @Override
        public void handle() {
            Runnable handler = eventLoopEventHandler;
            eventLoopEventHandler = null;


            callback = null;

            if (beforeHandleCheck.getAsBoolean()) {
                execute(handler);
                while (callback != null) {
                    Runnable cb = callback;
                    callback = null;
                    execute(cb);
                }
            }

            if (eventLoopEventHandler == null) {
                close();
            }
        }
    }
}
