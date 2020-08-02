package cc.aguesuka.maguet.peer;

import cc.aguesuka.maguet.util.ByteUtil;
import cc.aguesuka.maguet.util.HexUtil;
import cc.aguesuka.maguet.util.bencode.Bencode;
import cc.aguesuka.maguet.util.net.EventLoop;
import cc.aguesuka.maguet.util.net.TcpConnection;
import cc.aguesuka.maguet.util.timer.Timeout;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class PeerDownloadTaskImpl implements PeerDownloadTask {

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
     * {@code {m:{ut_metadata:1}}}
     */
    private static final byte[] MY_SUPPORT =
            HexUtil.decode("0000001A140064313A6D6431313A75745F6D657461646174616931656565");
    private final byte[] infoHash;
    private final SocketAddress address;
    private final byte[] selfNodeId;
    private final Observer observer;
    private final EventLoop eventLoop;
    private final Duration timeout;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final int readBufferSize;
    private final int writeBufferSize;

    private boolean working;
    private Progress progress = Progress.INIT;
    private TcpConnection<ConnectionObserver> tcpConnect;
    private Timeout taskTimeoutHolder;
    private Timeout ioTimeoutHolder;
    ByteBuffer readBuffer;
    ByteBuffer writeBuffer;
    private PeerPieceInfo pieceInfo;

    PeerDownloadTaskImpl(BuilderImpl builder) {
        this.eventLoop = builder.eventLoop;
        this.writeBufferSize = builder.writeBufferSize;
        this.observer = builder.observer;
        this.readTimeout = builder.readTimeout;
        this.selfNodeId = builder.selfNodeId;
        this.connectTimeout = builder.connectTimeout;
        this.address = builder.address;
        this.infoHash = builder.infoHash;
        this.timeout = builder.timeout;
        this.readBufferSize = builder.readBufferSize;
        start();
    }

    private static void writePeerWireMessage(ByteBuffer buffer, byte wireNum, byte[] msg) {
        buffer.putInt(msg.length + 2).put((byte) 0x14).put(wireNum).put(msg);
    }

    private void start() {
        working = true;
        taskTimeoutHolder = eventLoop.getTimer().createTimeout(() -> this.fail("TIMEOUT"), timeout);
        connect();
    }

    private void connect() {
        progress = Progress.CONNECT;
        tcpConnect = TcpConnection.of(eventLoop, new ConnectionObserver(this));
        ioTimeoutHolder = eventLoop.getTimer().createTimeout(() -> fail("CONNECT_TIMEOUT"), connectTimeout);
        tcpConnect.connect(address, observer -> observer.task.handshake());
    }

    private void handshake() {
        ioTimeoutHolder.cancel();
        ioTimeoutHolder = eventLoop.getTimer().createTimeout(() -> fail("READ_TIMEOUT"), readTimeout);
        progress = Progress.HANDSHAKE;

        writeBuffer = ByteBuffer.allocate(writeBufferSize);
        readBuffer = ByteBuffer.allocate(readBufferSize);
        writeBuffer.put(HANDSHAKE_BYTES).put(infoHash).put(selfNodeId).flip();
        tcpConnect.setWriteBuffer(writeBuffer);

        tcpConnect.read(readBuffer, 68, observer -> observer.task.recvHandshake());
    }

    private void recvHandshake() {
        readBuffer.flip().position(68).compact();


        ioTimeoutHolder.cancel();
        ioTimeoutHolder = eventLoop.getTimer().createTimeout(() -> fail("READ_TIMEOUT"), readTimeout);
        progress = Progress.GET_PEER_INFO;

        tcpConnect.setWriteBuffer(ByteBuffer.wrap(MY_SUPPORT));
        readPeerWire0x14Message(PeerDownloadTaskImpl::getPeerInfo);
    }

    private void getPeerInfo(byte[] message) {
        ioTimeoutHolder.cancel();
        Map<String, Object> msg = Bencode.parse(ByteBuffer.wrap(message).position(2));

        Number wireNum = ((Number) ((Map<?, ?>) msg.get("m")).get("ut_metadata"));
        if (wireNum == null) {
            fail("NONE_UT_METADATA");
            return;
        }
        Number size = ((Number) msg.get("metadata_size"));
        if (size == null) {
            fail("NONE_METADATA_SIZE");
            return;
        } else if (size.longValue() <= 0) {
            fail("NEGATIVE_METADATA_SIZE");
        }
        pieceInfo = new PeerPieceInfo(size.intValue(), wireNum.byteValue());
        downloadPrice();
    }

    private void downloadPrice() {
        progress = Progress.DOWNLOAD_METADATA;
        ioTimeoutHolder = eventLoop.getTimer().createTimeout(() -> fail("READ_TIMEOUT"), readTimeout);

        if (pieceInfo.isComplete()) {
            downloadOver();
            return;
        }
        Map<String, Object> query = new HashMap<>(2);
        query.put("msg_type", 0);
        query.put("piece", pieceInfo.getConcurrentPiece());
        byte[] queryBytes = Bencode.toBytes(query);
        writePeerWireMessage(writeBuffer.compact(), pieceInfo.getWireNum(), queryBytes);
        tcpConnect.setWriteBuffer(writeBuffer.flip());

        readPeerWire0x14Message((task, msg) -> {
            task.readBuffer.flip();
            ByteBuffer pieceBuffer = ByteBuffer.wrap(msg).position(2);
            Bencode.parse(pieceBuffer);
            pieceInfo.writePiece(pieceBuffer);
            downloadPrice();
        });
    }

    private void downloadOver() {
        progress = Progress.CHECK_HASH;
        byte[] metadata = pieceInfo.getInfo();
        byte[] hash = ByteUtil.sha1(metadata);
        if (!Arrays.equals(hash, infoHash)) {
            fail("CHECK");
            return;
        }
        working = false;
        observer.onCompete(metadata);
        free();
    }

    private void readPeerWire0x14Message(BiConsumer<PeerDownloadTaskImpl, byte[]> action) {
        tcpConnect.read(readBuffer, 4, observer -> {
            int length = readBuffer.flip().getInt();
            if(length == 0){
                readPeerWire0x14Message(action);
            }
            tcpConnect.read(readBuffer.compact(), length, ob -> {
                readBuffer.flip();
                if (readBuffer.array()[0] == 0x14) {
                    byte[] message = new byte[length];
                    readBuffer.get(message).compact();
                    action.accept(ob.task, message);
                } else {
                    readBuffer.position(length).compact();
                    readPeerWire0x14Message(action);
                }
            });
        });
    }


    private void fail(String reason) {
        if (!working) {
            return;
        }
        working = false;
        free();
        observer.onFail(reason);
    }

    @Override
    public void cancel() {
        fail("CANCEL");
    }

    @Override
    public Progress getProgress() {
        return progress;
    }

    private void free() {
        this.tcpConnect.close();
        if (taskTimeoutHolder != null) {
            taskTimeoutHolder.cancel();
        }
        if (ioTimeoutHolder != null) {
            ioTimeoutHolder.cancel();
        }
    }

    static final class BuilderImpl implements Builder {
        byte[] infoHash;
        SocketAddress address;
        byte[] selfNodeId;
        Observer observer;
        EventLoop eventLoop;

        Duration timeout;
        Duration connectTimeout;
        Duration readTimeout;
        int readBufferSize = 16 * 1024;
        int writeBufferSize = 16 * 1024;

        BuilderImpl() {
        }

        @Override
        public Builder infoHash(byte[] infoHash) {
            this.infoHash = infoHash;
            return this;
        }

        @Override
        public Builder address(SocketAddress address) {
            this.address = address;
            return this;
        }

        @Override
        public Builder selfNodeId(byte[] selfNodeId) {
            this.selfNodeId = selfNodeId;
            return this;
        }

        @Override
        public Builder observer(Observer observer) {
            this.observer = observer;
            return this;
        }

        @Override
        public Builder eventLoop(EventLoop eventLoop) {
            this.eventLoop = eventLoop;
            return this;
        }

        @Override
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        @Override
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Override
        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        @Override
        public Builder readBufferSize(int readBufferSize) {
            this.readBufferSize = readBufferSize;
            return this;
        }

        @Override
        public Builder writeBufferSize(int writeBufferSize) {
            this.writeBufferSize = writeBufferSize;
            return this;
        }

        @Override
        public PeerDownloadTask build() {
            return new PeerDownloadTaskImpl(this);
        }
    }

    private static class ConnectionObserver implements TcpConnection.Observer {
        private final PeerDownloadTaskImpl task;

        public ConnectionObserver(PeerDownloadTaskImpl task) {
            this.task = task;
        }

        @Override
        public void onClose() {
            task.fail("CLOSED");
        }

        @Override
        public void handleThrowable(Throwable throwable) {
            task.fail("THROW");
            task.observer.onThrow(throwable);
        }

        @Override
        public void onSelected() {
            task.observer.onSelected();
        }

        @Override
        public void beforeCallback() {
            task.observer.beforeCallback();
        }

        @Override
        public void onEOF() {
            task.fail("EOF");
        }
    }
}
