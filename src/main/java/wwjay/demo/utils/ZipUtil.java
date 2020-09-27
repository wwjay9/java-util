package wwjay.demo.utils;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.*;

/**
 * zip工具
 *
 * @author wwj
 */
@SuppressWarnings({"unused", "WeakerAccess", "EmptyTryBlock"})
public class ZipUtil {

    private ZipUtil() {
    }

    /**
     * 将多个文件打包成一个zip文件
     *
     * @param zipFile   zip文件路径
     * @param filePaths 文件路径
     */
    public static void pack(final Path zipFile, final Path... filePaths) {
        createZip(zipFile);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            if (filePaths != null && filePaths.length > 0) {
                for (Path path : filePaths) {
                    if (!Files.exists(path)) {
                        throw new ZipException(path.toString() + "文件不存在");
                    }
                    if (!Files.isRegularFile(path)) {
                        throw new ZipException(path.toString() + "不是一个文件");
                    }
                    ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
                    zipOutputStream.putNextEntry(zipEntry);
                    Files.copy(path, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new ZipException("打包zip文件失败:", e);
        }
    }

    /**
     * 打包文件夹，不包含空文件夹，zip文件在源文件夹的同级目录下，文件名为文件夹名
     *
     * @param sourceDirPath 源文件夹
     * @return zip包路径
     */
    public static Path pack(final Path sourceDirPath) {
        Path zipFilePath = Paths.get(sourceDirPath.toString() + ".zip");
        pack(sourceDirPath, zipFilePath);
        return zipFilePath;
    }

    /**
     * 打包文件夹，不包含空文件夹
     *
     * @param sourceDirPath 源文件夹
     * @param zipFile       zip文件路径
     */
    public static void pack(final Path sourceDirPath, final Path zipFile) {
        if (!Files.exists(sourceDirPath)) {
            throw new ZipException("源文件夹不存在");
        }
        if (!Files.isDirectory(sourceDirPath)) {
            throw new ZipException("源路径不是一个文件夹");
        }

        createZip(zipFile);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(sourceDirPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                        try {
                            zipOutputStream.putNextEntry(zipEntry);
                            Files.copy(path, zipOutputStream);
                            zipOutputStream.closeEntry();
                        } catch (IOException e) {
                            throw new ZipException("打包zip文件失败:", e);
                        }
                    });
        } catch (IOException e) {
            throw new ZipException("打包zip文件失败:", e);
        }
    }

    /**
     * 将文件或文件夹添加到zip包中
     *
     * @param zipFile zip文件
     * @param source  需要添加的源路径
     * @param target  添加到zip包中的路径，如果是null则为zip包中的根目录
     */
    public static void append(final Path zipFile, final Path source, final Path target) {
        isValid(zipFile);

        try (FileSystem fs = FileSystems.newFileSystem(zipFile, null)) {
            if (!Files.exists(source)) {
                throw new ZipException("源路径不存在");
            }

            Path path = fs.getPath(target != null ? target.toString() : "/");
            Files.createDirectories(path);

            if (Files.isDirectory(source)) {
                Files.walkFileTree(source, new SimpleFileVisitor<>() {
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
                throw new ZipException("源路径必须是文件或者文件夹");
            }
        } catch (IOException e) {
            throw new ZipException("添加文件失败:", e);
        }
    }

    /**
     * 从zip文件中读取文本内容
     *
     * @param zipFile  zip文件路径
     * @param filePath 要读取的文件在zip包中路径
     * @return 读取的文本
     */
    public static String readString(final Path zipFile, final String filePath) {
        return new String(readByte(zipFile, filePath), StandardCharsets.UTF_8);
    }

    /**
     * 从zip文件中读取文件内容
     *
     * @param zipFile  zip文件路径
     * @param filePath 要读取的文件在zip包中路径
     * @return 读取的数据
     */
    public static byte[] readByte(final Path zipFile, final String filePath) {
        try (FileSystem fs = FileSystems.newFileSystem(zipFile, null)) {
            Path path = fs.getPath(filePath);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ZipException("读取zip文件失败:", e);
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
            throw new ZipException("创建临时目录失败:", e);
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
     * FIXME 由于压缩工具的不同，压缩包的编码可能为操作系统的默认编码，导致压缩包路径中有中文时报java.nio.charset.MalformedInputException异常
     *
     * @param zipPath   zip文件
     * @param target    解压的目标路径
     * @param extension 需要解压的文件扩展名，如 git, jpg
     */
    public static void unzip(final Path zipPath, final Path target, String... extension) {
        isValid(zipPath);
        Set<String> extensionSet = Set.of(extension)
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.stream()
                    // 过滤指定扩展名的文件
                    .filter(zipEntry -> {
                        String extensionName = StringUtils.getFilenameExtension(zipEntry.getName());
                        return CollectionUtils.isEmpty(extensionSet) ||
                                !StringUtils.hasText(extensionName) ||
                                extensionSet.contains(extensionName.toLowerCase());
                    })
                    // 防止被恶意构造的zip文件进行覆盖文件攻击，https://snyk.io/research/zip-slip-vulnerability
                    .map(zipEntry -> {
                        Path zipElementPath = Paths.get(target.toString(), zipEntry.getName());
                        Assert.isTrue(zipElementPath.normalize().startsWith(target.normalize()),
                                "解压的文件在目标文件夹之外: " + zipEntry.getName());
                        return zipEntry;
                    })
                    .forEach(zipEntry -> {
                        Path zipElementPath = Paths.get(target.toString(), zipEntry.getName());
                        try {
                            if (zipEntry.isDirectory()) {
                                Files.createDirectories(zipElementPath);
                            } else {
                                Files.createDirectories(zipElementPath.getParent());
                                // 如果目标文件已经存在则覆盖
                                Files.copy(zipFile.getInputStream(zipEntry), zipElementPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            throw new ZipException("解压文件失败:", e);
                        }
                    });
        } catch (IOException e) {
            throw new ZipException("解压文件失败:", e);
        }
    }

    /**
     * 验证zip文件是否损坏或有效
     */
    public static void isValid(final String filePath) {
        isValid(Paths.get(filePath));
    }

    /**
     * 验证zip文件是否损坏或有效
     */
    public static void isValid(final Path file) {
        try (ZipFile zipfile = new ZipFile(file.toFile())) {
            // 忽略
        } catch (IOException e) {
            throw new ZipException("验证ZIP文件失败:", e);
        }
    }

    /**
     * 验证zip文件是否损坏或有效
     */
    public static void isValid(final InputStream inputStream) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            // 忽略
        } catch (IOException e) {
            throw new ZipException("验证ZIP文件失败:", e);
        }
    }

    /**
     * 解压gzip格式数据
     */
    public static String gunzip(byte[] bytes) {
        if (bytes.length <= 0) {
            return null;
        }
        try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            is.transferTo(os);
            return new String(os.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ZipException("GZip解压失败", e);
        }
    }

    /**
     * 创建zip文件
     */
    private static void createZip(Path path) {
        try {
            Files.createDirectories(path.getParent());
            // 新创建的zip文件会覆盖旧的同名文件
            Files.deleteIfExists(path);
            Files.createFile(path);
        } catch (IOException e) {
            throw new ZipException("创建zip文件失败:", e);
        }
    }

    public static class ZipException extends RuntimeException {

        private static final long serialVersionUID = 3939639697866528050L;

        public ZipException(String message) {
            super(message);
        }

        public ZipException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
