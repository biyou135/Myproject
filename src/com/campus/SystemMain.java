package com.campus;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * V1.0 项目初始化：程序入口 + 控制台登录。
 *
 * <p>版本约束（任务1）：本阶段全部逻辑写在主类中，不做分层、不抽工具类，
 * 故意形成代码耦合，为后续迭代（任务2~任务10）制造重构空间。</p>
 *
 * @author 课程设计
 * @version V1.0
 */
public class SystemMain {

    /**
     * 统一控制台输出编码，避免中文乱码。
     *
     * <p>JDK 18 起 {@code System.out} 使用 {@code stdout.encoding}（中文 Windows 默认 GBK），
     * 而 IntelliJ IDEA 控制台按 UTF-8 解码，两者不一致就会显示成乱码。
     * 这里在启动时把标准输出、标准错误固定为 UTF-8，做到源码、编译、运行全链路一致。</p>
     *
     * <p>提示：若在 Windows 命令行（cmd）直接运行，请先执行 {@code chcp 65001} 切换到 UTF-8。</p>
     */
    private static void initConsoleEncoding() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
    }

    /** 内置管理员账号（V1.0 阶段硬编码） */
    private static final String ADMIN_USERNAME = "admin";

    /** 内置管理员密码（V1.0 阶段硬编码） */
    private static final String ADMIN_PASSWORD = "123456";

    /** 内置学生账号（V1.0 阶段硬编码） */
    private static final String STUDENT_USERNAME = "student";

    /** 内置学生密码（V1.0 阶段硬编码） */
    private static final String STUDENT_PASSWORD = "123456";

    /**
     * 程序入口：基于 Scanner 读取账号密码，校验后区分管理员 / 学生身份。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        initConsoleEncoding();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================");
        System.out.println("        校园资源预约管理系统  V1.0（登录）");
        System.out.println("==================================================");
        System.out.println("内置测试账号：admin / 123456（管理员），student / 123456（学生）");

        while (true) {
            System.out.print("\n请输入账号（输入 exit 退出系统）：");
            if (!scanner.hasNextLine()) {
                break;
            }
            String username = scanner.nextLine().trim();
            if ("exit".equalsIgnoreCase(username)) {
                break;
            }

            System.out.print("请输入密码：");
            if (!scanner.hasNextLine()) {
                break;
            }
            String password = scanner.nextLine().trim();

            if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
                // 管理员登录成功
                System.out.println("登录成功！欢迎您，管理员【admin】。");
                System.out.println("提示：V1.0 仅实现登录功能，管理员业务菜单将在任务6 迭代。");
                break;
            } else if (STUDENT_USERNAME.equals(username) && STUDENT_PASSWORD.equals(password)) {
                // 学生登录成功
                System.out.println("登录成功！欢迎您，学生【student】。");
                System.out.println("提示：V1.0 仅实现登录功能，学生业务菜单将在任务6 迭代。");
                break;
            } else {
                // 登录失败，允许重试
                System.out.println("登录失败：账号或密码错误，请重新输入。");
            }
        }

        System.out.println("系统已退出，感谢使用。");
        scanner.close();
    }
}
