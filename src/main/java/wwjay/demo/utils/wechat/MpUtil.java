package wwjay.demo.utils.wechat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.util.StringUtils;
import wwjay.demo.utils.HttpUtil;
import wwjay.demo.utils.StringUtil;
import wwjay.demo.utils.bean.ApplicationContextProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * 微信公众号工具类
 *
 * @author wwj
 */
@SuppressWarnings({"SpellCheckingInspection", "unused"})
@Slf4j
public class MpUtil {

    /**
     * 获取公众号AccessToken的Api
     */
    private static final String ACCESS_TOKEN_API = "https://api.weixin.qq.com/cgi-bin/token";
    /**
     * 获取JS-SDK ticket的Api
     */
    private static final String JS_TICKET_API = "https://api.weixin.qq.com/cgi-bin/ticket/getticket";
    /**
     * AccessTokenRedis的key
     */
    private static final String ACCESS_TOKEN_REDIS_KEY = "wechat:access_token:%s";
    /**
     * AccessTokenRedis的key
     */
    private static final String JSAPI_TICKET_REDIS_KEY = "wechat:jsapi_ticket:%s";

    private MpUtil() {
    }

    /**
     * 获取微信公众号全局接口调用凭证AccessToken，AccessToken应该由此工具类统一管理
     *
     * @param appId     第三方用户唯一凭证
     * @param appSecret 第三方用户唯一凭证密钥
     * @return accessToken
     */
    public static WeChatAccessToken getAccessToken(String appId, String appSecret) {
        return getFromRedis(getAccessTokenRedisKey(appId), WeChatAccessToken.class,
                () -> requestAccessTokenApi(appId, appSecret));
    }

    /**
     * 获取JS-SDK的Ticket
     *
     * @param accessToken 全局接口调用凭证AccessToken
     * @return accessToken
     */
    public static WeChatJsApiTicket getJsApiTicket(String accessToken) {
        return getFromRedis(getJsapiTicketRedisKey(accessToken), WeChatJsApiTicket.class,
                () -> requestJsTicketApi(accessToken));
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

    private static WeChatAccessToken requestAccessTokenApi(String appId, String appSecret) {
        Map<String, String> params = Map.of(
                "grant_type", "client_credential",
                "appid", appId,
                "secret", appSecret);
        JSONObject json = requestApi(ACCESS_TOKEN_API, params);
        WeChatAccessToken accessToken = new WeChatAccessToken();
        accessToken.setAccessToken(json.getString("access_token"));
        accessToken.setExpiresIn(json.getInteger("expires_in"));
        // 提前5分钟过期
        accessToken.setExpireTime(LocalDateTime.now().plusSeconds(accessToken.getExpiresIn()).minusMinutes(5));
        return accessToken;
    }

    private static WeChatJsApiTicket requestJsTicketApi(String accessToken) {
        Map<String, String> params = Map.of(
                "access_token", accessToken,
                "type", "jsapi");
        JSONObject json = requestApi(JS_TICKET_API, params);
        WeChatJsApiTicket ticket = new WeChatJsApiTicket();
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
            log.error("请求微信AccessToken错误,返回数据:{}", responseBody);
            throw new IllegalArgumentException("请求微信AccessToken错误");
        }
        return responseJson;
    }

    private static <T extends Expire> boolean verifyToken(T token) {
        return token != null && !token.isExpired();
    }

    private static String getAccessTokenRedisKey(String appid) {
        return String.format(ACCESS_TOKEN_REDIS_KEY, appid);
    }

    private static String getJsapiTicketRedisKey(String accessToken) {
        return String.format(JSAPI_TICKET_REDIS_KEY, accessToken);
    }

    private static <T extends Expire> void saveToRedis(String key, T token) {
        StringRedisTemplate redisTemplate = ApplicationContextProvider.getBean(StringRedisTemplate.class);
        redisTemplate.opsForValue().set(key, JSON.toJSONString(token), Duration.ofSeconds(token.getExpiresIn()));
    }

    private static <T extends Expire> T getFromRedis(String key, Class<T> tokenType, Supplier<T> tokenSupplier) {
        StringRedisTemplate redisTemplate = ApplicationContextProvider.getBean(StringRedisTemplate.class);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .filter(StringUtils::hasText)
                .map(json -> JSON.parseObject(json, tokenType))
                .filter(MpUtil::verifyToken)
                .orElseGet(() -> {
                    T token = tokenSupplier.get();
                    saveToRedis(key, token);
                    return token;
                });
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
    public static class Expire {

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
    public static class WeChatAccessToken extends Expire {

        /**
         * access token
         */
        private String accessToken;
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class WeChatJsApiTicket extends Expire {

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
