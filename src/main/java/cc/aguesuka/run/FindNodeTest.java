package cc.aguesuka.run;

import cc.aguesuka.dht.connection.DhtRequest;
import cc.aguesuka.util.bencode.Bencode;
import cc.aguesuka.util.bencode.BencodeMap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;

/**
 * 用于测试某node是否可达
 *
 * @author :yangmingyuxing
 * 2019/10/15 22:15
 */
public class FindNodeTest {
    public static void main(String[] args) throws IOException {
        //设置地址和端口
        String address = "router.bittorrent.com";
        short port = 6881;
        if (args.length == 1) {
            String[] split = args[0].split(":");
            address = split[0];
            port = Short.parseShort(split[1]);
        }
        // find node 请求
        Random random = new SecureRandom();
        byte[] testId = new byte[20];
        random.nextBytes(testId);
        byte[] msg = DhtRequest.findNode(testId, testId).toBencodeBytes();

        // 发送请求
        DatagramPacket request = new DatagramPacket(msg, msg.length);
        request.setSocketAddress(new InetSocketAddress(address, port));
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(10000);
        socket.send(request);
        // 接受回复
        byte[] recvBuff = new byte[2 << 12];
        DatagramPacket responsePacket = new DatagramPacket(recvBuff, recvBuff.length);
        socket.receive(responsePacket);
        if (responsePacket.getLength() > 0) {
            // 解析回复
            BencodeMap responseMessage = Bencode.parse(ByteBuffer.wrap(responsePacket.getData()));
            System.out.println("responseMessage = " + responseMessage);
        } else {
            System.out.println("连接超时");
        }
    }
}
