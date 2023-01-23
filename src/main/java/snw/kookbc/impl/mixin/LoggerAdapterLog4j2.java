package snw.kookbc.impl.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

public class LoggerAdapterLog4j2 extends LoggerAdapterAbstract {

    private static final org.apache.logging.log4j.Level[] LEVELS = {
            org.apache.logging.log4j.Level.FATAL,
            org.apache.logging.log4j.Level.ERROR,
            org.apache.logging.log4j.Level.WARN,
            org.apache.logging.log4j.Level.INFO,
            org.apache.logging.log4j.Level.DEBUG,
            org.apache.logging.log4j.Level.TRACE
    };

    private final Logger logger;

    public LoggerAdapterLog4j2(String name) {
        super(name);
        this.logger = LogManager.getLogger(name);
    }

    @Override
    public String getType() {
        return "Log4j2 (via KookBC)";
    }

    @Override
    public void catching(Level level, Throwable t) {
        this.logger.catching(LoggerAdapterLog4j2.LEVELS[level.ordinal()], t);
    }

    @Override
    public void catching(Throwable t) {
        this.logger.catching(t);
    }

    @Override
    public void debug(String message, Object... params) {
        this.logger.debug(message, params);
    }

    @Override
    public void debug(String message, Throwable t) {
        this.logger.debug(message, t);
    }

    @Override
    public void error(String message, Object... params) {
        this.logger.error(message, params);
    }

    @Override
    public void error(String message, Throwable t) {
        this.logger.error(message, t);
    }

    @Override
    public void fatal(String message, Object... params) {
        this.logger.fatal(message, params);
    }

    @Override
    public void fatal(String message, Throwable t) {
        this.logger.fatal(message, t);
    }

    @Override
    public void info(String message, Object... params) {
        this.logger.info(message, params);
    }

    @Override
    public void info(String message, Throwable t) {
        this.logger.info(message, t);
    }

    @Override
    public void log(Level level, String message, Object... params) {
        this.logger.log(LoggerAdapterLog4j2.LEVELS[level.ordinal()], message, params);
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        this.logger.log(LoggerAdapterLog4j2.LEVELS[level.ordinal()], message, t);
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        return this.logger.throwing(t);
    }

    @Override
    public void trace(String message, Object... params) {
        this.logger.trace(message, params);
    }

    @Override
    public void trace(String message, Throwable t) {
        this.logger.trace(message, t);
    }

    @Override
    public void warn(String message, Object... params) {
        this.logger.warn(message, params);
    }

    @Override
    public void warn(String message, Throwable t) {
        this.logger.warn(message, t);
    }

}
