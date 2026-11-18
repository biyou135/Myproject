package com.campus;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * V3.0 资源预约核心业务：打通「浏览资源 → 提交预约 → 管理员审核 → 状态联动 → 取消预约」闭环。
 *
 * <p>本版本相对 V2.2 的变化：
 * <ol>
 *   <li>新增实体 {@link ReserveRecord} 与状态枚举 {@link ReserveStatus}；</li>
 *   <li>新增预约持久化 reserve.txt，启动加载、变更后落盘；</li>
 *   <li>学生菜单：提交预约（时间校验 + 冲突校验）、我的预约、取消预约；</li>
 *   <li>管理员菜单：全部预约、审核（通过 / 驳回）、按条件查询预约；</li>
 *   <li>业务联动：审核通过把资源置为占用，已通过预约取消后资源恢复可预约；</li>
 *   <li>资源删除校验升级：仅存在「已通过」预约时禁止删除。</li>
 * </ol>
 * </p>
 *
 * <p>版本约束：本阶段仍不分层，业务逻辑全写在主类中，
 * 为任务 10 的三层架构重构保留最后的「耦合痛点」。</p>
 *
 * @author 课程设计
 * @version V3.0
 */
public class SystemMain {

    /** 密码最小长度约束 */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /** 用户数据持久化文件 */
    private static final String USER_FILE = "user.txt";

    /** 资源数据持久化文件 */
    private static final String RESOURCE_FILE = "resource.txt";

    /** 预约记录持久化文件 */
    private static final String RESERVE_FILE = "reserve.txt";

    /** 数据文件字段分隔符 */
    private static final String SEPARATOR = ",";

    /** 资源 ID 前缀 */
    private static final String RESOURCE_ID_PREFIX = "R";

    /** 预约记录 ID 前缀 */
    private static final String RECORD_ID_PREFIX = "A";

    /** 时间展示格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 用户输入时间的格式（分钟级即可） */
    private static final String INPUT_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    /** 资源排序规则：按编号升序 */
    private static final Comparator<CampusResource> RESOURCE_BY_ID = Comparator.comparing(CampusResource::getId);

    /** 预约排序规则：按记录编号升序 */
    private static final Comparator<ReserveRecord> RECORD_BY_ID = Comparator.comparing(ReserveRecord::getRecordId);

    /** 全部用户对象 */
    private final List<User> userList = new ArrayList<User>();

    /** 全部资源对象 */
    private final List<CampusResource> resourceList = new ArrayList<CampusResource>();

    /** 全部预约记录 */
    private final List<ReserveRecord> reserveRecordList = new ArrayList<ReserveRecord>();

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
     * 启动主流程：加载三份数据 → 首页菜单 → 登录后进入角色菜单。
     */
    private void start() {
        loadUsers();
        loadResources();
        loadReserveRecords();
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
        System.out.println("    校园资源预约管理系统  V3.0（预约核心业务）");
        System.out.println("==================================================");
        System.out.println("内置管理员账号：admin / 123456");
        System.out.println("数据文件：" + USER_FILE + "、" + RESOURCE_FILE + "、" + RESERVE_FILE);
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
     * 登录业务：成功返回当前用户对象。
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
     * 按账号查询用户：Optional 表达「可能找不到」。
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

    /** 从 user.txt 加载用户数据；首次运行初始化内置管理员并落盘。 */
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
     * 管理员菜单：资源管理 + 预约审核。
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
            System.out.println("  5. 资源条件查询");
            System.out.println("  6. 查询全部预约记录");
            System.out.println("  7. 审核预约");
            System.out.println("  8. 预约条件查询");
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
                } else if ("6".equals(choice)) {
                    showResult(listAllReserves(admin));
                } else if ("7".equals(choice)) {
                    showResult(auditReserve(admin));
                } else if ("8".equals(choice)) {
                    showResult(queryReserves(admin));
                } else {
                    showResult(Result.fail("无效的菜单序号，请输入 1~8 或 0。"));
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
        List<CampusResource> all = resourceList.stream()
                .sorted(RESOURCE_BY_ID)
                .collect(Collectors.toList());
        if (all.isEmpty()) {
            return Result.fail("当前没有任何资源，请先新增。");
        }
        return Result.success("共查询到 " + all.size() + " 条资源。", all);
    }

