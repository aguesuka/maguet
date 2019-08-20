package cc.aguesuka.downloader.impl;

import cc.aguesuka.downloader.IDownloadInfoHash;
import cc.aguesuka.downloader.IInfoHashSaver;
import cc.aguesuka.downloader.IMetaDataDownloader;
import cc.aguesuka.downloader.IPeerFinder;
import cc.aguesuka.util.inject.annotation.Inject;

/**
 * @author :yangmingyuxing
 * 2019/7/25 11:05
 */
public class DownloadInfoHash implements IDownloadInfoHash {
    @Inject
    IMetaDataDownloader metaDataDownload;
    @Inject
    IInfoHashSaver infoHashSaver;
    @Inject
    IPeerFinder peerFinder;

    @Override
    public void download(byte[] infoHash) throws Exception {
        int infoHashLength = 20;
        if (infoHash.length != infoHashLength) {
            throw new IllegalArgumentException();
        }
        byte[] info = peerFinder.findPeer(infoHash, metaDataDownload);
        infoHashSaver.save(infoHash, info);
    }
}
