/**
 * @author wwj
 */
module wwjay.java.util {
    // java
    requires java.desktop;
    requires java.net.http;
    // servlet
    requires servlet.api;
    // lombok
    requires lombok;
    // spring
    requires spring.core;
    requires spring.beans;
    requires spring.context;
    requires spring.web;
    requires spring.data.redis;
    requires spring.security.core;
    // fastjson
    requires fastjson;
    // poi
    requires poi;
    requires poi.ooxml;
    // 二维码
    requires com.google.zxing;
    requires com.google.zxing.javase;
}