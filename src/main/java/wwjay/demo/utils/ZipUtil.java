package wwjay.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author wwjay
 */
public class ZipUtil {

    private final static Logger logger = LoggerFactory.getLogger(ZipUtil.class);

    /**
     * 验证zip文件是否损坏或有效
     */
    public static boolean isValid(final File file) {
        try (ZipFile zipfile = new ZipFile(file)) {
            return true;
        } catch (IOException e) {
            logger.error("验证ZIP文件失败:", e);
            return false;
        }
    }

    /**
     * 验证zip文件是否损坏或有效
     */
    public static boolean isValid(final InputStream inputStream) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            return true;
        } catch (IOException e) {
            logger.error("验证ZIP文件失败:", e);
            return false;
        }
    }

    /**
     * 验证zip文件是否损坏或有效
     */
    public static boolean isValid(final String filePath) {
        try (ZipFile zipFile = new ZipFile(filePath)) {
            return true;
        } catch (IOException e) {
            logger.error("验证ZIP文件失败:", e);
            return false;
        }
    }

    /**
     * 将zip包中包含指定扩展名的文件解压到临时目录，并返回临时目录路径
     *
     * @param zipFile zip文件
     */
    public static Path unzipToTemp(File zipFile, String... extension) {
        Path tempPath;
        try {
            tempPath = Files.createTempDirectory(null);
        } catch (IOException e) {
            logger.error("创建临时目录失败:", e);
            return null;
        }
        unzip(zipFile, tempPath, extension);
        return tempPath;
    }

    /**
     * 将zip包中包含指定扩展名的文件解压到当前目录，目录名为zip的文件名
     *
     * @param zipFile zip文件
     */
    public static void unzip(File zipFile, String... extension) {
        unzip(zipFile, Paths.get(StringUtils.stripFilenameExtension(zipFile.getPath())), extension);
    }

    /**
     * 将zip包中包含指定扩展名的文件解压到指定路径
     *
     * @param zipFile    zip文件
     * @param targetPath 解压的目标路径
     * @param extension  需要解压的文件扩展名，如 git, jpg
     */
    public static void unzip(File zipFile, Path targetPath, String... extension) {
        if (!isValid(zipFile)) {
            return;
        }

        try (FileSystem fs = FileSystems.newFileSystem(zipFile.toPath(), null)) {
            for (Path root : fs.getRootDirectories()) {
                Files.walk(root).forEach(path -> {
                    try {
                        Path zipElementPath = Paths.get(targetPath.toString(), path.toString());
                        if (Files.isDirectory(zipElementPath)) {
                            Files.createDirectories(zipElementPath);
                        } else {
                            if (extension != null) {
                                for (String s : extension) {
                                    if (Objects.equals(StringUtils.getFilenameExtension(path.toString()), s)) {
                                        return;
                                    }
                                }
                            }
                            // 如果目标文件已经存在则覆盖
                            Files.copy(path, zipElementPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        logger.error("解压文件失败:", e);
                    }
                });
            }
        } catch (IOException e) {
            logger.error("解压文件失败:", e);
        }
    }

    /**
     * 将文件添加到zip包中
     *
     * @param zipFile  zip文件
     * @param fromPath 需要添加的文件路径
     * @param toPath   添加到的zip包中路径
     * @return 添加结果
     */
    public static boolean appendFile(File zipFile, Path fromPath, Path toPath) {
        try (FileSystem fs = FileSystems.newFileSystem(zipFile.toPath(), null)) {
            Path path = fs.getPath(toPath.toString());
            Files.copy(fromPath, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("添加文件失败:", e);
            return false;
        }
        return true;
    }
}
