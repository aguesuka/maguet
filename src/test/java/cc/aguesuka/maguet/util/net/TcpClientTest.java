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

public class TcpClientTest {
    private static final int PORT = 18888;
    private static final int BUFFER_SIZE = 10;
    private ServerSocket serverSocket;
    private EchoServerThread echoServerThread;
    private SocketAddress address;

    /** Creates server socket and starts the {@link EchoServerThread} */
    @Before
    public void initServerSocket() throws IOException {
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

    private void withEchoServer(BiConsumer<EventLoop, SocketAddress> action) throws IOException {
        EventLoop.start(eventLoop -> action.accept(eventLoop, address));
    }


    /**
     * Creates EventLoop then create TcpClient, then invoke action. complete until {@link TcpClient#close()} has been
     * called.
     */
    private void withClient(Consumer<TcpClient<?>> action) throws IOException {
        withEchoServer((eventLoop, address) -> {
            TcpClient<?> client = TcpClient.of(eventLoop, new PrintSetting(eventLoop));
            client.connect(address, setting -> action.accept(client));
        });
    }

    @Test(timeout = 1000)
    public void testConnect() throws IOException {
        withClient(TcpClient::close);
    }

    @Test(timeout = 1000)
    public void testWriteThenReadByEchoServer() throws IOException {
        withClient(client -> {
            byte[] message = "testWriteThenReadByEchoServer".getBytes(StandardCharsets.UTF_8);
            int messageLength = message.length;
            ByteBuffer buffer = ByteBuffer.allocate(messageLength);
            client.setWriteBuffer(ByteBuffer.wrap(message));
            client.onWriteComplete(ws -> client.read(buffer, messageLength, rs -> {
                Assert.assertArrayEquals(buffer.array(), message);
                client.close();
            }));
        });
    }

    private static class PrintSetting implements TcpClient.Setting {

        private final EventLoop eventLoop;

        public PrintSetting(EventLoop eventLoop) {
            this.eventLoop = eventLoop;
        }

        @Override
        public void onClose() {
            eventLoop.close();
        }

        @Override
        public void handleThrowable(Throwable throwable) {
            System.out.println("TcpClientTest.handleThrowable");
            throwable.printStackTrace();
            Assert.fail(throwable.getMessage());
        }

        @Override
        public void onSelected() {
        }

        @Override
        public void onEOF() {
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