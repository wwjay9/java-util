package wwjay.demo.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 省市区工具类，工具会读取resources/static/pca-code.json文件
 * 数据来源：https://github.com/modood/Administrative-divisions-of-China
 *
 * @author wwj
 */
@SuppressWarnings("unused")
public class PcaUtil {

    private static final Map<String, String> CODE_NAME_MAP = new HashMap<>();
    private static final String MUNICIPALITY = "市辖区";

    static {
        try (InputStream inputStream = new ClassPathResource("static/pca-code.json").getInputStream()) {
            String json = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));
            JSONArray provinceJson = JSON.parseArray(json);
            jsonMap(provinceJson);
        } catch (IOException e) {
            throw new IllegalArgumentException("省市区json文件读取失败");
        }
    }

    private PcaUtil() {
    }

    /**
     * 根据区域代码获取区域名称，当code为直辖市或区时，返回上一级行政区域名称
     *
     * @param code 区域代码
     * @return 区域名称
     */
    public static String checkCode(String code) {
        String name = CODE_NAME_MAP.get(code);
        Assert.notNull(name, "地区代码" + code + "不存在");
        if (Objects.equals(name, MUNICIPALITY)) {
            String s = code.substring(0, code.length() - 2);
            return checkCode(s);
        }
        return name;
    }

    /**
     * 检查地区代码是否正确
     *
     * @param province 省
     * @param city     市
     * @param district 区
     * @return 完整地区名称
     */
    public static String checkCode(String province, String city, String district) {
        Assert.isTrue(StringUtils.hasText(province) && StringUtils.hasText(city) &&
                StringUtils.hasText(district), "地区代码不能为空");
        Assert.isTrue(city.startsWith(province) && district.startsWith(city), "地区代码格式不正确");
        return checkCode(province) + checkCode(city) + checkCode(district);
    }

    /**
     * 将json文件映射到map中
     */
    private static void jsonMap(JSONArray json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        IntStream.range(0, json.size()).forEachOrdered(i -> {
            JSONObject data = json.getJSONObject(i);
            String code = data.getString("code");
            String name = data.getString("name");
            JSONArray children = data.getJSONArray("children");
            CODE_NAME_MAP.put(code, name);
            jsonMap(children);
        });
    }
}
