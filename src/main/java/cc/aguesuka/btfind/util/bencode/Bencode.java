package cc.aguesuka.btfind.util.bencode;

import cc.aguesuka.btfind.util.HexUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author :aguesuka
 * 2019/12/12 21:36
 */
public class Bencode {
    public static Map<String, Object> parse(ByteBuffer buffer) {
        return new BencodeParser(new ByteBufferInput(buffer)).parse();
    }
    public static Map<String,Object> parse(byte[] array){
        return parse(ByteBuffer.wrap(array));
    }
    public static void writeTo(Object o, ByteBuffer buffer) {
        new BencodeEncoder(new ByteBufferOutput(buffer)).put(o);
    }

    public static byte[] toBytes(Object o) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new BencodeEncoder(new ByteArrayOutput(outputStream)).put(o);
        return outputStream.toByteArray();
    }

    private static class ByteBufferOutput implements BencodeEncoder.Output {
        final private ByteBuffer buffer;

        private ByteBufferOutput(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public void writeByteArray(byte[] bytes) {
            buffer.put(bytes);
        }

        @Override
        public void writeByte(byte b) {
            buffer.put(b);
        }
    }

    private static class ByteArrayOutput implements BencodeEncoder.Output {
        final private ByteArrayOutputStream outputStream;

        private ByteArrayOutput(ByteArrayOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void writeByteArray(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void writeByte(byte b) {
            outputStream.write(b);
        }
    }

    public static String toString(Object o) {
        return append(o, new StringBuilder()).toString();
    }

    private static StringBuilder append(Object o, StringBuilder builder) {
        if (o instanceof Map) {
            builder.append("{");
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
                builder.append(entry.getKey()).append("=");
                append(entry.getValue(), builder);
                builder.append(", ");
            }
            builder.append("}");
        } else if (o instanceof List) {
            builder.append("[");
            for (Object e : ((List<?>) o)) {
                append(e, builder);
                builder.append(",");
            }
            builder.append("]");
        } else if (o instanceof byte[]) {
            byte[] bytes = (byte[]) o;
            for (byte b : bytes) {
                if(b < 32){
                    builder.append(HexUtil.encode(bytes));
                    return builder;
                }
            }
            builder.append(new String(bytes, StandardCharsets.US_ASCII));
        } else {
            builder.append(o);
        }
        return builder;
    }

    private static class ByteBufferInput implements BencodeParser.Input {
        final private ByteBuffer buffer;

        ByteBufferInput(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public byte getByte() {
            return buffer.get();
        }

        @Override
        public byte[] getBytes(int length) {
            byte[] result = new byte[length];
            buffer.get(result);
            return result;
        }
    }
}
