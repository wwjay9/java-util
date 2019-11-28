package wwjay.demo.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel的字段属性
 *
 * @author wwj
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelProperty {

    /**
     * 表头名称
     */
    String value();

    /**
     * 宽度
     */
    short width() default -1;
}
