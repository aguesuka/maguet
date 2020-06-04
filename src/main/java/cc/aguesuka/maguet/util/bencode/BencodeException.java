package cc.aguesuka.maguet.util.bencode;

/**
 * @author :aguesuka
 * 2019/9/1 23:15
 */
public final class BencodeException extends RuntimeException {

    BencodeException(Throwable cause) {
        super(cause);

    }


    BencodeException(String message) {
        super(message);
    }


}
