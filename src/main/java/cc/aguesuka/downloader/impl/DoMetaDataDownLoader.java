package cc.aguesuka.downloader.impl;

import cc.aguesuka.downloader.IMetaDataDownloader;
import cc.aguesuka.util.ByteUtil;
import cc.aguesuka.util.HexUtil;
import cc.aguesuka.util.bencode.Bencode;
import cc.aguesuka.util.bencode.BencodeMap;
import cc.aguesuka.util.inject.annotation.Config;
import cc.aguesuka.util.log.LogSetting;
import cc.aguesuka.util.stop.ShutdownFlag;
import cc.aguesuka.util.stop.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * @author :yangmingyuxing
 * 2019/7/14 23:07
 */
public class DoMetaDataDownLoader implements IMetaDataDownloader {

    @Config("bucket.self.id")
    private byte[] id;
    @Config("peerDownload.timeout")
    private int timeout;
    @Config("DoMetaDataDownLoader.connectionTimeout")
    private int connectionTimeout;

    @Override
    public byte[]
    downloadMataData(byte[] infoHash, InetSocketAddress address) throws IOException {
        return new DownLoader(this, address, infoHash).downloadMataData();
    }

    private static class DownLoader {
        private static final byte[] HANDSHAKE_BYTES = {
                // 19 代表长度
                19,
                // BitTorrent protocol 协议名称
                66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
                111, 116, 111, 99, 111, 108,
                //
                0, 0, 0, 0, 0, 16, 0, 1};
        byte[] infoHash;
        private Logger logger = Logger.getLogger(LogSetting.DEFAULT_NAME);
        private DoMetaDataDownLoader config;
        private Socket socket;
        private InetSocketAddress address;
        private ShutdownFlag shutdownFlag = new ShutdownFlag();
        private int utMetadata;
        private int metadataSize;
        private Timeout timeout;

        DownLoader(DoMetaDataDownLoader config, InetSocketAddress address, byte[] infoHash) {
            this.config = config;
            this.address = address;
            this.infoHash = infoHash;
        }

        private void send(byte[] msg) throws IOException {
            logger.fine("send message:" + address + " " + HexUtil.encode(msg));
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(msg);
            outputStream.flush();
        }

        private Socket newSocket() throws IOException {
            Socket socket = new Socket();
            socket.setSoTimeout(config.connectionTimeout);
            socket.connect(address, config.connectionTimeout);
            return socket;
        }

        private byte[] downloadMataData() throws IOException {
            logger.info("开始下载:" + address);
            this.timeout = Timeout.getMilliSecond(config.timeout);
            try (Socket socket = newSocket()) {
                this.socket = socket;
                // 握手并获得种子信息和对方支持的协议
                sendHandShake();
                recvHandShake();
                recvPeerInfo();
                // 发送本地支持的协议
                sendMySupport();
                // 下载种子
                return downloadPeer();
            }
        }

        private void sendHandShake() throws IOException {
            byte[] msg = new byte[68];
            System.arraycopy(HANDSHAKE_BYTES, 0, msg, 0, 28);
            System.arraycopy(infoHash, 0, msg, 28, 20);
            System.arraycopy(config.id, 0, msg, 48, 20);
            send(msg);
        }

        private void recvHandShake() throws IOException {
            // jdk 1.8
            recvByCount(68);
             /* jdk 1.9
            byte[] bytes = recvByCount(68);

            int checkLength = 20;

            if (!Arrays.equals(HANDSHAKE_BYTES, 0, checkLength, bytes, 0, checkLength)) {
                throw new IOException("recv hand shake error");
            }
            */
        }

        private void recvPeerInfo() throws IOException {
            byte[] message = recvExtendedMessage0x14();
            BencodeMap bencodeMap = Bencode.parse(ByteBuffer.wrap(message, 2, message.length - 2));
            utMetadata = (int) bencodeMap.getBencodeMap("m").getLong("ut_metadata");
            metadataSize = (int) bencodeMap.getLong("metadata_size");
            logger.fine("metadataSize = " + metadataSize);
            if (utMetadata < 0 || metadataSize < 0) {
                throw new IOException("recv peer info error");
            }
        }

        private byte[] recvExtendedMessage0x14() throws IOException {
            for (; ; ) {

                byte[] message = recvExtendedMessage();
                if (message[0] == 0x14) {
                    return message;
                }
            }
        }

        private byte[] recvExtendedMessage() throws IOException {
            int length = ByteUtil.intValue(recvByCount(4));
            return recvByCount(length);
        }

        private byte[] recvByCount(int count) throws IOException {
            timeout.checkTimeout();
            InputStream inputStream = socket.getInputStream();
            byte[] result = new byte[count];
            int readCount = 0;
            while (readCount < count) {
                timeout.checkTimeout();
                shutdownFlag.check();
                int read = inputStream.read(result, readCount, count - readCount);
                if(read == -1){
                    throw new IOException("连接已经关闭");
                }
                readCount += read;
            }
            logger.fine("recv message:" + address + " " + HexUtil.encode(result));
            return result;

        }


        private byte[] downloadPeer() throws IOException {
            byte[] info = new byte[metadataSize];
            int pieceCount = (int) Math.ceil(metadataSize / (1024 * 16.0));
            int offset = 0;
            for (int pieceNum = 0; pieceNum < pieceCount; pieceNum++) {
                offset += downloadPiece(pieceNum, info, offset);
            }

            return info;
        }

        private void sendMySupport() throws IOException {
            BencodeMap m = new BencodeMap();
            m.putLong("ut_metadata", 1);
            BencodeMap message = new BencodeMap();
            message.put("m", m);
            sendExtended(0, message);
        }

        private int downloadPiece(int pieceNum, byte[] toByteArray, int offset) throws IOException {
            logger.fine("正在下载第:" + pieceNum + " 块");
            BencodeMap bencode = new BencodeMap();
            bencode.putLong("msg_type", 0);
            bencode.putLong("piece", pieceNum);
            sendExtended(utMetadata, bencode);
            byte[] recv = recvExtendedMessage0x14();
            ByteBuffer buffer = ByteBuffer.wrap(recv, 2, recv.length - 2);
            Bencode.parse(buffer);
            int remaining = buffer.remaining();
            buffer.get(toByteArray, offset, remaining);
            return remaining;
        }

        private void sendExtended(int metadataNum, BencodeMap messageMap) throws IOException {
            logger.fine("send extended = " + messageMap);
            byte[] message = messageMap.toBencodeBytes();
            ByteBuffer msg = ByteBuffer.allocate(message.length + 6);
            msg.putInt(message.length + 2);
            msg.put((byte) 0x14);
            msg.put((byte) metadataNum);
            msg.put(message);
            send(msg.array());
        }
    }

}
