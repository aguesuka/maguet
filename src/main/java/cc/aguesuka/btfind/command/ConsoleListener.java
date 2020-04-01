package cc.aguesuka.btfind.command;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author :aguesuka
 * 2020/2/21 09:38
 */
public class ConsoleListener {
    private Scanner scanner = new Scanner(System.in);
    private ConcurrentLinkedQueue<String> commandQueue = new ConcurrentLinkedQueue<>();
    private Map<String, Command> handlers;

    public ConsoleListener() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (scanner.hasNextLine()) {
                    commandQueue.add(scanner.nextLine());
                }
            }
        });
        handlers = new LinkedHashMap<>();
        thread.setDaemon(true);
        thread.start();
    }



    public void addCommand(String name, String example, String comment, Consumer<String> handler) {
        Command command = new Command(name, example, comment, handler);
        handlers.put(name, command);
    }

    private void printTime() {
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
    }

    public void start() {
        printTime();
        System.out.println("application start");
        printHelp();
        System.out.print(">>>");
    }

    private void printHelp() {
        final String table = "%-20s%-40s%s";
        System.out.println(String.format(table, "name", "format", "comment"));
        System.out.println("======================================================================================");
        handlers.forEach((key, value) -> {

            String row = String.format(table, value.name, value.format, value.comment);
            System.out.println(row);
        });
    }

    public void run() {
        while (true) {
            String command = commandQueue.poll();
            if (null == command) {
                break;
            }
            try {
                printTime();
                String[] s = command.split(" ", 2);
                String method = s[0];
                String param = s.length < 2 ? null : s[1];
                Command cmd = handlers.get(method);
                if (cmd != null) {
                    cmd.handler.accept(param);
                } else {
                    printHelp();
                }
            } catch (Exception e) {
                System.out.println("err " + e.getMessage());
            } finally {
                System.out.print(">>>");
            }
        }

    }

    public static class Command {
        private String name;
        private String format;
        private Consumer<String> handler;
        private String comment;

        private Command(String name, String format, String comment, Consumer<String> handler) {
            this.name = name;
            this.format = format;
            this.comment = comment;
            this.handler = handler;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Command.class.getSimpleName() + "[", "]")
                    .add("name='" + name + "'")
                    .add("comment='" + comment + "'")
                    .toString();
        }
    }
}
