package cc.aguesuka.btfind.metadata;

import cc.aguesuka.btfind.util.HexUtil;
import cc.aguesuka.btfind.util.bencode.Bencode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author :yangmingyuxing
 * 2020/2/18 16:57
 */
public class MetadataDao {
    private static MetadataDao instance = new MetadataDao();

    private MetadataDao() {
    }

    public static MetadataDao instance() {
        return instance;
    }

    private Path name(byte[] infoHash, Path basePath) {
        String name = HexUtil.encode(infoHash) + ".metadata";
        return basePath.resolve(name);
    }

    public void save(byte[] infoHash, byte[] metadata, Path basePath) {
        Path path = name(infoHash, basePath);
        if (!Files.exists(path)) {
            try {
                Files.write(path, metadata, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }


    public boolean has(byte[] infoHash, Path metadataPath) {
        Path path = name(infoHash, metadataPath);
        return Files.exists(path);
    }

    public static void main(String[] args) throws IOException {
        List<Map<String, Object>> result = instance().metadataList(Path.of("C:\\code\\kotlin\\ague-dht3\\data\\metadata"));
        for (Map<String, Object> data : result) {
            data.remove("pieces");
            byte[] name = (byte[])data.get("name");
            if(name !=null){
                System.out.println("name = " + new String(name, StandardCharsets.UTF_8));
            }
        }
    }


    private List<Map<String, Object>> metadataList(Path metadataPath) throws IOException {
        return Files.walk(metadataPath, 1).filter(p -> !Files.isDirectory(p)).map(p -> {
            try {
                return Bencode.parse(Files.readAllBytes(p));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).collect(Collectors.toList());
    }
}
