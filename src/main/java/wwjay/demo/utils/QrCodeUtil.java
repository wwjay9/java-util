package wwjay.demo.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * 二维码生成工具
 *
 * @author wwj
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class QrCodeUtil {

    /**
     * 生成二维码的默认格式
     */
    private static final String DEFAULT_FORMAT = "png";
    /**
     * 二维码默认的内边距
     */
    private static final int DEFAULT_MARGIN = 0;
    /**
     * 二维码默认的容错等级
     */
    private static final ErrorCorrectionLevel DEFAULT_ERROR_CORRECTION = ErrorCorrectionLevel.L;

    private QrCodeUtil() {
    }

    /**
     * 保存二维码到文件系统
     *
     * @param text 二维码文本
     * @param size 二维码大小
     * @param path 保存路径
     * @return 二维码的路径
     */
    public static Path writeToPath(String text, int size, Path path) {
        BitMatrix bitMatrix = generate(text, size, size);
        // 从文件路径中提出文件格式
        String format = StringUtils.getFilenameExtension(path.toString());
        Assert.notNull(format, "文件路径不正确,路径必须包含文件扩展名");
        try {
            MatrixToImageWriter.writeToPath(bitMatrix, format, path);
        } catch (IOException e) {
            throw new RuntimeException("保存二维码图片出错", e);
        }
        Assert.isTrue(Files.isRegularFile(path), "未能检测到生成的文件");
        return path;
    }

    /**
     * 生成BufferedImage的二维码
     *
     * @param text 二维码文本
     * @param size 二维码大小
     * @return BufferedImage
     */
    public static BufferedImage generateQrCodeImage(String text, int size) {
        BitMatrix bitMatrix = generate(text, size, size);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * 将二维码写出到输出流
     *
     * @param text         二维码文本
     * @param size         二维码大小
     * @param outputStream 输出流
     */
    public static void writeToStream(String text, int size, OutputStream outputStream) {
        BitMatrix bitMatrix = generate(text, size, size);
        try {
            MatrixToImageWriter.writeToStream(bitMatrix, DEFAULT_FORMAT, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("将二维码写出输出流失败", e);
        }
    }

    /**
     * 生成一个带背景图的二维码
     *
     * @param text             二维码文本
     * @param size             二维码大小
     * @param resourceFilePath 背景图在CalssPath Resource中的路径
     * @param x                二维码偏移量
     * @param y                二维码偏移量
     * @return BufferedImage
     */
    public static BufferedImage generateBackdropQrCode(String text, int size, String resourceFilePath, int x, int y) {
        BufferedImage qrCode = generateQrCodeImage(text, size);
        try (InputStream backdropIs = new ClassPathResource(resourceFilePath).getInputStream()) {
            return splice(ImageIO.read(backdropIs), qrCode, x, y);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取背景图失败", e);
        }
    }

    /**
     * 拼接图片
     *
     * @param backdrop 背景图
     * @param front    前景图
     * @param x        前景图偏移量
     * @param y        前景图偏移量
     * @return 拼接后的图
     */
    public static BufferedImage splice(BufferedImage backdrop, BufferedImage front, int x, int y) {
        Graphics graphics = backdrop.getGraphics();
        graphics.drawImage(front, x, y, null);
        return backdrop;
    }

    /**
     * 在背景图上居中写文本
     *
     * @param backdrop 背景图
     * @param text     文本
     * @param y        垂直偏移量
     * @return 拼接后的图
     */
    public static BufferedImage centerWriteText(BufferedImage backdrop, String text, int y) {
        Graphics graphics = backdrop.getGraphics();
        // 设置字体大小
        Font font = graphics.getFont();
        graphics.setFont(font.deriveFont(Font.BOLD, 30));

        FontMetrics metrics = graphics.getFontMetrics();
        int x = (backdrop.getWidth() - metrics.stringWidth(text)) / 2;

        graphics.drawString(text, x, y);
        return backdrop;
    }

    /**
     * 将正方形的头像修剪成圆形,并覆盖到背景图上
     *
     * @param backdrop 背景图
     * @param avatar   头像
     * @param x        头像的偏移量
     * @param y        头像的偏移量
     * @return 拼接后的图
     */
    public static BufferedImage avatarOverlay(BufferedImage backdrop, BufferedImage avatar, int x, int y) {
        return avatarOverlay(backdrop, avatar, x, y, 1.0);
    }

    /**
     * 将正方形的头像修剪成圆形,并覆盖到背景图上
     *
     * @param backdrop 背景图
     * @param avatar   头像
     * @param x        头像的偏移量
     * @param y        头像的偏移量
     * @param scale    缩放比例,1.0为原比例
     * @return 拼接后的图
     */
    public static BufferedImage avatarOverlay(BufferedImage backdrop, BufferedImage avatar, int x, int y, double scale) {
        BufferedImage zoom = zoom(toCircle(avatar), scale);
        return splice(backdrop, zoom, x, y);
    }

    /**
     * 生成BufferedImage格式的二维码
     *
     * @param text   二维码文本
     * @param width  二维码图片宽度
     * @param height 二维码图片高度
     * @return BitMatrix
     */
    private static BitMatrix generate(String text, int width, int height) {
        Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
        // 字符集
        hintMap.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        // 边界的大小，默认为4像素
        hintMap.put(EncodeHintType.MARGIN, DEFAULT_MARGIN);
        // 容错等级
        hintMap.put(EncodeHintType.ERROR_CORRECTION, DEFAULT_ERROR_CORRECTION);
        try {
            return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hintMap);
        } catch (WriterException e) {
            throw new RuntimeException("二维码生成错误", e);
        }
    }

    /**
     * 将矩形图片转换成圆形图片
     */
    private static BufferedImage toCircle(BufferedImage image) {
        int diameter = Math.min(image.getWidth(), image.getHeight());
        BufferedImage mask = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = mask.createGraphics();
        applyQualityRenderingHints(g2d);
        g2d.fillOval(0, 0, diameter - 1, diameter - 1);
        g2d.dispose();

        BufferedImage masked = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        g2d = masked.createGraphics();
        applyQualityRenderingHints(g2d);
        int x = (diameter - image.getWidth()) / 2;
        int y = (diameter - image.getHeight()) / 2;
        g2d.drawImage(image, x, y, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
        g2d.drawImage(mask, 0, 0, null);
        g2d.dispose();

        return masked;
    }

    /**
     * 消除锯齿
     */
    private static void applyQualityRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /**
     * 保持高宽比来缩放图片
     *
     * @param image 图片源
     * @param scale 缩放比例,1.0为原比例
     * @return 缩放后的图片
     */
    private static BufferedImage zoom(BufferedImage image, double scale) {
        int newWidth = Double.valueOf(image.getWidth() * scale).intValue();
        int newHeight = Double.valueOf(image.getHeight() * scale).intValue();
        BufferedImage after = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g = after.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, newWidth, newHeight, 0, 0, image.getWidth(),
                image.getHeight(), null);
        g.dispose();
        return after;
    }
}