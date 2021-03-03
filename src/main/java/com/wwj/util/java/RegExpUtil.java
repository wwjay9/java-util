package com.wwj.util.java;

import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 *
 * @author wwj
 */
@SuppressWarnings("unused")
public class RegExpUtil {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^(?:(?:\\+|00)86)?1\\d{10}$");

    private RegExpUtil() {
    }

    /**
     * 验证字符串是否是手机号
     */
    public static boolean isMobile(String s) {
        return MOBILE_PATTERN.matcher(s).matches();
    }
}
