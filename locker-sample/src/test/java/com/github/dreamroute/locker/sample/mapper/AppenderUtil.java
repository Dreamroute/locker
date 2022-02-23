package com.github.dreamroute.locker.sample.mapper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

/**
 * 描述：logback assert console util
 * @see https://stackoverflow.com/questions/1827677/how-to-do-a-junit-assert-on-a-message-in-a-logger/50268580
 *
 * @author w.dehi.2022-02-23
 */
public class AppenderUtil {

    public static ListAppender<ILoggingEvent> create(Class<?> c) {
        Logger logger = (Logger) LoggerFactory.getLogger(c);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    public static String getMessage(ListAppender<ILoggingEvent> appender, int index) {
        return appender.list.get(index).getFormattedMessage();
    }
}
