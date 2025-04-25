package fansirsqi.xposed.sesame.util;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.List;
public class Logback {
    static String LOG_DIR;
    static {
        assert Files.LOG_DIR != null;
        LOG_DIR = Files.LOG_DIR.getPath()+File.separator;
    }
    public static List<String> logNames = List.of(
            "runtime", "system", "record", "debug", "forest",
            "farm", "other", "error", "capture");
    public static void configureLogbackDirectly() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.stop();
        for (String logName : logNames) {
            setupAppender(lc, logName);
        }
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setContext(lc);
        ple.setPattern("[%thread] %logger{80} %msg%n");
        ple.start();
        LogcatAppender la = new LogcatAppender();
        la.setContext(lc);
        la.setEncoder(ple);
        la.start();
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ROOT");
        root.addAppender(la);
    }
    static void setupAppender(LoggerContext loggerContext, String logName) {
        RollingFileAppender<ILoggingEvent> rfa = new RollingFileAppender<>();
        rfa.setContext(loggerContext);
        rfa.setName(logName);
        rfa.setFile(LOG_DIR + logName + ".log");

        SizeAndTimeBasedRollingPolicy<ILoggingEvent> satbrp = new SizeAndTimeBasedRollingPolicy<>();
        satbrp.setContext(loggerContext);
        satbrp.setFileNamePattern(LOG_DIR + "bak/" + logName + "-%d{yyyy-MM-dd}.%i.log");
        satbrp.setMaxFileSize(FileSize.valueOf("50MB"));
        satbrp.setTotalSizeCap(FileSize.valueOf("100MB"));
        satbrp.setMaxHistory(7);
        satbrp.setCleanHistoryOnStart(true);
        satbrp.setParent(rfa);
        satbrp.start();

        
        rfa.setRollingPolicy(satbrp);


        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setContext(loggerContext);
        ple.setPattern("%d{dd日 HH:mm:ss.SS} %msg%n");
        ple.start();

        rfa.setEncoder(ple);
        rfa.start();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(logName);
        root.addAppender(rfa);
    }
}
