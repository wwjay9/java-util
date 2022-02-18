/**
 * @author wwj
 */
module com.wwj.util.java {
    // java
    requires java.desktop;
    requires java.net.http;
    requires java.sql;
    // servlet
    requires servlet.api;
    // lombok
    requires static lombok;
    // slf4j
    requires static org.slf4j;
    // spring
    requires spring.core;
    requires spring.beans;
    requires spring.context;
    requires spring.web;
    requires spring.tx;
    requires spring.data.redis;
    requires spring.security.core;
    requires spring.security.crypto;
    // fastjson
    requires fastjson;
    // poi
    requires poi;
    requires poi.ooxml;
    // 二维码
    requires com.google.zxing;
    requires com.google.zxing.javase;
    // jsoup
    requires org.jsoup;
}