package com.campus;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * V2.2 函数式编程：用 Stream + Lambda + Optional 重构全部集合查询。
 *
 * <p>本版本相对 V2.1 的变化：
 * <ol>
 *   <li>资源集合的遍历全部由 for 循环改为 Stream 流式调用（旧写法以注释保留对比）；</li>
 *   <li>新增按名称模糊查询、按状态 + 名称多条件组合查询，结果按资源 ID 升序；</li>
 *   <li>单个资源 / 单个用户的查找改用 {@link Optional}，用它显式表达「可能不存在」；</li>
 *   <li>Result 返回结构、异常机制、持久化格式全部保持不变。</li>
 * </ol>
 * </p>
 *
 * @author 课程设计
 * @version V2.2
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

    /** 资源排序规则：按编号升序 */
    private static final Comparator<CampusResource> BY_ID = Comparator.comparing(CampusResource::getId);

    /** 全部用户对象 */
    private final List<User> userList = new ArrayList<User>();

    /** 全部资源对象 */
    private final List<CampusResource> resourceList = new ArrayList<CampusResource>();

    /** 控制台输入器 */
    private final Scanner scanner = new Scanner(System.in);

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
            if ("0".equals(choice)) {
                running = false;
                continue;
            }
            try {
                if ("1".equals(choice)) {
                    Result<User> loginResult = doLogin();
                    showResult(loginResult);
                    if (loginResult.isSuccess()) {
                        User user = loginResult.getData();
                        if (user.isAdmin()) {
                            showAdminMenu(user);
                        } else {
                            showStudentMenu(user);
                        }
                    }
                } else if ("2".equals(choice)) {
                    showResult(doRegister());
                } else {
                    showResult(Result.fail("无效的菜单序号，请输入 1、2 或 0。"));
                }
            } catch (BusinessException e) {
                // 业务异常统一在此转为失败结果，控制台只看到中文提示，看不到堆栈
                showResult(Result.fail(e.getMessage()));
            } catch (RuntimeException e) {
                showResult(Result.fail("系统异常：操作未能完成，请稍后重试。"));
            }
        }
        System.out.println("系统已退出，感谢使用。");
        scanner.close();
    }

    /** 打印系统横幅。 */
    private void showBanner() {
        System.out.println("==================================================");
        System.out.println("    校园资源预约管理系统  V2.2（函数式编程）");
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
     * 登录业务：成功返回当前用户对象，失败返回原因。
     *
     * @return Result&lt;User&gt; 登录结果
     */
    private Result<User> doLogin() {
        String username = requireText(readLine("请输入账号："), "账号");
        String password = requireText(readLine("请输入密码："), "密码");
        Result<User> findResult = findByUsername(username);
        if (!findResult.isSuccess()) {
            return findResult;
        }
        User user = findResult.getData();
        if (!user.getPassword().equals(password)) {
            throw new PasswordErrorException();
        }
        return Result.success("登录成功！欢迎您，" + user.getRoleName() + "【" + user.getUsername() + "】。", user);
    }

    /**
     * 注册业务：注册入口固定创建学生账号。
     *
     * @return Result&lt;Void&gt; 注册结果
     */
    private Result<Void> doRegister() {
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
        userList.add(new User(username, password, User.ROLE_STUDENT, LocalDateTime.now()));
        saveUsers();
        return Result.success("注册成功！当前用户总数：" + userList.size() + "。");
    }

    /**
     * 按账号查询用户：用 Optional 表达「可能找不到」。
     *
     * <p>重构前（for 循环版，保留注释用于对比）：</p>
     * <pre>
     * for (User user : userList) {
     *     if (user.getUsername().equals(username)) {
     *         return Result.success("查询成功", user);
     *     }
     * }
     * return Result.fail("账号不存在……");
     * </pre>
     *
     * @param username 账号
     * @return 查询结果
     */
    private Result<User> findByUsername(String username) {
        Optional<User> optional = userList.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
        return optional
                .map(user -> Result.success("查询成功", user))
                .orElseGet(() -> Result.fail("账号【" + username + "】不存在，请确认后重试或先注册。"));
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
     * 管理员菜单：资源的完整增删改查权限 + 多条件查询。
     *
     * @param admin 当前登录的管理员
     */
    private void showAdminMenu(User admin) {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("------------- 管理员菜单 -------------");
            System.out.println("  1. 新增校园资源");
            System.out.println("  2. 查询全部资源");
            System.out.println("  3. 修改资源信息");
            System.out.println("  4. 删除资源");
            System.out.println("  5. 资源条件查询（状态 + 名称模糊）");
            System.out.println("  0. 退出登录");
            System.out.println("--------------------------------------");
            String choice = readLine("请选择功能：");
            if (choice == null) {
                return;
            }
            if ("0".equals(choice)) {
                running = false;
                continue;
            }
            try {
                if ("1".equals(choice)) {
                    showResult(addResource(admin));
                } else if ("2".equals(choice)) {
                    showResult(listAllResources(admin));
                } else if ("3".equals(choice)) {
                    showResult(updateResource(admin));
                } else if ("4".equals(choice)) {
                    showResult(deleteResource(admin));
                } else if ("5".equals(choice)) {
                    showResult(queryResourceByCondition(admin));
                } else {
                    showResult(Result.fail("无效的菜单序号，请输入 1、2、3、4、5 或 0。"));
                }
            } catch (BusinessException e) {
                showResult(Result.fail(e.getMessage()));
            } catch (RuntimeException e) {
                showResult(Result.fail("系统异常：操作未能完成，请稍后重试。"));
            }
        }
    }

    /**
     * 新增资源业务。
     *
     * @param admin 操作人
     * @return Result&lt;CampusResource&gt; 新增结果
     */
    private Result<CampusResource> addResource(User admin) {
        requireAdmin(admin, "新增校园资源");
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
        return Result.success("资源新增成功，已写入 " + RESOURCE_FILE + "。", resource);
    }

    /**
     * 查询全部资源业务：Stream 收集 + 按编号升序。
     *
     * <p>重构前（for 循环版，保留注释用于对比）：</p>
     * <pre>
     * List&lt;CampusResource&gt; all = new ArrayList&lt;&gt;();
     * for (CampusResource resource : resourceList) {
     *     all.add(resource);
     * }
     * </pre>
     *
     * @param admin 操作人
     * @return Result&lt;List&lt;CampusResource&gt;&gt; 资源列表
     */
    private Result<List<CampusResource>> listAllResources(User admin) {
        requireAdmin(admin, "查询全部资源");
        List<CampusResource> all = resourceList.stream()
                .sorted(BY_ID)
                .collect(Collectors.toList());
        if (all.isEmpty()) {
            return Result.fail("当前没有任何资源，请先新增。");
        }
        return Result.success("共查询到 " + all.size() + " 条资源。", all);
    }

    /**
     * 修改资源业务：按 ID 查找后逐项修改，输入留空表示该项不变。
     *
     * @param admin 操作人
     * @return Result&lt;CampusResource&gt; 修改后的资源
     */
    private Result<CampusResource> updateResource(User admin) {
        requireAdmin(admin, "修改资源信息");
        String id = requireText(readLine("请输入要修改的资源编号："), "资源编号");
        Result<CampusResource> findResult = findResourceById(id);
        if (!findResult.isSuccess()) {
            return findResult;
        }
        CampusResource resource = findResult.getData();
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
        return Result.success("资源修改成功。", resource);
    }

    /**
     * 删除资源业务：存在预约记录的资源禁止删除。
     *
     * @param admin 操作人
     * @return Result&lt;Void&gt; 删除结果
     */
    private Result<Void> deleteResource(User admin) {
        requireAdmin(admin, "删除资源");
        String id = requireText(readLine("请输入要删除的资源编号："), "资源编号");
        Result<CampusResource> findResult = findResourceById(id);
        if (!findResult.isSuccess()) {
            return Result.fail(findResult.getMsg());
        }
        if (hasReserveRecord(id)) {
            return Result.fail("资源【" + id + "】已存在预约记录，禁止删除。");
        }
        resourceList.remove(findResult.getData());
        saveResources();
        return Result.success("资源【" + id + "】已删除。");
    }

    /**
     * 按条件查询资源：状态可留空，名称支持模糊匹配，结果按编号升序。
     *
     * @param admin 操作人
     * @return Result&lt;List&lt;CampusResource&gt;&gt; 查询结果
     */
    private Result<List<CampusResource>> queryResourceByCondition(User admin) {
        requireAdmin(admin, "资源条件查询");
        String statusText = readLine("状态筛选（1.可预约 2.占用 3.停用，留空表示不限）：");
        ResourceStatus status = null;
        if (statusText != null && !statusText.isEmpty()) {
            status = ResourceStatus.parse(statusText);
            if (status == null) {
                throw ParamEmptyException.illegal("状态只能是 1（可预约）、2（占用）、3（停用）或留空。");
            }
        }
        String keyword = readLine("名称关键字（留空表示不限）：");
        return filterResources(status, keyword == null ? "" : keyword.trim());
    }

    // ================================================== 学生菜单

    /**
     * 学生菜单：只读，且只能看到状态为可预约的资源。
     *
     * @param student 当前登录的学生
     */
    private void showStudentMenu(User student) {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("------------- 学生菜单 -------------");
            System.out.println("  1. 查看可预约资源");
            System.out.println("  2. 按名称搜索可预约资源");
            System.out.println("  0. 退出登录");
            System.out.println("------------------------------------");
            String choice = readLine("请选择功能：");
            if (choice == null) {
                return;
            }
            if ("0".equals(choice)) {
                running = false;
                continue;
            }
            try {
                if ("1".equals(choice)) {
                    showResult(listAvailableResources());
                } else if ("2".equals(choice)) {
                    String keyword = readLine("请输入资源名称关键字：");
                    showResult(filterResources(ResourceStatus.AVAILABLE, keyword == null ? "" : keyword.trim()));
                } else {
                    showResult(Result.fail("无效的菜单序号，请输入 1、2 或 0。"));
                }
            } catch (BusinessException e) {
                showResult(Result.fail(e.getMessage()));
            } catch (RuntimeException e) {
                showResult(Result.fail("系统异常：操作未能完成，请稍后重试。"));
            }
        }
    }

    /**
     * 查询可预约资源：Stream 过滤掉占用与停用，按编号升序。
     *
     * <p>重构前（for 循环版，保留注释用于对比）：</p>
     * <pre>
     * List&lt;CampusResource&gt; available = new ArrayList&lt;&gt;();
     * for (CampusResource resource : resourceList) {
     *     if (resource.isAvailable()) {
     *         available.add(resource);
     *     }
     * }
     * </pre>
     *
     * @return Result&lt;List&lt;CampusResource&gt;&gt; 可预约资源列表
     */
    private Result<List<CampusResource>> listAvailableResources() {
        List<CampusResource> available = resourceList.stream()
                .filter(CampusResource::isAvailable)
                .sorted(BY_ID)
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            return Result.fail("当前没有可预约的资源。");
        }
        return Result.success("共 " + available.size() + " 条可预约资源。", available);
    }

    // ================================================== 结果展示

    /**
     * 统一展示结果：成功打印提示并按类型展示数据，失败只打印提示。
     *
     * @param result 业务结果
     */
    private void showResult(Result<?> result) {
        if (result == null) {
            return;
        }
        System.out.println((result.isSuccess() ? "[成功] " : "[失败] ") + result.getMsg());
        Object data = result.getData();
        if (data == null) {
            return;
        }
        if (data instanceof List) {
            for (Object item : (List<?>) data) {
                System.out.println("  " + item);
            }
        } else {
            System.out.println("  " + data);
        }
    }

    // ================================================== 资源查询核心

    /**
     * 多条件组合查询：状态 + 名称模糊（忽略大小写），结果按编号升序。
     *
     * <p>这是本版本的核心方法：把「做什么」（筛选、排序）和「怎么做」（循环）解耦。</p>
     *
     * @param status  状态条件，null 表示不限
     * @param keyword 名称关键字，空串表示不限
     * @return Result&lt;List&lt;CampusResource&gt;&gt; 查询结果
     */
    private Result<List<CampusResource>> filterResources(ResourceStatus status, String keyword) {
        List<CampusResource> matched = resourceList.stream()
                .filter(resource -> status == null || status.equals(resource.getStatus()))
                .filter(resource -> keyword.isEmpty()
                        || resource.getName().toLowerCase().contains(keyword.toLowerCase()))
                .sorted(BY_ID)
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            return Result.fail("没有符合条件的资源。");
        }
        return Result.success("共查询到 " + matched.size() + " 条资源。", matched);
    }

    /**
     * 按资源编号查询资源：Optional 显式表达「可能不存在」。
     *
     * <p>重构前（for 循环版，保留注释用于对比）：</p>
     * <pre>
     * for (CampusResource resource : resourceList) {
     *     if (resource.getId().equals(id)) {
     *         return Result.success("查询成功", resource);
     *     }
     * }
     * return Result.fail("未找到……");
     * </pre>
     *
     * @param id 资源编号
     * @return 查询结果
     */
    private Result<CampusResource> findResourceById(String id) {
        Optional<CampusResource> optional = resourceList.stream()
                .filter(resource -> resource.getId().equals(id))
                .findFirst();
        return optional
                .map(resource -> Result.success("查询成功", resource))
                .orElseGet(() -> Result.fail("未找到 ID 为【" + id + "】的资源。"));
    }

    // ================================================== 资源业务辅助

    /**
     * 管理员权限校验：非管理员调用资源增删改一律抛权限异常。
     *
     * @param operator 操作人
     * @param action   被保护的操作名
     */
    private void requireAdmin(User operator, String action) {
        if (operator == null || !operator.isAdmin()) {
            String roleName = operator == null ? "未登录用户" : operator.getRoleName();
            throw new PermissionDeniedException(action, roleName);
        }
    }

    /**
     * 判断资源是否已有预约记录。
     *
     * <p>任务 8 阶段预约模块尚未开发，这里先返回 false；
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
     * <p>用 Stream 求已有编号的最大值，替代手工维护计数器。</p>
     *
     * @return 新编号
     */
    private String nextResourceId() {
        int max = resourceList.stream()
                .map(CampusResource::getId)
                .filter(id -> id != null && id.startsWith(RESOURCE_ID_PREFIX))
                .map(id -> id.substring(RESOURCE_ID_PREFIX.length()))
                .mapToInt(text -> {
                    try {
                        return Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
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
     * 判断账号是否已存在（注册查重用）。
     *
     * <p>重构前（for 循环版）：遍历并 return true / false。</p>
     *
     * @param username 账号
     * @return 已存在返回 true
     */
    private boolean existsUsername(String username) {
        return userList.stream().anyMatch(user -> user.getUsername().equals(username));
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
