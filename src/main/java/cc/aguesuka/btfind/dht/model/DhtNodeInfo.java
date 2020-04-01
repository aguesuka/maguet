package cc.aguesuka.btfind.dht.model;

/**
 * @author :aguesuka
 * 2020/2/14 10:15
 */
public class DhtNodeInfo {
    private final DhtNode node;
    private final DhtNodeRecord record;

    public DhtNode getNode() {
        return node;
    }

    private DhtNodeInfo(DhtNode node, DhtNodeRecord record) {
        this.node = node;
        this.record = record;
    }


    public static DhtNodeInfo of(DhtNode node) {
        return new DhtNodeInfo(node, new DhtNodeRecord());
    }


    public DhtNodeRecord getRecord() {
        return record;
    }

    @Override
    public String toString() {
        return "DhtNodeInfo[node=" + node + ",record=" + record + "]";
    }

}
