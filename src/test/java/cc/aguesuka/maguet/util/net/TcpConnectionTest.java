package cc.aguesuka.maguet.util.net;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TcpConnectionTest {
    private static final int PORT = 18888;
    private static final int BUFFER_SIZE = 10;
    private ServerSocket serverSocket;
    private EchoServerThread echoServerThread;
    private SocketAddress address;

    /** Creates server socket and starts the {@link EchoServerThread} */
    @Before
    public void initServerSocket() throws IOException {
        System.out.println("TcpConnectionTest.initServerSocket");
        serverSocket = new ServerSocket();
        address = new InetSocketAddress(PORT);
        serverSocket.bind(address);
        echoServerThread = new EchoServerThread(serverSocket, new byte[BUFFER_SIZE]);
        echoServerThread.start();
    }

    @After
    public void closeServerSocket() throws IOException {
        serverSocket.close();
        try {
            echoServerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.fail("EchoServerThread not shutdown");
        }
        if (echoServerThread.fail) {
            Assert.fail("echo server fail");
        }
    }

    private void withEventLoop(BiConsumer<EventLoop, SocketAddress> action) throws IOException {
        EventLoop.start(eventLoop -> action.accept(eventLoop, address));
    }


    /**
     * Creates EventLoop then create TcpConnect, then invoke action. complete until {@link TcpConnection#close()} has
     * been called.
     */
    private void withConnect(Consumer<TcpConnection<?>> action) throws IOException {
        withEventLoop((eventLoop, address) -> {
            TcpConnection<?> connect = TcpConnection.of(eventLoop, new PrintSetting(eventLoop));
            connect.connect(address, setting -> action.accept(connect));
        });
    }

    @Test(timeout = 1000)
    public void testConnect() throws IOException {
        withConnect(TcpConnection::close);
    }

    @Test(timeout = 1000)
    public void testWriteThenReadByEchoServer() throws IOException {
        withConnect(connection -> {
            byte[] message = "testWriteThenReadByEchoServer".getBytes(StandardCharsets.UTF_8);
            int messageLength = message.length;
            ByteBuffer buffer = ByteBuffer.allocate(messageLength);
            connection.setWriteBuffer(ByteBuffer.wrap(message));
            connection.onWriteComplete(ws -> connection.read(buffer, messageLength, rs -> {
                Assert.assertArrayEquals(buffer.array(), message);
                connection.close();
            }));
        });
    }

    @Test(timeout = 1000)
    public void testAutoCloseOnThrowException() throws IOException {
        withEventLoop((eventLoop, address) -> new PrintSetting(eventLoop) {
            final TcpConnection<?> connect;
            RuntimeException exception;
            {
                connect = TcpConnection.of(eventLoop, this);
                connect.connect(address, setting -> {
                    exception = new RuntimeException();
                    throw exception;
                });
            }

            @Override
            public void handleThrowable(Throwable throwable) {
                System.out.println("TcpConnectionTest.handleThrowable");
                assert null != exception;
                assert exception == throwable;
                assert connect.isClosed();
            }
        });
    }

    private static class PrintSetting implements TcpConnection.Setting {

        private final EventLoop eventLoop;

        public PrintSetting(EventLoop eventLoop) {
            this.eventLoop = eventLoop;
        }

        @Override
        public void onClose() {
            System.out.println("PrintSetting.onClose");
            eventLoop.close();
        }

        @Override
        public void handleThrowable(Throwable throwable) {
            System.out.println("TcpConnectTest.handleThrowable");
            throwable.printStackTrace();
            Assert.fail(throwable.getMessage());
        }

        @Override
        public void onSelected() {
            System.out.println("PrintSetting.onSelected");
        }

        @Override
        public void onEOF() {
            System.out.println("PrintSetting.onEOF");
        }
    }

    private static class EchoServerThread extends Thread {
        private final ServerSocket serverSocket;
        private final byte[] buffer;
        private boolean fail = false;

        public EchoServerThread(ServerSocket serverSocket, byte[] buffer) {
            super();
            this.serverSocket = serverSocket;
            this.buffer = buffer;
            this.setDaemon(true);
        }

        @Override
        public void run() {
            try (Socket socket = serverSocket.accept()) {
                echo(socket);
            } catch (Throwable e) {
                fail = true;
                e.printStackTrace();
            }
        }

        private void echo(Socket socket) throws IOException {
            while (!Thread.currentThread().isInterrupted()) {
                int length = socket.getInputStream().read(buffer);
                if (length == -1) {
                    return;
                }
                socket.getOutputStream().write(buffer, 0, length);
            }
        }
    }

}