package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.CurrentUserContext;
import com.example.entity.ClassInfo;
import com.example.entity.Major;
import com.example.entity.User;
import com.example.enums.RoleEnum;
import com.example.enums.StatusEnum;
import com.example.exception.CustomerException;
import com.example.mapper.ClassInfoMapper;
import com.example.mapper.MajorMapper;
import com.example.mapper.UserMapper;
import com.example.utils.TokenUtils;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ClassInfoMapper classInfoMapper;
    private final MajorMapper majorMapper;
    private final FileStorageService fileStorageService;

    public UserService(BCryptPasswordEncoder passwordEncoder, UserMapper userMapper,
                       ClassInfoMapper classInfoMapper, MajorMapper majorMapper,
                       FileStorageService fileStorageService) {
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.classInfoMapper = classInfoMapper;
        this.majorMapper = majorMapper;
        this.fileStorageService = fileStorageService;
    }

    public User login(String username, String password, boolean rememberMe) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new CustomerException("账号或密码不能为空");
        }
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser == null) {
            throw new CustomerException("账号不存在");
        }
        if (!passwordEncoder.matches(password, dbUser.getPassword())) {
            throw new CustomerException("账号或密码错误");
        }
        if (StatusEnum.DISABLED.name().equals(dbUser.getStatus())) {
            throw new CustomerException("账号已被禁用");
        }
        int days = rememberMe ? 7 : 1;
        String token = TokenUtils.createToken(dbUser.getId() + "-" + dbUser.getRole(), days);
        dbUser.setPassword(null);
        dbUser.setToken(token);
        return dbUser;
    }

    public User getCurrentProfile() {
        Integer userId = CurrentUserContext.getUserId();
        User user = getById(userId);
        if (user == null) {
            throw new CustomerException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public void updateProfile(User user) {
        Integer userId = CurrentUserContext.getUserId();
        User dbUser = getById(userId);
        if (dbUser == null) {
            throw new CustomerException("用户不存在");
        }
        if (StrUtil.isNotBlank(user.getAvatar()) && user.getAvatar().startsWith("data:")) {
            throw new CustomerException("请通过头像上传功能更新头像");
        }
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getId, userId)
                .set(StrUtil.isNotBlank(user.getNickname()), User::getNickname, user.getNickname())
                .set(StrUtil.isNotBlank(user.getAvatar()), User::getAvatar, user.getAvatar())
                .set(StrUtil.isNotBlank(user.getIntro()), User::getIntro, user.getIntro())
                .set(StrUtil.isNotBlank(user.getEmail()), User::getEmail, user.getEmail())
                .set(StrUtil.isNotBlank(user.getPhone()), User::getPhone, user.getPhone());
        update(wrapper);
    }

    /**
     * 上传头像，文件存本地存储，数据库只保存访问地址。
     */
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomerException("请选择要上传的图片");
        }
        if (file.getSize() > 2L * 1024 * 1024) {
            throw new CustomerException("头像大小不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new CustomerException("仅支持上传图片文件");
        }
        Integer userId = CurrentUserContext.getUserId();
        User dbUser = getById(userId);
        if (dbUser == null) {
            throw new CustomerException("用户不存在");
        }
        String filename = fileStorageService.store(file);
        String url = fileStorageService.getPreviewUrl(filename);
        update(Wrappers.lambdaUpdate(User.class)
                .eq(User::getId, userId)
                .set(User::getAvatar, url));
        String oldAvatar = dbUser.getAvatar();
        String prefix = fileStorageService.getPreviewUrl("");
        if (oldAvatar != null && oldAvatar.startsWith(prefix)) {
            fileStorageService.delete(oldAvatar.substring(prefix.length()));
        }
        return url;
    }

    public void updatePassword(String oldPassword, String newPassword) {
        if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
            throw new CustomerException("密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new CustomerException("新密码长度不能少于6位");
        }
        Integer userId = CurrentUserContext.getUserId();
        User dbUser = getById(userId);
        if (dbUser == null) {
            throw new CustomerException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, dbUser.getPassword())) {
            throw new CustomerException("旧密码错误");
        }
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getId, userId).set(User::getPassword, passwordEncoder.encode(newPassword));
        update(wrapper);
    }

    public Page<User> selectUserPage(Integer pageNum, Integer pageSize, String username, String role, String status) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(username), User::getUsername, username)
                .eq(StrUtil.isNotBlank(role), User::getRole, role)
                .eq(StrUtil.isNotBlank(status), User::getStatus, status)
                .orderByDesc(User::getCreatedAt);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    public Page<User> selectUserPageExcludeAdmin(Integer pageNum, Integer pageSize, String username, String role, String status) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(User::getRole, RoleEnum.ADMIN.name())
                .like(StrUtil.isNotBlank(username), User::getUsername, username)
                .eq(StrUtil.isNotBlank(role), User::getRole, role)
                .eq(StrUtil.isNotBlank(status), User::getStatus, status)
                .orderByDesc(User::getCreatedAt);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    public void addByAdmin(User user) {
        if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPassword())) {
            throw new CustomerException("账号或密码不能为空");
        }
        if (user.getPassword().length() < 6) {
            throw new CustomerException("密码长度不能少于6位");
        }
        if (StrUtil.isNotBlank(user.getEmail()) && !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            throw new CustomerException("邮箱格式不正确");
        }
        User dbUser = userMapper.selectByUsername(user.getUsername());
        if (dbUser != null) {
            throw new CustomerException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setNickname(StrUtil.blankToDefault(user.getNickname(), user.getUsername()));
        user.setStatus(StatusEnum.ENABLED.name());
        if (StrUtil.isBlank(user.getRole())) {
            user.setRole(RoleEnum.STUDENT.name());
        }
        userMapper.insert(user);
    }

    /**
     * 按班级批量导入学生（Excel：第一列学号，第二列姓名）。用户名=学号，默认密码 123456。
     */
    public Map<String, Object> importStudents(Integer classId, MultipartFile file) {
        if (classId == null) {
            throw new CustomerException("请选择目标班级");
        }
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new CustomerException("班级不存在");
        }
        Integer majorId = classInfo.getMajorId();
        Integer schoolId = null;
        if (majorId != null) {
            Major major = majorMapper.selectById(majorId);
            if (major != null) {
                schoolId = major.getSchoolId();
            }
        }
        if (file == null || file.isEmpty()) {
            throw new CustomerException("请上传 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new CustomerException("仅支持 .xlsx 或 .xls 文件");
        }

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String studentNo = formatter.formatCellValue(row.getCell(0)).trim();
                if (studentNo.isEmpty()) {
                    continue;
                }
                String name = formatter.formatCellValue(row.getCell(1)).trim();
                if (name.isEmpty()) {
                    name = studentNo;
                }
                if (userMapper.selectByUsername(studentNo) != null) {
                    skipped++;
                    errors.add("第" + (i + 1) + "行：学号 " + studentNo + " 已存在");
                    continue;
                }

                User user = new User();
                user.setUsername(studentNo);
                user.setPassword(passwordEncoder.encode("123456"));
                user.setNickname(name);
                user.setRole(RoleEnum.STUDENT.name());
                user.setStudentNo(studentNo);
                user.setSchoolId(schoolId);
                user.setMajorId(majorId);
                user.setClassId(classId);
                user.setGrade(classInfo.getGrade());
                user.setStatus(StatusEnum.ENABLED.name());
                userMapper.insert(user);
                imported++;
            }
        } catch (Exception e) {
            throw new CustomerException("Excel 解析失败：" + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("total", imported + skipped);
        result.put("errors", errors);
        result.put("className", classInfo.getName());
        return result;
    }

    /**
     * 批量导入教师（Excel：第一列工号，第二列姓名）。用户名=工号，默认密码 123456。
     */
    public Map<String, Object> importTeachers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomerException("请上传 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new CustomerException("仅支持 .xlsx 或 .xls 文件");
        }

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String teacherNo = formatter.formatCellValue(row.getCell(0)).trim();
                if (teacherNo.isEmpty()) {
                    continue;
                }
                String name = formatter.formatCellValue(row.getCell(1)).trim();
                if (name.isEmpty()) {
                    name = teacherNo;
                }
                if (userMapper.selectByUsername(teacherNo) != null) {
                    skipped++;
                    errors.add("第" + (i + 1) + "行：工号 " + teacherNo + " 已存在");
                    continue;
                }

                User user = new User();
                user.setUsername(teacherNo);
                user.setPassword(passwordEncoder.encode("123456"));
                user.setNickname(name);
                user.setRole(RoleEnum.TEACHER.name());
                user.setTeacherNo(teacherNo);
                user.setStatus(StatusEnum.ENABLED.name());
                userMapper.insert(user);
                imported++;
            }
        } catch (Exception e) {
            throw new CustomerException("Excel 解析失败：" + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("total", imported + skipped);
        result.put("errors", errors);
        return result;
    }

    public void updateStatus(Integer id, String status) {
        if (!StatusEnum.ENABLED.name().equals(status) && !StatusEnum.DISABLED.name().equals(status)) {
            throw new CustomerException("状态值非法");
        }
        User dbUser = getById(id);
        if (dbUser == null) {
            throw new CustomerException("用户不存在");
        }
        Integer currentUserId = CurrentUserContext.getUserId();
        if (currentUserId != null && id.equals(currentUserId) && StatusEnum.DISABLED.name().equals(status)) {
            throw new CustomerException("不能禁用自己的账号");
        }
        // 禁用系统管理员时，确保至少保留一名启用的系统管理员
        if (StatusEnum.DISABLED.name().equals(status)
                && RoleEnum.ADMIN.name().equals(dbUser.getRole())
                && StatusEnum.ENABLED.name().equals(dbUser.getStatus())
                && countAdmins() <= 1) {
            throw new CustomerException("至少需要保留一名启用的系统管理员");
        }
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getId, id).set(User::getStatus, status);
        update(wrapper);
    }

    public void resetPassword(Integer id, String confirmPassword) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new CustomerException("请输入管理员密码进行确认");
        }
        User currentDb = getById(current.getId());
        if (currentDb == null || !passwordEncoder.matches(confirmPassword, currentDb.getPassword())) {
            throw new CustomerException("管理员密码错误，无法重置");
        }
        User dbUser = getById(id);
        if (dbUser == null) {
            throw new CustomerException("用户不存在");
        }
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getId, id).set(User::getPassword, passwordEncoder.encode("123456"));
        update(wrapper);
    }

    public void batchResetPassword(List<Integer> ids, String confirmPassword) {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new CustomerException("请输入管理员密码进行确认");
        }
        User currentDb = getById(current.getId());
        if (currentDb == null || !passwordEncoder.matches(confirmPassword, currentDb.getPassword())) {
            throw new CustomerException("管理员密码错误，无法重置");
        }
        if (ids == null || ids.isEmpty()) {
            throw new CustomerException("请选择要操作的用户");
        }
        if (ids.contains(current.getId())) {
            throw new CustomerException("不能对当前登录账号执行此操作，请通过个人设置修改密码");
        }
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.in(User::getId, ids).set(User::getPassword, passwordEncoder.encode("123456"));
        update(wrapper);
    }

    public void deleteUser(Integer id) {
        User dbUser = getById(id);
        if (dbUser == null) {
            throw new CustomerException("用户不存在");
        }
        if (RoleEnum.ADMIN.name().equals(dbUser.getRole()) && countAdmins() <= 1) {
            throw new CustomerException("至少保留一名系统管理员");
        }
        long managingProjects = userMapper.countManagingProjects(id);
        if (managingProjects > 0) {
            throw new CustomerException("该用户为在岗项目负责人，请先移交项目后再删除");
        }
        removeById(id);
    }

    public void batchUpdateStatus(List<Integer> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomerException("请选择要操作的用户");
        }
        if (!StatusEnum.ENABLED.name().equals(status) && !StatusEnum.DISABLED.name().equals(status)) {
            throw new CustomerException("状态值非法");
        }
        Integer currentUserId = CurrentUserContext.getUserId();
        if (currentUserId != null && ids.contains(currentUserId)) {
            throw new CustomerException("不能对当前登录账号执行此操作");
        }
        // 如果是禁用操作，确保至少保留一个启用的系统管理员
        if (StatusEnum.DISABLED.name().equals(status)) {
            long enabledAdmins = countAdmins();
            List<User> selectedAdmins = listByIds(ids).stream()
                    .filter(u -> RoleEnum.ADMIN.name().equals(u.getRole()) && StatusEnum.ENABLED.name().equals(u.getStatus()))
                    .toList();
            if (enabledAdmins - selectedAdmins.size() <= 0) {
                throw new CustomerException("至少需要保留一名启用的系统管理员");
            }
        }
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.in(User::getId, ids).set(User::getStatus, status);
        update(wrapper);
    }

    public void batchDeleteUsers(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomerException("请选择要删除的用户");
        }
        Integer currentUserId = CurrentUserContext.getUserId();
        if (currentUserId != null && ids.contains(currentUserId)) {
            throw new CustomerException("不能删除当前登录账号");
        }
        List<User> users = listByIds(ids);
        if (users.isEmpty()) {
            throw new CustomerException("用户不存在");
        }
        long adminCount = countAdmins();
        long selectedAdminCount = users.stream()
                .filter(u -> RoleEnum.ADMIN.name().equals(u.getRole()) && StatusEnum.ENABLED.name().equals(u.getStatus()))
                .count();
        if (adminCount - selectedAdminCount <= 0) {
            throw new CustomerException("至少保留一名系统管理员");
        }
        // 检查是否有在岗项目负责人
        List<String> managingUsers = new ArrayList<>();
        for (User user : users) {
            long managingProjects = userMapper.countManagingProjects(user.getId());
            if (managingProjects > 0) {
                managingUsers.add(user.getUsername());
            }
        }
        if (!managingUsers.isEmpty()) {
            throw new CustomerException("以下用户为在岗项目负责人，请先移交项目后再删除：" + String.join("、", managingUsers));
        }
        removeByIds(ids);
    }

    public Page<User> searchEnabledUsers(Integer pageNum, Integer pageSize, String keyword, String role) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getStudentNo, keyword)
                    .or().like(User::getTeacherNo, keyword)
                    .or().like(User::getNickname, keyword));
        }
        wrapper.eq(User::getStatus, StatusEnum.ENABLED.name())
                .ne(User::getRole, "ADMIN")
                .eq(StrUtil.isNotBlank(role), User::getRole, role)
                .orderByDesc(User::getCreatedAt);
        Page<User> page = page(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }

    public List<User> listTeachers() {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getRole, RoleEnum.TEACHER.name())
                .eq(User::getStatus, StatusEnum.ENABLED.name())
                .orderByAsc(User::getId);
        List<User> list = list(wrapper);
        list.forEach(u -> u.setPassword(null));
        return list;
    }

    private long countAdmins() {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getRole, RoleEnum.ADMIN.name()).eq(User::getStatus, StatusEnum.ENABLED.name());
        return count(wrapper);
    }

}
