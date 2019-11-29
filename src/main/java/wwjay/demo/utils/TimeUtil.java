package wwjay.demo.utils;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间相关工具
 *
 * @author wwj
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class TimeUtil {

    public static final String DATA_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATA_PATTERN);

    private TimeUtil() {
    }

    /**
     * yyyy-MM-dd HH:mm:ss格式的字符串转LocalDateTime
     */
    @Nullable
    public static LocalDateTime toLocalDateTime(String date) {
        return StringUtils.hasText(date) ? LocalDateTime.parse(date, FORMATTER) : null;
    }

    /**
     * Date转LocalDateTime
     */
    @Nullable
    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date.toInstant());
    }

    /**
     * 毫秒的时间戳转LocalDateTime
     */
    @Nullable
    public static LocalDateTime toLocalDateTime(long timestamp) {
        return toLocalDateTime(Instant.ofEpochMilli(timestamp));
    }

    /**
     * Instant转LocalDateTime
     */
    @Nullable
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    /**
     * LocalDateTime转Instant
     */
    @Nullable
    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
    }

    /**
     * LocalDateTime转Date
     */
    @Nullable
    public static Date toDate(LocalDateTime dateTime) {
        Instant instant = toInstant(dateTime);
        return instant != null ? Date.from(instant) : null;
    }

    /**
     * 将Duration格式化成 H:MM:SS 的字符串
     */
    @Nullable
    public static String prettyPrint(Duration duration) {
        if (duration == null) {
            return null;
        }
        long s = duration.toSeconds();
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }
}
