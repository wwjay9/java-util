package com.wwj.util.java.bean;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * 事务处理程序，用于在service层调用本地事务方法
 *
 * @author wwj
 */
@Service
public class TransactionHandler {

    /**
     * 在当前事务调用
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public <T> T runInTransaction(Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * 新建一个事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T runInNewTransaction(Supplier<T> supplier) {
        return supplier.get();
    }
}
