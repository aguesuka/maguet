package cc.aguesuka.btfind.dht.client;

import cc.aguesuka.btfind.dht.model.DhtMessage;
import cc.aguesuka.btfind.util.TimeoutMap;

import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * query sender. send with callback, callback will be ignore if timeout.
 *
 * @author :yangmingyuxing
 * 2020/2/17 15:53
 */
public class DhtQuerySender {
    private Consumer<DhtMessage> sender;
    private TimeoutMap<SocketAddress, Consumer<DhtMessage>> callbackMap = new TimeoutMap<>();

    public DhtQuerySender(DhtClient dhtClient) {
        dhtClient.addListener(new Listener());
    }

    public void send(DhtMessage dhtMessage) {
        sender.accept(dhtMessage);
    }

    public void send(DhtMessage dhtMessage, Consumer<DhtMessage> callback) {
        callbackMap.put(dhtMessage.getAddress(), callback, 3000);
        send(dhtMessage);
    }

    public void refresh() {
        callbackMap.refresh();
    }

    private class Listener implements DhtListener {
        @Override
        public void setQuerySender(Consumer<DhtMessage> sender) {
            DhtQuerySender.this.sender = sender;
        }

        @Override
        public void onRecv(DhtMessage msg) {
            Consumer<DhtMessage> callback = callbackMap.getAndRemove(msg.getAddress());
            if (null != callback) {
                callback.accept(msg);
            }
        }
    }
}
