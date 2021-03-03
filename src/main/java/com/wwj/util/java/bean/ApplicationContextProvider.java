package com.wwj.util.java.bean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 通过此类的静态方法获取当前的应用上下文
 *
 * @author wwj
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * 获取Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext还未初始化完成");
        }
        return applicationContext.getBean(clazz);
    }

    @Override
    public synchronized void setApplicationContext(ApplicationContext ac) {
        applicationContext = ac;
    }
}
