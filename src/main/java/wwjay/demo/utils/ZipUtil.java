package wwjay.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author wwjay
 */
public class ZipUtil {

    private final static Logger logger = LoggerFactory.getLogger(ZipUtil.class);

    /**
     * 缓存大小
     */
    private static final int BUFFER_SIZE = 4096;

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
     * 将zip文件解压到zip文件的当前目录，目录名为zip的文件名
     *
     * @param zipFile zip文件
     */
    public static void unzip(File zipFile) {
        String path = zipFile.getPath();
        Path targetDir = path.contains(".") ?
                Paths.get(path.substring(0, path.lastIndexOf('.'))) : Paths.get(path);
        unzip(zipFile, targetDir);
    }

    /**
     * 解压zip文件到指定路径
     *
     * @param zipFile    zip文件
     * @param targetPath 解压的目标路径
     */
    public static void unzip(File zipFile, Path targetPath) {
        if (!isValid(zipFile)) {
            return;
        }

        try (FileSystem fs = FileSystems.newFileSystem(zipFile.toPath(), null)) {
            fs.getRootDirectories()
                    .forEach(root -> {
                        try {
                            Files.walk(root).forEach(path -> {
                                Path zipElementPath = targetPath.resolve(path);
                                try {
                                    Files.copy(path, zipElementPath);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // try (ZipFile zip = new ZipFile(zipFile)) {
        //     // 如果解压目录不存在则创建
        //     Files.createDirectories(targetPath);
        //     // 遍历所有的zip文件内容
        //     Enumeration<? extends ZipEntry> entries = zip.entries();
        //     while (entries.hasMoreElements()) {
        //         ZipEntry zipEntry = entries.nextElement();
        //         Path entryPath = Paths.get(zipEntry.getName());
        //         Path resolve = targetPath.resolve(entryPath);
        //         if (zipEntry.isDirectory()) {
        //             System.out.println(zipEntry);
        //         }
        //     }
        // } catch (IOException e) {
        //     logger.error("解压zip文件失败:", e);
        //     e.printStackTrace();
        // }

        // try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile.getPath()))) {
        //     ZipEntry entry;
        //     while ((entry = zin.getNextEntry()) != null) {
        //
        //     }
        //     // 遍历zip包中所有文件
        //     while (entry != null) {
        //         String filePath = targetPath + File.separator + entry.getName();
        //         if (!entry.isDirectory()) {
        //             // 如果是为文件则提取
        //             extractFile(zin, filePath);
        //         } else {
        //             // 如果是文件夹则创建
        //             File dir = new File(filePath);
        //             dir.mkdir();
        //         }
        //         zin.closeEntry();
        //         entry = zin.getNextEntry();
        //     }
        // } catch (IOException e) {
        //     logger.error("解压zip文件失败:", e);
        // }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    public static void main(String[] args) throws IOException {
        ZipUtil.unzip(new File("D:/新建文件夹/DnsJumper.zip"));
        // Path path = Paths.get("a", "b", "c");
        // Path d = path.resolveSibling("d");

    }
}
