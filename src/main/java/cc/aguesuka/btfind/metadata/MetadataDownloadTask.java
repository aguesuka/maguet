package cc.aguesuka.btfind.metadata;

import cc.aguesuka.btfind.app.Application;
import cc.aguesuka.btfind.dht.model.DhtMessage;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * @author :yangmingyuxing
 * 2020/2/20 11:48
 */
public class MetadataDownloadTask {
    private boolean running = true;
    private Task task;

    public MetadataDownloadTask(Application application) {
        task = Task.of(application);
        application.getEventLoop().interval(4000, () -> running, () -> {
            if (task != null && !task.isAlive()) {
                task.stop();
                // odl task will gc after get_peers query callback or timeout
                task = Task.of(application);
            }

            if (task == null) {
                running = false;
            } else {
                task.startDownloader();
            }
        });
    }


    private static class Task {
        private Application application;
        private MetadataDownloaderGroup group;
        private Set<SocketAddress> hasGetPeers = new HashSet<>();
        private Set<SocketAddress> hasDownload = new HashSet<>();

        private Task(Application application, MetadataDownloaderGroup group) {
            this.application = application;
            this.group = group;
        }

        private static Task of(Application application) {
            MetadataDownloaderGroup group = application.createGroup();
            if (group == null) {
                return null;
            }
            return new Task(application, group);
        }

        private boolean isAlive() {
            return group.isAlive() && (System.currentTimeMillis() - group.getCreateTime()) < 120_000;
        }

        private void sendGetPeerQuery(SocketAddress address) {
            hasGetPeers.add(address);
            Map<String, Object> query = application.getMessageUtil()
                    .getPeers(application.getSelfNodeId(), group.getInfoHash());

            application.getQuerySender().send(new DhtMessage(address, query), msg -> {


                List<InetSocketAddress> values = msg.values();
                if (isAlive() && hasDownload.add(address)) {
                    for (InetSocketAddress addr : values) {
                        application.runTask(group, addr);
                    }
                }

            });
        }

        private void startDownloader() {
            application.getRoutingTable().coldDownNodes(120_000, Integer.MAX_VALUE)
                    .stream()
                    .filter(n -> !hasGetPeers.contains(n.getNode().getAddress()))
                    .min(Comparator.comparing(n -> n.getNode().xor(group.getInfoHash()),
                            Arrays::compareUnsigned))
                    .map(n -> n.getNode().getAddress())
                    .ifPresent(this::sendGetPeerQuery);
        }

        private void stop() {
            group.stop();
            hasDownload.clear();
            hasGetPeers.clear();
        }
    }
}
