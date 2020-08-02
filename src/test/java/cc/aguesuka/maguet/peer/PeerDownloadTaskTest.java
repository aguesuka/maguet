package cc.aguesuka.maguet.peer;

import cc.aguesuka.maguet.util.HexUtil;
import cc.aguesuka.maguet.util.net.EventLoop;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class PeerDownloadTaskTest {
    private byte[] getRemaining(ByteBuffer buffer) {
        int position = buffer.position();
        int length = buffer.remaining();
        byte[] result = new byte[length];
        buffer.get(result);
        buffer.position(position);
        return result;
    }

    @Test
    public void test() throws IOException {
        var observer = new PeerDownloadTask.Observer() {
            PeerDownloadTaskImpl task;
            EventLoop eventLoop;
            boolean fail = false;

            @Override
            public void onCompete(byte[] metadata) {
                eventLoop.close();
                assert task.getProgress() == PeerDownloadTask.Progress.COMPLETE;
            }

            @Override
            public void onFail(String reason) {
                System.out.println("PeerDownloadTaskTest.onFail");
                System.out.println("reason = " + reason);
                System.out.println("task.getProgress() = " + task.getProgress());
                fail = true;
                eventLoop.close();
            }

            @Override
            public void onThrow(Throwable throwable) {
                System.out.println("PeerDownloadTaskTest.onThrow");
                System.out.println("task.getProgress() = " + task.getProgress());
                throwable.printStackTrace();
                fail = true;
                eventLoop.close();
            }

            @Override
            public void onSelected() {
                System.out.println("PeerDownloadTaskTest.onSelected");
            }

            @Override
            public void beforeCallback() {
                System.out.println("PeerDownloadTaskTest.beforeCallback");
                if (task.writeBuffer != null) {
                    byte[] bytes = getRemaining(task.writeBuffer);
                    System.out.println("write =" + HexUtil.encode(bytes));
                    System.out.println(new String(bytes, StandardCharsets.US_ASCII));
                }

                ByteBuffer readBuffer = task.readBuffer;
                if (readBuffer != null) {
                    int limit = readBuffer.limit();
                    int position = readBuffer.position();
                    byte[] bytes = getRemaining(readBuffer.flip());
                    System.out.println("read = " + HexUtil.encode(bytes));
                    System.out.println(new String(bytes, StandardCharsets.US_ASCII));
                    readBuffer.position(position);
                    readBuffer.limit(limit);
                }
            }
        };

        EventLoop.start(eventLoop -> {
            PeerDownloadTask.Builder builder = PeerDownloadTask.builder()
                    .eventLoop(eventLoop)
                    .address(new InetSocketAddress("192.168.1.4", 8888))
                    .infoHash(HexUtil.decode("A72F16876F312258E485748A56B54D77040D08A2"))
                    .selfNodeId(HexUtil.decode("A72F16876F312258E485748A56B54D77040D0000"))
                    .timeout(Duration.ofSeconds(59))
                    .connectTimeout(Duration.ofSeconds(59))
                    .readTimeout(Duration.ofSeconds(59))
                    .observer(observer);
            observer.eventLoop = eventLoop;
            observer.task = (PeerDownloadTaskImpl) builder.build();
        });
        assert !observer.fail;
    }
}