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
 * V2.0 校园资源核心模块：系统从「用户模块」升级为「业务系统」。
 *
 * <p>本版本相对 V1.4 的变化：
 * <ol>
 *   <li>新增实体 {@link CampusResource} 与状态枚举 {@link ResourceStatus}；</li>
 *   <li>新增资源持久化 resource.txt，启动加载、增删改后落盘；</li>
 *   <li>登录后按角色进入不同菜单：管理员可增删改查，学生只能查可预约资源；</li>
 *   <li>新增越权异常 {@link PermissionDeniedException} 与资源不存在异常
 *       {@link ResourceNotFoundException}。</li>
 * </ol>
 * </p>
 *
 * <p>版本约束：本阶段仍不分层，资源业务方法全部写在本类中，
 * 为任务 10 的三层架构重构保留「痛点」。</p>
 *
 * @author 课程设计
 * @version V2.0
 */
public class SystemMain {

    /** 密码最小长度约束 */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /** 用户数据持久化文件 */
    private static final String USER_FILE = "user.txt";

    /** 资源数据持久化文件 */
    private static final String RESOURCE_FILE = "resource.txt";

    /** 数据文件字段分隔符 */
    private static final String SEPARATOR = ",";

    /** 资源 ID 前缀 */
    private static final String RESOURCE_ID_PREFIX = "R";

    /** 注册时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 全部用户对象 */
    private final List<User> userList = new ArrayList<User>();

    /** 全部资源对象 */
    private final List<CampusResource> resourceList = new ArrayList<CampusResource>();

    /** 控制台输入器 */
    private final Scanner scanner = new Scanner(System.in);

    /** 当前登录用户，null 表示未登录 */
    private User currentUser;

