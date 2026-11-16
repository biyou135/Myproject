package com.campus;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * V1.1 用户注册功能：改造控制台首页菜单，新增动态注册能力。
 *
 * <p>本版本相对 V1.0 的变化：
 * <ol>
 *   <li>删除 V1.0 硬编码固定账号，全部改用动态集合数据；</li>
 *   <li>首页菜单改为「1.登录  2.注册  0.退出」；</li>
 *   <li>使用 HashMap&lt;String, String&gt; 内存存储用户账号密码；</li>
 *   <li>注册支持非空校验、密码长度 ≥ 6 校验、用户名唯一查重；</li>
 *   <li>抽离独立校验方法（账号校验、密码合法性校验），减少主流程冗余代码。</li>
 * </ol>
 * </p>
 *
 * <p>版本约束：本阶段仍不做分层、不抽工具类（分层重构在任务10）。</p>
 *
 * @author 课程设计
 * @version V1.1
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

    /** 密码最小长度约束 */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /** 用户账号密码内存存储：key = 用户名，value = 密码 */
    private final Map<String, String> userStore = new HashMap<String, String>();

    /** 控制台输入器 */
    private final Scanner scanner = new Scanner(System.in);

    /**
     * 程序入口。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        initConsoleEncoding();
        new SystemMain().start();
    }

    /**
     * 启动主循环：展示首页菜单，分发登录 / 注册 / 退出指令。
     */
    private void start() {
        showBanner();
        boolean running = true;
        while (running) {
            showMainMenu();
            String choice = readLine("请选择功能：");
            if (choice == null) {
                break;
            }
            if ("1".equals(choice)) {
                doLogin();
            } else if ("2".equals(choice)) {
                doRegister();
            } else if ("0".equals(choice)) {
                running = false;
            } else {
                System.out.println("无效的菜单序号，请输入 1、2 或 0。");
            }
        }
        System.out.println("系统已退出，感谢使用。");
        scanner.close();
    }

    /** 打印系统横幅。 */
    private void showBanner() {
        System.out.println("==================================================");
        System.out.println("        校园资源预约管理系统  V1.1（登录 + 注册）");
        System.out.println("==================================================");
        System.out.println("提示：V1.1 已取消内置账号，请先注册账号再登录。");
    }

    /** 打印首页菜单。 */
    private void showMainMenu() {
        System.out.println();
        System.out.println("---------------- 首页菜单 ----------------");
        System.out.println("  1. 登录");
        System.out.println("  2. 注册");
        System.out.println("  0. 退出系统");
        System.out.println("------------------------------------------");
    }

    /**
     * 登录流程：读取账号密码，校验是否与内存集合中的记录一致。
     */
    private void doLogin() {
        String username = readLine("请输入账号：");
        if (username == null) {
            return;
        }
        String password = readLine("请输入密码：");
        if (password == null) {
            return;
        }
        String storedPassword = userStore.get(username);
        if (storedPassword == null) {
            System.out.println("登录失败：该账号不存在，请先注册。");
        } else if (!storedPassword.equals(password)) {
            System.out.println("登录失败：密码错误，请重新输入。");
        } else {
            System.out.println("登录成功！欢迎您，" + username + "。");
            System.out.println("提示：V1.1 仅实现登录与注册，业务菜单将在任务6 迭代。");
        }
    }

    /**
     * 注册流程：账号校验 → 密码合法性校验 → 用户名唯一查重 → 写入内存集合。
     */
    private void doRegister() {
        String username = readLine("请输入注册账号：");
        if (username == null) {
            return;
        }
        if (!checkUsername(username)) {
            System.out.println("注册失败：账号不能为空，且不能包含空格。");
            return;
        }
        if (!checkUsernameUnique(username)) {
            System.out.println("注册失败：账号【" + username + "】已存在，请更换账号。");
            return;
        }
        String password = readLine("请输入注册密码（长度不少于 " + MIN_PASSWORD_LENGTH + " 位）：");
        if (password == null) {
            return;
        }
        if (!checkPassword(password)) {
            System.out.println("注册失败：密码不能为空，且长度不能少于 " + MIN_PASSWORD_LENGTH + " 位。");
            return;
        }
        String confirmPassword = readLine("请再次输入密码确认：");
        if (confirmPassword == null) {
            return;
        }
        if (!password.equals(confirmPassword)) {
            System.out.println("注册失败：两次输入的密码不一致。");
            return;
        }
        userStore.put(username, password);
        System.out.println("注册成功！账号【" + username + "】已创建，当前系统用户总数：" + userStore.size() + "。");
    }

    /**
     * 账号校验（抽离出的独立校验方法）。
     *
     * @param username 待校验账号
     * @return 合法返回 true
     */
    private boolean checkUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        // 账号中不允许出现空白字符
        return username.indexOf(' ') < 0 && username.indexOf('\t') < 0;
    }

    /**
     * 密码合法性校验（抽离出的独立校验方法）。
     *
     * @param password 待校验密码
     * @return 合法返回 true
     */
    private boolean checkPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * 用户名唯一查重。
     *
     * @param username 待校验账号
     * @return 不存在同名账号返回 true
     */
    private boolean checkUsernameUnique(String username) {
        return !userStore.containsKey(username);
    }

    /**
     * 读取一行控制台输入。
     *
     * @param tip 输入提示语
     * @return 去除首尾空白后的输入内容；输入流结束返回 null
     */
    private String readLine(String tip) {
        System.out.print(tip);
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }
}
