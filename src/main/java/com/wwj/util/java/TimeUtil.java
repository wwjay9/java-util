package com.wwj.util.java;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * 将Date转换成yyyy-MM-dd HH:mm:ss格式的字符串
     */
    @Nullable
    public static String toString(Date date) {
        return toString(toLocalDateTime(date));
    }

    /**
     * 将LocalDateTime转换成yyyy-MM-dd HH:mm:ss格式的字符串
     */
    @Nullable
    public static String toString(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(FORMATTER);
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

    /**
     * 列出开始时间到结束时间之前所有的月份
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 包含开始时间和结束时间的月份
     */
    public static List<YearMonth> between(YearMonth start, YearMonth end) {
        Assert.isTrue(start.compareTo(end) <= 0, "开始时间必须在结束时间之前");
        return Stream.iterate(start, date -> date.plusMonths(1))
                .limit(ChronoUnit.MONTHS.between(start, end) + 1)
                .collect(Collectors.toList());
    }

    /**
     * 列出开始时间到结束时间之前所有的日期
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 包含开始时间和结束时间的日期
     */
    public static List<LocalDate> between(LocalDate start, LocalDate end) {
        Assert.isTrue(start.compareTo(end) <= 0, "开始时间必须在结束时间之前");
        return Stream.iterate(start, date -> date.plusDays(1))
                .limit(ChronoUnit.DAYS.between(start, end) + 1)
                .collect(Collectors.toList());
    }
}
