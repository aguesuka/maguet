package cc.aguesuka.maguet.peer;

import java.nio.ByteBuffer;

/**
 * save peer piece info for download metadata with <a href="http://www.bittorrent.org/beps/bep_0009.html">bep_0009</a>
 *
 * @author :yangmingyuxing
 * 2019/12/8 14:01
 */
public final class PeerPieceInfo {
    private final int totalPiece;
    private final ByteBuffer peer;
    private final byte wireNum;
    private int concurrentPiece;

    /**
     * create by <a href="http://www.bittorrent.org/beps/bep_0009.html#extension-header">bep_0009 extension header</a>,
     * Example extension handshake message:
     * <pre>
     * {'m': {'ut_metadata', 3}, 'metadata_size': 31235}
     * </pre>
     * size = 31235, wireNum = 3
     *
     * @param size    metadata_size
     * @param wireNum m.ut_metadata
     */
    public PeerPieceInfo(int size, byte wireNum) {
        peer = ByteBuffer.allocate(size);
        totalPiece = (int) Math.ceil(size / (1024 * 16.0));
        this.wireNum = wireNum;
        concurrentPiece = 0;
    }

    /**
     * check state and return peer is download over
     *
     * @return peer is download over
     */
    public boolean isComplete() {
        if (concurrentPiece > totalPiece) {
            throw new IllegalStateException("download fail:concurrentPiece > totalPiece");
        }
        return concurrentPiece == totalPiece;
    }

    /**
     * recv msg <a href="http://www.bittorrent.org/beps/bep_0009.html#data">bep_0009 data</a>,
     * Example:
     * <pre>
     * {'msg_type': 1, 'piece': 0, 'total_size': 3425}
     * d8:msg_typei1e5:piecei0e10:total_sizei34256eexxxxxxxx...
     *  </pre>
     * The x represents binary data (the metadata).
     *
     * @param pieceBuffer buffer with remaining metadata
     */
    @SuppressWarnings("SpellCheckingInspection")
    public void writePiece(ByteBuffer pieceBuffer) {
        peer.put(pieceBuffer);
        concurrentPiece++;
    }

    /**
     * concurrent piece for
     * <a href="http://www.bittorrent.org/beps/bep_0009.html#request">bep_0009 request</a>,
     * <pre>
     * {'msg_type': 0, 'piece': 0}
     * d8:msg_typei0e5:piecei0ee
     * </pre>
     *  piece = concurrentPiece
     * @return concurrent piece
     */
    @SuppressWarnings("SpellCheckingInspection")
    public int getConcurrentPiece() {
        return this.concurrentPiece;
    }

    /**
     * m.ut_metadata
     *
     * @return wireNum
     * @see PeerPieceInfo#PeerPieceInfo(int, byte)
     */
    public byte getWireNum() {
        return wireNum;
    }

    /**
     * get metadata
     *
     * @return byte of metadata
     * @throws IllegalStateException not complete
     */
    public byte[] getInfo() {
        if (!isComplete()) {
            throw new IllegalStateException("not complete");
        }
        return peer.array();
    }

}
