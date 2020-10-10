package wwjay.demo.utils;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;

/**
 * 字符串工具
 *
 * @author wwj
 */
@SuppressWarnings({"WeakerAccess", "unused", "SpellCheckingInspection"})
public class StringUtil {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    private static final String DIGITS = "0123456789";
    private static final String ALPHANUM = DIGITS + LOWER + UPPER;
    private static final Random RANDOM = new SecureRandom();
    private static final char[] SYMBOLS = ALPHANUM.toCharArray();

    private StringUtil() {
    }

    /**
     * 生成一个12位随机字符串
     */
    public static String randomString() {
        return randomString(12);
    }

    /**
     * 生成一个指定长度的随机字符串
     *
     * @param length 随机字符串的长度
     */
    public static String randomString(int length) {
        char[] buf = new char[length];
        for (int i = 0; i < length; ++i) {
            buf[i] = SYMBOLS[RANDOM.nextInt(SYMBOLS.length)];
        }
        return new String(buf);
    }

    /**
     * 生成一个没有-分隔符的uuid字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 提取一个字符串的前面部分
     *
     * @param str  原始字符串
     * @param size 提取的字符串大小，如size比str的长度小，则返回str
     * @return 提取的前面部分字符串
     */
    public static String substringBegin(String str, int size) {
        if (str.length() < size) {
            return str;
        }
        return str.substring(0, size);
    }

    /**
     * 将十进制数字转换成62进制
     *
     * @param num 十进制数字
     * @return 返回0-9、a-z、A-Z的62进制字符
     */
    public static String base62(long num) {
        if (num == 0) {
            return String.valueOf(SYMBOLS[0]);
        }
        List<String> arr = new ArrayList<>();
        int base = SYMBOLS.length;
        while (num > 0) {
            long rem = num % base;
            num = (num - rem) / base;
            arr.add(String.valueOf(SYMBOLS[Math.toIntExact(rem)]));
        }
        Collections.reverse(arr);
        return String.join("", arr);
    }

    /**
     * 将byte字节大小格式化成方便阅读的格式
     *
     * @param bytes 字节大小
     * @return 格式化字符串
     */
    public static String dataSizeFormat(long bytes) {
        long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        return b < 1024L ? bytes + " B"
                : b <= 0xfffccccccccccccL >> 40 ? String.format("%.1f KB", bytes / 0x1p10)
                : b <= 0xfffccccccccccccL >> 30 ? String.format("%.1f MB", bytes / 0x1p20)
                : b <= 0xfffccccccccccccL >> 20 ? String.format("%.1f GB", bytes / 0x1p30)
                : b <= 0xfffccccccccccccL >> 10 ? String.format("%.1f TB", bytes / 0x1p40)
                : b <= 0xfffccccccccccccL ? String.format("%.1f PB", (bytes >> 10) / 0x1p40)
                : String.format("%.1f EB", (bytes >> 20) / 0x1p40);
    }

    /**
     * 将字符串转换成驼峰格式，如 Name -> name
     *
     * @param s 字符串
     * @return 驼峰格式字符串
     */
    public static String toCamelCase(String s) {
        return Introspector.decapitalize(s);
    }

    /**
     * 将字符串转换成Long类型，避免出现空指针异常
     *
     * @param s 字符串
     * @return long
     */
    @Nullable
    public static Long toLong(@Nullable String s) {
        return stringConvert(s, Long::valueOf);
    }

    /**
     * 将字符串转换成Integer类型，避免出现空指针异常
     *
     * @param s 字符串
     * @return integer
     */
    @Nullable
    public static Integer toInteger(@Nullable String s) {
        return stringConvert(s, Integer::valueOf);
    }

    /**
     * 字符串转换
     *
     * @param s         字符串
     * @param converter 转换器
     * @return 转换的值
     */
    private static <T> T stringConvert(String s, Function<String, T> converter) {
        return Optional.ofNullable(s)
                .filter(StringUtils::hasText)
                .map(StringUtils::trimWhitespace)
                .map(converter)
                .orElse(null);
    }
}