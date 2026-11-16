package com.campus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * V1.3 文件操作通用工具类：封装本地文本文件的创建、读取、写入能力。
 *
 * <p>设计原则（迪米特法则）：业务代码不直接操作 IO 流，统一调用工具类，降低耦合。</p>
 *
 * <p>实现约束：全部使用 try-with-resources 自动关闭流；统一 UTF-8 编码，
 * 避免 Windows 默认 GBK 造成中文乱码。</p>
 *
 * @author 课程设计
 * @version V1.3
 */
public final class FileUtil {

    /** 统一文件编码 */
    private static final Charset CHARSET = Charset.forName("UTF-8");

    /** 工具类禁止实例化。 */
    private FileUtil() {
    }

    /**
     * 确保文件存在，不存在则自动创建（含父目录）。
     *
     * @param filePath 文件路径
     */
    public static void createIfNotExists(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!file.createNewFile()) {
                throw new IllegalStateException("文件创建失败：" + filePath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("数据文件初始化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取文件全部行内容。
     *
     * @param filePath 文件路径
     * @return 文件行列表；文件不存在时先自动创建，返回空列表
     */
    public static List<String> readAllLines(String filePath) {
        createIfNotExists(filePath);
        List<String> lines = new ArrayList<String>();
        // try-with-resources 自动关闭流资源
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), CHARSET))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("数据文件读取失败：" + e.getMessage(), e);
        }
        return lines;
    }

    /**
     * 覆盖写入文件全部行内容（先清空再写入）。
     *
     * @param filePath 文件路径
     * @param lines    待写入的行列表
     */
    public static void writeAllLines(String filePath, List<String> lines) {
        createIfNotExists(filePath);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath, false), CHARSET))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("数据文件写入失败：" + e.getMessage(), e);
        }
    }

    /**
     * 追加写入一行内容。
     *
     * @param filePath 文件路径
     * @param line     待追加内容
     */
    public static void appendLine(String filePath, String line) {
        createIfNotExists(filePath);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath, true), CHARSET))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("数据文件写入失败：" + e.getMessage(), e);
        }
    }
}
