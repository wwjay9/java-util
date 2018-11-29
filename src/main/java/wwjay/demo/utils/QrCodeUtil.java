package wwjay.demo.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
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
public class QrCodeUtil {

    /**
     * 生成二维码的默认格式
     */
    private static final String DEFAULT_FORMAT = "png";

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
        if (format == null) {
            throw new IllegalArgumentException("文件路径不正确,路径必须包含文件扩展名");
        }
        try {
            MatrixToImageWriter.writeToPath(bitMatrix, format, path);
        } catch (IOException e) {
            throw new RuntimeException("保存二维码图片出错", e);
        }
        if (Files.isRegularFile(path)) {
            return path;
        } else {
            throw new IllegalArgumentException("未能检测到生成的文件");
        }
    }

    /**
     * 生成BufferedImage的二维码
     *
     * @param text 二维码文本
     * @param size 二维码大小
     * @return BufferedImage
     */
    public static BufferedImage generateBufferedImage(String text, int size) {
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
        hintMap.put(EncodeHintType.MARGIN, 0);
        // 容错等级
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        try {
            return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hintMap);
        } catch (WriterException e) {
            throw new RuntimeException("二维码生成错误", e);
        }
    }
}
