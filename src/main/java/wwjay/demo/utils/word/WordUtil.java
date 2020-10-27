package wwjay.demo.utils.word;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Word工具类
 *
 * @author wwj
 */
@SuppressWarnings("unused")
public class WordUtil {

    private WordUtil() {
    }

    /**
     * 替换文档中的占位符，占位符的格式为${……}
     *
     * @param doc           Word文档数据
     * @param templateValue 模板变量
     */
    public static void replace(XWPFDocument doc, Map<String, String> templateValue) {
        templateValue.forEach((k, v) -> replace(doc, k, v));
    }

    /**
     * 替换文档中的占位符，占位符的格式为${……}
     *
     * @param doc     Word文档数据
     * @param oldText 旧文本
     * @param newText 需要替换的文本
     */
    private static void replace(XWPFDocument doc, String oldText, String newText) {
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            getPlaceholderRange(paragraph, oldText)
                    .forEach(range -> replaceText(paragraph, range[0], range[1], newText));
        }
    }

    /**
     * 搜索占位符在段落中的位置
     *
     * @param paragraph   段落
     * @param placeholder 占位符
     * @return 占位符在段落上的开始位置和结束位置
     */
    private static List<int[]> getPlaceholderRange(XWPFParagraph paragraph, String placeholder) {
        String paragraphText = paragraph.getText();
        if (paragraphText == null || !paragraphText.contains(placeholder)) {
            return new ArrayList<>();
        }
        List<XWPFRun> runs = paragraph.getRuns();
        if (CollectionUtils.isEmpty(runs)) {
            return new ArrayList<>();
        }
        List<int[]> list = new ArrayList<>();
        Integer start = null;
        for (int i = 0; i < runs.size(); i++) {
            XWPFRun run = runs.get(i);
            if (!StringUtils.hasText(run.text())) {
                continue;
            }
            String text = run.text().trim();
            if (placeholder.startsWith(text)) {
                start = i;
            }
            if (start != null && placeholder.endsWith(text)) {
                String join = runs.subList(start, i + 1).stream()
                        .map(XWPFRun::text)
                        .collect(Collectors.joining())
                        .trim();
                if (Objects.equals(join, placeholder)) {
                    list.add(new int[]{start, i});
                }
                start = null;
            }
        }
        return list;
    }

    /**
     * 替换段落中的文字
     *
     * @param paragraph   段落
     * @param startRun    开始位置
     * @param endStartRun 结束位置
     * @param newText     替换的文本
     */
    private static void replaceText(XWPFParagraph paragraph, int startRun, int endStartRun, String newText) {
        Assert.isTrue(endStartRun >= startRun, "无效的段落参数");
        List<XWPFRun> runs = paragraph.getRuns();
        runs.get(startRun).setText(newText, 0);
        for (int i = endStartRun; i > startRun; i--) {
            paragraph.removeRun(i);
        }
    }
}
