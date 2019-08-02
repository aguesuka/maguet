package cc.aguesuka.util.log;

import cc.aguesuka.util.inject.annotation.Config;
import cc.aguesuka.util.inject.annotation.Init;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * 使用jdk自带的日志
 *
 * @author :yangmingyuxing
 * 2019/7/24 21:51
 */
public class LogSetting {
    public static final String DEFAULT_NAME = "cc.aguesuka.default.log.name";
    @Config("log.file")
    private String logSettingFile;

    private static void changeLoggerOutput(Logger logger) throws UnsupportedEncodingException {
        // ConsoleHandler.output is System.out;change to System.err for default log
        final Logger parent = logger.getParent();
        logger.setUseParentHandlers(false);
        for (Handler handler : parent.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                final ConsoleHandler needChangeHandler = new ConsoleHandler() {
                    {
                        setOutputStream(System.out);
                    }
                };
                // class name is LogSetting$1 not java.util.logging.ConsoleHandler;can not read config from properties
                needChangeHandler.setEncoding(handler.getEncoding());
                needChangeHandler.setErrorManager(handler.getErrorManager());
                needChangeHandler.setFilter(handler.getFilter());
                needChangeHandler.setFormatter(handler.getFormatter());
                needChangeHandler.setLevel(handler.getLevel());
                logger.addHandler(needChangeHandler);
            } else {
                logger.addHandler(handler);
            }
        }
    }

    @Init
    private void init() throws IOException {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(logSettingFile)) {

            if (inputStream == null) {
                throw new NullPointerException("日志文件加载失败");
            }
            InputStream buff = new BufferedInputStream(inputStream);
            buff.mark(0);
            LogManager.getLogManager().readConfiguration(buff);
            buff.reset();
            Properties properties = new Properties();
            properties.load(buff);
            String logFile = properties.getProperty("java.util.logging.FileHandler.pattern");
            Path logDir = Paths.get(logFile).getParent();
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
        }
        Logger logger = Logger.getLogger(DEFAULT_NAME);
        changeLoggerOutput(logger);

    }
}
