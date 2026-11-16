package com.campus;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * V1.2 面向对象封装：User 实体类重构后的控制台主流程。
 *
 * <p>本版本相对 V1.1 的变化：
 * <ol>
 *   <li>新增实体类 {@link User}，承载账号、密码、角色、注册时间；</li>
 *   <li>用户存储由 HashMap&lt;String, String&gt; 升级为 List&lt;User&gt;；</li>
 *   <li>登录成功后按角色输出身份欢迎提示（管理员 / 学生）；</li>
 *   <li>内置管理员账号首次启动初始化，注册入口固定创建学生账号；</li>
 *   <li>新增「查看已注册用户」菜单，验证角色与注册时间完整保存。</li>
 * </ol>
 * </p>
 *
 * <p>版本约束：本阶段仍不做分层（分层重构在任务10）。</p>
 *
 * @author 课程设计
 * @version V1.2
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
     * 启动主循环。
     */
    private void start() {
        initAdmin();
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
     * 初始化内置管理员账号，保证管理端功能可用。
     * 避免登录逻辑中再次出现 V1.0 式的硬编码判断。
     */
    private void initAdmin() {
        User admin = new User("admin", "123456", User.ROLE_ADMIN, LocalDateTime.now());
        userList.add(admin);
    }

    /** 打印系统横幅。 */
    private void showBanner() {
        System.out.println("==================================================");
        System.out.println("        校园资源预约管理系统  V1.2（用户实体化）");
        System.out.println("==================================================");
        System.out.println("内置管理员账号：admin / 123456");
        System.out.println("学生账号请通过「注册」菜单创建。");
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
     * 登录流程：按账号查找 User 对象，再比对密码，最后按角色输出欢迎语。
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
            System.out.println("提示：V1.2 尚未实现业务菜单，资源与预约功能将在任务6、任务9 迭代。");
        }
    }

    /**
     * 注册流程：账号校验 → 唯一查重 → 密码校验 → 构建 User 实体并加入集合。
     */
    private void doRegister() {
        String username = readLine("请输入注册账号：");
        if (username == null) {
            return;
        }
        if (!checkUsername(username)) {
            System.out.println("注册失败：账号不能为空，且不能包含空白字符。");
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
        // 注册入口只能创建学生账号，管理员账号由系统内置
        User user = new User(username, password, User.ROLE_STUDENT, LocalDateTime.now());
        userList.add(user);
        System.out.println("注册成功！当前系统用户总数：" + userList.size() + "。");
        System.out.println("您的账号信息：" + user);
    }

    /** 打印全部已注册用户，用于验证角色、注册时间等字段完整保存。 */
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
     * @return 找到返回 User 对象，找不到返回 null（V1.2 尚未引入统一返回结构）
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