    /**
     * 修改资源业务。
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
     * 删除资源业务：存在「已通过」预约的资源禁止删除。
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
        if (hasPassReservation(id)) {
            return Result.fail("资源【" + id + "】存在已通过的预约，禁止删除。");
        }
        resourceList.remove(findResult.getData());
        saveResources();
        return Result.success("资源【" + id + "】已删除。");
    }

    /**
     * 按条件查询资源。
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

    /**
     * 查询全部预约记录（管理员视角）。
     *
     * @param admin 操作人
     * @return Result&lt;List&lt;ReserveRecord&gt;&gt; 预约列表
     */
    private Result<List<ReserveRecord>> listAllReserves(User admin) {
        requireAdmin(admin, "查询全部预约记录");
        List<ReserveRecord> all = reserveRecordList.stream()
                .sorted(RECORD_BY_ID)
                .collect(Collectors.toList());
        if (all.isEmpty()) {
            return Result.fail("当前没有任何预约记录。");
        }
        return Result.success("共查询到 " + all.size() + " 条预约记录。", all);
    }

    /**
     * 审核预约业务：通过 → 记录置 PASS 且资源置占用；驳回 → 记录置 REJECT，资源不变。
     *
     * @param admin 操作人
     * @return Result&lt;ReserveRecord&gt; 审核后的记录
     */
    private Result<ReserveRecord> auditReserve(User admin) {
        requireAdmin(admin, "审核预约");
        String recordId = requireText(readLine("请输入要审核的预约记录编号："), "预约记录编号");
        Result<ReserveRecord> findResult = findReserveById(recordId);
        if (!findResult.isSuccess()) {
            return findResult;
        }
        ReserveRecord record = findResult.getData();
        System.out.println("待审核记录：" + record);
        String action = readLine("请选择审核结果（1.通过 2.驳回）：");
        if ("1".equals(action)) {
            if (!ReserveStatus.PENDING.equals(record.getStatus())) {
                throw new ReserveStatusException("审核通过", record.getStatus().getLabel());
            }
            record.setStatus(ReserveStatus.PASS);
            // 业务联动：审核通过把对应资源置为占用
            Result<CampusResource> resourceResult = findResourceById(record.getResourceId());
            if (resourceResult.isSuccess()) {
                resourceResult.getData().setStatus(ResourceStatus.OCCUPIED);
                saveResources();
            }
            saveReserveRecords();
            return Result.success("审核通过，资源【" + record.getResourceId() + "】已置为占用。", record);
        } else if ("2".equals(action)) {
            if (!ReserveStatus.PENDING.equals(record.getStatus())) {
                throw new ReserveStatusException("驳回", record.getStatus().getLabel());
            }
            record.setStatus(ReserveStatus.REJECT);
            saveReserveRecords();
            return Result.success("已驳回该预约，资源状态保持不变。", record);
        }
        return Result.fail("无效的审核选项，请输入 1 或 2。");
    }

    /**
     * 按条件查询预约：资源编号 / 申请人 / 状态均可留空表示不限。
     *
     * @param admin 操作人
     * @return Result&lt;List&lt;ReserveRecord&gt;&gt; 查询结果
     */
    private Result<List<ReserveRecord>> queryReserves(User admin) {
        requireAdmin(admin, "预约条件查询");
        String resourceId = readLine("资源编号（留空表示不限）：");
        String applicant = readLine("申请人账号（留空表示不限）：");
        String statusText = readLine("状态（1.待审核 2.已通过 3.已驳回 4.已取消，留空表示不限）：");
        ReserveStatus status = null;
        if (statusText != null && !statusText.isEmpty()) {
            status = ReserveStatus.parse(statusText);
            if (status == null) {
                throw ParamEmptyException.illegal("状态只能是 1~4 或留空。");
            }
        }
        return filterReserves(resourceId == null ? "" : resourceId.trim(),
                applicant == null ? "" : applicant.trim(), status);
    }

