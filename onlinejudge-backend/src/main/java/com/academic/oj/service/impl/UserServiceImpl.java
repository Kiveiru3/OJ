package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.LoginDTO;
import com.academic.oj.dto.RegisterDTO;
import com.academic.oj.dto.RoleProfileDTO;
import com.academic.oj.dto.SubmissionVO;
import com.academic.oj.dto.TokenDTO;
import com.academic.oj.dto.UserDailySubmissionVO;
import com.academic.oj.dto.UserInfoDTO;
import com.academic.oj.dto.UserListDTO;
import com.academic.oj.dto.UserProblemProgressVO;
import com.academic.oj.dto.UserPublicProfileVO;
import com.academic.oj.entity.AdminProfile;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.StudentProfile;
import com.academic.oj.entity.TeacherProfile;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.AdminProfileMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.mapper.StudentProfileMapper;
import com.academic.oj.mapper.TeacherProfileMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.UserService;
import com.academic.oj.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final AdminProfileMapper adminProfileMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            log.warn("Login failed: user not found - {}", loginDTO.getUsername());
            throw new BusinessException("Invalid username or password");
        }

        String encodedPassword = user.getPassword();
        if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
            log.error("User {} has null or empty password", loginDTO.getUsername());
            throw new BusinessException("Invalid username or password");
        }

        // 移除可能的空格、换行符等，并确保长度正确（BCrypt标准是60字符）
        encodedPassword = encodedPassword.trim();
        
        // 如果长度不是60，尝试截取前60个字符（可能是数据库中有额外字符）
        if (encodedPassword.length() != 60) {
            log.warn("Password hash length is {} (expected 60), truncating to 60 characters", encodedPassword.length());
            if (encodedPassword.length() > 60) {
                encodedPassword = encodedPassword.substring(0, 60);
            } else {
                log.error("Password hash too short: {} characters", encodedPassword.length());
                throw new BusinessException("Invalid username or password");
            }
        }
        
        // 检查密码长度（BCrypt密码应该是60个字符）
        int passwordLength = encodedPassword.length();
        if (passwordLength < 60) {
            log.error("Password for user {} is too short ({} chars), expected 60 for BCrypt", 
                    loginDTO.getUsername(), passwordLength);
            log.error("Password value: {}", encodedPassword);
            throw new BusinessException("Invalid username or password");
        }

        // 确保密码是BCrypt格式（以$2a$、$2b$或$2y$开头）
        if (!encodedPassword.startsWith("$2a$") && !encodedPassword.startsWith("$2b$") && !encodedPassword.startsWith("$2y$")) {
            log.error("Password for user {} is not in BCrypt format. First 10 chars: {}", 
                    loginDTO.getUsername(), encodedPassword.substring(0, Math.min(10, encodedPassword.length())));
            throw new BusinessException("Invalid username or password");
        }

        // 验证密码
        try {
            log.info("=== Password Verification Debug ===");
            log.info("Username: {}", loginDTO.getUsername());
            log.info("Password hash length: {}", passwordLength);
            log.info("Password hash prefix: {}", encodedPassword.substring(0, 7));
            log.info("Password hash suffix: {}", encodedPassword.substring(Math.max(0, passwordLength - 10)));
            log.info("Input password length: {}", loginDTO.getPassword() != null ? loginDTO.getPassword().length() : 0);
            
            boolean matches = passwordEncoder.matches(loginDTO.getPassword(), encodedPassword);
            log.info("Password match result: {}", matches);
            
            if (!matches) {
                log.error("=== Login Failed ===");
                log.error("Password mismatch for user: {}", loginDTO.getUsername());
                log.error("Stored hash: {}", encodedPassword);
                log.error("This usually means:");
                log.error("  1. The password entered is incorrect");
                log.error("  2. The password hash in database doesn't match the entered password");
                log.error("Solution: Use /dev/token/{} to get token without password", loginDTO.getUsername());
                throw new BusinessException("Invalid username or password");
            }
            
            log.info("Password verified successfully for user: {}", loginDTO.getUsername());
        } catch (IllegalArgumentException e) {
            log.error("Password encoding error for user {}: {}", loginDTO.getUsername(), e.getMessage());
            log.error("Password length: {}, First 20 chars: {}", 
                    passwordLength, encodedPassword.substring(0, Math.min(20, encodedPassword.length())));
            throw new BusinessException("Invalid username or password");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("User account is disabled");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRole(user.getRole());

        return new TokenDTO(token, userInfo);
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw new BusinessException("Username already exists");
        }

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, registerDTO.getEmail());
        if (userMapper.selectOne(wrapper) != null) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setRole(Constants.ROLE_STUDENT);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        ensureRoleProfile(user.getId(), user.getRole());
    }

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRole(user.getRole());
        ensureRoleProfile(user.getId(), user.getRole());

        return userInfo;
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void updateUserInfo(Long userId, UserInfoDTO userInfo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (userInfo.getEmail() != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, userInfo.getEmail());
            wrapper.ne(User::getId, userId);
            if (userMapper.selectOne(wrapper) != null) {
                throw new BusinessException("Email already exists");
            }
            user.setEmail(userInfo.getEmail());
        }

        if (userInfo.getNickname() != null) {
            user.setNickname(userInfo.getNickname());
        }
        if (userInfo.getAvatar() != null) {
            String avatar = userInfo.getAvatar().trim();
            user.setAvatar(avatar.isEmpty() ? null : avatar);
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public RoleProfileDTO getRoleProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "User not found");
        }
        ensureRoleProfile(user.getId(), user.getRole());
        return loadRoleProfile(user);
    }

    @Override
    public UserPublicProfileVO getPublicProfile(Long targetUserId) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "User not found");
        }

        ensureRoleProfile(user.getId(), user.getRole());
        RoleProfileDTO roleProfile = loadRoleProfile(user);

        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, targetUserId)
                .orderByDesc(Submission::getCreateTime)
                .orderByDesc(Submission::getId));

        Map<Long, String> problemTitleMap = loadProblemTitleMap(submissions);
        List<SubmissionVO> recentSubmissions = submissions.stream()
                .limit(30)
                .map(item -> toSubmissionVO(item, problemTitleMap))
                .toList();

        List<UserProblemProgressVO> progressList = buildProblemProgress(submissions, problemTitleMap);
        Set<Long> attemptedProblemSet = progressList.stream()
                .map(UserProblemProgressVO::getProblemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> solvedProblemSet = progressList.stream()
                .filter(item -> Boolean.TRUE.equals(item.getPassed()))
                .map(UserProblemProgressVO::getProblemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        int totalSubmissions = submissions.size();
        int acceptedSubmissions = (int) submissions.stream()
                .filter(item -> Constants.STATUS_ACCEPTED.equals(item.getStatus()))
                .count();

        UserPublicProfileVO vo = new UserPublicProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setRoleProfile(roleProfile);
        vo.setTotalSubmissions(totalSubmissions);
        vo.setAcceptedSubmissions(acceptedSubmissions);
        vo.setAttemptedProblems(attemptedProblemSet.size());
        vo.setSolvedProblems(solvedProblemSet.size());
        vo.setAcceptanceRate(calculateRate(acceptedSubmissions, totalSubmissions));
        vo.setDailySubmissionActivity(buildDailySubmissionActivity(submissions));
        vo.setProblemProgress(progressList);
        vo.setRecentSubmissions(recentSubmissions);
        return vo;
    }

    @Override
    public void updateRoleProfile(Long userId, RoleProfileDTO profileDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "User not found");
        }
        ensureRoleProfile(user.getId(), user.getRole());
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
        switch (role) {
            case Constants.ROLE_STUDENT:
                upsertStudentProfile(user.getId(), profileDTO);
                return;
            case Constants.ROLE_TEACHER:
                upsertTeacherProfile(user.getId(), profileDTO);
                return;
            case Constants.ROLE_ADMIN:
                upsertAdminProfile(user.getId(), profileDTO);
                return;
            default:
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid user role");
        }
    }

    @Override
    public Page<UserListDTO> getUserList(Integer page, Integer size, String keyword, String role, Integer status) {
        Page<User> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, normalizeRole(role));
        }
        if (status != null) {
            validateStatus(status);
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> userPage = userMapper.selectPage(pageObj, wrapper);

        Page<UserListDTO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(userPage.getRecords().stream().map(user -> {
            UserListDTO dto = new UserListDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setAvatar(user.getAvatar());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setStatus(user.getStatus());
            dto.setCreateTime(user.getCreateTime());
            dto.setUpdateTime(user.getUpdateTime());
            return dto;
        }).toList());
        return resultPage;
    }

    @Override
    public void adminUpdateUser(Long operatorId, Long targetUserId, String role, Integer status) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "User not found");
        }

        String normalizedRole = normalizeRole(role);
        validateStatus(status);

        if (operatorId.equals(targetUserId)) {
            if (!Constants.ROLE_ADMIN.equals(normalizedRole)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Cannot change your own role from ADMIN");
            }
            if (Integer.valueOf(0).equals(status)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Cannot disable yourself");
            }
        }

        user.setRole(normalizedRole);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        ensureRoleProfile(user.getId(), normalizedRole);
    }

    @Override
    public void adminResetPassword(Long operatorId, Long targetUserId, String newPassword) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "User not found");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6 || newPassword.length() > 50) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Password length must be between 6 and 50");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private Map<Long, String> loadProblemTitleMap(List<Submission> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> problemIds = submissions.stream()
                .map(Submission::getProblemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (problemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle, (a, b) -> a));
    }

    private List<UserProblemProgressVO> buildProblemProgress(List<Submission> submissions, Map<Long, String> problemTitleMap) {
        if (submissions == null || submissions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProblemProgressVO> map = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            if (submission.getProblemId() == null) {
                continue;
            }
            UserProblemProgressVO item = map.get(submission.getProblemId());
            if (item == null) {
                item = new UserProblemProgressVO();
                item.setProblemId(submission.getProblemId());
                item.setProblemTitle(problemTitleMap.get(submission.getProblemId()));
                item.setSubmitCount(0);
                item.setAcceptedCount(0);
                item.setLatestStatus(submission.getStatus());
                item.setLastSubmitTime(submission.getCreateTime());
                map.put(submission.getProblemId(), item);
            }

            item.setSubmitCount(item.getSubmitCount() + 1);
            if (Constants.STATUS_ACCEPTED.equals(submission.getStatus())) {
                item.setAcceptedCount(item.getAcceptedCount() + 1);
            }
        }

        List<UserProblemProgressVO> list = new ArrayList<>(map.values());
        list.forEach(item -> item.setPassed(item.getAcceptedCount() != null && item.getAcceptedCount() > 0));
        list.sort(Comparator.comparing(UserProblemProgressVO::getLastSubmitTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private SubmissionVO toSubmissionVO(Submission submission, Map<Long, String> problemTitleMap) {
        SubmissionVO vo = new SubmissionVO();
        vo.setId(submission.getId());
        vo.setProblemId(submission.getProblemId());
        vo.setProblemTitle(problemTitleMap.get(submission.getProblemId()));
        vo.setLanguage(submission.getLanguage());
        vo.setStatus(submission.getStatus());
        vo.setExecuteTime(submission.getTimeUsed());
        vo.setExecuteMemory(submission.getMemoryUsed());
        vo.setErrorMessage(submission.getErrorMessage());
        vo.setSubmitTime(submission.getCreateTime());
        return vo;
    }

    private List<UserDailySubmissionVO> buildDailySubmissionActivity(List<Submission> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<LocalDate, Integer> dateCountMap = new TreeMap<>();
        for (Submission submission : submissions) {
            if (submission == null || submission.getCreateTime() == null) {
                continue;
            }
            LocalDate day = submission.getCreateTime().toLocalDate();
            dateCountMap.put(day, dateCountMap.getOrDefault(day, 0) + 1);
        }

        if (dateCountMap.isEmpty()) {
            return Collections.emptyList();
        }
        return dateCountMap.entrySet().stream().map(entry -> {
            UserDailySubmissionVO item = new UserDailySubmissionVO();
            item.setDate(entry.getKey().toString());
            item.setCount(entry.getValue());
            return item;
        }).toList();
    }

    private double calculateRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        double value = numerator * 100.0 / denominator;
        return Math.round(value * 100.0) / 100.0;
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Role is required");
        }
        String normalized = role.trim().toUpperCase();
        if (!Constants.ROLE_ADMIN.equals(normalized)
                && !Constants.ROLE_TEACHER.equals(normalized)
                && !Constants.ROLE_STUDENT.equals(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid role");
        }
        return normalized;
    }

    private void validateStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid status");
        }
    }

    private void ensureRoleProfile(Long userId, String role) {
        if (userId == null || !StringUtils.hasText(role)) {
            return;
        }
        String normalized = role.trim().toUpperCase();
        if (Constants.ROLE_STUDENT.equals(normalized)) {
            ensureStudentProfile(userId);
            return;
        }
        if (Constants.ROLE_TEACHER.equals(normalized)) {
            ensureTeacherProfile(userId);
            return;
        }
        if (Constants.ROLE_ADMIN.equals(normalized)) {
            ensureAdminProfile(userId);
        }
    }

    private void ensureStudentProfile(Long userId) {
        StudentProfile existing = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        studentProfileMapper.insert(profile);
    }

    private void ensureTeacherProfile(Long userId) {
        TeacherProfile existing = teacherProfileMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>()
                        .eq(TeacherProfile::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(userId);
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        teacherProfileMapper.insert(profile);
    }

    private void ensureAdminProfile(Long userId) {
        AdminProfile existing = adminProfileMapper.selectOne(
                new LambdaQueryWrapper<AdminProfile>()
                        .eq(AdminProfile::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        AdminProfile profile = new AdminProfile();
        profile.setUserId(userId);
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        adminProfileMapper.insert(profile);
    }

    private RoleProfileDTO loadRoleProfile(User user) {
        RoleProfileDTO dto = new RoleProfileDTO();
        dto.setUserId(user.getId());
        dto.setRole(user.getRole());
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
        switch (role) {
            case Constants.ROLE_STUDENT:
                StudentProfile student = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, user.getId()).last("LIMIT 1"));
                if (student != null) {
                    dto.setStudentNo(student.getStudentNo());
                    dto.setClassName(student.getClassName());
                    dto.setMajor(student.getMajor());
                    dto.setRealName(student.getRealName());
                    dto.setGender(student.getGender());
                    dto.setBio(student.getBio());
                }
                break;
            case Constants.ROLE_TEACHER:
                TeacherProfile teacher = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                        .eq(TeacherProfile::getUserId, user.getId()).last("LIMIT 1"));
                if (teacher != null) {
                    dto.setTeacherNo(teacher.getTeacherNo());
                    dto.setDepartment(teacher.getDepartment());
                    dto.setTitle(teacher.getTitle());
                    dto.setRealName(teacher.getRealName());
                    dto.setGender(teacher.getGender());
                    dto.setBio(teacher.getBio());
                }
                break;
            case Constants.ROLE_ADMIN:
                AdminProfile admin = adminProfileMapper.selectOne(new LambdaQueryWrapper<AdminProfile>()
                        .eq(AdminProfile::getUserId, user.getId()).last("LIMIT 1"));
                if (admin != null) {
                    dto.setAdminCode(admin.getAdminCode());
                    dto.setDepartment(admin.getDepartment());
                    dto.setRealName(admin.getRealName());
                    dto.setBio(admin.getBio());
                }
                break;
            default:
                break;
        }
        return dto;
    }

    private void upsertStudentProfile(Long userId, RoleProfileDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId).last("LIMIT 1"));
        boolean isNew = profile == null;
        if (isNew) {
            profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setCreateTime(now);
        }
        profile.setStudentNo(trimToNull(dto.getStudentNo()));
        profile.setClassName(trimToNull(dto.getClassName()));
        profile.setMajor(trimToNull(dto.getMajor()));
        profile.setRealName(trimToNull(dto.getRealName()));
        profile.setGender(trimToNull(dto.getGender()));
        profile.setBio(trimToNull(dto.getBio()));
        profile.setUpdateTime(now);
        if (isNew) {
            studentProfileMapper.insert(profile);
        } else {
            studentProfileMapper.updateById(profile);
        }
    }

    private void upsertTeacherProfile(Long userId, RoleProfileDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        TeacherProfile profile = teacherProfileMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId).last("LIMIT 1"));
        boolean isNew = profile == null;
        if (isNew) {
            profile = new TeacherProfile();
            profile.setUserId(userId);
            profile.setCreateTime(now);
        }
        profile.setTeacherNo(trimToNull(dto.getTeacherNo()));
        profile.setDepartment(trimToNull(dto.getDepartment()));
        profile.setTitle(trimToNull(dto.getTitle()));
        profile.setRealName(trimToNull(dto.getRealName()));
        profile.setGender(trimToNull(dto.getGender()));
        profile.setBio(trimToNull(dto.getBio()));
        profile.setUpdateTime(now);
        if (isNew) {
            teacherProfileMapper.insert(profile);
        } else {
            teacherProfileMapper.updateById(profile);
        }
    }

    private void upsertAdminProfile(Long userId, RoleProfileDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        AdminProfile profile = adminProfileMapper.selectOne(
                new LambdaQueryWrapper<AdminProfile>().eq(AdminProfile::getUserId, userId).last("LIMIT 1"));
        boolean isNew = profile == null;
        if (isNew) {
            profile = new AdminProfile();
            profile.setUserId(userId);
            profile.setCreateTime(now);
        }
        profile.setAdminCode(trimToNull(dto.getAdminCode()));
        profile.setDepartment(trimToNull(dto.getDepartment()));
        profile.setRealName(trimToNull(dto.getRealName()));
        profile.setBio(trimToNull(dto.getBio()));
        profile.setUpdateTime(now);
        if (isNew) {
            adminProfileMapper.insert(profile);
        } else {
            adminProfileMapper.updateById(profile);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
