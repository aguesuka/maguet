package cc.aguesuka.downloader.impl;

import cc.aguesuka.dht.action.DhtAction;
import cc.aguesuka.dht.connection.Bucket;
import cc.aguesuka.downloader.IMetaDataDownloader;
import cc.aguesuka.downloader.IPeerFinder;
import cc.aguesuka.util.bencode.BencodeByteArray;
import cc.aguesuka.util.bencode.BencodeMap;
import cc.aguesuka.util.inject.annotation.Config;
import cc.aguesuka.util.inject.annotation.Inject;
import cc.aguesuka.util.log.LogSetting;
import cc.aguesuka.util.stop.Timeout;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cc.aguesuka.dht.token.KrpcToken.NODES;
import static cc.aguesuka.dht.token.KrpcToken.R;

/**
 * @author :yangmingyuxing
 * 2019/7/25 16:38
 */
public class PeerFinder implements IPeerFinder {
    @Inject
    private DhtAction dhtAction;
    @Inject
    private Bucket<BencodeByteArray, InetSocketAddress> bucket;
    /**
     * 第一次访问从桶中获得地址的数量
     */
    @Config("dhtAction.getPeerCount")
    private int initVisitCount;
    /**
     * dht中查询的线程数
     */
    @Config("PeerFinder.thread.count")
    private int threadCount;
    /**
     * 下载种子的线程数
     */
    @Config("PeerFinder.downloadThread")
    private int downloadThreadCount;
    /**
     * 下载种子的超时时间
     */
    @Config("PeerFinder.getPeerTimeout")
    private int getPeerTimeout;
    /**
     * dht访问的最大节点数量,超过则抛出异常
     */
    @Config("PeerFinder.maxVisitCount")
    private int maxVisitCount;

    @Override
    public byte[] findPeer(byte[] infoHash, IMetaDataDownloader metaDataDownloader) throws ExecutionException, InterruptedException {
        return new DoFinder(this, infoHash, metaDataDownloader).doFind();
    }

    /**
     * 内部类,一次任务对应一个类
     */
    private static class DoFinder {

        private static final byte[] EMPTY_ARRAY = new byte[0];
        /**
         * 下一轮需要访问的dht节点
         */
        Set<InetSocketAddress> waitToVisit = Collections.newSetFromMap(new ConcurrentHashMap<>());
        /**
         * 已经访问过的dht节点
         */
        Set<InetSocketAddress> hasVisit = Collections.newSetFromMap(new ConcurrentHashMap<>());
        /**
         * 已经访问过的种子节点
         */
        Set<InetSocketAddress> hasGetPeer = Collections.newSetFromMap(new ConcurrentHashMap<>());
        /**
         * 该次任务是否已经结束
         */
        boolean isShutDown;
        private Logger logger = Logger.getLogger(LogSetting.DEFAULT_NAME);
        private PeerFinder config;
        private byte[] infoHash;
        private IMetaDataDownloader metaDataDownloader;
        /**
         * 获得种子地址任务的线程池
         */
        private ForkJoinPool forkJoinPool;

        /**
         * 下载种子任务的线程池
         */
        private ForkJoinPool downloadForkJoinPool;

        private DoFinder(PeerFinder config, byte[] infoHash, IMetaDataDownloader metaDataDownloader) {
            this.config = config;
            this.infoHash = infoHash;
            this.metaDataDownloader = metaDataDownloader;
            forkJoinPool = new ForkJoinPool(config.threadCount);
            downloadForkJoinPool = new ForkJoinPool(config.downloadThreadCount);
            isShutDown = false;
        }

