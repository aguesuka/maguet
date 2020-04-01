package cc.aguesuka.btfind.dht.client;

import cc.aguesuka.btfind.dht.model.DhtMessage;

import java.util.function.Consumer;

/**
 * listener of {@link DhtClient}. call {@link DhtClient#addListener(DhtListener)} to bind.
 * all of method has void return.
 *
 * @author :yangmingyuxing
 * 2020/2/13 10:12
 */
@SuppressWarnings("unused")
public interface DhtListener {
    /**
     * recv response msg
     *
     * @param msg message
     */
    default void recvResponse(DhtMessage msg) {
    }

    /**
     * on send msg
     *
     * @param msg message
     */
    default void onSend(DhtMessage msg) {
    }

    /**
     * before send query
     *
     * @param msg message
     */
    default void beforeSendQuery(DhtMessage msg) {

    }

    /**
     * before send response
     *
     * @param msg message
     */
    default void beforeSendResponse(DhtMessage msg) {

    }

    /**
     * recv query type is query.query type unknown
     *
     * @param msg message
     */
    default void recvUnknownTypeQuery(DhtMessage msg) {
    }

    /**
     * recv message type is error
     *
     * @param msg message
     */
    default void recvError(DhtMessage msg) {
    }

    /**
     * recv message type is unknown
     *
     * @param msg message
     */
    default void recvUnknownTypeMsg(DhtMessage msg) {
    }

    /**
     * exception when handler
     *
     * @param throwable exception
     */
    default void onException(Throwable throwable) {
    }

    /**
     * recv message type is query
     *
     * @param msg message
     */
    default void recvQuery(DhtMessage msg) {
    }

    /**
     * recv message type is query,query type is 'ping'
     *
     * @param msg message
     */
    default void recvPing(DhtMessage msg) {
    }

    /**
     * recv message type is query,query type is 'find_node'
     *
     * @param msg message
     */
    default void recvFindNode(DhtMessage msg) {
    }

    /**
     * recv message type is query,query type is 'get_peers'
     *
     * @param msg message
     */
    default void recvGetPeers(DhtMessage msg) {
    }

    /**
     * recv message type is query,query type is 'announce_peer'
     *
     * @param msg message
     */
    default void recvAnnouncePeer(DhtMessage msg) {
    }

    /**
     * recv msg
     *
     * @param msg message
     */
    default void onRecv(DhtMessage msg) {
    }

    /**
     * called at {@link DhtClient#addListener(DhtListener)},save sender if need send query
     *
     * @param sender call {@code sender.accept(msg)} to add message to wait send queue
     */
    default void setQuerySender(Consumer<DhtMessage> sender) {

    }

    /**
     * called at {@link DhtClient#addListener(DhtListener)},save sender if need send response
     *
     * @param responseSender call {@code sender.accept(msg)} to add message to wait send queue
     */
    default void setResponseSender(Consumer<DhtMessage> responseSender) {

    }
}
