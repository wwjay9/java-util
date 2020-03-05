package wwjay.demo.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.codec.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信公众号工具类
 *
 * @author wwj
 */
@SuppressWarnings({"SpellCheckingInspection", "unused"})
public class MpUtil {

    private MpUtil() {
    }

    private static final Logger logger = LoggerFactory.getLogger(MpUtil.class);
    /**
     * 获取公众号AccessToken的Api
     */
    private static final String ACCESS_TOKEN_API = "https://api.weixin.qq.com/cgi-bin/token";
    /**
     * 获取JS-SDK ticket的Api
     */
    private static final String JS_TICKET_API = "https://api.weixin.qq.com/cgi-bin/ticket/getticket";
    /**
     * AccessToken缓存，正式使用时可存放在Redis，并设置过期时间
     */
    private static final ConcurrentHashMap<String, AccessToken> ACCESS_TOKEN_CACHE = new ConcurrentHashMap<>();
    /**
     * JsapiTicket缓存，正式使用时可存放在Redis，并设置过期时间
     */
    private static final ConcurrentHashMap<String, JsApiTicket> JSAPI_TICKET_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取微信公众号全局接口调用凭证AccessToken，AccessToken应该由此工具类统一管理
     *
     * @param appId     第三方用户唯一凭证
     * @param appSecret 第三方用户唯一凭证密钥
     * @return accessToken
     */
    public static AccessToken getAccessToken(String appId, String appSecret) {
        // 利用ConcurrentHashMap的并发操作原子性来更新accessToken，确保不会重复更新
        return ACCESS_TOKEN_CACHE.compute(appId, (key, accessToken) ->
                verifyToken(accessToken) ? accessToken : requestAccessTokenApi(appId, appSecret));

        // 当使用Redis作为缓存时需要使用DCL（双重检查锁）
        // AccessToken accessToken = ACCESS_TOKEN_CACHE.get(appId);
        // if (!isTokenValid(accessToken)) {
        //     synchronized (ACCESS_TOKEN_CACHE) {
        //         accessToken = ACCESS_TOKEN_CACHE.get(appId);
        //         if (isTokenValid(accessToken)) {
        //             return accessToken;
        //         }
        //         accessToken = requestAccessTokenApi(appId, appSecret);
        //         ACCESS_TOKEN_CACHE.put(appId, accessToken);
        //     }
        // }
        // return accessToken;
    }

    /**
     * 获取JS-SDK的Ticket
     *
     * @param accessToken 全局接口调用凭证AccessToken
     * @return accessToken
     */
    public static JsApiTicket getJsApiTicket(String accessToken) {
        return JSAPI_TICKET_CACHE.compute(accessToken, (key, ticket) ->
                verifyToken(ticket) ? ticket : requestJsTicketApi(accessToken));
    }

    /**
     * 获取微信JS-SDK的配置
     *
     * @param jsApiTicket JS-SDK的Ticket
     * @param url         调用JS-SDK网页的url，不包含#及其后面部分
     * @return wx.config的配置
     */
    public static WxConfig getWxConfig(String jsApiTicket, String url) {
        WxConfig wxConfig = new WxConfig();
        wxConfig.setNonceStr(StringUtil.randomString(12));
        wxConfig.setTimestamp(Instant.now().getEpochSecond());

        SortedMap<String, String> params = new TreeMap<>();
        params.put("noncestr", wxConfig.getNonceStr());
        params.put("jsapi_ticket", jsApiTicket);
        params.put("timestamp", wxConfig.getTimestamp().toString());
        params.put("url", url);

        StringJoiner joiner = new StringJoiner("&");
        params.forEach((k, v) -> joiner.add(k + "=" + v));
        wxConfig.setSignature(sha1Hex(joiner.toString()));
        return wxConfig;
    }

    private static AccessToken requestAccessTokenApi(String appId, String appSecret) {
        Map<String, String> params = Map.of(
                "grant_type", "client_credential",
                "appid", appId,
                "secret", appSecret);
        JSONObject json = requestApi(ACCESS_TOKEN_API, params);
        AccessToken accessToken = new AccessToken();
        accessToken.setAccessToken(json.getString("access_token"));
        accessToken.setExpiresIn(json.getInteger("expires_in"));
        // 提前5分钟过期
        accessToken.setExpireTime(LocalDateTime.now().plusSeconds(accessToken.getExpiresIn()).minusMinutes(5));
        return accessToken;
    }

    private static JsApiTicket requestJsTicketApi(String accessToken) {
        Map<String, String> params = Map.of(
                "access_token", accessToken,
                "type", "jsapi");
        JSONObject json = requestApi(JS_TICKET_API, params);
        JsApiTicket ticket = new JsApiTicket();
        ticket.setTicket(json.getString("ticket"));
        ticket.setExpiresIn(json.getInteger("expires_in"));
        // 提前5分钟过期
        ticket.setExpireTime(LocalDateTime.now().plusSeconds(ticket.getExpiresIn()).minusMinutes(5));
        return ticket;
    }

    private static JSONObject requestApi(String api, Map<String, String> params) {
        String url = HttpUtil.buildUrl(api, params);
        String responseBody = HttpUtil.get(url);
        JSONObject responseJson = JSON.parseObject(responseBody);
        Integer errCode = responseJson.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            logger.error("请求微信AccessToken错误,返回数据:{}", responseBody);
            throw new IllegalArgumentException("请求微信AccessToken错误");
        }
        return responseJson;
    }

    private static <T extends Expired> boolean verifyToken(T token) {
        return token != null && !token.isExpired();
    }

    private static String sha1Hex(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        return new String(Hex.encode(digest.digest(data.getBytes(StandardCharsets.UTF_8))));
    }

    @Getter
    @Setter
    @ToString
    public static class Expired {

        /**
         * 有效期，单位为秒
         */
        private Integer expiresIn;

        /**
         * 到期时间
         */
        private LocalDateTime expireTime;

        /**
         * 是否已过期
         */
        private boolean expired;

        public boolean isExpired() {
            return expireTime.isBefore(LocalDateTime.now());
        }
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class AccessToken extends Expired {

        /**
         * access token
         */
        private String accessToken;
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class JsApiTicket extends Expired {

        /**
         * ticket
         */
        private String ticket;
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class WxConfig {

        /**
         * 是否开启调试模式
         */
        private boolean debug = false;

        /**
         * appId
         */
        private String appId;

        /**
         * 生成签名的时间戳
         */
        private Long timestamp;

        /**
         * 生成签名的随机串
         */
        private String nonceStr;

        /**
         * 签名
         */
        private String signature;
    }
}
