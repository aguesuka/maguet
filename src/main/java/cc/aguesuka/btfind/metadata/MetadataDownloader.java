package cc.aguesuka.btfind.metadata;

import cc.aguesuka.btfind.metadata.model.PeerPieceInfo;
import cc.aguesuka.btfind.socket.AsyncTcpConnection;
import cc.aguesuka.btfind.util.ByteUtil;
import cc.aguesuka.btfind.util.HexUtil;
import cc.aguesuka.btfind.util.TimeoutListener;
import cc.aguesuka.btfind.util.bencode.Bencode;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author :yangmingyuxing
 * 2020/2/8 12:32
 */
public class MetadataDownloader {
    /**
     * handshake message
     */
    private static final byte[] HANDSHAKE_BYTES = new byte[]{
            // length of string "BitTorrent protocol"
            19,
            // string "BitTorrent protocol"
            66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
            111, 116, 111, 99, 111, 108,
            // support extend protocol
            0, 0, 0, 0, 0, 16, 0, 1};

    /**
     * d1:md11:ut_metadatai1eee
     * {m:{ut_metadata:1}}
     */
    private static final byte[] MY_SUPPORT =
            HexUtil.decode("0000001A140064313A6D6431313A75745F6D657461646174616931656565");
    private MetadataDownloaderGroup group;
    private AsyncTcpConnection connection;
    private PeerPieceInfo pieceInfo;

    private InetSocketAddress address;
    private Consumer<MetadataDownloader> onComplete;
    private Consumer<MetadataDownloader> onClose;
    private BiConsumer<MetadataDownloader, Throwable> onException;
    private Consumer<MetadataDownloader> onTimeout;
    private Progress progress = Progress.CONNECTING;
    private Timeout timeout = new Timeout();

    public MetadataDownloader(MetadataDownloaderGroup group, InetSocketAddress address) {
        this.group = group;
        this.address = address;
    }

    public MetadataDownloader setOnComplete(Consumer<MetadataDownloader> onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    public MetadataDownloader setOnClose(Consumer<MetadataDownloader> onClose) {
        this.onClose = onClose;
        return this;
    }

    public MetadataDownloader setOnException(BiConsumer<MetadataDownloader, Throwable> onException) {
        this.onException = onException;
        return this;
    }

    public MetadataDownloader setOnTimeout(Consumer<MetadataDownloader> onTimeout) {
        this.onTimeout = onTimeout;
        return this;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public Progress getProgress() {
        return progress;
    }

    public PeerPieceInfo getPieceInfo() {
        return pieceInfo;
    }

    public void connect() {
        connection = AsyncTcpConnection.of(this::failed, this::onClose, group::isAlive);
        timeout.setTimeout(9000);
        group.aliveCount++;
        connection.connect(group.eventLoop, address, this::sendHandshake);
    }

    private void sendHandshake() {
        progress = Progress.HANDSHAKING;
        timeout.resetTimeout(5000);
        connection.write(HANDSHAKE_BYTES).write(group.infoHash).write(group.selfNodeId)
                .send(this::recvHandshake);
    }

    private void recvHandshake() {
        timeout.resetTimeout(5000);
        connection.recvBytes(68, bytes -> sendMySupport());
    }

    private void sendMySupport() {
        progress = Progress.GETTING_PEER_INFO;
        timeout.resetTimeout(5000);
        connection.write(MY_SUPPORT)
                .send(this::recvPeerInfo);
    }

    private void recvPeerInfo() {
        timeout.resetTimeout(5000);
        recvPeerWire(bytes -> {
            Map<String, Object> response = Bencode.parse(ByteBuffer.wrap(bytes).position(2));

            Number wireNum = ((Number) ((Map<?, ?>) response.get("m")).get("ut_metadata"));
            if (wireNum == null) {
                throw new MetadataDownloadException("not has 'ut_metadata'");
            }
            Number size = ((Number) response.get("metadata_size"));
            if (size == null) {
                throw new MetadataDownloadException("not has 'metadata_size'");
            } else if (size.longValue() <= 0) {
                throw new MetadataDownloadException("'metadata_size' <= 0");
            }
            pieceInfo = new PeerPieceInfo(size.intValue(), wireNum.byteValue());
            downloadPrice();
        });
    }

    private void downloadPrice() {
        progress = Progress.DOWNLOADING;
        timeout.resetTimeout(8000);
        if (pieceInfo.isComplete()) {
            doComplete();
            return;
        }
        Map<String, Object> query = new HashMap<>(2);
        query.put("msg_type", 0);
        query.put("piece", pieceInfo.getConcurrentPiece());
        byte[] queryBytes = Bencode.toBytes(query);
        writePeerWireMessage(pieceInfo.getWireNum(), queryBytes);
        connection.send(() -> recvPeerWire(msg -> {
            ByteBuffer pieceBuffer = ByteBuffer.wrap(msg).position(2);
            Bencode.parse(pieceBuffer);
            pieceInfo.writePiece(pieceBuffer);
            downloadPrice();
        }));
    }

    private void writePeerWireMessage(byte wireNum, byte[] msg) {
        connection.writeInt(msg.length + 2).write((byte) 0x14).write(wireNum).write(msg);
    }

    private void doComplete() {
        byte[] info = pieceInfo.getInfo();
        byte[] infoHash = ByteUtil.sha1(info);
        if (!Arrays.equals(infoHash, group.infoHash)) {
            progress = Progress.CHECK_FAILED;
        } else {
            progress = Progress.COMPETED;
            if (onComplete != null) {
                group.isAlive = false;
                onComplete.accept(this);
            }
        }
    }

    private void recvPeerWire(Consumer<byte[]> callback) {
        connection.recvInt(msgLength -> {
            if (msgLength == 0) {
                recvPeerWire(callback);
                return;
            }
            connection.recvBytes(msgLength, msg -> {
                if (msg[0] == 0x14) {
                    callback.accept(msg);
                } else {
                    recvPeerWire(callback);
                }
            });
        });
    }

    private void failed(Throwable throwable) {
        if (onException != null) {
            onException.accept(this, throwable);
        }
    }

    private void onClose() {
        timeout.cancelTimeout();
        if (onClose != null) {
            group.aliveCount--;
            onClose.accept(this);
        }
    }

    private void cancel() {
        connection.close();
    }

    public enum Progress {
        CONNECTING,
        HANDSHAKING,
        GETTING_PEER_INFO,
        DOWNLOADING,
        CHECK_FAILED,
        COMPETED,
        ;
    }

    public MetadataDownloaderGroup getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MetadataDownloader.class.getSimpleName() + "[", "]")
                .add("address=" + getAddress())
                .add("progress=" + getProgress())
                .toString();
    }

    public class Timeout implements TimeoutListener {
        private long timeoutIndex;

        @Override
        public void timeout() {
            if (onTimeout != null) {
                onTimeout.accept(MetadataDownloader.this);
            }
            cancel();
        }

        private void setTimeout(int timeout) {
            timeoutIndex = group.eventLoop.timeout(timeout, this);
        }

        private void cancelTimeout() {
            group.eventLoop.cancelTimeout(timeoutIndex, this);
        }

        private void resetTimeout(int timeout) {
            cancelTimeout();
            setTimeout(timeout);
        }
    }
}
