package cc.aguesuka.dht.connection;

import cc.aguesuka.util.inject.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * @author :yangmingyuxing
 * 2019/7/8 19:33
 */
public class Bucket<T extends Comparable<T>, D> {
    @Config("bucket.self.id")
    byte[] selfId;
    private ConcurrentSkipListMap<T, Node<T, D>> nodeMap = new ConcurrentSkipListMap<>();
    @Config("bucket.max.time")
    private int maxTime;
    @Config("bucket.max.fail")
    private int maxFail;

    public byte[] getSelfId() {
        return selfId;
    }

    public ConcurrentSkipListMap<T, Node<T, D>> getNodeMap() {
        return nodeMap;
    }

    public List<Node<T, D>> get(T t, int count) {
        Iterator<Node<T, D>> head = nodeMap.headMap(t).values().iterator();
        Iterator<Node<T, D>> tail = nodeMap.tailMap(t).values().iterator();
        List<Node<T, D>> collect = new ArrayList<>();
        int halfCount = count / 2;
        for (int i = halfCount; i > 0; i--) {
            if (head.hasNext()) {
                collect.add(head.next());
            }
            if (tail.hasNext()) {
                collect.add(tail.next());
            }
        }
        return collect.stream().filter(node -> {
            if (node.failCount > maxFail || System.currentTimeMillis() - node.lastTime.getTime() > maxTime) {
                nodeMap.remove(node.t);
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    public void update(T t, D d, boolean ok) {
        if (t == null) {
            return;
        }
        if (nodeMap.containsKey(t)) {
            Node node = nodeMap.get(t);
            if (ok) {
                node.onOk();
            } else {
                node.onFail();
            }
        } else if (ok) {
            nodeMap.put(t, new Node<>(t, d));
        }

        if (!ok) {
            Node node = nodeMap.get(t);
            if (node.failCount > maxFail) {
                nodeMap.remove(t);
            }
        }
    }

    public static class Node<T, D> {
        private T t;
        private D data;
        private Date lastTime;
        private int failCount;
        private int okCount;

        Node(T t, D data) {
            this.t = t;
            this.data = data;
            lastTime = new Date();
        }

        public T getT() {
            return t;
        }

        public D getData() {
            return data;
        }

        void onOk() {
            lastTime = new Date();
            failCount = 0;
            okCount++;
        }

        void onFail() {
            lastTime = new Date();
            okCount = 0;
            failCount++;
        }
    }
}
