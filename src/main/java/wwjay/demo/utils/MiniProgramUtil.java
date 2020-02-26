package wwjay.demo.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/**
 * 小程序工具类
 *
 * @author wwj
 */
@SuppressWarnings({"SpellCheckingInspection", "unused"})
public class MiniProgramUtil {

    private static final Logger logger = LoggerFactory.getLogger(MiniProgramUtil.class);
    private static final String CODE2_SESSION_API = "https://api.weixin.qq.com/sns/jscode2session";

    private MiniProgramUtil() {
    }

    /**
     * 获取小程序的JsCode2Session
     *
     * @param appId  小程序appId
     * @param secret 小程序appSecret
     * @param jsCode wx.login时获取的code
     * @return session信息
     */
    public static JsCode2Session getJsCode2Session(String appId, String secret, String jsCode) {
        String url = HttpUtil.buildUrl(CODE2_SESSION_API, Map.of(
                "appid", appId, "secret",
                secret, "js_code", jsCode,
                "grant_type", "authorization_code"));
        String responseBody = HttpUtil.get(url);
        JSONObject responseJson = JSON.parseObject(responseBody);
        Integer errCode = responseJson.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            logger.error("获取小程序JsCode2Session错误:{}", responseBody);
            throw new IllegalArgumentException("获取小程序JsCode2Session错误");
        }
        JsCode2Session session = new JsCode2Session();
        session.setOpenid(responseJson.getString("openid"));
        session.setSessionKey(responseJson.getString("session_key"));
        session.setUnionId(responseJson.getString("unionid"));
        return session;
    }

    /**
     * 小程序前端调用getPhoneNumber后解密出手机号
     *
     * @param appId         小程序appId
     * @param sessionKey    JsCode2Session的sessionKey
     * @param encryptedData 加密数据
     * @param iv            加密算法的初始向量
     * @return 手机号
     */
    public static String getPhoneNumber(String appId, String sessionKey, String encryptedData, String iv) {
        JSONObject data = decodeData(appId, sessionKey, encryptedData, iv);
        return data.getString("phoneNumber");
    }

    /**
     * 小程序加密数据的解密
     *
     * @param appId         小程序appId
     * @param sessionKey    JsCode2Session的sessionKey
     * @param encryptedData 加密数据
     * @param iv            加密算法的初始向量
     * @return 解密出的原始数据
     */
    private static JSONObject decodeData(String appId, String sessionKey, String encryptedData, String iv) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] keyByte = decoder.decode(sessionKey);
        byte[] ivByte = decoder.decode(iv);
        byte[] encryptedDataByte = decoder.decode(encryptedData);
        String data;
        try {
            // 生成Key对象
            Key key = new SecretKeySpec(keyByte, "AES");
            // 把向量初始化到算法参数
            AlgorithmParameterSpec params = new IvParameterSpec(ivByte);

            // 指定算法，模式，填充方法 创建一个Cipher实例
            // PKCS#5与PKCS#7大致相同，当使用"AES/CBC/PKCS7Padding"时需要添加org.bouncycastle:bcprov-jdk15on依赖，
            // 并加上Security.addProvider(new BouncyCastleProvider())
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            // 指定用途，密钥，参数 初始化Cipher对象
            cipher.init(Cipher.DECRYPT_MODE, key, params);
            // 解密数据
            data = new String(cipher.doFinal(encryptedDataByte));
        } catch (GeneralSecurityException e) {
            logger.error("解密小程序数据失败", e);
            throw new IllegalArgumentException("数据解析失败");
        }
        JSONObject dataJson = JSON.parseObject(data);
        Assert.isTrue(Objects.equals(dataJson.getJSONObject("watermark").getString("appid"), appId), "appId不匹配");
        return dataJson;
    }

    @Getter
    @Setter
    @ToString
    public static class JsCode2Session {

        /**
         * 用户唯一标识
         */
        private String openid;

        /**
         * 会话密钥
         */
        private String sessionKey;

        /**
         * 用户在开放平台的唯一标识符
         */
        private String unionId;
    }
}
