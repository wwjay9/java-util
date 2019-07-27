package wwjay.demo.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间相关转换工具
 *
 * @author wwj
 * @date 2019-03-16
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DateTimeUtil {

    private DateTimeUtil() {
    }

    public static final String DATA_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATA_PATTERN);

    /**
     * yyyy-MM-dd HH:mm:ss格式的字符串转LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(String date) {
        return LocalDateTime.parse(date, FORMATTER);
    }

    /**
     * Date转LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * 毫秒的时间戳转LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime转Instant
     */
    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * LocalDateTime转Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        return Date.from(toInstant(dateTime));
    }
}