    // ================================================== 学生菜单

    /**
     * 学生菜单：浏览资源 + 预约相关操作。
     *
     * @param student 当前登录的学生
     */
    private void showStudentMenu(User student) {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("-------------- 学生菜单 --------------");
            System.out.println("  1. 查看可预约资源");
            System.out.println("  2. 按名称搜索可预约资源");
            System.out.println("  3. 提交预约申请");
            System.out.println("  4. 我的预约列表");
            System.out.println("  5. 取消预约");
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
                    showResult(listAvailableResources());
                } else if ("2".equals(choice)) {
                    String keyword = readLine("请输入资源名称关键字：");
                    showResult(filterResources(ResourceStatus.AVAILABLE, keyword == null ? "" : keyword.trim()));
                } else if ("3".equals(choice)) {
                    showResult(submitReserve(student));
                } else if ("4".equals(choice)) {
                    showResult(listMyReserves(student));
                } else if ("5".equals(choice)) {
                    showResult(cancelReserve(student));
                } else {
                    showResult(Result.fail("无效的菜单序号，请输入 1~5 或 0。"));
                }
            } catch (BusinessException e) {
                showResult(Result.fail(e.getMessage()));
            } catch (RuntimeException e) {
                showResult(Result.fail("系统异常：操作未能完成，请稍后重试。"));
            }
        }
    }

    /**
     * 提交预约申请：校验时间合法性、资源可预约性与时间段冲突。
     *
     * @param student 申请人
     * @return Result&lt;ReserveRecord&gt; 新建的预约记录
     */
    private Result<ReserveRecord> submitReserve(User student) {
        requireStudent(student, "提交预约申请");
        String resourceId = requireText(readLine("请输入要预约的资源编号："), "资源编号");

        Result<CampusResource> resourceResult = findResourceById(resourceId);
        if (!resourceResult.isSuccess()) {
            throw new ResourceUnavailableException(resourceId);
        }
        CampusResource resource = resourceResult.getData();
        if (!resource.isAvailable()) {
            throw new ResourceUnavailableException(resourceId, resource.getStatus().getLabel());
        }

        LocalDateTime start = parseDateTime(readLine("预约开始时间（yyyy-MM-dd HH:mm）："), "预约开始时间");
        LocalDateTime end = parseDateTime(readLine("预约结束时间（yyyy-MM-dd HH:mm）："), "预约结束时间");
        if (!start.isBefore(end)) {
            throw ParamEmptyException.illegal("预约开始时间必须早于结束时间。");
        }
        if (end.isBefore(LocalDateTime.now())) {
            throw ParamEmptyException.illegal("预约结束时间不能早于当前时间。");
        }
        checkTimeConflict(resourceId, start, end);

        String remark = readLine("预约备注（可留空）：");
        ReserveRecord record = new ReserveRecord(nextRecordId(), resourceId, student.getUsername(),
                LocalDateTime.now(), start, end, ReserveStatus.PENDING, remark == null ? "" : remark);
        reserveRecordList.add(record);
        saveReserveRecords();
        return Result.success("预约申请已提交，状态为待审核，请等待管理员审核。", record);
    }

    /**
     * 查询我的预约列表：只展示当前登录学生自己的记录。
     *
     * @param student 当前学生
     * @return Result&lt;List&lt;ReserveRecord&gt;&gt; 预约列表
     */
    private Result<List<ReserveRecord>> listMyReserves(User student) {
        List<ReserveRecord> mine = reserveRecordList.stream()
                .filter(record -> record.getApplicantUsername().equals(student.getUsername()))
                .sorted(RECORD_BY_ID)
                .collect(Collectors.toList());
        if (mine.isEmpty()) {
            return Result.fail("你还没有任何预约记录。");
        }
        return Result.success("你共有 " + mine.size() + " 条预约记录。", mine);
    }

    /**
     * 取消预约：仅「待审核」或「已通过」可取消；已通过的取消后资源恢复可预约。
     *
     * @param student 当前学生
     * @return Result&lt;ReserveRecord&gt; 取消后的记录
     */
    private Result<ReserveRecord> cancelReserve(User student) {
        requireStudent(student, "取消预约");
        String recordId = requireText(readLine("请输入要取消的预约记录编号："), "预约记录编号");
        Result<ReserveRecord> findResult = findReserveById(recordId);
        if (!findResult.isSuccess()) {
            return findResult;
        }
        ReserveRecord record = findResult.getData();
        if (!record.getApplicantUsername().equals(student.getUsername())) {
            throw new PermissionDeniedException("取消他人的预约", student.getRoleName());
        }
        boolean cancelable = ReserveStatus.PENDING.equals(record.getStatus())
                || ReserveStatus.PASS.equals(record.getStatus());
        if (!cancelable) {
            throw new ReserveStatusException("取消", record.getStatus().getLabel());
        }
        boolean wasPass = ReserveStatus.PASS.equals(record.getStatus());
        record.setStatus(ReserveStatus.CANCEL);
        // 业务联动：已通过的预约被取消后，资源恢复可预约
        if (wasPass) {
            Result<CampusResource> resourceResult = findResourceById(record.getResourceId());
            if (resourceResult.isSuccess()) {
                resourceResult.getData().setStatus(ResourceStatus.AVAILABLE);
                saveResources();
            }
        }
        saveReserveRecords();
        return Result.success(wasPass ? "预约已取消，资源已恢复可预约。" : "预约已取消。", record);
    }

    // ================================================== 预约查询核心

    /**
     * 按条件筛选预约记录。
     *
     * @param resourceId 资源编号，空串表示不限
     * @param applicant  申请人账号，空串表示不限
     * @param status     状态，null 表示不限
     * @return Result&lt;List&lt;ReserveRecord&gt;&gt; 查询结果
     */
    private Result<List<ReserveRecord>> filterReserves(String resourceId, String applicant, ReserveStatus status) {
        List<ReserveRecord> matched = reserveRecordList.stream()
                .filter(record -> resourceId.isEmpty() || record.getResourceId().equals(resourceId))
                .filter(record -> applicant.isEmpty() || record.getApplicantUsername().equals(applicant))
                .filter(record -> status == null || status.equals(record.getStatus()))
                .sorted(RECORD_BY_ID)
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            return Result.fail("没有符合条件的预约记录。");
        }
        return Result.success("共查询到 " + matched.size() + " 条预约记录。", matched);
    }

    /**
     * 按记录编号查询预约记录。
     *
     * @param recordId 记录编号
     * @return 查询结果
     */
    private Result<ReserveRecord> findReserveById(String recordId) {
        Optional<ReserveRecord> optional = reserveRecordList.stream()
                .filter(record -> record.getRecordId().equals(recordId))
                .findFirst();
        return optional
                .map(record -> Result.success("查询成功", record))
                .orElseGet(() -> Result.fail("预约记录【" + recordId + "】不存在。"));
    }

    /**
     * 时间冲突校验：同一资源下已通过的预约若与申请时段重叠，则拒绝提交。
     *
     * @param resourceId 资源编号
     * @param start      申请开始时间
     * @param end        申请结束时间
     */
    private void checkTimeConflict(String resourceId, LocalDateTime start, LocalDateTime end) {
        Optional<ReserveRecord> conflict = reserveRecordList.stream()
                .filter(record -> record.getResourceId().equals(resourceId))
                .filter(record -> ReserveStatus.PASS.equals(record.getStatus()))
                .filter(record -> overlaps(start, end, record.getReserveStartTime(), record.getReserveEndTime()))
                .findFirst();
        if (conflict.isPresent()) {
            throw new ReserveTimeConflictException(resourceId, conflict.get().getRecordId());
        }
    }

    /**
     * 判断两个时间段是否重叠（首尾相接不算重叠）。
     *
     * @param start1 时段 1 开始
     * @param end1   时段 1 结束
     * @param start2 时段 2 开始
     * @param end2   时段 2 结束
     * @return 重叠返回 true
     */
    private boolean overlaps(LocalDateTime start1, LocalDateTime end1,
                             LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
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

    // ================================================== 资源查询与辅助

    /**
     * 查询可预约资源：Stream 过滤掉占用与停用，按编号升序。
     *
     * @return Result&lt;List&lt;CampusResource&gt;&gt; 可预约资源列表
     */
    private Result<List<CampusResource>> listAvailableResources() {
        List<CampusResource> available = resourceList.stream()
                .filter(CampusResource::isAvailable)
                .sorted(RESOURCE_BY_ID)
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            return Result.fail("当前没有可预约的资源。");
        }
        return Result.success("共 " + available.size() + " 条可预约资源。", available);
    }

    /**
     * 多条件组合查询资源：状态 + 名称模糊（忽略大小写），按编号升序。
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
                .sorted(RESOURCE_BY_ID)
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            return Result.fail("没有符合条件的资源。");
        }
        return Result.success("共查询到 " + matched.size() + " 条资源。", matched);
    }

    /**
     * 按资源编号查询资源：Optional 显式表达「可能不存在」。
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

    /**
     * 判断资源是否存在「已通过」的预约记录。
     *
     * @param resourceId 资源编号
     * @return 存在已通过预约返回 true
     */
    private boolean hasPassReservation(String resourceId) {
        return reserveRecordList.stream()
                .anyMatch(record -> record.getResourceId().equals(resourceId)
                        && ReserveStatus.PASS.equals(record.getStatus()));
    }

    /**
     * 管理员权限校验。
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
     * 学生权限校验：管理员不能代替学生提交 / 取消预约。
     *
     * @param operator 操作人
     * @param action   被保护的操作名
     */
    private void requireStudent(User operator, String action) {
        if (operator == null || operator.isAdmin()) {
            String roleName = operator == null ? "未登录用户" : operator.getRoleName();
            throw new PermissionDeniedException(action + "（该操作仅限学生本人）", roleName);
        }
    }

    /**
     * 生成下一个资源编号，形如 R001。
     *
     * @return 新编号
     */
    private String nextResourceId() {
        int max = resourceList.stream()
                .map(CampusResource::getId)
                .filter(id -> id != null && id.startsWith(RESOURCE_ID_PREFIX))
                .mapToInt(id -> parseIntOrZero(id.substring(RESOURCE_ID_PREFIX.length())))
                .max()
                .orElse(0);
        return RESOURCE_ID_PREFIX + String.format("%03d", max + 1);
    }

    /**
     * 生成下一个预约记录编号，形如 A001。
     *
     * @return 新编号
     */
    private String nextRecordId() {
        int max = reserveRecordList.stream()
                .map(ReserveRecord::getRecordId)
                .filter(id -> id != null && id.startsWith(RECORD_ID_PREFIX))
                .mapToInt(id -> parseIntOrZero(id.substring(RECORD_ID_PREFIX.length())))
                .max()
                .orElse(0);
        return RECORD_ID_PREFIX + String.format("%03d", max + 1);
    }

    /**
     * 安全地把字符串转成整数，失败返回 0。
     *
     * @param text 文本
     * @return 整数或 0
     */
    private int parseIntOrZero(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ================================================== 资源持久化

    /** 从 resource.txt 加载资源数据。 */
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
     * 解析一行资源数据。
     *
     * @param line 文本行
     * @return 资源对象，格式非法返回 null
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

    // ================================================== 预约持久化

    /** 从 reserve.txt 加载预约记录。 */
    private void loadReserveRecords() {
        for (String line : FileUtil.readAllLines(RESERVE_FILE)) {
            ReserveRecord record = parseReserve(line);
            if (record != null) {
                reserveRecordList.add(record);
            }
        }
        System.out.println("[持久化] 已从 " + RESERVE_FILE + " 加载 " + reserveRecordList.size() + " 条预约记录。");
    }

    /**
     * 解析一行预约数据：recordId,resourceId,applicant,applyTime,start,end,status,remark。
     *
     * <p>使用 limit=8 切分，备注字段允许包含英文逗号（它是最后一个字段）。</p>
     *
     * @param line 文本行
     * @return 预约记录，格式非法返回 null
     */
    private ReserveRecord parseReserve(String line) {
        String[] parts = line.split(SEPARATOR, 8);
        if (parts.length < 8) {
            return null;
        }
        try {
            ReserveStatus status = ReserveStatus.parse(parts[6].trim());
            if (status == null) {
                return null;
            }
            return new ReserveRecord(parts[0].trim(), parts[1].trim(), parts[2].trim(),
                    LocalDateTime.parse(parts[3].trim(), FORMATTER),
                    LocalDateTime.parse(parts[4].trim(), FORMATTER),
                    LocalDateTime.parse(parts[5].trim(), FORMATTER),
                    status, parts[7].trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 把内存中的预约记录整体写回 reserve.txt。 */
    private void saveReserveRecords() {
        List<String> lines = new ArrayList<String>();
        for (ReserveRecord record : reserveRecordList) {
            lines.add(record.getRecordId() + SEPARATOR + record.getResourceId() + SEPARATOR
                    + record.getApplicantUsername() + SEPARATOR + record.getApplyTime().format(FORMATTER)
                    + SEPARATOR + record.getReserveStartTime().format(FORMATTER)
                    + SEPARATOR + record.getReserveEndTime().format(FORMATTER)
                    + SEPARATOR + record.getStatus().name() + SEPARATOR + record.getRemark());
        }
        FileUtil.writeAllLines(RESERVE_FILE, lines);
    }

    // ================================================== 通用校验

    /**
     * 判断账号是否已存在（注册查重用）。
     *
     * @param username 账号
     * @return 已存在返回 true
     */
    private boolean existsUsername(String username) {
        return userList.stream().anyMatch(user -> user.getUsername().equals(username));
    }

    /**
     * 解析用户输入的时间。
     *
     * @param text      输入文本
     * @param fieldName 字段中文名
     * @return 时间对象
     */
    private LocalDateTime parseDateTime(String text, String fieldName) {
        String value = requireText(text, fieldName);
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(INPUT_TIME_PATTERN));
        } catch (DateTimeParseException ignored) {
            // 再尝试带秒的写法
        }
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException e) {
            throw ParamEmptyException.illegal(fieldName + "格式应为 yyyy-MM-dd HH:mm，例如 2026-12-01 09:00。");
        }
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
     * 账号格式校验。
     *
     * @param username 账号
     */
    private void checkUsernameFormat(String username) {
        if (username.indexOf(' ') >= 0 || username.indexOf('\t') >= 0 || username.indexOf(',') >= 0) {
            throw ParamEmptyException.illegal("账号不能包含空白字符或英文逗号。");
        }
    }

    /**
     * 密码强度校验。
     *
     * @param password 密码
     */
    private void checkPasswordFormat(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw ParamEmptyException.illegal("密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位。");
        }
    }

    /**
     * 资源字段分隔符校验。
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
