package cc.aguesuka.maguet.util.net;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TcpClientTest {

    @Test(timeout = 1000)
    public void testByLocalSimpleEchoServer() throws IOException {
        byte[] message = "testByLocalSimpleEchoServer".getBytes(StandardCharsets.UTF_8);
        InetSocketAddress address = new InetSocketAddress(18888);
        int messageLength = message.length;
        Thread echoServerThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.bind(address);
                Socket socket = serverSocket.accept();
                byte[] buffer = new byte[messageLength];
                int size = socket.getInputStream().read(buffer);
                assert size == messageLength : "echo server has not read all bytes";
                Assert.assertArrayEquals(message, buffer);
                socket.getOutputStream().write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        echoServerThread.setDaemon(true);
        echoServerThread.start();

        EventLoop.start(eventLoop -> {
            TcpClient<TcpClient.Setting> client = TcpClient.of(eventLoop, new TcpClient.Setting() {
                @Override
                public void onClose() {
                    System.out.println("TcpClientTest.onClose");
                }

                @Override
                public void handleThrowable(Throwable throwable) {
                    System.out.println("TcpClientTest.handleThrowable");
                    throwable.printStackTrace();
                }

                @Override
                public void onEOF() {
                    System.out.println("TcpClientTest.onEOF");
                }
            });
            client.connect(address, setting -> {
                ByteBuffer buffer = ByteBuffer.allocate(messageLength);
                client.setWriteBuffer(ByteBuffer.wrap(message));
                client.onWriteComplete(ws -> client.read(buffer, messageLength, rs -> {
                    Assert.assertArrayEquals(buffer.array(), message);
                    client.close();
                    assert client.isClosed();
                    eventLoop.close();
                }));
            });
        });
    }
}