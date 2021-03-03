package com.wwj.util.java.wechat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wwj.util.java.HttpUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 微信工具类
 *
 * @author wwj
 */
@SuppressWarnings("unused")
@Slf4j
public class WeChatUtil {

    private static final String ACCESS_TOKEN_API = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
    private static final String USER_INFO_API = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";

    private WeChatUtil() {
    }

    /**
     * 获取微信登录的AccessToken
     */
    public static WeChatAccessToken getAccessToken(String appId, String appSecret, String code) {
        String response = HttpUtil.get(String.format(ACCESS_TOKEN_API, appId, appSecret, code));
        JSONObject json = JSON.parseObject(response);
        String accessToken = json.getString("access_token");
        if (accessToken == null) {
            log.error("获取AccessToken失败,微信返回数据:{}", response);
            throw new IllegalArgumentException("获取AccessToken失败:" + json.getString("errmsg"));
        }
        WeChatAccessToken token = new WeChatAccessToken();
        token.setAccessToken(accessToken);
        token.setExpiresIn(json.getInteger("expires_in"));
        token.setRefreshToken(json.getString("refresh_token"));
        token.setOpenId(json.getString("openid"));
        token.setScope(json.getString("scope"));
        token.setUnionId(json.getString("unionid"));
        return token;
    }

    /**
     * 获取微信用户信息
     */
    public static WeChatUserInfo getUserInfo(String appId, String appSecret, String code) {
        WeChatAccessToken accessToken = getAccessToken(appId, appSecret, code);
        return getUserInfo(accessToken.getAccessToken(), accessToken.getOpenId());
    }

    /**
     * 获取微信用户信息
     */
    public static WeChatUserInfo getUserInfo(String accessToken, String openId) {
        String response = HttpUtil.get(String.format(USER_INFO_API, accessToken, openId));
        JSONObject json = JSON.parseObject(response);
        String openid = json.getString("openid");
        if (openid == null) {
            log.error("获取微信用户信息失败,微信返回数据:{}", response);
            throw new IllegalArgumentException("获取微信用户信息失败:" + json.getString("errmsg"));
        }
        WeChatUserInfo userInfo = new WeChatUserInfo();
        userInfo.setOpenId(openId);
        userInfo.setNickname(json.getString("nickname"));
        userInfo.setSex(json.getInteger("sex"));
        userInfo.setProvince(json.getString("province"));
        userInfo.setCity(json.getString("city"));
        userInfo.setCountry(json.getString("country"));
        userInfo.setHeadImgUrl(json.getString("headimgurl"));
        JSONArray privilege = json.getJSONArray("privilege");
        if (privilege != null && !privilege.isEmpty()) {
            userInfo.setPrivilege(IntStream.range(0, privilege.size())
                    .mapToObj(privilege::getString)
                    .collect(Collectors.toList()));
        }
        userInfo.setUnionId(json.getString("unionid"));
        return userInfo;
    }

    @Data
    public static class WeChatAccessToken {

        private String accessToken;
        private Integer expiresIn;
        private String refreshToken;
        private String openId;
        private String scope;
        private String unionId;
    }

    @Data
    public static class WeChatUserInfo {

        private String openId;
        private String nickname;
        private Integer sex;
        private String province;
        private String city;
        private String country;
        private String headImgUrl;
        private List<String> privilege;
        private String unionId;
    }

}
