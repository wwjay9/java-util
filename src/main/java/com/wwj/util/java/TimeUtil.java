package com.wwj.util.java;

import org.springframework.util.Assert;

import java.time.*;
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
    public static LocalDateTime toLocalDateTime(String date) {
        return LocalDateTime.parse(date, FORMATTER);
    }

    /**
     * Date转LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date.toInstant());
    }

    /**
     * 毫秒的时间戳转LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timestamp) {
        return toLocalDateTime(Instant.ofEpochMilli(timestamp));
    }

    /**
     * Instant转LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
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

    /**
     * 将Date转换成yyyy-MM-dd HH:mm:ss格式的字符串
     */
    public static String toString(Date date) {
        return toString(toLocalDateTime(date));
    }

    /**
     * 将LocalDateTime转换成yyyy-MM-dd HH:mm:ss格式的字符串
     */
    public static String toString(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    /**
     * 将Duration格式化成 H:MM:SS 的字符串
     */
    public static String prettyPrint(Duration duration) {
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