    /**
     * 程序入口。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        initConsoleEncoding();
        try {
            new SystemMain().start();
        } catch (BusinessException e) {
            System.out.println("操作失败：" + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("系统异常：操作未能完成，请联系管理员。");
        }
    }

    /**
     * 统一控制台输出编码为 UTF-8，避免中文乱码。
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

    /**
     * 启动主流程：加载数据 → 首页菜单 → 登录后进入角色菜单。
     */
    private void start() {
        loadUsers();
        loadResources();
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
                } else if ("0".equals(choice)) {
                    running = false;
                } else {
                    System.out.println("无效的菜单序号，请输入 1、2 或 0。");
                }
            } catch (BusinessException e) {
                System.out.println("操作失败：" + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("系统异常：操作未能完成，请稍后重试。");
            }
        }
        System.out.println("系统已退出，感谢使用。");
        scanner.close();
    }

    /** 打印系统横幅。 */
    private void showBanner() {
        System.out.println("==================================================");
        System.out.println("      校园资源预约管理系统  V2.0（资源管理）");
        System.out.println("==================================================");
        System.out.println("内置管理员账号：admin / 123456");
        System.out.println("数据文件：" + USER_FILE + "（用户）、" + RESOURCE_FILE + "（资源）");
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

    // ================================================== 用户模块

    /**
     * 登录流程：校验通过后按角色进入对应菜单。
     */
    private void doLogin() {
        String username = requireText(readLine("请输入账号："), "账号");
        String password = requireText(readLine("请输入密码："), "密码");
        User user = findByUsername(username);
        if (!user.getPassword().equals(password)) {
            throw new PasswordErrorException();
        }
        currentUser = user;
        System.out.println("登录成功！欢迎您，" + user.getRoleName() + "【" + user.getUsername() + "】。");
        if (user.isAdmin()) {
            showAdminMenu();
        } else {
            showStudentMenu();
        }
        currentUser = null;
    }

    /**
     * 注册流程：注册入口固定创建学生账号。
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
    }

    /**
     * 从 user.txt 加载用户数据；首次运行初始化内置管理员并落盘。
     */
    private void loadUsers() {
        for (String line : FileUtil.readAllLines(USER_FILE)) {
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

    /** 把内存中的用户列表整体写回 user.txt。 */
    private void saveUsers() {
        List<String> lines = new ArrayList<String>();
        for (User user : userList) {
            lines.add(user.getUsername() + SEPARATOR + user.getPassword() + SEPARATOR
                    + user.getRole() + SEPARATOR + user.getRegisterTime().format(FORMATTER));
        }
        FileUtil.writeAllLines(USER_FILE, lines);
    }

    // ================================================== 管理员菜单

    /**
     * 管理员菜单：资源的完整增删改查权限。
     */
    private void showAdminMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("------------- 管理员菜单 -------------");
            System.out.println("  1. 新增校园资源");
            System.out.println("  2. 查询全部资源");
            System.out.println("  3. 修改资源信息");
            System.out.println("  4. 删除资源");
            System.out.println("  0. 退出登录");
            System.out.println("--------------------------------------");
            String choice = readLine("请选择功能：");
            if (choice == null) {
                return;
            }
            try {
                if ("1".equals(choice)) {
                    addResource();
                } else if ("2".equals(choice)) {
                    listAllResources();
                } else if ("3".equals(choice)) {
                    updateResource();
                } else if ("4".equals(choice)) {
                    deleteResource();
                } else if ("0".equals(choice)) {
                    running = false;
                } else {
                    System.out.println("无效的菜单序号，请输入 1、2、3、4 或 0。");
                }
            } catch (BusinessException e) {
                System.out.println("操作失败：" + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("系统异常：操作未能完成，请稍后重试。");
            }
        }
    }

    /**
     * 新增资源：自动生成唯一 ID，默认状态 AVAILABLE。
     */
    private void addResource() {
        requireAdmin("新增校园资源");
        System.out.println("--- 新增校园资源（各字段不要输入英文逗号）---");
        String name = requireText(readLine("资源名称："), "资源名称");
        String type = requireText(readLine("资源类型（教室 / 实验室 / 设备 / 场地）："), "资源类型");
        String location = requireText(readLine("位置描述："), "位置描述");
        String description = readLine("资源描述（可留空）：");
        checkNoSeparator(name, "资源名称");
        checkNoSeparator(type, "资源类型");
        checkNoSeparator(location, "位置描述");

        CampusResource resource = new CampusResource(nextResourceId(), name, type,
                ResourceStatus.AVAILABLE, location, description == null ? "" : description);
        resourceList.add(resource);
        saveResources();
        System.out.println("资源新增成功，已写入 " + RESOURCE_FILE + "。");
        System.out.println("  " + resource);
    }

    /** 查询全部资源（管理员视角）。 */
    private void listAllResources() {
        requireAdmin("查询全部资源");
        System.out.println();
        System.out.println("---------------- 全部资源列表 ----------------");
        if (resourceList.isEmpty()) {
            System.out.println("当前没有任何资源，请先新增。");
            return;
        }
        for (CampusResource resource : resourceList) {
            System.out.println("  " + resource);
        }
        System.out.println("共 " + resourceList.size() + " 条资源。");
    }

    /**
     * 修改资源：按 ID 查找后逐项修改，输入留空表示该项不变。
     */
    private void updateResource() {
        requireAdmin("修改资源信息");
        String id = requireText(readLine("请输入要修改的资源编号："), "资源编号");
        CampusResource resource = findResourceById(id);
        System.out.println("当前信息：" + resource);
        System.out.println("（直接回车表示该项不变）");

        String name = readLine("新名称：");
        if (name != null && !name.isEmpty()) {
            checkNoSeparator(name, "资源名称");
            resource.setName(name);
        }
        String type = readLine("新类型：");
        if (type != null && !type.isEmpty()) {
            checkNoSeparator(type, "资源类型");
            resource.setType(type);
        }
        String location = readLine("新位置：");
        if (location != null && !location.isEmpty()) {
            checkNoSeparator(location, "位置描述");
            resource.setLocation(location);
        }
        String statusText = readLine("新状态（1.可预约 2.占用 3.停用，留空不变）：");
        if (statusText != null && !statusText.isEmpty()) {
            ResourceStatus status = ResourceStatus.parse(statusText);
            if (status == null) {
                throw ParamEmptyException.illegal("资源状态只能是 AVAILABLE / OCCUPIED / DISABLED 之一。");
            }
            resource.setStatus(status);
        }
        saveResources();
        System.out.println("资源修改成功。");
        System.out.println("  " + resource);
    }

    /**
     * 删除资源：存在预约记录的资源禁止删除（任务 9 接入预约模块后生效）。
     */
    private void deleteResource() {
        requireAdmin("删除资源");
        String id = requireText(readLine("请输入要删除的资源编号："), "资源编号");
        CampusResource resource = findResourceById(id);
        if (hasReserveRecord(id)) {
            throw new BusinessException("资源【" + id + "】已存在预约记录，禁止删除。");
        }
        resourceList.remove(resource);
        saveResources();
        System.out.println("资源【" + id + "】已删除。");
    }

    // ================================================== 学生菜单

    /**
     * 学生菜单：只读，且只能看到状态为可预约的资源。
     */
    private void showStudentMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("------------- 学生菜单 -------------");
            System.out.println("  1. 查看可预约资源");
            System.out.println("  0. 退出登录");
            System.out.println("------------------------------------");
            String choice = readLine("请选择功能：");
            if (choice == null) {
                return;
            }
            try {
                if ("1".equals(choice)) {
                    listAvailableResources();
                } else if ("0".equals(choice)) {
                    running = false;
                } else {
                    System.out.println("无效的菜单序号，请输入 1 或 0。");
                }
            } catch (BusinessException e) {
                System.out.println("操作失败：" + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("系统异常：操作未能完成，请稍后重试。");
            }
        }
    }

    /**
     * 查询可预约资源：自动过滤掉占用与停用的资源。
     */
    private void listAvailableResources() {
        System.out.println();
        System.out.println("------------ 可预约资源列表 ------------");
        List<CampusResource> available = new ArrayList<CampusResource>();
        for (CampusResource resource : resourceList) {
            if (resource.isAvailable()) {
                available.add(resource);
            }
        }
        if (available.isEmpty()) {
            System.out.println("当前没有可预约的资源。");
            return;
        }
        for (CampusResource resource : available) {
            System.out.println("  " + resource);
        }
        System.out.println("共 " + available.size() + " 条可预约资源。");
    }

    // ================================================== 资源业务辅助

    /**
     * 管理员权限校验：非管理员调用资源增删改一律抛权限异常。
     *
     * @param action 被保护的操作名
     */
    private void requireAdmin(String action) {
        if (currentUser == null || !currentUser.isAdmin()) {
            String roleName = currentUser == null ? "未登录用户" : currentUser.getRoleName();
            throw new PermissionDeniedException(action, roleName);
        }
    }

    /**
     * 按资源编号查找资源。
     *
     * @param id 资源编号
     * @return 资源对象
     */
    private CampusResource findResourceById(String id) {
        for (CampusResource resource : resourceList) {
            if (resource.getId().equals(id)) {
                return resource;
            }
        }
        throw new ResourceNotFoundException(id);
    }

    /**
     * 判断资源是否已有预约记录。
     *
     * <p>任务 6 阶段预约模块尚未开发，这里先返回 false；
     * 任务 9 接入 ReserveRecord 后会替换为真实判断。</p>
     *
     * @param resourceId 资源编号
     * @return 存在预约记录返回 true
     */
    private boolean hasReserveRecord(String resourceId) {
        return false;
    }

    /**
     * 生成下一个资源编号，形如 R001、R002。
     *
     * @return 新编号
     */
    private String nextResourceId() {
        int max = 0;
        for (CampusResource resource : resourceList) {
            String id = resource.getId();
            if (id != null && id.startsWith(RESOURCE_ID_PREFIX)) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(RESOURCE_ID_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    // 编号格式异常的数据直接跳过，不影响新编号生成
                }
            }
        }
        return RESOURCE_ID_PREFIX + String.format("%03d", max + 1);
    }

    // ================================================== 资源持久化

    /**
     * 从 resource.txt 加载资源数据；文件不存在会自动创建空文件。
     */
    private void loadResources() {
        for (String line : FileUtil.readAllLines(RESOURCE_FILE)) {
            CampusResource resource = parseResource(line);
            if (resource != null) {
                resourceList.add(resource);
            }
        }
        System.out.println("[持久化] 已从 " + RESOURCE_FILE + " 加载 " + resourceList.size() + " 条资源。");
    }

    /**
     * 解析一行资源数据：id,name,type,status,location,description。
     *
     * <p>使用 limit=6 切分，描述字段允许包含英文逗号（它是最后一个字段）。</p>
     *
     * @param line 文本行
     * @return 解析成功返回资源对象，格式非法返回 null
     */
    private CampusResource parseResource(String line) {
        String[] parts = line.split(SEPARATOR, 6);
        if (parts.length < 6) {
            return null;
        }
        ResourceStatus status = ResourceStatus.parse(parts[3].trim());
        if (status == null) {
            return null;
        }
        return new CampusResource(parts[0].trim(), parts[1].trim(), parts[2].trim(),
                status, parts[4].trim(), parts[5].trim());
    }

    /** 把内存中的资源列表整体写回 resource.txt。 */
    private void saveResources() {
        List<String> lines = new ArrayList<String>();
        for (CampusResource resource : resourceList) {
            lines.add(resource.getId() + SEPARATOR + resource.getName() + SEPARATOR
                    + resource.getType() + SEPARATOR + resource.getStatus().name() + SEPARATOR
                    + resource.getLocation() + SEPARATOR + resource.getDescription());
        }
        FileUtil.writeAllLines(RESOURCE_FILE, lines);
    }

    // ================================================== 通用校验

    /**
     * 按账号查找用户对象。
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
     * 资源字段分隔符校验：名称 / 类型 / 位置不能含英文逗号，否则会破坏持久化格式。
     *
     * @param value     待校验内容
     * @param fieldName 参数中文名
     */
    private void checkNoSeparator(String value, String fieldName) {
        if (value.indexOf(',') >= 0) {
            throw ParamEmptyException.illegal(fieldName + "不能包含英文逗号。");
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
