package cc.aguesuka.downloader.impl;

import cc.aguesuka.downloader.IInfoHashSaver;
import cc.aguesuka.util.HexUtil;
import cc.aguesuka.util.bencode.Bencode;
import cc.aguesuka.util.bencode.BencodeMap;
import cc.aguesuka.util.log.LogSetting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * @author :yangmingyuxing
 * 2019/7/25 11:03
 */
public class InfoHashSaver implements IInfoHashSaver {
    private Logger logger = Logger.getLogger(LogSetting.DEFAULT_NAME);

    @Override
    public void save(byte[] infoHash, byte[] peerInfo) throws IOException {
        logger.fine("准备保存文件");
        String fileName = HexUtil.encode(infoHash) + ".torrent";
        BencodeMap peerMap = new BencodeMap();
        peerMap.putByteArray("announce", "udp://tracker.openbittorrent.com:80/announce".getBytes(StandardCharsets.UTF_8));
        peerMap.put("info", Bencode.parse(ByteBuffer.wrap(peerInfo)));
        Path path = Paths.get(fileName);
        logger.fine("保存文件:" + path);
        Files.write(path, peerMap.toBencodeBytes(), StandardOpenOption.CREATE);
        Files.write(Paths.get(HexUtil.encode(infoHash)+".info"), peerInfo, StandardOpenOption.CREATE);
    }
}
