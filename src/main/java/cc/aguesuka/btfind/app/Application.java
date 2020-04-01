package cc.aguesuka.btfind.app;

import cc.aguesuka.btfind.app.config.ApplicationConfig;
import cc.aguesuka.btfind.command.ForShell;
import cc.aguesuka.btfind.dht.client.*;
import cc.aguesuka.btfind.dht.dao.DhtNodeDao;
import cc.aguesuka.btfind.dht.model.DhtMessageUtil;
import cc.aguesuka.btfind.dht.routingtable.RoutingTable;
import cc.aguesuka.btfind.dispatch.EventLoop;
import cc.aguesuka.btfind.metadata.MetadataDao;
import cc.aguesuka.btfind.metadata.MetadataDownloadException;
import cc.aguesuka.btfind.metadata.MetadataDownloader;
import cc.aguesuka.btfind.metadata.MetadataDownloaderGroup;
import cc.aguesuka.btfind.socket.AsyncTcpException;
import cc.aguesuka.btfind.util.CountMap;
import cc.aguesuka.btfind.util.HexUtil;
import cc.aguesuka.btfind.util.NumberLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author :yangmingyuxing
 * 2020/2/17 15:42
 */
public class Application {
    private static final String DOWNLOAD_COST = "download cost";
    private final DhtNodeDao nodeDao = DhtNodeDao.instance();
    private final DhtMessageUtil messageUtil = DhtMessageUtil.instance();
    private final EventLoop eventLoop;
    private final RoutingTable routingTable;
    private final DhtQuerySender querySender;
    private final MetadataDao metadataDao = MetadataDao.instance();
    private final Queue<byte[]> infoHashQueue = new ArrayDeque<>();
    private final NumberLogger numberLogger;
    private final ApplicationConfig applicationConfig;
    @ForShell
    private final long startTime = System.currentTimeMillis();

    public Application(EventLoop eventLoop, int port, ApplicationConfig applicationConfig) throws IOException {
        this.eventLoop = eventLoop;
        this.applicationConfig = applicationConfig;
        this.numberLogger = new NumberLogger();

        DhtClient client = new DhtClient(eventLoop, port);
        routingTable = new RoutingTable(client);
        querySender = new DhtQuerySender(client);
        client.addListener(new DhtResponder(getSelfNodeId()));
        client.addListener(DhtCountLogger.of(numberLogger.getCountMap()));
        client.addListener(new DhtListener() {
            @Override
            public void onException(Throwable throwable) {
                numberLogger.getCountMap().put("dht", throwable.getClass().getName(), throwable.getMessage());
            }
        });
        reloadInfoHashQueue();
    }

    @ForShell
    public String info() {
        long costTime = System.currentTimeMillis() - startTime;
        CountMap countMap = numberLogger.getCountMap();
        long successCount = countMap.get("complete", MetadataDownloader.Progress.COMPETED.toString());
        long createGroup = countMap.get("create group");
        float costMin = (costTime / (1000f * 60));
        float downloadOfMin = successCount / Math.max(0.01f, costMin);
        float createOfMin = createGroup / costMin;
        float successRate = createGroup == 0f ? 0f : successCount / (float) createGroup;
        return "cost time=" + costMin + "(min)\n" +
                "create task=" + createGroup +
                "\nsuccessCount=" + successCount +
                "\ndownload speed=" + downloadOfMin + "(/min)" +
                "\ncreate speed=" + createOfMin + "(/min)" +
                "\nsuccess rate=" + successRate * 100 + "%\n";


    }

    private void reloadInfoHashQueue() {
        try {
            infoHashQueue.clear();
            List<String> infoHashList = Files.readAllLines(applicationConfig.getInfoHashPath());
            Set<String> downloadedSet;
            try (Stream<Path> walk = Files.walk(applicationConfig.getMetadataDirPath())) {

                downloadedSet = walk.filter(path -> !Files.isDirectory(path))
                        .map(path -> path.getFileName().toString().split("\\.")[0])
                        .collect(Collectors.toSet());
            }
            List<byte[]> collect = infoHashList.stream()
                    .filter(row -> row != null && row.length() > 0 && !downloadedSet.contains(row))
                    .map(HexUtil::decode)
                    .filter(bytes -> bytes.length == 20)
                    .collect(Collectors.toList());

            Collections.shuffle(collect);
            infoHashQueue.addAll(collect);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] getSelfNodeId() {
        return applicationConfig.getSelfNodeId();
    }

    public EventLoop getEventLoop() {
        return eventLoop;
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }


    public DhtQuerySender getQuerySender() {
        return querySender;
    }

    public DhtNodeDao getNodeDao() {
        return nodeDao;
    }

    public DhtMessageUtil getMessageUtil() {
        return messageUtil;
    }


    public MetadataDownloaderGroup createGroup() {
        numberLogger.getCountMap().put("create group");
        if (infoHashQueue.isEmpty()) {
            return null;
        }
        byte[] infoHash = infoHashQueue.remove();
        return new MetadataDownloaderGroup(eventLoop, infoHash, getSelfNodeId());
    }

    public void runTask(MetadataDownloaderGroup group, InetSocketAddress address) {
        if (!group.isAlive()) {
            return;
        }

        CountMap countMap = numberLogger.getCountMap();
        new MetadataDownloader(group, address).setOnComplete(downloader -> {
            byte[] info = downloader.getPieceInfo().getInfo();
            metadataDao.save(downloader.getGroup().getInfoHash(), info, applicationConfig.getMetadataDirPath());
            countMap.put("complete", downloader.getProgress().toString());
            numberLogger.getSumMap().put(DOWNLOAD_COST, System.currentTimeMillis() - group.getCreateTime());

        }).setOnException((downloader, throwable) -> {

            if (throwable instanceof AsyncTcpException) {
                AsyncTcpException asyncTcpException = (AsyncTcpException) throwable;
                Throwable cause = asyncTcpException.getCause();
                if (cause != null) {
                    throwable = cause;
                }

            } else if (!(throwable instanceof MetadataDownloadException) && !(throwable instanceof IOException)) {
                throwable.printStackTrace();
            }

            countMap.put("err", downloader.getProgress().toString(),
                    throwable.getClass().toString(), throwable.getMessage());
            countMap.put("err", downloader.getProgress().toString());

        }).setOnClose(downloader -> {
            countMap.put("close");
            countMap.put("close", downloader.getProgress().toString());
        }).setOnTimeout(downloader -> {
            countMap.put("timeout");
            countMap.put("timeout", downloader.getProgress().toString());
        }).connect();

    }

}
