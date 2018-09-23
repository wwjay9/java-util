package wwjay.demo.utils;

import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * @author wwj
 */
public class StringUtil {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    private static final String DIGITS = "0123456789";
    private static final String ALPHANUM = UPPER + LOWER + DIGITS;
    private static final Random RANDOM = new SecureRandom();
    private static final char[] SYMBOLS = ALPHANUM.toCharArray();

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
    public static String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 拼接url的便捷方法，解决常规拼接url时斜杠的问题
     *
     * @param httpUrl     拼接的基础url
     * @param queryParams 需要拼接的查询参数，可以为null
     * @param paths       需要拼接的path，可以传递为"/path"或"path"，拼接的结果会自动去除多余的斜杠
     * @return 拼接的url
     */
    public static String buildUrl(String httpUrl, Map<String, String> queryParams, String... paths) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(httpUrl);
        if (!ObjectUtils.isEmpty(paths)) {
            for (String path : paths) {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                builder.path("/" + path);
            }
        }
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        return builder.build()
                .normalize()
                .toUri()
                .normalize()
                .toString();
    }
}