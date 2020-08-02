package cc.aguesuka.maguet.peer;


import cc.aguesuka.maguet.util.net.EventLoop;

import java.net.SocketAddress;
import java.time.Duration;

public interface PeerDownloadTask {
    static PeerDownloadTask.Builder builder() {
        return new PeerDownloadTaskImpl.BuilderImpl();
    }

    void cancel();

    Progress getProgress();

    enum Progress {
        INIT,
        CONNECT,
        HANDSHAKE,
        GET_PEER_INFO,
        DOWNLOAD_METADATA,
        CHECK_HASH,
        COMPLETE
    }


    interface Observer {

        void onCompete(byte[] metadata);

        void onFail(String reason);

        void onThrow(Throwable throwable);

        void onSelected();

        void beforeCallback();
    }

    interface Builder {
        Builder infoHash(byte[] infoHash);

        Builder address(SocketAddress address);

        Builder selfNodeId(byte[] selfNodeId);

        Builder observer(Observer observer);

        Builder eventLoop(EventLoop eventLoop);

        Builder timeout(Duration timeout);

        Builder connectTimeout(Duration connectTimeout);

        Builder readTimeout(Duration readTimeout);

        Builder readBufferSize(int readBufferSize);

        Builder writeBufferSize(int writeBufferSize);

        PeerDownloadTask build();
    }

}
