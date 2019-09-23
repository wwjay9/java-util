package wwjay.demo.utils;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * 微信支付工具类
 *
 * @author wwj
 */
@SuppressWarnings({"unused", "WeakerAccess", "AlibabaLowerCamelCaseVariableNaming"})
public class WeChatPayUtil {

    public static final String APP_ID = "appid";
    public static final String MCH_ID = "mch_id";
    public static final String DEVICE_INFO = "device_info";
    public static final String NONCE_STR = "nonce_str";
    public static final String SIGN = "sign";
    public static final String SIGN_TYPE = "sign_type";
    public static final String BODY = "body";
    public static final String DETAIL = "detail";
    public static final String ATTACH = "attach";
    public static final String OUT_TRADE_NO = "out_trade_no";
    public static final String FEE_TYPE = "fee_type";
    public static final String TOTAL_FEE = "total_fee";
    public static final String SP_BILL_CREATE_IP = "spbill_create_ip";
    public static final String TIME_START = "time_start";
    public static final String TIME_EXPIRE = "time_expire";
    public static final String GOODS_TAG = "goods_tag";
    public static final String NOTIFY_URL = "notify_url";
    public static final String TRADE_TYPE = "trade_type";
    public static final String PRODUCT_ID = "product_id";
    public static final String LIMIT_PAY = "limit_pay";
    public static final String OPEN_ID = "openid";
    public static final String SCENE_INFO = "scene_info";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String RETURN_CODE = "return_code";
    public static final String RETURN_MSG = "return_msg";
    public static final String RESULT_CODE = "result_code";
    public static final String ERR_CODE = "err_code";
    public static final String ERR_CODE_DES = "err_code_des";
    public static final String PREPAY_ID = "prepay_id";
    public static final String CODE_URL = "code_url";
    public static final String SUCCESS = "SUCCESS";
    /**
     * 微信的统一下单接口地址
     */
    private static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();
    /**
     * 默认的支付方式，禁止使用信用卡
     */
    private static final String DEFAULT_LIMIT_PAY = "no_credit";
    /**
     * 默认的签名类型
     */
    private static final String DEFAULT_SIGN_TYPE = "HMAC-SHA256";

