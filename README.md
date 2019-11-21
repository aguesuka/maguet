## torrent-finder  
torrent-finder 可以在已知磁力链接的情况下,加入dht网络并下载种子
## 环境要求
只需要JDK8以上版本即可运行,使用Maven是为了方便打包;  
## 实现协议:  
http://www.bittorrent.org/beps/bep_0005.html
http://www.bittorrent.org/beps/bep_0009.html  
http://www.bittorrent.org/beps/bep_0010.html
## 使用方法  
1.编译  
```mvn  package```  
2.执行命令  
```java -classpath bit-finder-0.1.jar cc.aguesuka.run.Main 磁力链接的info_hash部分```   
##  磁力链接嗅探器
https://github.com/aguesuka/ague-dht
