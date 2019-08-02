package cc.aguesuka.dht.action;

import cc.aguesuka.dht.connection.Bucket;
import cc.aguesuka.dht.connection.DhtConnectionImpl;
import cc.aguesuka.dht.connection.DhtRequest;
import cc.aguesuka.downloader.impl.DoMetaDataDownLoader;
import cc.aguesuka.util.bencode.BencodeByteArray;
import cc.aguesuka.util.bencode.BencodeMap;
import cc.aguesuka.util.bencode.IBencode;
import cc.aguesuka.util.inject.annotation.Config;
import cc.aguesuka.util.inject.annotation.Init;
import cc.aguesuka.util.inject.annotation.Inject;
import cc.aguesuka.util.log.LogSetting;
import cc.aguesuka.util.stop.Timeout;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cc.aguesuka.dht.token.KrpcToken.*;

/**
 * @author :yangmingyuxing
 * 2019/7/7 20:10
 */
public class DhtAction {
    private static Logger logger = Logger.getLogger(LogSetting.DEFAULT_NAME);
    @Inject
    Bucket<BencodeByteArray, InetSocketAddress> bucket;
    @Inject
    DhtConnectionImpl connection;
    @Config("dhtAction.timeout")
    private int timeout;
    @Config("bucket.self.id")
    private byte[] selfId;
    @Config("dhtAction.initAddress")
    private String initAddress;

    private InetSocketAddress parserAddress(ByteBuffer byteBuffer) throws UnknownHostException {
        byte[] host = new byte[4];
        byteBuffer.get(host);
        int port = 0xffff & byteBuffer.getShort();
        return new InetSocketAddress(InetAddress.getByAddress(host), port);
    }

    public List<InetSocketAddress> parserGetPeersResultValue(BencodeMap response) throws UnknownHostException {
        List<InetSocketAddress> result = new ArrayList<>();
        BencodeMap r = response.getBencodeMap(R);
        if (r.containsKey(VALUES)) {
            for (IBencode addr : r.getBencodeList(VALUES)) {
                byte[] bytes = ((BencodeByteArray) addr).getBytes();
                result.add(parserAddress(ByteBuffer.wrap(bytes)));
            }
        }
        return result;
    }

    @Init
    private void init() {
        logger.fine("开始初始化node");
        String splitChar = ";";
        for (String s : initAddress.split(splitChar)) {
            if (s.length() == 0) {
                break;
            }
            String[] address = s.split(":");
            InetSocketAddress socketAddress = new InetSocketAddress(address[0], Integer.parseInt(address[1]));
            try {
                findNodeRequest(socketAddress, selfId);
            } catch (IOException | RuntimeException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    private void findNodeRequest(InetSocketAddress socketAddress, byte[] peerId)
            throws IOException {

        BencodeMap msg = DhtRequest.findNode(selfId, peerId);
        BencodeMap response = connection.sendAndRecv(socketAddress, msg, Timeout.getMilliSecond(timeout));
        BencodeMap r = response.getBencodeMap(R);
        byte[] nodesArray = r.getByteArray(NODES);
        ByteBuffer wrap = ByteBuffer.wrap(nodesArray);
        while (wrap.hasRemaining()) {
            byte[] id = new byte[20];
            wrap.get(id);
            byte[] host = new byte[4];
            wrap.get(host);
            int port = 0xffff & wrap.getShort();
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(host), port);
            bucket.update(new BencodeByteArray(id), addr, true);
        }


    }

    public BencodeMap getPeers(byte[] peerId, InetSocketAddress address, Timeout timeout) throws IOException {
        return connection.sendAndRecv(address, DhtRequest.getPeer(selfId, peerId), timeout);
    }


}