    static {
        // 微信接口返回的数据没有指定字符编码
        REST_TEMPLATE.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    private WeChatPayUtil() {
    }

    /**
     * 请求jsApi统一下单接口
     *
     * @param appId          微信支付分配的公众账号ID（企业号corpid即为此appId）
     * @param key            key
     * @param mchId          微信支付分配的商户号
     * @param body           商品描述
     * @param outTradeNo     商户订单号
     * @param totalFee       订单总金额，单位为分
     * @param spBillCreateIp 终端IP，APP和网页支付提交用户端ip，Native支付填调用微信支付API的机器IP
     * @param notifyUrl      通知地址，异步接收微信支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
     * @param openId         用户标识
     * @return 返回前端JS调用微信支付的请求参数列表
     */
    public static Map<String, String> jsApiPay(String appId, String key, String mchId, String body, String outTradeNo,
                                               int totalFee, String spBillCreateIp, String notifyUrl, String openId) {
        String bodyXml = generateUnifiedOrderXml(appId, key, mchId, body, outTradeNo,
                totalFee, spBillCreateIp, notifyUrl, TradeType.JSAPI, null, openId);
        Map<String, String> responseMap = unifiedOrderRequest(bodyXml, key);
        String prepayId = responseMap.get(PREPAY_ID);

        SortedMap<String, String> jsApiPayParams = new TreeMap<>();
        jsApiPayParams.put("appId", appId);
        jsApiPayParams.put("timeStamp", Instant.now().getEpochSecond() + "");
        jsApiPayParams.put("nonceStr", StringUtil.randomString());
        jsApiPayParams.put("package", "prepay_id=" + prepayId);
        jsApiPayParams.put("signType", DEFAULT_SIGN_TYPE);
        jsApiPayParams.put("paySign", sign(jsApiPayParams, key));
        return jsApiPayParams;
    }

    /**
     * 请求Native统一下单接口
     *
     * @param appId          微信支付分配的公众账号ID
     * @param key            key
     * @param mchId          微信支付分配的商户号
     * @param body           商品描述
     * @param outTradeNo     商户订单号
     * @param totalFee       订单总金额，单位为分
     * @param spBillCreateIp 终端IP，APP和网页支付提交用户端ip，Native支付填调用微信支付API的机器IP
     * @param notifyUrl      通知地址，异步接收微信支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
     * @param productId      商品ID
     * @return 返回二维码链接
     */
    public static String nativePay(String appId, String key, String mchId, String body, String outTradeNo,
                                   int totalFee, String spBillCreateIp, String notifyUrl, String productId) {
        String bodyXml = generateUnifiedOrderXml(appId, key, mchId, body, outTradeNo,
                totalFee, spBillCreateIp, notifyUrl, TradeType.NATIVE, productId, null);
        Map<String, String> responseMap = unifiedOrderRequest(bodyXml, key);
        return responseMap.get(CODE_URL);
    }

    /**
     * XML格式字符串转换为Map
     */
    public static Map<String, String> xmlToMap(String xmlString) {
        DocumentBuilder documentBuilder = newDocumentBuilder();
        try (InputStream stream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))) {
            Document doc = documentBuilder.parse(stream);
            Element rootElement = doc.getDocumentElement();
            rootElement.normalize();

            Map<String, String> data = new LinkedHashMap<>();
            DomUtils.getChildElements(rootElement)
                    .forEach(element -> data.put(element.getTagName(), DomUtils.getTextValue(element)));
            return data;
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException("解析xml异常", e);
        }
    }

    /**
     * 验证微信请求的参数
     *
     * @param params 参数列表
     * @param key    key
     * @return 验证结果
     */
    public static boolean verify(Map<String, String> params, String key) {
        String sign = params.get(SIGN);
        Assert.notNull(sign, "未包含签名参数");
        SortedMap<String, String> sortedMap = new TreeMap<>(params);
        sortedMap.remove(SIGN);
        return Objects.equals(sign(sortedMap, key), sign);
    }

    /**
     * 微信支付通知时返回的成功信息
     */
    public static String successXml() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    /**
     * 生成统一订单接口的请求xml
     *
     * @param appId          微信支付分配的公众账号ID（企业号corpid即为此appId）
     * @param key            key
     * @param mchId          微信支付分配的商户号
     * @param body           商品描述
     * @param outTradeNo     商户订单号
     * @param totalFee       订单总金额，单位为分
     * @param spBillCreateIp 终端IP，APP和网页支付提交用户端ip，Native支付填调用微信支付API的机器IP
     * @param notifyUrl      通知地址，异步接收微信支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
     * @param tradeType      交易类型
     * @param productId      商品ID，trade_type=NATIVE时，此参数必传。此参数为二维码中包含的商品ID，商户自行定义。
     * @param openId         用户标识，trade_type=JSAPI时（即JSAPI支付），此参数必传
     * @return 订单请求的xml
     */
    private static String generateUnifiedOrderXml(String appId, String key, String mchId, String body, String outTradeNo,
                                                  int totalFee, String spBillCreateIp, String notifyUrl,
                                                  TradeType tradeType, String productId, String openId) {
        Assert.hasText(appId, "appId不能为空值");
        Assert.hasText(key, "key不能为空值");
        Assert.hasText(mchId, "mchId不能为空值");
        Assert.hasText(body, "body不能为空值");
        Assert.hasText(outTradeNo, "outTradeNo不能为空值");
        Assert.isTrue(totalFee > 0, "totalFee必须大于0");
        Assert.hasText(spBillCreateIp, "spbillCreateIp不能为空值");
        Assert.hasText(notifyUrl, "notifyUrl不能为空值");
        Assert.notNull(tradeType, "tradeType不能为null");
        if (tradeType == TradeType.NATIVE && !StringUtils.hasText(productId)) {
            throw new IllegalArgumentException("当使用NATIVE支付时productId必传");
        }
        if (tradeType == TradeType.JSAPI && !StringUtils.hasText(openId)) {
            throw new IllegalArgumentException("当使用JSAPI支付时openId必传");
        }

        SortedMap<String, String> params = new TreeMap<>();
        params.put(APP_ID, appId);
        params.put(MCH_ID, mchId);
        params.put(NONCE_STR, StringUtil.randomString());
        params.put(SIGN_TYPE, DEFAULT_SIGN_TYPE);
        params.put(BODY, body);
        params.put(OUT_TRADE_NO, outTradeNo);
        params.put(TOTAL_FEE, totalFee + "");
        params.put(SP_BILL_CREATE_IP, spBillCreateIp);
        params.put(NOTIFY_URL, notifyUrl);
        params.put(TRADE_TYPE, tradeType.name());
        params.put(LIMIT_PAY, DEFAULT_LIMIT_PAY);
        if (productId != null) {
            params.put(PRODUCT_ID, productId);
        }
        if (openId != null) {
            params.put(OPEN_ID, openId);
        }
        params.put(SIGN, sign(params, key));

        return mapToXmlString(params);
    }

    /**
     * 请求微信的统一下单接口
     */
    private static Map<String, String> unifiedOrderRequest(String requestBody, String key) {
        String responseXml = REST_TEMPLATE.postForObject(UNIFIED_ORDER_URL, requestBody, String.class);
        Map<String, String> responseMap = xmlToMap(responseXml);
        if (!(responseMap.containsKey(RETURN_CODE) && responseMap.containsKey(RESULT_CODE) &&
                Objects.equals(responseMap.get(RETURN_CODE).toUpperCase(), SUCCESS) &&
                Objects.equals(responseMap.get(RESULT_CODE).toUpperCase(), SUCCESS))) {
            throw new IllegalArgumentException("请求微信支付下单接口失败,返回的错误信息:" + responseXml);
        }
        if (!verify(responseMap, key)) {
            throw new IllegalArgumentException("验证微信返回数据失败,返回信息:" + responseXml);
        }
        return responseMap;
    }

    /**
     * 参数签名
     */
    private static String sign(SortedMap<String, String> params, String key) {
        StringJoiner stringJoiner = new StringJoiner("&");
        params.forEach((k, v) -> stringJoiner.add(k + "=" + v));
        stringJoiner.add("key=" + key);
        return digest(stringJoiner.toString(), key).toUpperCase();
    }

    /**
     * 加密文本
     */
    private static String digest(String text, String key) {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSHA256.init(secretKeySpec);
            byte[] bytes = hmacSHA256.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(bytes));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * map转xml字符串
     */
    private static String mapToXmlString(Map<String, String> map) {
        DocumentBuilder docBuilder = newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("xml");
        map.forEach((k, v) -> {
            Element e = doc.createElement(k);
            e.appendChild(doc.createCDATASection(v));
            rootElement.appendChild(e);
        });
        doc.appendChild(rootElement);
        return documentToString(doc);
    }

    /**
     * xml文档转字符串
     */
    private static String documentToString(Document document) {
        try {
            StringWriter stringWriter = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (TransformerException e) {
            throw new IllegalArgumentException("转换xml错误,", e);
        }
    }

    /**
     * 创建文档构建器
     */
    private static DocumentBuilder newDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException("创建xml文档构建器异常", e);
        }
    }

    /**
     * 创建自定义的秘钥创建SSLContext
     *
     * @param p12File  *.p12的证书文件
     * @param password 证书密码
     * @return SSLContext
     */
    private static SSLContext createSSLContext(InputStream p12File, String password) {
        SSLContext sc;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(p12File, password.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());
            sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("创建SSLContext失败");
        }
        return sc;
    }

    /**
     * 交易类型枚举类
     */
    @SuppressWarnings("SpellCheckingInspection")
    public enum TradeType {
        /**
         * JSAPI支付（或小程序支付）
         */
        JSAPI,
        /**
         * Native支付
         */
        NATIVE,
        /**
         * app支付
         */
        APP,
        /**
         * H5支付
         */
        MWEB
    }
}
