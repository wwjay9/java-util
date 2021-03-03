package com.wwj.util.java;

import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 集合工具类
 *
 * @author wwj
 */
@SuppressWarnings("unused")
public class CollectionUtil {

    private CollectionUtil() {
    }

    /**
     * 将一个List拆分成大小相等的多个List
     */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        Assert.isTrue(size > 0, "size必须大于0");
        return IntStream.iterate(0, i -> i < list.size(), i -> i + size)
                .mapToObj(i -> list.subList(i, Math.min(i + size, list.size())))
                .collect(Collectors.toList());
    }

    /**
     * 将一个List转成另一个List
     */
    public static <F, T> List<T> transform(List<F> fromList, Function<? super F, ? extends T> function) {
        return fromList.stream()
                .map(function)
                .collect(Collectors.toList());
    }
}
