package wwjay.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author wwj
 */
public class ZipUtil {

    private final static Logger logger = LoggerFactory.getLogger(ZipUtil.class);

    /**
     * 验证zip文件是否损坏或有效
     */
    public static void isValid(final Path file) {
        try (ZipFile zipfile = new ZipFile(file.toFile())) {
        } catch (IOException e) {
            throw new IllegalArgumentException("验证ZIP文件失败:", e);
        }
    }

    /**
     * 验证zip文件是否损坏或有效
     */
    public static void isValid(final InputStream inputStream) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
        } catch (IOException e) {
            throw new IllegalArgumentException("验证ZIP文件失败:", e);
        }
    }

    /**
     * 验证zip文件是否损坏或有效
     */
    public static void isValid(final String filePath) {
        try (ZipFile zipFile = new ZipFile(filePath)) {
        } catch (IOException e) {
            throw new IllegalArgumentException("验证ZIP文件失败:", e);
        }
    }

    /**
     * 将zip包中包含指定扩展名的文件解压到临时目录，并返回临时目录路径
     *
     * @param zipFile zip文件
     */
    public static Path unzipToTemp(final Path zipFile, String... extension) {
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
    public static void unzip(final Path zipFile, String... extension) {
        unzip(zipFile, Paths.get(StringUtils.stripFilenameExtension(zipFile.toString())), extension);
    }

    /**
     * 将zip包中包含指定扩展名的文件解压到指定路径
     *
     * @param zipFile   zip文件
     * @param target    解压的目标路径
     * @param extension 需要解压的文件扩展名，如 git, jpg
     */
    public static void unzip(final Path zipFile, final Path target, String... extension) {
        isValid(zipFile);

        try (FileSystem fs = FileSystems.newFileSystem(zipFile, null)) {
            for (Path root : fs.getRootDirectories()) {
                Files.walk(root).forEach(path -> {
                    try {
                        Path zipElementPath = Paths.get(target.toString(), path.toString());
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(zipElementPath);
                        } else {
                            if (extension != null && extension.length > 0) {
                                if (Stream.of(extension)
                                        .noneMatch(s ->
                                                Objects.equals(StringUtils.getFilenameExtension(path.toString()), s))) {
                                    // 跳过不是指定扩展名的文件
                                    return;
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
     * 将文件或文件夹添加到zip包中
     *
     * @param zipFile zip文件
     * @param source  需要添加的源路径
     * @param target  添加到zip包中的路径，如果是null则为zip包中的根目录
     * @return 添加结果
     */
    public static boolean append(final Path zipFile, final Path source, final Path target) {
        isValid(zipFile);

        try (FileSystem fs = FileSystems.newFileSystem(zipFile, null)) {
            if (!Files.exists(source)) {
                throw new IllegalArgumentException("源路径不存在");
            }

            Path path = fs.getPath(target != null ? target.toString() : "/");
            Files.createDirectories(path);

            if (Files.isDirectory(source)) {
                Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Files.createDirectories(path.resolve(source.relativize(dir).toString()));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.copy(file, path.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else if (Files.isRegularFile(source)) {
                Files.copy(source, path.resolve(source.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new IllegalArgumentException("源路径必须是文件或者文件夹");
            }
        } catch (IOException e) {
            logger.error("添加文件失败:", e);
            return false;
        }
        return true;
    }
}
