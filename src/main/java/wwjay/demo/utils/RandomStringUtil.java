package wwjay.demo.utils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * @author wwjay
 */
public class RandomStringUtil {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    private static final String DIGITS = "0123456789";
    private static final String ALPHANUM = UPPER + LOWER + DIGITS;
    private static final Random RANDOM = new SecureRandom();
    private static final char[] SYMBOLS = ALPHANUM.toCharArray();

    /**
     * 生成一个12位随机字符串
     */
    public static String nextString() {
        return nextString(12);
    }

    /**
     * 生成一个指定长度的随机字符串
     *
     * @param length 随机字符串的长度
     */
    public static String nextString(int length) {
        char[] buf = new char[length];
        for (int i = 0; i < length; ++i) {
            buf[i] = SYMBOLS[RANDOM.nextInt(SYMBOLS.length)];
        }
        return new String(buf);
    }

    /**
     * 生成一个没有-分隔符的uuid字符串
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}