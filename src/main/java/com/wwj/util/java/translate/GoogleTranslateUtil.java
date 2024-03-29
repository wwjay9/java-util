package com.wwj.util.java.translate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wwj.util.java.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.Optional;

/**
 * Google翻译工具类
 * <a href="https://translation.googleapis.com/language/translate/v2/languages/?target=zh&key=YOUR_API_KEY_HERE">支持的语言列表</a>
 *
 * @author wwj
 */
@SuppressWarnings("unused")
@Slf4j
public class GoogleTranslateUtil {

    private static final String TRANSLATE_API = "https://translation.googleapis.com/language/translate/v2";

    private final String key;

    private GoogleTranslateUtil(String key) {
        this.key = key;
    }

    public static GoogleTranslateUtil create(String key) {
        return new GoogleTranslateUtil(key);
    }

    /**
     * 翻译文本
     *
     * @param q      文本
     * @param source 源语言的iso-639-1码
     * @param target 译文语言iso-639-1码
     * @return 译文
     */
    public String translate(String q, String source, String target) {
        String requestJson = JSON.toJSONString(Map.of(
                "q", q,
                "source", source,
                "target", target,
                "format", "text"
        ));

        String responseBody;
        try {
            responseBody = HttpUtil.post(TRANSLATE_API + "?key=" + key, requestJson);
        } catch (HttpClientErrorException e) {
            log.error("Google翻译异常:{}，请求参数:{}", e.getResponseBodyAsString(), requestJson);
            return null;
        }
        JSONObject response = JSON.parseObject(responseBody);

        return Optional.ofNullable(response.getJSONObject("data"))
                .map(json -> json.getJSONArray("translations"))
                .filter(array -> !array.isEmpty())
                .map(array -> array.getJSONObject(0))
                .map(json -> json.getString("translatedText"))
                .orElse("");
    }
}
