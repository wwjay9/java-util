package wwjay.demo.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * HTTP工具类
 *
 * @author wwj
 * @date 2019-01-09
 */
public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private HttpUtil() {
    }

    /**
     * 构建url
     */
    public static String buildUrl(String httpUrl, String... paths) {
        return buildUrl(httpUrl, null, paths);
    }

    /**
     * 拼接url的便捷方法，解决常规拼接url时斜杠的问题
     *
     * @param httpUrl     拼接的基础url
     * @param queryParams 需要拼接的查询参数，可以为null
     * @param paths       需要拼接的path，可以传递为"/path"或"path"，拼接的结果会自动去除多余的斜杠
     * @return 拼接的url
     */
    public static String buildUrl(String httpUrl, Map<String, String> queryParams, String... paths) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(httpUrl);
        if (!ObjectUtils.isEmpty(paths)) {
            for (String path : paths) {
                builder.path("/" + StringUtils.trimTrailingCharacter(path, '/'));
            }
        }
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        return builder.build()
                .normalize()
                .toUri()
                .normalize()
                .toString();
    }

    /**
     * 创建Basic认证的Authorization请求头
     *
     * @param username 用户名
     * @param password 密码
     * @return Base64编码后的token
     */
    public static String basicAuth(String username, String password) {
        Assert.notNull(username, "username不能为null");
        Assert.notNull(password, "password不能为null");
        String credentialsString = username + ":" + password;
        byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 发送一个GET请求
     *
     * @param url 请求地址
     * @return 请求成功后返回的数据
     */
    public static String get(String url) throws RestClientException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();
        return send(request);
    }

    /**
     * 发送一个GET请求
     *
     * @param username Basic认证的用户名
     * @param password Basic认证的密码
     * @param url      请求地址
     * @return 请求成功后返回的数据
     */
    public static String get(String username, String password, String url) throws RestClientException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, basicAuth(username, password))
                .GET()
                .build();
        return send(request);
    }

    /**
     * 发送一个json的POST请求
     *
     * @param url         请求地址
     * @param requestJson 请求json
     * @return 请求成功后返回的数据
     */
    public static String post(String url, String requestJson) throws RestClientException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        return send(request);
    }

    /**
     * 发送一个json的post请求
     *
     * @param username    Basic认证的用户名
     * @param password    Basic认证的密码
     * @param url         请求地址
     * @param requestJson 请求json
     * @return 请求成功后返回的数据
     */
    public static String post(String username, String password, String url, String requestJson) throws RestClientException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, basicAuth(username, password))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        return send(request);
    }

    /**
     * 发送HTTP请求,返回的HTTP状态码为2xx则认为请求成功
     *
     * @param request HTTP请求
     * @return 请求成功返回的结果
     */
    public static String send(HttpRequest request) throws RestClientException {
        return send(request, (statusCode, body) -> HttpStatus.valueOf(statusCode).is2xxSuccessful());
    }

    /**
     * 发送HTTP请求
     *
     * @param request          HTTP请求
     * @param successPredicate 判断请求成功的断言
     * @return 请求成功返回的结果
     */
    public static String send(HttpRequest request, BiPredicate<Integer, String> successPredicate) throws RestClientException {
        HttpResponse<byte[]> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            logger.error("HTTP请求网络错误:{}", e);
            throw new RestClientException("HTTP请求网络错误", e);
        }
        byte[] body = response.body();
        String responseBody = isGzip(body) ? gunzip(body) : new String(body, StandardCharsets.UTF_8);
        if (successPredicate.test(response.statusCode(), responseBody)) {
            return responseBody;
        }
        logger.error("HTTP请求错误,HTTP响应头:{},返回内容:{}", response.headers(), responseBody);
        HttpStatus responseHttpStatus = HttpStatus.valueOf(response.statusCode());
        throw new HttpClientErrorException(responseHttpStatus, responseHttpStatus.toString(),
                body, StandardCharsets.UTF_8);
    }

    /**
     * 异步发送请求
     *
     * @param request         HTTP请求
     * @param completedAction 请求执行完成后执行动作
     */
    public static void sendAsync(HttpRequest request, Consumer<String> completedAction) {
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(completedAction);
    }

    /**
     * 是否压缩
     */
    public static boolean isGzip(byte[] bytes) {
        if ((bytes == null) || (bytes.length < 2)) {
            return false;
        } else {
            return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                    && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
        }
    }

    /**
     * 解压gzip
     */
    public static String gunzip(byte[] bytes) {
        if (bytes.length <= 0) {
            return null;
        }
        try {
            return new BufferedReader(
                    new InputStreamReader(
                            new GZIPInputStream(
                                    new ByteArrayInputStream(bytes)), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining());
        } catch (IOException e) {
            throw new RestClientException("GZip解压失败", e);
        }
    }
}
