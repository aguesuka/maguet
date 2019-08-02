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
    @Config("dhtAction.getPeerCount")
    private int initVisitCount;
    @Config("PeerFinder.thread.count")
    private int threadCount;
    @Config("PeerFinder.downloadThread")
    private int downloadThreadCount;
    @Config("PeerFinder.getPeerTimeout")
    private int getPeerTimeout;
    @Config("PeerFinder.maxVisitCount")
    private int maxVisitCount;

    @Override
    public byte[] findPeer(byte[] infoHash, IMetaDataDownloader metaDataDownloader) throws ExecutionException, InterruptedException {
        return new DoFinder(this, infoHash, metaDataDownloader).doFind();
    }

    private static class DoFinder {
        private static final byte[] EMPTY_ARRAY = new byte[0];
        volatile Set<InetSocketAddress> waitToVisit = Collections.newSetFromMap(new ConcurrentHashMap<>());
        volatile Set<InetSocketAddress> hasVisit = Collections.newSetFromMap(new ConcurrentHashMap<>());
        volatile Set<InetSocketAddress> hasGetPeer = Collections.newSetFromMap(new ConcurrentHashMap<>());
        boolean isShutDown;
        private Logger logger = Logger.getLogger(LogSetting.DEFAULT_NAME);
        private PeerFinder config;
        private byte[] infoHash;
        private IMetaDataDownloader metaDataDownloader;
        private ForkJoinPool forkJoinPool;
        private ForkJoinPool downloadForkJoinPool;

        private DoFinder(PeerFinder config, byte[] infoHash, IMetaDataDownloader metaDataDownloader) {
            this.config = config;
            this.infoHash = infoHash;
            this.metaDataDownloader = metaDataDownloader;
            forkJoinPool = new ForkJoinPool(config.threadCount);
            downloadForkJoinPool = new ForkJoinPool(config.downloadThreadCount);
            isShutDown = false;
        }

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

        boolean shouldShutDown() {
            if (hasVisit.size() >= config.maxVisitCount) {
                return true;
            }
            return isShutDown;
        }

        byte[] getInfoHashFromAddress(InetSocketAddress address) {
            if (shouldShutDown()) {
                return EMPTY_ARRAY;
            }
            try {

                BencodeMap response = config.dhtAction.getPeers(infoHash, address, Timeout.getMilliSecond(config.getPeerTimeout));
                dealNodes(response);
                byte[] result = dealValues(response);
                hasVisit.add(address);
                return result;
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
                                    logger.log(Level.WARNING, addr + " 下载失败 " + e.getMessage(),e);
                                    return null;
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
                                .filter(Objects::nonNull)
                                .findAny()).get();
                if (submit.isPresent()) {
                    byte[] bytes = submit.get();
                    if (bytes.length > 0) {
                        isShutDown = true;
                        return bytes;
                    }
                }
            }

            throw new RuntimeException("未找到种子");
        }
    }
}
