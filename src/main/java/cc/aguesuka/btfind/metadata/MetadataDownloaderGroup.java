package cc.aguesuka.btfind.metadata;

import cc.aguesuka.btfind.dispatch.EventLoop;

/**
 * @author :aguesuka
 * 2020/2/9 15:12
 */
public class MetadataDownloaderGroup {
    byte[] infoHash;
    byte[] selfNodeId;
    EventLoop eventLoop;
    boolean isAlive = true;
    int aliveCount = 0;
    private long createTime;

    public MetadataDownloaderGroup(EventLoop eventLoop, byte[] infoHash, byte[] selfNodeId) {
        // infoHash is immutable
        this.infoHash = infoHash.clone();
        // selfNodeId is mutable
        this.selfNodeId = selfNodeId;
        this.eventLoop = eventLoop;
        createTime = System.currentTimeMillis();
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void stop() {
        isAlive = false;
    }

    public long getCreateTime() {
        return createTime;
    }
}