        /**
         * 处理返回内容的NODES信息,如果返回的节点未访问过,添加到下一轮访问
         *
         * @param response get_peers 的返回内容
         */
        void dealNodes(BencodeMap response) {
            try {
                BencodeMap r = response.getBencodeMap(R);
                ByteBuffer byteBuffer = ByteBuffer.wrap(r.getByteArray(NODES));
                byte[] id = new byte[20];
                byte[] host = new byte[4];
                while (byteBuffer.hasRemaining()) {
                    byteBuffer.get(id);
                    byteBuffer.get(host);
                    int port = 0xffff & byteBuffer.getShort();
                    InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(host), port);
                    if (!hasVisit.contains(addr)) {
                        waitToVisit.add(addr);
                    }
                }
            } catch (RuntimeException | UnknownHostException e) {
                //
            }
        }

        /**
         * 判断是否应该结束
         *
         * @return 是否应该结束本次任务
         */
        boolean shouldShutDown() {
            if (hasVisit.size() >= config.maxVisitCount) {
                return true;
            }
            return isShutDown;
        }

        /**
         * 访问某dht节点,发送getPeer请求,如果返回有种子的节点,尝试下载
         *
         * @param address dht节点地址
         * @return 下载种子成功, 返回非空数组, 里面是种子的Info信息;中断时,返回空数组;没有找到,返回null
         */
        byte[] getInfoHashFromAddress(InetSocketAddress address) {
            if (shouldShutDown()) {
                return EMPTY_ARRAY;
            }
            try {
                hasVisit.add(address);
                BencodeMap response = config.dhtAction.getPeers(infoHash, address, Timeout.getMilliSecond(config.getPeerTimeout));
                dealNodes(response);
                return dealValues(response);
            } catch (SocketTimeoutException e) {
                logger.log(Level.FINE, address + " 连接超时");
            } catch (Exception e) {
                logger.log(Level.WARNING, "连接失败", e);
            }
            return null;
        }

        byte[] dealValues(BencodeMap response) throws ExecutionException, InterruptedException, UnknownHostException {
            List<InetSocketAddress> address = config.dhtAction.parserGetPeersResultValue(response);
            if (address.isEmpty()) {
                return null;
            }
            Optional<byte[]> result = downloadForkJoinPool.submit(() -> address.parallelStream()
                    .filter(addr -> !hasGetPeer.contains(addr))
                    .map(addr -> {
                                try {
                                    if (shouldShutDown()) {
                                        return EMPTY_ARRAY;
                                    }
                                    hasGetPeer.add(addr);
                                    return metaDataDownloader.downloadMataData(infoHash, addr);
                                } catch (Exception e) {
                                    logger.log(Level.FINE, addr + " 下载失败 " + e.getMessage());
                                    return shouldShutDown() ? EMPTY_ARRAY : null;
                                }
                            }
                    )
                    .filter(Objects::nonNull)
                    .findAny())
                    .get();
            return result.orElse(null);
        }

        void initWaitVisit() {
            config.bucket.get(new BencodeByteArray(infoHash), config.initVisitCount).forEach(
                    o -> waitToVisit.add(o.getData())
            );
        }

        private void shutDown() {
            isShutDown = true;
        }

        byte[] doFind() throws ExecutionException, InterruptedException {
            initWaitVisit();


            while (!shouldShutDown()) {
                Set<InetSocketAddress> addressHashSet = new HashSet<>(waitToVisit);
                if (addressHashSet.isEmpty()) {
                    shutDown();
                    break;
                }

                waitToVisit.clear();
                Optional<byte[]> submit = forkJoinPool.submit(
                        () -> addressHashSet.parallelStream()
                                .map(this::getInfoHashFromAddress)
                                // 只有在非空的时候提前结束 如果数组为空,代表未下载种子但是 shouldShutDown()返回了true
                                .filter(Objects::nonNull)
                                .findAny()).get();
                if (submit.isPresent()) {
                    byte[] bytes = submit.get();
                    if (bytes.length > 0) {
                        isShutDown = true;
                        logger.fine("info信息下载完成");
                        return bytes;
                    }
                }
            }

            throw new RuntimeException("未找到种子");
        }
    }
}
