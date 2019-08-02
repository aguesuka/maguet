#torrent-finder  
只需要JDK8以上版本即可运行,使用Maven是为了方便打包;  
实现协议:  
http://www.bittorrent.org/beps/bep_0005.html cc.aguesuka.dht  
http://www.bittorrent.org/beps/bep_0009.html  
http://www.bittorrent.org/beps/bep_0010.html  cc.aguesuka.downloader.impl.DoMetaDataDownLoader  

实现功能:从磁力链接下载种子  
使用方法  
1.编译  
2.执行命令  
`java -classpath bit-finder-0.1.jar cc.aguesuka.run.Main 磁力链接的info_hash部分`   
