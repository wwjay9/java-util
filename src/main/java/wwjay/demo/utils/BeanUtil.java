package wwjay.demo.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author wwj
 */
public class BeanUtil {

    private BeanUtil() {
    }

    /**
     * 将source的List转换为List<target>
     *
     * @param source 源对象
     * @param target 转换的泛型
     * @param <B>    泛型
     * @return List<target>
     */
    public static <B> List<B> copyListBean(List<?> source, Class<B> target) {
        if (source == null) {
            return null;
        }
        List<B> targetList = new ArrayList<>(source.size());
        try {
            for (Object o : source) {
                B b = target.newInstance();
                BeanUtils.copyProperties(o, b);
                targetList.add(b);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("复制List的Bean属性时出错", e);
        }
        return targetList;
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
     * 获取对象中属性值为null的字段名
     *
     * @param source 源对象
     * @return 字段名数组
     */
    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper wrappedSource = new BeanWrapperImpl(source);
        return Stream.of(wrappedSource.getPropertyDescriptors())
                .map(FeatureDescriptor::getName)
                .filter(propertyName -> wrappedSource.getPropertyValue(propertyName) == null)
                .toArray(String[]::new);
    }
}
