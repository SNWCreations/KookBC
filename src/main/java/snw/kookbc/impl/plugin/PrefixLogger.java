/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.plugin;

import org.slf4j.Logger;
import org.slf4j.Marker;

// A SLF4J Logger implementation that added a prefix on every log message
public final class PrefixLogger implements Logger {
    private final String prefix;
    private final Logger logger;

    public PrefixLogger(String prefix, Logger logger) {
        this.prefix = "[" + prefix + "]";
        this.logger = logger;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        logger.trace(format(s));
    }

    @Override
    public void trace(String s, Object o) {
        logger.trace(format(s), o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        logger.trace(format(s), o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        logger.trace(format(s), objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logger.trace(format(s), throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        logger.trace(marker, format(s));
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        logger.trace(marker, format(s), o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        logger.trace(marker, format(s), o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        logger.trace(marker, format(s), objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        logger.trace(marker, format(s), throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        logger.debug(format(s));
    }

    @Override
    public void debug(String s, Object o) {
        logger.debug(format(s), o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        logger.debug(format(s), o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        logger.debug(format(s), objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logger.debug(format(s), throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        logger.debug(marker, format(s));
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        logger.debug(marker, format(s), o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        logger.debug(marker, format(s), o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        logger.debug(marker, format(s), objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        logger.debug(marker, format(s), throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        logger.info(format(s));
    }

    @Override
    public void info(String s, Object o) {
        logger.info(format(s), o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        logger.info(format(s), o, o1);
    }

    @Override
    public void info(String s, Object... objects) {
        logger.info(format(s), objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        logger.info(format(s), throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        logger.info(marker, format(s));
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        logger.info(marker, format(s), o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        logger.info(marker, format(s), o, o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        logger.info(marker, format(s), objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        logger.info(marker, format(s), throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        logger.warn(format(s));
    }

    @Override
    public void warn(String s, Object o) {
        logger.warn(format(s), o);
    }

    @Override
    public void warn(String s, Object... objects) {
        logger.warn(format(s), objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        logger.warn(format(s), o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(format(s), throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        logger.warn(marker, format(s));
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        logger.warn(marker, format(s), o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        logger.warn(marker, format(s), o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        logger.warn(marker, format(s), objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        logger.warn(marker, format(s), throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        logger.error(format(s));
    }

    @Override
    public void error(String s, Object o) {
        logger.error(format(s), o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        logger.error(format(s), o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        logger.error(format(s), objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        logger.error(format(s), throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        logger.error(marker, format(s));
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        logger.error(marker, format(s), o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        logger.error(marker, format(s), o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        logger.error(marker, format(s), objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        logger.error(marker, format(s), throwable);
    }

    private String format(String s) {
        return prefix + " " + s;
    }
}
