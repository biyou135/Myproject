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
 * V1.4 自定义异常：业务错误统一用中文友好提示，屏蔽原生异常堆栈。
 *
 * <p>本版本相对 V1.3 的变化：
 * <ol>
 *   <li>新增异常父类 {@link BusinessException} 与 4 个业务子异常；</li>
 *   <li>校验方法由「返回 boolean」改为「不合法直接抛异常」，主流程更清爽；</li>
 *   <li>登录 / 注册 / 参数校验处主动抛出自定义异常；</li>
 *   <li>程序入口统一捕获，只打印中文提示，绝不打印 printStackTrace。</li>
 * </ol>
 * </p>
 *
 * @author 课程设计
 * @version V1.4
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

    /** 注册时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 全部用户对象 */
    private final List<User> userList = new ArrayList<User>();

    /** 控制台输入器 */
    private final Scanner scanner = new Scanner(System.in);

    /**
     * 程序入口：最外层兜底捕获，保证任何异常都不会把堆栈直接抛给用户。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        initConsoleEncoding();
        try {
            new SystemMain().start();
        } catch (BusinessException e) {
            // 业务异常：只输出中文友好提示
            System.out.println("操作失败：" + e.getMessage());
        } catch (RuntimeException e) {
            // 系统异常：屏蔽堆栈，仅提示联系管理员
            System.out.println("系统异常：操作未能完成，请联系管理员。");
        }
    }

    /**
     * 启动主循环：加载数据 → 菜单分发，单次操作异常不影响本轮会话。
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
            try {
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
            } catch (BusinessException e) {
                // 业务异常统一在此转为友好提示，不打印堆栈
                System.out.println("操作失败：" + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("系统异常：操作未能完成，请稍后重试。");
            }
        }
        System.out.println("系统已退出，感谢使用。");
        scanner.close();
    }

    /**
     * 从 user.txt 加载用户数据；首次运行初始化内置管理员并落盘。
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
            userList.add(new User("admin", "123456", User.ROLE_ADMIN, LocalDateTime.now()));
            saveUsers();
            System.out.println("[持久化] 首次运行，已初始化内置管理员账号 admin / 123456。");
        }
    }

    /**
     * 解析一行用户数据。
     *
     * @param line 文本行
     * @return 解析成功返回 User，格式非法返回 null
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
        System.out.println("        校园资源预约管理系统  V1.4（自定义异常）");
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
     * 登录流程：校验失败直接抛出自定义异常，由上层统一转为友好提示。
     */
    private void doLogin() {
        String username = requireText(readLine("请输入账号："), "账号");
        String password = requireText(readLine("请输入密码："), "密码");
        User user = findByUsername(username);
        if (!user.getPassword().equals(password)) {
            throw new PasswordErrorException();
        }
        System.out.println("登录成功！欢迎您，" + user.getRoleName() + "【" + user.getUsername() + "】。");
        System.out.println("提示：V1.4 尚未实现业务菜单，资源与预约功能将在任务6、任务9 迭代。");
    }

    /**
     * 注册流程：账号格式、唯一性、密码强度全部通过校验后才落盘。
     */
    private void doRegister() {
        String username = requireText(readLine("请输入注册账号："), "账号");
        checkUsernameFormat(username);
        if (existsUsername(username)) {
            throw new UserAlreadyExistsException(username);
        }
        String password = requireText(readLine("请输入注册密码（长度不少于 " + MIN_PASSWORD_LENGTH + " 位）："), "密码");
        checkPasswordFormat(password);
        String confirmPassword = requireText(readLine("请再次输入密码确认："), "确认密码");
        if (!password.equals(confirmPassword)) {
            throw ParamEmptyException.illegal("两次输入的密码不一致，请重新注册。");
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
     * 按账号查找用户对象，找不到抛出 {@link UserNotFoundException}。
     *
     * @param username 账号
     * @return 用户对象
     */
    private User findByUsername(String username) {
        for (User user : userList) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        throw new UserNotFoundException(username);
    }

    /**
     * 判断账号是否已存在（注册查重用，不抛异常）。
     *
     * @param username 账号
     * @return 已存在返回 true
     */
    private boolean existsUsername(String username) {
        for (User user : userList) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 必填文本校验。
     *
     * @param value     待校验内容
     * @param fieldName 参数中文名
     * @return 校验通过的原值
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw ParamEmptyException.empty(fieldName);
        }
        return value.trim();
    }

    /**
     * 账号格式校验：不能包含空白字符与持久化分隔符。
     *
     * @param username 账号
     */
    private void checkUsernameFormat(String username) {
        if (username.indexOf(' ') >= 0 || username.indexOf('\t') >= 0 || username.indexOf(',') >= 0) {
            throw ParamEmptyException.illegal("账号不能包含空白字符或英文逗号。");
        }
    }

    /**
     * 密码强度校验：长度不得小于约定值。
     *
     * @param password 密码
     */
    private void checkPasswordFormat(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw ParamEmptyException.illegal("密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位。");
        }
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
