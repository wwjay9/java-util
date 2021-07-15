package com.wwj.util.java.bean;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.beans.FeatureDescriptor;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Bean工具
 *
 * @author wwj
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BeanUtil {

    private BeanUtil() {
    }

    /**
     * 将一个对象拷贝为一个新对象
     *
     * @param source    源对象
     * @param newObject 新对象生成器
     * @return 拷贝后的对象
     */
    public static <T> T copyProperties(Object source, Supplier<T> newObject) {
        if (source == null) {
            return null;
        }
        T t = newObject.get();
        BeanUtils.copyProperties(source, t);
        return t;
    }

    /**
     * 将source对象中不为空的属性值复制到target对象中
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyNotNullProperties(Object source, Object target) {
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    /**
     * 将source对象中不为null或toString不为空字符串的属性值复制到target对象中
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyNotEmptyProperties(Object source, Object target) {
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(source);
        String[] emptyFields = Stream.of(beanWrapper.getPropertyDescriptors())
                .filter(property -> {
                    Object value = beanWrapper.getPropertyValue(property.getName());
                    return value == null || !StringUtils.hasText(value.toString());
                })
                .map(FeatureDescriptor::getName)
                .toArray(String[]::new);
        BeanUtils.copyProperties(source, target, emptyFields);
    }

    /**
     * 判断对象的所有字段是否为null
     *
     * @param obj 对象
     * @return 当对象为null或所有字段都为null时返回true
     */
    public static boolean allFieldIsNull(Object obj) {
        if (obj == null) {
            return true;
        }
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        return Stream.of(beanWrapper.getPropertyDescriptors())
                .map(FeatureDescriptor::getName)
                .filter(name -> !Objects.equals(name, "class"))
                .map(beanWrapper::getPropertyValue)
                .allMatch(Objects::isNull);
    }

    /**
     * 将一个List拷贝为另一个List
     *
     * @param source   源List
     * @param supplier 新类型生成器
     * @return 新类型List
     */
    public static <T, R> List<R> listCopy(List<T> source, Supplier<R> supplier) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream().map(copyFunction(supplier)).collect(Collectors.toList());
    }

    /**
     * 根据List中的字段进行去重
     *
     * @param list         原始list
     * @param keyExtractor 判断重复的key
     * @return 去重后的列表
     */
    public static <T> List<T> listDistinctByKey(List<T> list, Function<? super T, ?> keyExtractor) {
        return new ArrayList<>(list.stream()
                .collect(Collectors.toMap(keyExtractor, Function.identity(), (o1, o2) -> o1, LinkedHashMap::new))
                .values());
    }

    /**
     * 将一个List拆分成大小相等的多个List
     */
    public static <T> List<List<T>> listPartition(List<T> list, int size) {
        Assert.isTrue(size > 0, "size必须大于0");
        return IntStream.iterate(0, i -> i < list.size(), i -> i + size)
                .mapToObj(i -> list.subList(i, Math.min(i + size, list.size())))
                .collect(Collectors.toList());
    }

    /**
     * 将一个List转成另一个List
     *
     * @param list      原始List
     * @param convertor 转换器
     * @return 转换后的List
     */
    public static <F, T> List<T> listTransform(List<F> list, Function<? super F, ? extends T> convertor) {
        return list.stream().map(convertor).collect(Collectors.toList());
    }

    /**
     * 将集合转成MAP
     *
     * @param collection 集合
     * @param getKey     map的key
     * @return LinkedHashMap
     */
    public static <K, V> Map<K, V> listToMap(Collection<V> collection, Function<V, K> getKey) {
        return collection.stream()
                .collect(Collectors.toMap(getKey, Function.identity(), (o1, o2) -> o2, LinkedHashMap::new));
    }

    /**
     * 转换Map的Value类型
     *
     * @param map          原始Map
     * @param valueConvert value的类型转换
     * @return 转换后的Map
     */
    public static <K, V, U> Map<K, U> mapConvertValueType(Map<K, V> map, Function<V, U> valueConvert) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> valueConvert.apply(e.getValue())));
    }

    /**
     * 将一个对象拷贝为另一个对象
     *
     * @param supplier 新类型生成器
     * @return 新类型List
     */
    public static <T, R> Function<T, R> copyFunction(Supplier<R> supplier) {
        return copyFunction(supplier, (t, r) -> {
        });
    }

    /**
     * 将一个对象拷贝为另一个对象
     *
     * @param supplier 新类型生成器
     * @param addition 拷贝对象后的额外操作
     * @return 新类型List
     */
    private static <T, R> Function<T, R> copyFunction(Supplier<R> supplier, BiConsumer<T, R> addition) {
        return t -> {
            if (t == null) {
                return null;
            }
            R r = supplier.get();
            BeanUtils.copyProperties(t, r);
            addition.accept(t, r);
            return r;
        };
    }

    /**
     * 获取对象中属性值为null的字段名
     *
     * @param source 源对象
     * @return 字段名数组
     */
    private static String[] getNullPropertyNames(Object source) {
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(source);
        return Stream.of(beanWrapper.getPropertyDescriptors())
                .map(FeatureDescriptor::getName)
                .filter(propertyName -> beanWrapper.getPropertyValue(propertyName) == null)
                .toArray(String[]::new);
    }
}
