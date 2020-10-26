package wwjay.demo.utils;


import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * HTTP工具类
 *
 * @author wwj
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class HttpUtil {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

    private HttpUtil() {
    }

    /**
     * 新建一个带Cookie管理器的HttpClient
     */
    public static HttpClient newCookieHttpClient() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
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
    public static String basicEncoder(String username, String password) {
        Assert.notNull(username, "username不能为null");
        Assert.notNull(password, "password不能为null");
        String credentialsString = username + ":" + password;
        byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 将BasicAuth的请求头解码
     *
     * @param encoderStr 包含basic的请求头
     * @return [username, password]
     */
    public static UsernamePasswordAuthenticationToken basicDecoder(String encoderStr) {
        Assert.isTrue(StringUtils.startsWithIgnoreCase(encoderStr, "Basic "), "BasicAuth格式不正确");
        String base64Credentials = encoderStr.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] split = credentials.split(":", 2);
        return new UsernamePasswordAuthenticationToken(split[0], split[1]);
    }

    /**
     * 构建一个form-urlencoded的post请求
     *
     * @param url      请求地址
     * @param formData 表单数据
     * @return HttpRequest
     */
    public static HttpRequest formUrlencodedRequest(String url, Map<String, String> formData) {
        StringJoiner body = new StringJoiner("&");
        formData.forEach((k, v) -> body.add(k + "=" + v));
        return HttpRequest.newBuilder(URI.create(url))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }

    /**
     * 尝试获取当前请求的真实ip地址
     *
     * @return ip地址
     */
    public static String getCurrentRequestIpAddr() {
        HttpServletRequest request = getCurrentHttpServletRequest();
        return List.of("X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR")
                .stream()
                .map(request::getHeader)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(s -> !"unknown".equalsIgnoreCase(s))
                .map(s -> new StringTokenizer(s, ",").nextToken().trim())
                .findFirst()
                .orElseGet(request::getRemoteAddr);
    }

    /**
     * 获取当前线程绑定的HttpServletRequest
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getCurrentHttpServletRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        throw new IllegalArgumentException("未在当前线程找到HttpServletRequest");
    }

    /**
     * 发送一个GET请求
     *
     * @param url 请求地址
     * @return 请求成功后返回的数据
     * @throws RestClientException 网络异常
     */
    public static String get(String url) {
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
     * @throws RestClientException 网络异常
     */
    public static String get(String username, String password, String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, basicEncoder(username, password))
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
     * @throws RestClientException 网络异常
     */
    public static String post(String url, String requestJson) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        return send(request);
    }

    /**
     * 发送一个Form-Data的POST请求
     *
     * @param url      请求地址
     * @param formData 请求formData数据
     * @return 请求成功后返回的数据
     * @throws RestClientException 网络异常
     */
    public static String post(String url, Map<String, String> formData) {
        return send(formUrlencodedRequest(url, formData));
    }

    /**
     * 发送一个json的post请求
     *
     * @param username    Basic认证的用户名
     * @param password    Basic认证的密码
     * @param url         请求地址
     * @param requestJson 请求json
     * @return 请求成功后返回的数据
     * @throws RestClientException 网络异常
     */
    public static String post(String username, String password, String url, String requestJson) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, basicEncoder(username, password))
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        return send(request);
    }

    /**
     * 发送HTTP请求,返回的HTTP状态码为2xx则认为请求成功
     *
     * @param request HTTP请求
     * @return 请求成功返回的结果
     * @throws RestClientException 网络异常
     */
    public static String send(HttpRequest request) {
        return send(request, (statusCode, body) -> HttpStatus.valueOf(statusCode).is2xxSuccessful());
    }

    /**
     * 发送HTTP请求
     *
     * @param request          HTTP请求
     * @param successPredicate 判断请求成功的断言
     * @return 请求成功返回的结果
     * @throws RestClientException 网络异常
     */
    public static String send(HttpRequest request, BiPredicate<Integer, String> successPredicate) {
        HttpResponse<byte[]> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new RestClientException("HTTP请求网络错误", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestClientException("HTTP请求网络错误", e);
        }
        byte[] body = response.body();
        String responseBody = isGzip(body) ? gunzip(body) : new String(body, StandardCharsets.UTF_8);
        if (successPredicate.test(response.statusCode(), responseBody)) {
            return responseBody;
        }
        HttpStatus responseHttpStatus = HttpStatus.valueOf(response.statusCode());
        HttpHeaders httpHeaders = new HttpHeaders();
        response.headers().map().forEach(httpHeaders::addAll);

        throw HttpClientErrorException.create(responseHttpStatus, responseHttpStatus.name(),
                httpHeaders, body, StandardCharsets.UTF_8);
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
        int minLength = 2;
        if ((bytes == null) || (bytes.length < minLength)) {
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
        try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            is.transferTo(os);
            return new String(os.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RestClientException("GZip解压失败", e);
        }
    }

    @Getter
    public static class UsernamePasswordAuthenticationToken {

        public UsernamePasswordAuthenticationToken(String username, String password) {
            this.username = username;
            this.password = password;
        }

        private final String username;
        private final String password;
    }
}
