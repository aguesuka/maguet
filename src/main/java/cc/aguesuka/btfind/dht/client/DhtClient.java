package cc.aguesuka.btfind.dht.client;

import cc.aguesuka.btfind.dht.model.DhtMessage;
import cc.aguesuka.btfind.dispatch.EventLoop;
import cc.aguesuka.btfind.util.bencode.Bencode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author :yangmingyuxing
 * 2020/2/13 10:17
 */
public class DhtClient {
    private Queue<DhtMessage> sendQueryQueue = new ArrayDeque<>();
    private Queue<DhtMessage> sendResponseQueue = new ArrayDeque<>();

    private Consumer<DhtMessage> querySender = msg -> {
        accept(DhtListener::beforeSendQuery, msg);
        this.sendQueryQueue.add(msg);
        this.key.interestOpsOr(SelectionKey.OP_WRITE);
    };
    private Consumer<DhtMessage> responseSender = msg -> {
        accept(DhtListener::beforeSendResponse, msg);
        this.sendResponseQueue.add(msg);
        this.key.interestOpsOr(SelectionKey.OP_WRITE);
    };
    private List<DhtListener> handlerList = new ArrayList<>();
    private SelectionKey key;
    private DatagramChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(1024 * 32);

    public DhtClient(EventLoop eventLoop, int port) throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(port));
        key = eventLoop.register(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this::tryHandle);
    }


    private void tryHandle() {
        try {
            handler();
        } catch (Exception e) {
            for (DhtListener dhtListener : handlerList) {
                dhtListener.onException(e);
            }
        }
    }

    private void handler() throws Exception {
        if (key.isReadable()) {
            doRead();
        } else {
            assert key.isWritable();
            doWrite();
        }
        if (sendQueryQueue.isEmpty() && sendResponseQueue.isEmpty() && key.isValid()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void doWrite() throws IOException {
        DhtMessage msg;
        if (!sendResponseQueue.isEmpty()) {
            msg = sendResponseQueue.remove();
        } else if (!sendQueryQueue.isEmpty()) {
            msg = sendQueryQueue.remove();
        } else {
            return;
        }
        buffer.clear();
        Bencode.writeTo(msg.getData(), buffer);
        buffer.flip();

        channel.send(buffer, msg.getAddress());
        accept(DhtListener::onSend, msg);
    }

    private void doRead() throws IOException {
        buffer.clear();
        SocketAddress address = channel.receive(buffer);
        Map<String, Object> data = Bencode.parse(buffer.flip());
        DhtMessage message = new DhtMessage(address, data);
        accept(DhtListener::onRecv, message);
        switchMessage(message);
    }

    private void switchMessage(DhtMessage message) {
        byte[] y = (byte[]) message.getData().get("y");
        if (y == null || y.length == 0) {
            accept(DhtListener::recvUnknownTypeMsg, message);
            return;
        }
        switch (y[0]) {
            default:
                accept(DhtListener::recvUnknownTypeMsg, message);
                break;
            case 'q':
                accept(DhtListener::recvQuery, message);
                switchQuery(message);
                break;
            case 'r':
                accept(DhtListener::recvResponse, message);
                break;
            case 'e':
                accept(DhtListener::recvError, message);
                break;
        }
    }

    private void switchQuery(DhtMessage message) {
        byte[] q = (byte[]) message.getData().get("q");
        if (q == null) {
            accept(DhtListener::recvUnknownTypeQuery, message);
            return;
        }
        switch (new String(q, StandardCharsets.US_ASCII)) {
            default:
                accept(DhtListener::recvUnknownTypeQuery, message);
                break;
            case "find_node":
                accept(DhtListener::recvFindNode, message);
                break;
            case "ping":
                accept(DhtListener::recvPing, message);
                break;
            case "get_peers":
                accept(DhtListener::recvGetPeers, message);
                break;
            case "announce_peer":
                accept(DhtListener::recvAnnouncePeer, message);
                break;
        }
    }

    private void accept(BiConsumer<DhtListener, DhtMessage> handler, DhtMessage message) {
        for (DhtListener dhtListener : handlerList) {
            handler.accept(dhtListener, message);
        }
    }

    public void addListener(DhtListener handler) {
        handlerList.add(handler);
        handler.setQuerySender(querySender);
        handler.setResponseSender(responseSender);
    }

}
