package com.campus;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * V1.3 IO 文件持久化：用户数据落盘，程序重启不丢失。
 *
 * <p>本版本相对 V1.2 的变化：
 * <ol>
 *   <li>新增工具类 {@link FileUtil}，封装文件创建 / 读取 / 写入；</li>
 *   <li>用户数据持久化到 user.txt，程序启动自动加载；</li>
 *   <li>注册成功后自动写入文件，重启程序数据不丢失；</li>
 *   <li>文件格式：username,password,role,registerTime（逗号分隔）。</li>
 * </ol>
 * </p>
 *
 * @author 课程设计
 * @version V1.3
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

    /** 用户数据持久化文件 */
    private static final String USER_FILE = "user.txt";

    /** 持久化文件字段分隔符 */
    private static final String SEPARATOR = ",";

    /** 注册时间格式化器（与 User 的展示格式保持一致） */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 全部用户对象 */
    private final List<User> userList = new ArrayList<User>();

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
     * 启动主循环：先加载持久化数据，再进入菜单。
     */
    private void start() {
        loadUsers();
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
            } else if ("3".equals(choice)) {
                showAllUsers();
            } else if ("0".equals(choice)) {
                running = false;
            } else {
                System.out.println("无效的菜单序号，请输入 1、2、3 或 0。");
            }
        }
        System.out.println("系统已退出，感谢使用。");
        scanner.close();
    }

    /**
     * 从 user.txt 加载用户数据；首次运行（文件为空）时初始化内置管理员并落盘。
     */
    private void loadUsers() {
        List<String> lines = FileUtil.readAllLines(USER_FILE);
        for (String line : lines) {
            User user = parseUser(line);
            if (user != null) {
                userList.add(user);
            }
        }
        System.out.println("[持久化] 已从 " + USER_FILE + " 加载 " + userList.size() + " 个用户。");
        if (userList.isEmpty()) {
            // 首次启动：初始化内置管理员账号，保证管理端可用
            userList.add(new User("admin", "123456", User.ROLE_ADMIN, LocalDateTime.now()));
            saveUsers();
            System.out.println("[持久化] 首次运行，已初始化内置管理员账号 admin / 123456。");
        }
    }

    /**
     * 解析一行用户数据。
     *
     * @param line 文本行
     * @return 解析成功返回 User，格式非法返回 null（跳过该行，不中断启动）
     */
    private User parseUser(String line) {
        String[] parts = line.split(SEPARATOR, 4);
        if (parts.length < 4) {
            return null;
        }
        try {
            LocalDateTime registerTime = LocalDateTime.parse(parts[3].trim(), FORMATTER);
            return new User(parts[0].trim(), parts[1].trim(), parts[2].trim(), registerTime);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 把内存中的用户列表整体写回 user.txt。
     */
    private void saveUsers() {
        List<String> lines = new ArrayList<String>();
        for (User user : userList) {
            lines.add(user.getUsername() + SEPARATOR + user.getPassword() + SEPARATOR
                    + user.getRole() + SEPARATOR + user.getRegisterTime().format(FORMATTER));
        }
        FileUtil.writeAllLines(USER_FILE, lines);
    }

    /** 打印系统横幅。 */
    private void showBanner() {
        System.out.println("==================================================");
        System.out.println("        校园资源预约管理系统  V1.3（文件持久化）");
        System.out.println("==================================================");
        System.out.println("内置管理员账号：admin / 123456；数据文件：" + USER_FILE);
    }

    /** 打印首页菜单。 */
    private void showMainMenu() {
        System.out.println();
        System.out.println("---------------- 首页菜单 ----------------");
        System.out.println("  1. 登录");
        System.out.println("  2. 注册");
        System.out.println("  3. 查看已注册用户");
        System.out.println("  0. 退出系统");
        System.out.println("------------------------------------------");
    }

    /**
     * 登录流程。
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
        User user = findByUsername(username);
        if (user == null) {
            System.out.println("登录失败：该账号不存在，请先注册。");
        } else if (!user.getPassword().equals(password)) {
            System.out.println("登录失败：密码错误，请重新输入。");
        } else {
            System.out.println("登录成功！欢迎您，" + user.getRoleName() + "【" + user.getUsername() + "】。");
            System.out.println("提示：V1.3 尚未实现业务菜单，资源与预约功能将在任务6、任务9 迭代。");
        }
    }

    /**
     * 注册流程：校验通过后构建 User 实体 → 加入内存集合 → 立即落盘持久化。
     */
    private void doRegister() {
        String username = readLine("请输入注册账号：");
        if (username == null) {
            return;
        }
        if (!checkUsername(username)) {
            System.out.println("注册失败：账号不能为空，不能包含空白字符或英文逗号。");
            return;
        }
        if (findByUsername(username) != null) {
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
        User user = new User(username, password, User.ROLE_STUDENT, LocalDateTime.now());
        userList.add(user);
        saveUsers();
        System.out.println("注册成功！数据已写入 " + USER_FILE + "，当前用户总数：" + userList.size() + "。");
        System.out.println("您的账号信息：" + user);
    }

    /** 打印全部已注册用户。 */
    private void showAllUsers() {
        System.out.println();
        System.out.println("---------------- 已注册用户列表 ----------------");
        if (userList.isEmpty()) {
            System.out.println("当前没有任何用户。");
            return;
        }
        for (int i = 0; i < userList.size(); i++) {
            System.out.println((i + 1) + ". " + userList.get(i));
        }
        System.out.println("共 " + userList.size() + " 位用户。");
    }

    /**
     * 按账号查找用户对象。
     *
     * @param username 账号
     * @return 找到返回 User 对象，找不到返回 null
     */
    private User findByUsername(String username) {
        for (User user : userList) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
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
        // 逗号是持久化文件的分隔符，账号中禁止出现，避免数据被截断
        if (username.indexOf(' ') >= 0 || username.indexOf('\t') >= 0 || username.indexOf(',') >= 0) {
            return false;
        }
        return true;
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
