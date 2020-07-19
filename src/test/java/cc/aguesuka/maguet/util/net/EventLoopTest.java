package cc.aguesuka.maguet.util.net;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * EventLoopTest
 *
 * @author :aguesuka
 * 2020/6/7 13:35
 */
public class EventLoopTest {
    private EventLoop eventLoop;
    private long startTime;

    @Test(timeout = 5000L)
    public void closeTest() throws IOException {
        EventLoop.start(eventLoop -> {
            assert !eventLoop.isClosed() : "closed without close()";
            try {
                eventLoop.close();
            } catch (BatchCloseException e) {
                assert false : "close fail";
            }
            assert eventLoop.isClosed() : "not close fail";

        });
    }

    @Test(timeout = 5000L)
    public void startErrorTest() {
        String message = "test";
        try {
            EventLoop.start(eventLoop -> {
                this.eventLoop = eventLoop;
                throw new RuntimeException(message);
            });
            assert false : "not throw";
        } catch (RuntimeException e) {
            assert this.eventLoop.isClosed();
            assert e.getMessage().equals(message);
        } catch (Throwable e) {
            assert false : e;
        }
    }

    @Test(timeout = 5000L)
    public void closeByTimerTest() throws IOException {

        int delay = 1;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        EventLoop.start(eventLoop -> {
            startTime = System.currentTimeMillis();
            eventLoop.getTimer().createTimeout(eventLoop::close, delay, timeUnit);
        });
        long cost = System.currentTimeMillis() - startTime;
        long error = cost - timeUnit.toMillis(delay);
        assert error >= 0 : error;
        assert error <= EventLoop.TIME_UNIT.toMillis(EventLoop.TICK_DURATION * 2) : error;
    }

    @Test(timeout = 5000L)
    public void closeByInterrupt() throws IOException {
        int delay = 1;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        EventLoop.start(eventLoop -> {
            startTime = System.currentTimeMillis();
            eventLoop.getTimer().createTimeout(() -> Thread.currentThread().interrupt(), delay, timeUnit);
        });
        long cost = System.currentTimeMillis() - startTime;
        long error = cost - timeUnit.toMillis(delay);
        assert error >= 0 : error;
        assert error <= EventLoop.TIME_UNIT.toMillis(EventLoop.TICK_DURATION * 2) : error;
    }

    private void readInt(EventLoop eventLoop, Pipe.SourceChannel source, List<Integer> targetList) throws IOException {
        source.configureBlocking(false);
        eventLoop.register(source, SelectionKey.OP_READ, () -> {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            try {
                source.read(buffer);
            } catch (IOException e) {
                assert false : e;
            }
            if (buffer.position() == 4) {
                int integer = buffer.flip().getInt();
                buffer.clear();
                targetList.add(integer);
            }
        });
    }

    private void writeInt(EventLoop eventLoop, Pipe.SinkChannel sink, int inputNum) throws IOException {
        sink.configureBlocking(false);
        var field = new Object() {
            SelectionKey selectionKey;
        };
        field.selectionKey = eventLoop.register(sink, SelectionKey.OP_WRITE, () -> {
            ByteBuffer inputBuffer = ByteBuffer.allocate(4).putInt(inputNum).flip();
            try {
                sink.write(inputBuffer);
                field.selectionKey.interestOps(0);
            } catch (IOException e) {
                assert false : e;
            }
        });
    }

    @Test
    public void pipeTest() throws IOException {
        int size = 100;
        List<Integer> integerList = new ArrayList<>(size);
        List<Pipe> pipeList = new ArrayList<>(size);

        EventLoop.start(eventLoop -> {
            this.eventLoop = eventLoop;
            for (int i = 0; i < size; i++) {
                Pipe pipe = Pipe.open();
                pipeList.add(pipe);
                readInt(eventLoop, pipe.source(), integerList);
                writeInt(eventLoop, pipe.sink(), i);
            }
            eventLoop.getTimer().createTimeout(eventLoop::close, 1, TimeUnit.SECONDS);
        });

        integerList.sort(Integer::compareTo);
        for (int i = 0; i < size; i++) {
            Integer integer = integerList.get(i);
            assert i == integer : integer;
        }
        assert eventLoop.isClosed();
        for (Pipe pipe : pipeList) {
            assert !pipe.sink().isOpen();
            assert !pipe.source().isOpen();
        }

    }
}