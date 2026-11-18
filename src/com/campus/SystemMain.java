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
 * V2.1 统一返回结果封装：所有业务方法不再返回 boolean / 裸对象 / null，统一返回 {@link Result}。
 *
 * <p>本版本相对 V2.0 的变化：
 * <ol>
 *   <li>新增泛型结果类 {@link Result}（success / fail 四个静态工厂）；</li>
 *   <li>用户模块与资源模块的业务方法全部改为返回 Result；</li>
 *   <li>控制台统一处理结果：成功打印提示并按类型展示 data，失败只打印 msg；</li>
 *   <li>业务异常仍照常抛出，由菜单层统一捕获并包装成 {@code Result.fail(...)}；</li>
 *   <li>「资源不存在」改由 {@code Result.fail} 表达，不再需要专用异常类。</li>
 * </ol>
 * </p>
 *
 * <p>版本约束：本阶段仍不分层，为任务 10 的三层架构重构保留「痛点」。</p>
 *
 * @author 课程设计
 * @version V2.1
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
        System.out.println("    校园资源预约管理系统  V2.1（统一返回结果）");
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
     * 按账号查询用户。
     *
     * @param username 账号
     * @return 查询结果，失败时 msg 说明原因
     */
    private Result<User> findByUsername(String username) {
        for (User user : userList) {
            if (user.getUsername().equals(username)) {
                return Result.success("查询成功", user);
            }
        }
        return Result.fail("账号【" + username + "】不存在，请确认后重试或先注册。");
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
                } else {
                    showResult(Result.fail("无效的菜单序号，请输入 1、2、3、4 或 0。"));
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
     * 查询全部资源业务。
     *
     * @param admin 操作人
     * @return Result&lt;List&lt;CampusResource&gt;&gt; 资源列表
     */
    private Result<List<CampusResource>> listAllResources(User admin) {
        requireAdmin(admin, "查询全部资源");
        if (resourceList.isEmpty()) {
            return Result.fail("当前没有任何资源，请先新增。");
        }
        return Result.success("共查询到 " + resourceList.size() + " 条资源。",
                new ArrayList<CampusResource>(resourceList));
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
     * 删除资源业务：存在预约记录的资源禁止删除（任务 9 接入预约模块后生效）。
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
                } else {
                    showResult(Result.fail("无效的菜单序号，请输入 1 或 0。"));
                }
            } catch (BusinessException e) {
                showResult(Result.fail(e.getMessage()));
            } catch (RuntimeException e) {
                showResult(Result.fail("系统异常：操作未能完成，请稍后重试。"));
            }
        }
    }

    /**
     * 查询可预约资源：自动过滤掉占用与停用的资源。
     *
     * @return Result&lt;List&lt;CampusResource&gt;&gt; 可预约资源列表
     */
    private Result<List<CampusResource>> listAvailableResources() {
        List<CampusResource> available = new ArrayList<CampusResource>();
        for (CampusResource resource : resourceList) {
            if (resource.isAvailable()) {
                available.add(resource);
            }
        }
        if (available.isEmpty()) {
            return Result.fail("当前没有可预约的资源。");
        }
        return Result.success("共 " + available.size() + " 条可预约资源。", available);
    }

    // ================================================== 结果展示

    /**
     * 统一展示结果：成功打印提示并按类型展示数据，失败只打印提示。
     *
     * <p>这里只依赖 Result 的 success / msg / data 三个属性，
     * 不关心业务内部怎么实现（迪米特法则）。</p>
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
     * 按资源编号查询资源。
     *
     * @param id 资源编号
     * @return 查询结果，找不到时 msg 说明原因
     */
    private Result<CampusResource> findResourceById(String id) {
        for (CampusResource resource : resourceList) {
            if (resource.getId().equals(id)) {
                return Result.success("查询成功", resource);
            }
        }
        return Result.fail("未找到 ID 为【" + id + "】的资源。");
    }

    /**
     * 判断资源是否已有预约记录。
     *
     * <p>任务 7 阶段预约模块尚未开发，这里先返回 false；
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
