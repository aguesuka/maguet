package cc.aguesuka.maguet.util.timer;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author :aguesuka
 * 2020/4/11 12:02
 */
public class WheelTickMockTimerTest {
    private static final int TICK_PER_WHEEL = 100;
    private static final long TICK_DURATION = 100;

    @Test
    public void testSingletonTimer() {
        MockTime mockTime = new MockTime();
        CountDownLatch count = new CountDownLatch(1);
        mockTime.createTimeout(count::countDown, 1, TimeUnit.SECONDS);
        mockTime.pass(999);
        assert count.getCount() == 1;
        mockTime.pass(20000);
        assert count.getCount() == 0;
    }

    @Test
    public void testCancel() {
        MockTime mockTime = new MockTime();
        CountDownLatch count = new CountDownLatch(1);
        Timeout timeout = mockTime.createTimeout(count::countDown, 1, TimeUnit.SECONDS);
        mockTime.pass(999);
        boolean cancel = timeout.cancel();
        assert count.getCount() == 1;
        assert cancel;
        mockTime.pass(20000);
        assert count.getCount() == 1;
    }

    @Test
    public void testCannotCancelExpired() {
        MockTime mockTime = new MockTime();
        CountDownLatch count = new CountDownLatch(1);
        Timeout timeout = mockTime.createTimeout(count::countDown, 1, TimeUnit.SECONDS);
        mockTime.pass(1000 + TICK_DURATION);
        boolean cancel = timeout.cancel();
        assert !cancel;
        assert count.getCount() == 0;
    }


    @Test
    public void testInterval() {
        MockTime mockTime = new MockTime();
        IntStream.of(0, 1, 8, 64, 99).forEach(intervalMethodCost -> {
            var interval = new Runnable() {
                final long createTime = mockTime.getNow();
                final long delayMill = 1000;
                int currentCount = 0;

                public void addNextTimeout() {
                    long nextTime = createTime + (currentCount + 1) * delayMill;

                    // do something
                    mockTime.pass(intervalMethodCost);

                    mockTime.createTimeout(this, Math.max(0, nextTime - mockTime.getNow()), TimeUnit.MILLISECONDS);
                }

                @Override
                public void run() {
                    currentCount++;
                    addNextTimeout();
                }
            };
            interval.addNextTimeout();
            assert interval.currentCount == 0;

            int totalIntervalCount = 100;
            while (interval.currentCount < totalIntervalCount) {
                mockTime.pass(1);
            }
            long cost = mockTime.getNow() - interval.createTime;
            long expect = interval.delayMill * totalIntervalCount;
            assert cost >= expect;
            assert cost <= expect + TICK_DURATION + intervalMethodCost;
        });
    }

    private static class MockTime implements Timer {
        private long now = System.currentTimeMillis();
        private final WheelTick<Runnable> wheelTick =
                new WheelTick<>(TICK_DURATION, TimeUnit.MILLISECONDS, TICK_PER_WHEEL, this::getNow);

        public long getNow() {
            return now;
        }

        private void pass(long mill) {
            long target = now + mill;
            while (true) {
                wheelTick.tick(TimeUnit.MILLISECONDS);
                List<Runnable> take = wheelTick.take();
                if (!take.isEmpty()) {
                    take.forEach(Runnable::run);
                }
                if (now >= target) {
                    break;
                } else {
                    now++;
                }
            }
        }

        @Override
        public Timeout createTimeout(Runnable task, long delay, TimeUnit timeUnit) {
            long addTime = getNow();
            long delayMill = timeUnit.toMillis(delay);
            return wheelTick.add(() -> {
                long actually = getNow() - addTime;
                assert actually >= delayMill && actually <= delayMill + TICK_DURATION :
                        String.format(" %d(delayMill) <= %d(actually) <=  %d(delayMill + TICK_DURATION)",
                                delayMill, actually, delayMill + TICK_DURATION);
                task.run();
            }, delay, timeUnit);
        }
    }
}