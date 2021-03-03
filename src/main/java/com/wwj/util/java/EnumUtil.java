package com.wwj.util.java;

import org.springframework.lang.Nullable;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * 枚举工具类
 *
 * @author wwj
 */
@SuppressWarnings("unused")
public class EnumUtil {

    private EnumUtil() {
    }

    /**
     * 根据code查询枚举的getName字段值
     */
    @Nullable
    public static <E extends Enum<E>> String getNameByCode(Class<E> clazz, ToIntFunction<E> getCode,
                                                           Function<E, String> getName, Integer code) {
        return Optional.ofNullable(getEnumByCode(clazz, getCode, code))
                .map(getName)
                .orElse(null);
    }

    /**
     * 根据code查询枚举类
     */
    @Nullable
    public static <E extends Enum<E>> E getEnumByCode(Class<E> clazz, ToIntFunction<E> getCode, Integer code) {
        return EnumSet.allOf(clazz).stream()
                .filter(e -> Objects.equals(getCode.applyAsInt(e), code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取枚举类的下拉框选项
     */
    public static <E extends Enum<E>, K, V> Map<K, V> getEnumOption(Class<E> clazz, Function<E, K> getKey,
                                                                    Function<E, V> getValue) {
        return EnumSet.allOf(clazz)
                .stream()
                .collect(Collectors.toMap(getKey, getValue, (s1, s2) -> s2, LinkedHashMap::new));
    }
}
