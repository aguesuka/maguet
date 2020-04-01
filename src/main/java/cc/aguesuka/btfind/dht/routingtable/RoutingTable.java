package cc.aguesuka.btfind.dht.routingtable;

import cc.aguesuka.btfind.dht.client.DhtClient;
import cc.aguesuka.btfind.dht.client.DhtListener;
import cc.aguesuka.btfind.dht.model.*;
import cc.aguesuka.btfind.util.TimeoutMap;

import java.net.SocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author :yangmingyuxing
 * 2020/2/14 11:58
 */
public class RoutingTable {

    private Blacklist blacklist = new Blacklist();
    private NodeTable nodeTable = new NodeTable();
    private DhtMessageUtil messageUtil = DhtMessageUtil.instance();

    private TimeoutMap<SocketAddress, DhtNodeInfo> handlerMap = new TimeoutMap<>();

    public RoutingTable(DhtClient dhtClient) {
        dhtClient.addListener(new Listener());
    }

    public List<DhtNodeInfo> coldDownNodes(long coldDown, int limit) {
        return nodeTable.coldDownNodes(coldDown,limit);
    }

    public void refresh() {
        handlerMap.refresh();
        nodeTable.refresh();
        blacklist.refresh();
    }

    public List<DhtNodeInfo> nodes() {
        return new ArrayList<>(nodeTable.nodeMap.values());
    }




    public void putNode(DhtNode node) {
        nodeTable.putNode(node);
    }

    public void putNodeWithResponse(DhtNode node) {
        nodeTable.putNodeWithResponse(node);
    }

    @Override
    public String toString() {
        refresh();
        return new StringJoiner(", ", RoutingTable.class.getSimpleName() + "[", "]")
                .add("blacklist.size()=" + blacklist.addressMap.size())
                .add("nodeTable.size()=" + nodeTable.nodeMap.size())
                .add("handlerMap.size()=" + handlerMap.size())
                .toString();
    }

    private static class NodeTable {
        private Map<SocketAddress, DhtNodeInfo> nodeMap = new HashMap<>();

        private List<DhtNodeInfo> coldDownNodes(long coldDown, int limit) {
            long now = System.currentTimeMillis();
            long maxTime = now - coldDown;
            return nodeMap.values().stream()
                    .filter(nodeRecord -> {
                        DhtNodeRecord record = nodeRecord.getRecord();
                        return record.getLastQueryTime() < maxTime && record.getLastResponseTime() < maxTime;
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        private void putNodeWithResponse(DhtNode dhtNode) {
            putNode(dhtNode).getRecord().onResponse();
        }

        private DhtNodeInfo putNode(DhtNode dhtNode) {
            DhtNodeInfo dhtNodeInfo = get(dhtNode.getAddress());
            if (dhtNodeInfo == null) {
                dhtNodeInfo = DhtNodeInfo.of(dhtNode);
                nodeMap.put(dhtNode.getAddress(), dhtNodeInfo);
            }
            return dhtNodeInfo;
        }

        private DhtNodeInfo get(SocketAddress address) {
            return nodeMap.get(address);
        }

        private void remove(SocketAddress address) {
            nodeMap.remove(address);
        }

        private void refresh() {
            int maxCount = 2_0000;
            int updateCount = maxCount / 2;
            if (nodeMap.size() > maxCount) {
                nodeMap = sortedNodes(updateCount);
            }
        }

        private Map<SocketAddress, DhtNodeInfo> sortedNodes(int count) {
            Comparator<DhtNodeRecord> comparator = Comparator
                    // less recent timeout
                    .<DhtNodeRecord>comparingInt(r -> Math.max(3, r.getRecentTimeoutCount()))
                    // more recent success
                    .thenComparing(Comparator.comparingInt(DhtNodeRecord::getRecentSuccessCount).reversed())
                    // earlier create time
                    .thenComparingLong(DhtNodeRecord::getCreateTime);

            return nodeMap.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getValue().getRecord(), comparator))
                    .limit(count)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public String toString() {
            StringJoiner stringJoiner = new StringJoiner("\n", "[", "]");
            stringJoiner.add("nodeTable:size " + nodeMap.size());
            for (DhtNodeInfo value : nodeMap.values()) {
                stringJoiner.add(value.toString());
            }
            return stringJoiner.toString();
        }

    }

    private class Listener implements DhtListener {
        @Override
        public void beforeSendQuery(DhtMessage msg) {
            DhtNodeInfo dhtNodeInfo = nodeTable.get(msg.getAddress());
            if (dhtNodeInfo != null) {
                dhtNodeInfo.getRecord().onQuery();
                handlerMap.put(msg.getAddress(), dhtNodeInfo, 3000,
                        (address, nodeRecord) -> nodeRecord.getRecord().onTimeout());
            }
        }

        @Override
        public void recvResponse(DhtMessage msg) {
            SocketAddress address = msg.getAddress();

            // if query no send or has deal,ignore
            DhtNodeInfo dhtNodeInfo = handlerMap.getAndRemove(address);
            if (null == dhtNodeInfo) {
                return;
            }

            // if id has changed,add to blacklist
            byte[] id = msg.id();
            if (!dhtNodeInfo.getNode().equalsId(id)) {
                nodeTable.remove(address);
                blacklist.add(address);
                return;
            }

            // update record
            dhtNodeInfo.getRecord().onResponse();

            // save nodes
            List<DhtNode> dhtNodes = messageUtil.parserNodes(msg.nodes());
            for (DhtNode dhtNode : dhtNodes) {
                if (!blacklist.has(dhtNode.getAddress())) {
                    nodeTable.putNode(dhtNode);
                }
            }
        }
    }

    private static class Blacklist {
        private TimeoutMap<SocketAddress, Boolean> addressMap = new TimeoutMap<>();

        private void add(SocketAddress address) {
            addressMap.put(address, Boolean.TRUE, 3600_0000);
        }

        private boolean has(SocketAddress address) {
            return addressMap.get(address) == Boolean.TRUE;
        }

        private void refresh() {
            addressMap.refresh();
        }

        @Override
        public String toString() {
            StringJoiner stringJoiner = new StringJoiner("\n", "[", "]");
            addressMap.refresh();
            stringJoiner.add("Blacklist:size " + addressMap.size());
            for (SocketAddress socketAddress : addressMap.keySet()) {
                stringJoiner.add(socketAddress.toString());
            }
            return stringJoiner.toString();
        }
    }
}
