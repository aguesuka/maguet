package cc.aguesuka.dht.connection;

import cc.aguesuka.dht.token.KrpcToken;
import cc.aguesuka.util.bencode.Bencode;
import cc.aguesuka.util.bencode.BencodeMap;
import cc.aguesuka.util.inject.annotation.Config;
import cc.aguesuka.util.inject.annotation.Init;
import cc.aguesuka.util.inject.annotation.Inject;
import cc.aguesuka.util.log.LogSetting;
import cc.aguesuka.util.stop.Timeout;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author :yangmingyuxing
 * 2019/7/2 23:36
 */
public class DhtConnectionImpl implements DhtConnection<InetSocketAddress, BencodeMap> {
    private static Logger logger = Logger.getLogger(LogSetting.DEFAULT_NAME);
    @Inject
    private DhtHandler handler;
    @Config("udp.timeout")
    private int timeout;
    private Map<InetSocketAddress, Subscriber> subscriberMap = new ConcurrentHashMap<>();
    private boolean isStart = false;
    private int buffLength = 1024 * 16;
    @Config("udp.port")
    private int port;
    @Config("udp.thread.name")
    private String threadName;
    private ThreadPoolExecutor threadPoolExecutor;
    private DatagramSocket socket;

    @Init
    private void init() {
        this.threadPoolExecutor = new ThreadPoolExecutor(
                1, 1, 0L,
                TimeUnit.MILLISECONDS, new SynchronousQueue<>(), runnable -> new Thread(runnable, threadName));
        start();
    }

    @Override
    public void start() {
        try {
            isStart = true;
            threadPoolExecutor.execute(() -> {
                try {
                    socket = new DatagramSocket(port);
                    socket.setSoTimeout(timeout);
                    byte[] buff = new byte[buffLength];
                    DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
                    while (isStart) {
                        loopRecv(datagramPacket);
                    }
                } catch (RuntimeException | SocketException e) {
                    throw new RuntimeException(e);
                } finally {
                    isStart = false;
                }
            });
        } catch (RejectedExecutionException e) {
            isStart = false;
        }
    }

    private void loopRecv(DatagramPacket datagramPacket) {
        try {
            socket.receive(datagramPacket);
            if (datagramPacket.getLength() > 0) {
                byte[] b = Arrays.copyOfRange(datagramPacket.getData(), 0, datagramPacket.getLength());
                InetSocketAddress address = (InetSocketAddress) datagramPacket.getSocketAddress();
                BencodeMap bencode = Bencode.parse(ByteBuffer.wrap(b));
                byte[] y = bencode.getByteArray(KrpcToken.Y);
                if (Arrays.equals(KrpcToken.RETURN, y)) {
                    logger.fine("收到回复:" + address + " " + bencode);
                    Subscriber subscriber = subscriberMap.get(address);
                    if (subscriber != null) {
                        subscriber.onResponse(bencode);
                    }
                } else if (Arrays.equals(KrpcToken.QUERY_TYPE, y)) {
                    logger.fine("收到请求:" + address + " " + bencode);
                    handler.onRequest(address, bencode);
                } else {
                    logger.fine("错误消息:" + address + " " + bencode);
                    throw new RuntimeException();
                }
            }
        } catch (SocketTimeoutException e) {
            //
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void send(InetSocketAddress address, BencodeMap message) throws IOException {
        logger.fine("发送消息:" + address + " " + message);
        byte[] msg = Bencode.toBytes(message);
        DatagramPacket datagramPacket = new DatagramPacket(msg, msg.length);
        datagramPacket.setSocketAddress(address);
        try {
            socket.send(datagramPacket);
        } catch (NullPointerException e) {
            throw new IOException("连接已关闭");
        }
    }

    @Override
    public BencodeMap sendAndRecv(InetSocketAddress address, BencodeMap message, Timeout timeout) throws IOException {
        return new Subscriber().request(this, address, message, timeout);
    }

    @Override
    public void close() {
        isStart = false;
        threadPoolExecutor.shutdownNow();
        socket = null;
    }

    /**
     * 订阅者
     */
    private static class Subscriber {
        private BencodeMap response;

        /**
         * 发送消息到目标地址,并订阅目标地址返回的消息,进入阻塞状态,
         * 直到发布者收到消息调用{@link Subscriber#onResponse},返回消息
         *
         * @param dhtConnectionImpl 连接实现
         * @param address           目标地址
         * @param request           请求的消息
         * @param timeout           超时
         * @return 返回的结果
         * @throws IOException 连接异常
         */
        private synchronized BencodeMap request(DhtConnectionImpl dhtConnectionImpl, InetSocketAddress address,
                                                BencodeMap request, Timeout timeout) throws IOException {

            dhtConnectionImpl.subscriberMap.put(address, this);
            dhtConnectionImpl.send(address, request);
            try {
                wait(timeout.checkTimeout());
                Objects.requireNonNull(response);
            } catch (InterruptedException | NullPointerException e) {
                throw new SocketTimeoutException("请求 " + address + " 连接超时");
            } finally {
                dhtConnectionImpl.subscriberMap.remove(address, this);
            }
            return response;
        }

        /**
         * 发布者{@link DhtConnectionImpl}收到订阅的消息调用该方法
         *
         * @param bencode 收到的消息
         */
        private synchronized void onResponse(BencodeMap bencode) {
            this.response = bencode;
            this.notify();
        }
    }
}
