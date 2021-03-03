package com.wwj.util.java.translate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wwj.util.java.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 百度翻译工具类
 *
 * @author wwj
 */
@SuppressWarnings("unused")
@Slf4j
public class BaiduTranslateUtil {

    private static final String TRANSLATE_API = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    private final String appId;
    private final String key;

    private BaiduTranslateUtil(String appId, String key) {
        this.appId = appId;
        this.key = key;
    }

    public static BaiduTranslateUtil create(String appId, String key) {
        return new BaiduTranslateUtil(appId, key);
    }

    /**
     * 翻译文本
     *
     * @param q    文本，UTF-8格式
     * @param from 源语言，可以设置为auto
     * @param to   译文语言，不可设置为auto
     * @return 译文
     */
    public String translate(String q, Lang from, Lang to) {
        Assert.isTrue(to != Lang.AUTO, "译文语言不能为auto");
        String salt = UUID.randomUUID().toString();
        Map<String, String> requestParams = Map.of("q", q,
                "from", from.getIsoCode(),
                "to", to.getIsoCode(),
                "appid", appId,
                "salt", salt,
                "sign", sign(q, salt));
        String responseBody = HttpUtil.post(TRANSLATE_API, requestParams);
        JSONObject response = JSON.parseObject(responseBody);

        String errorCode = response.getString("error_code");
        if (errorCode != null) {
            log.error("百度翻译异常:{}，请求参数:{}", responseBody, requestParams);
            return null;
        }
        return Optional.ofNullable(response.getJSONArray("trans_result"))
                .filter(array -> !array.isEmpty())
                .map(array -> array.getJSONObject(0))
                .map(json -> json.getString("dst"))
                .map(dst -> new String(dst.getBytes(), StandardCharsets.UTF_8))
                .orElse("");
    }

    private String sign(String q, String salt) {
        String s = appId + q + salt + key;
        return DigestUtils.md5DigestAsHex(s.getBytes());
    }

    @SuppressWarnings("unused")
    public enum Lang {
        /**
         * 自动
         */
        AUTO("auto"),
        /**
         * 中文
         */
        ZH("zh"),
        /**
         * 繁体中文
         */
        CHT("cht"),
        /**
         * 英语
         */
        EN("en"),
        /**
         * 粤语
         */
        YUE("yue"),
        /**
         * 文言文
         */
        WYW("wyw"),
        /**
         * 日语
         */
        JP("jp"),
        /**
         * 韩语
         */
        KOR("kor"),
        /**
         * 法语
         */
        FRA("fra"),
        /**
         * 西班牙语
         */
        SPA("spa"),
        /**
         * 泰语
         */
        TH("th"),
        /**
         * 阿拉伯语
         */
        ARA("ara"),
        /**
         * 俄语
         */
        RU("ru"),
        /**
         * 葡萄牙语
         */
        PT("pt"),
        /**
         * 德语
         */
        DE("de"),
        /**
         * 意大利语
         */
        IT("it"),
        /**
         * 希腊语
         */
        EL("el"),
        /**
         * 荷兰语
         */
        NL("nl"),
        /**
         * 波兰语
         */
        PL("pl"),
        /**
         * 保加利亚语
         */
        BUL("bul"),
        /**
         * 爱沙尼亚语
         */
        EST("est"),
        /**
         * 丹麦语
         */
        DAN("dan"),
        /**
         * 芬兰语
         */
        FIN("fin"),
        /**
         * 捷克语
         */
        CS("cs"),
        /**
         * 罗马尼亚语
         */
        ROM("rom"),
        /**
         * 斯洛文尼亚语
         */
        SLO("slo"),
        /**
         * 瑞典语
         */
        SWE("swe"),
        /**
         * 匈牙利语
         */
        HU("hu"),
        /**
         * 越南语
         */
        VIE("vie");

        private final String isoCode;

        Lang(String isoCode) {
            this.isoCode = isoCode;
        }

        public String getIsoCode() {
            return isoCode;
        }
    }

}
