-- 阶段一：用户与权限管理模块数据库脚本
-- 数据库：code2026
-- 字符集：utf8mb4

CREATE DATABASE IF NOT EXISTS code2026
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE code2026;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    intro VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色：ADMIN/TEACHER/STUDENT',
    student_no VARCHAR(30) DEFAULT NULL COMMENT '学号',
    teacher_no VARCHAR(30) DEFAULT NULL COMMENT '工号',
    school_id INT DEFAULT NULL COMMENT '所属学院ID',
    major_id INT DEFAULT NULL COMMENT '所属专业ID',
    class_id INT DEFAULT NULL COMMENT '所属班级ID',
    grade VARCHAR(20) DEFAULT NULL COMMENT '年级，如 2023',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_role (role),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_school_id (school_id),
    INDEX idx_class_id (class_id),
    UNIQUE KEY uk_username_deleted ((CASE WHEN deleted = 0 THEN username END)),
    UNIQUE KEY uk_student_no ((CASE WHEN deleted = 0 THEN student_no END))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 初始化管理员账号 admin/123456（BCrypt加密后的密码）
INSERT INTO sys_user (username, password, nickname, role, status, created_at, updated_at, deleted)
VALUES (
    'admin',
    '$2b$10$PpiEjLVHhUGgUkD9WPVgPumvqv8U/G71ho.5PcaQ6vLZvPhrVjbNe',
    '系统管理员',
    'ADMIN',
    'ENABLED',
    NOW(),
    NOW(),
    0
) ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 阶段二：项目管理模块表

-- 项目信息表
CREATE TABLE IF NOT EXISTS project_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    description TEXT DEFAULT NULL COMMENT '项目简介',
    tags VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
    project_type VARCHAR(30) DEFAULT 'COURSE_TRAINING' COMMENT '类型：COURSE_TRAINING课程实训/COURSE_DESIGN课程设计/GRADUATION_DESIGN毕业设计',
    course_name VARCHAR(100) DEFAULT NULL COMMENT '实训课程名称',
    school_id INT DEFAULT NULL COMMENT '学院ID',
    major_id INT DEFAULT NULL COMMENT '专业ID',
    class_id INT DEFAULT NULL COMMENT '班级ID',
    course_id INT DEFAULT NULL COMMENT '所属教学班（课程）ID，可为空',
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    end_date DATE DEFAULT NULL COMMENT '结束日期',
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '状态：NOT_STARTED/IN_PROGRESS/DELAYED/COMPLETED',
    creator_id INT NOT NULL COMMENT '创建人ID（即指导教师）',
    owner_id INT NOT NULL COMMENT '项目归属人ID（=指导教师，仅教师间转移）',
    teacher_id INT DEFAULT NULL COMMENT '指导教师ID',
    archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否归档：0否 1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_status (status),
    INDEX idx_owner (owner_id),
    INDEX idx_creator (creator_id),
    INDEX idx_school_id (school_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目信息表';

-- 项目成员关联表
-- 唯一索引包含 deleted 字段，支持逻辑删除后重新添加成员
CREATE TABLE IF NOT EXISTS project_member (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id INT NOT NULL COMMENT '项目ID',
    user_id INT NOT NULL COMMENT '用户ID',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_project_user ((CASE WHEN deleted = 0 THEN project_id END), (CASE WHEN deleted = 0 THEN user_id END)),
    INDEX idx_project_id (project_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

-- 阶段三：智能任务协同核心模块表

-- 任务信息表
CREATE TABLE IF NOT EXISTS task_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id INT NOT NULL COMMENT '所属项目ID',
    parent_task_id INT DEFAULT NULL COMMENT '父任务ID',
    task_level TINYINT NOT NULL DEFAULT 1 COMMENT '任务层级：1/2/3',
    task_type VARCHAR(30) DEFAULT 'SKILL' COMMENT '任务类型：COGNITIVE认知/SKILL实操/COMPREHENSIVE综合/DELIVERABLE成果',
    difficulty VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '任务难度：EASY简单/MEDIUM中等/HARD困难',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT DEFAULT NULL COMMENT '任务描述',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级：HIGH/MEDIUM/LOW',
    status VARCHAR(20) NOT NULL DEFAULT 'TODO' COMMENT '状态：TODO/IN_PROGRESS/BLOCKED/REVIEW/DONE',
    progress TINYINT NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    planned_hours DOUBLE DEFAULT NULL COMMENT '计划学时',
    actual_hours DOUBLE DEFAULT NULL COMMENT '实际学时',
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    end_date DATE DEFAULT NULL COMMENT '结束日期',
    assignee_id INT DEFAULT NULL COMMENT '负责人ID',
    group_id INT DEFAULT NULL COMMENT '任务池归属小组ID（空=项目级任务池）',
    creator_id INT NOT NULL COMMENT '创建人ID',
    reviewer_id INT DEFAULT NULL COMMENT '审核人ID',
    review_comment VARCHAR(500) DEFAULT NULL COMMENT '审核/驳回意见',
    score DECIMAL(5,2) DEFAULT NULL COMMENT '评分（0-100）',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_parent_task_id (parent_task_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_group_id (group_id),
    INDEX idx_end_date (end_date),
    INDEX idx_task_type (task_type),
    INDEX idx_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务信息表';

-- 任务前置依赖表
-- 唯一索引包含 deleted 字段，支持逻辑删除后重新添加依赖关系
CREATE TABLE IF NOT EXISTS task_dependency (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id INT NOT NULL COMMENT '当前任务ID',
    depends_on_task_id INT NOT NULL COMMENT '依赖的任务ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_task_dependency ((CASE WHEN deleted = 0 THEN task_id END), (CASE WHEN deleted = 0 THEN depends_on_task_id END)),
    INDEX idx_task_id (task_id),
    INDEX idx_depends_on_task_id (depends_on_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务前置依赖表';

-- 任务评论表
CREATE TABLE IF NOT EXISTS task_comment (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id INT NOT NULL COMMENT '任务ID',
    user_id INT NOT NULL COMMENT '评论人ID',
    parent_comment_id INT DEFAULT NULL COMMENT '父评论ID，支持嵌套回复',
    content TEXT NOT NULL COMMENT '评论内容',
    is_resolved TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解决：0否 1是',
    edited_at DATETIME DEFAULT NULL COMMENT '最后编辑时间，NULL表示未编辑',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_task_id (task_id),
    INDEX idx_parent_comment_id (parent_comment_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务评论表';

-- 小组讨论消息表
CREATE TABLE IF NOT EXISTS group_message (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id INT NOT NULL COMMENT '小组ID',
    sender_id INT NOT NULL COMMENT '发送人ID',
    content VARCHAR(500) NOT NULL COMMENT '发言内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_group_created (group_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组讨论消息表';

-- 任务变更历史表
CREATE TABLE IF NOT EXISTS task_change_log (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id INT NOT NULL COMMENT '任务ID',
    field_name VARCHAR(50) NOT NULL COMMENT '变更字段名称',
    field_label VARCHAR(50) DEFAULT NULL COMMENT '字段中文标签',
    old_value TEXT DEFAULT NULL COMMENT '旧值',
    new_value TEXT DEFAULT NULL COMMENT '新值',
    changed_by INT NOT NULL COMMENT '操作人ID',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    change_type VARCHAR(20) NOT NULL COMMENT '变更类型：UPDATE/STATUS/ASSIGN/PROGRESS/REVIEW',
    comment TEXT DEFAULT NULL COMMENT '变更备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_task_id (task_id),
    INDEX idx_changed_by (changed_by),
    INDEX idx_changed_at (changed_at),
    INDEX idx_field_name (field_name),
    CONSTRAINT chk_change_type CHECK (change_type IN ('UPDATE','STATUS','ASSIGN','PROGRESS','REVIEW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务变更历史表';

-- 阶段四：DeepSeek 文本大模型智能模块

-- AI 调用日志表
CREATE TABLE IF NOT EXISTS ai_call_log (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '调用用户ID',
    project_id INT DEFAULT NULL COMMENT '关联项目ID',
    ai_type VARCHAR(20) DEFAULT NULL COMMENT 'AI类型：DEEPSEEK',
    scene VARCHAR(50) DEFAULT NULL COMMENT 'AI场景：TASK_SPLIT/RISK_ANALYZ/DOCUMENT_EXTRACT',
    input_tokens INT DEFAULT NULL COMMENT '输入token数',
    output_tokens INT DEFAULT NULL COMMENT '输出token数',
    total_tokens INT DEFAULT NULL COMMENT '总token数',
    request TEXT COMMENT '请求内容',
    response TEXT COMMENT '响应内容',
    status VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS/FAILED',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    latency_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_project_id (project_id),
    INDEX idx_ai_type (ai_type),
    INDEX idx_scene (scene),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI调用日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_config_key (config_key),
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 兼容升级：确保 config_value 是 TEXT 类型（旧版本可能是 BLOB）
ALTER TABLE sys_config MODIFY COLUMN config_value TEXT COMMENT '配置值';

-- 系统公告表
-- 管理员发布的系统公告，所有用户可见
CREATE TABLE IF NOT EXISTS sys_announcement (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT COMMENT '公告内容',
    type VARCHAR(20) NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE通知/IMPORTANT重要/MAINTENANCE维护',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT草稿/PUBLISHED已发布/OFFLINE已下线',
    is_top TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0否 1是',
    publisher_id INT COMMENT '发布人ID',
    publish_time DATETIME COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_status (status),
    INDEX idx_is_top (is_top),
    INDEX idx_publish_time (publish_time),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    permission_key VARCHAR(100) NOT NULL COMMENT '权限标识',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    module VARCHAR(50) DEFAULT NULL COMMENT '所属模块',
    description VARCHAR(500) DEFAULT NULL COMMENT '权限描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_permission_key (permission_key),
    INDEX idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN/TEACHER/STUDENT',
    permission_id INT NOT NULL COMMENT '权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_role_permission ((CASE WHEN deleted = 0 THEN role END), (CASE WHEN deleted = 0 THEN permission_id END)),
    INDEX idx_role (role),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 初始化系统配置（AI 限流与 DeepSeek 默认配置）
INSERT INTO sys_config (config_key, config_value, description) VALUES
('deepseek.api_key', '', 'DeepSeek API Key'),
('deepseek.base_url', 'https://api.deepseek.com', 'DeepSeek Base URL'),
('deepseek.model', 'deepseek-v4-flash', 'DeepSeek 文本模型'),
('deepseek.timeout', '60', 'DeepSeek 超时时间(秒)'),
('ai.rate.task_split', '10', '任务拆解限流(次/分钟)'),
('ai.rate.risk_analyze', '10', '风险分析限流(次/分钟)'),
('overdue.warning.days', '3', '逾期预警阈值（天）'),
('sse.enabled', '1', 'SSE流式开关（1开启/0关闭）')
ON DUPLICATE KEY UPDATE config_value = config_value;

-- 初始化权限数据
INSERT INTO sys_permission (permission_key, permission_name, module, description) VALUES
-- 用户管理模块
('user.create', '创建用户', '用户管理', '新增系统用户'),
('user.edit', '编辑用户', '用户管理', '修改用户信息'),
('user.delete', '删除用户', '用户管理', '删除系统用户'),
('user.view', '查看用户', '用户管理', '查看用户列表'),
-- 项目管理模块
('project.create', '创建项目', '项目管理', '创建新项目'),
('project.edit', '编辑项目', '项目管理', '编辑项目信息'),
('project.delete', '删除项目', '项目管理', '删除项目'),
('project.view', '查看项目', '项目管理', '查看项目列表'),
('project.transfer', '移交负责人', '项目管理', '移交项目权属'),
-- 任务管理模块
('task.create', '创建任务', '任务管理', '创建新任务'),
('task.edit', '编辑任务', '任务管理', '编辑任务信息'),
('task.delete', '删除任务', '任务管理', '删除任务'),
('task.view', '查看任务', '任务管理', '查看任务列表'),
('task.review', '审核任务', '任务管理', '审核任务成果'),
('task.comment', '任务评论', '任务管理', '添加、编辑、删除任务评论'),
-- AI功能模块
('ai.task_split', 'AI任务拆解', 'AI功能', '使用AI智能任务拆解'),
('ai.risk_analyze', 'AI风险分析', 'AI功能', '使用AI风险分析'),
-- 系统配置模块
('config.system', '系统配置', '系统配置', '配置系统参数'),
('log.ai', 'AI日志', '日志管理', '查看AI调用日志'),
('log.operation', '操作日志', '日志管理', '查看操作日志'),
('log.login', '登录日志', '日志管理', '查看登录日志'),
('log.statistics', 'AI统计', '统计分析', '查看AI调用统计'),
('data.export', '数据导出', '系统管理', '导出系统数据'),
('data.export.users', '导出用户数据', '系统管理', '导出用户列表数据'),
('data.export.projects', '导出项目数据', '系统管理', '导出项目列表数据'),
('data.export.tasks', '导出任务数据', '系统管理', '导出任务列表数据'),
('system.announcement', '系统公告', '系统管理', '管理系统公告'),
('dashboard.view', '仪表盘查看', '仪表盘', '查看管理员仪表盘')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), module = VALUES(module), 
    description = VALUES(description), updated_at = NOW();

-- 初始化角色权限关联
INSERT IGNORE INTO sys_role_permission (role, permission_id)
SELECT 'ADMIN', id FROM sys_permission WHERE deleted = 0;

INSERT IGNORE INTO sys_role_permission (role, permission_id)
SELECT 'TEACHER', id FROM sys_permission 
WHERE deleted = 0 AND permission_key NOT IN (
    'user.view', 'user.create', 'user.edit', 'user.delete', 'config.system',
    'log.operation', 'log.login', 'log.ai', 'log.statistics',
    'data.export', 'data.export.users', 'dashboard.view', 'system.announcement'
);

INSERT IGNORE INTO sys_role_permission (role, permission_id)
SELECT 'STUDENT', id FROM sys_permission 
WHERE deleted = 0 AND permission_key IN (
    'project.view', 'task.view', 'task.create', 'task.edit', 'task.comment',
    'ai.task_split', 'ai.risk_analyze'
);

-- 阶段五：文件管理模块

-- 文件资源表
CREATE TABLE IF NOT EXISTS file_resource (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id INT DEFAULT NULL COMMENT '关联项目ID',
    filename VARCHAR(255) NOT NULL COMMENT '存储文件名',
    original_name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    mime_type VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    business_type VARCHAR(50) DEFAULT 'OTHER' COMMENT '业务类型：REQUIREMENT/UI/WHITEBOARD/ERROR_SCREENSHOT/GUIDE_MATERIAL/GRADING/REFERENCE/OTHER',
    ref_count INT NOT NULL DEFAULT 0 COMMENT '引用计数，大于0时禁止物理删除',
    is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公共文件：0个人临时文件 1项目公共文件',
    uploader_id INT DEFAULT NULL COMMENT '上传用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_business_type (business_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件资源表';

-- 任务附件关联表
-- 支持一个文件附加到多个任务，也支持一个任务有多个附件
CREATE TABLE IF NOT EXISTS task_attachment (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id INT NOT NULL COMMENT '任务ID',
    file_id INT NOT NULL COMMENT '文件资源ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_task_file ((CASE WHEN deleted = 0 THEN task_id END), (CASE WHEN deleted = 0 THEN file_id END)),
    INDEX idx_task_id (task_id),
    INDEX idx_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务附件关联表';

-- 消息通知表
CREATE TABLE IF NOT EXISTS notification (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '接收用户ID',
    type VARCHAR(50) NOT NULL COMMENT '通知类型：TASK_STATUS/ASSIGN/OVERDUE/MEMBER/AI_RISK/REJECT',
    title VARCHAR(255) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    target_url VARCHAR(500) DEFAULT NULL COMMENT '点击跳转链接',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id INT DEFAULT NULL COMMENT '操作用户ID',
    username VARCHAR(100) DEFAULT NULL COMMENT '操作用户名',
    project_id INT DEFAULT NULL COMMENT '关联项目ID',
    operation_type VARCHAR(100) DEFAULT NULL COMMENT '操作类型',
    method_name VARCHAR(500) DEFAULT NULL COMMENT '方法名',
    params TEXT DEFAULT NULL COMMENT '请求参数',
    ip VARCHAR(100) DEFAULT NULL COMMENT 'IP地址',
    duration_ms INT DEFAULT NULL COMMENT '耗时毫秒',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '结果状态：0失败 1成功',
    error_msg TEXT DEFAULT NULL COMMENT '异常信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_project_id (project_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id INT DEFAULT NULL COMMENT '用户ID（登录成功时有值）',
    username VARCHAR(100) NOT NULL COMMENT '登录账号',
    login_type VARCHAR(20) NOT NULL DEFAULT 'PASSWORD' COMMENT '登录方式：PASSWORD/TOKEN',
    ip VARCHAR(100) DEFAULT NULL COMMENT '登录IP',
    location VARCHAR(128) DEFAULT NULL COMMENT 'IP归属地',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '浏览器/User-Agent',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '登录状态：0失败 1成功',
    fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- 数据迁移与结构修正（幂等执行）
DELIMITER //

-- 修正 file_resource 表缺失字段（兼容旧版本数据库）
CREATE PROCEDURE IF NOT EXISTS migrate_file_resource()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'file_resource'
                     AND column_name = 'ref_count') THEN
        ALTER TABLE file_resource ADD COLUMN ref_count INT NOT NULL DEFAULT 0 COMMENT '引用计数，大于0时禁止物理删除';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'file_resource'
                     AND column_name = 'is_public') THEN
        ALTER TABLE file_resource ADD COLUMN is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公共文件：0个人临时文件 1项目公共文件' AFTER ref_count;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'file_resource'
                     AND column_name = 'uploader_id') THEN
        ALTER TABLE file_resource ADD COLUMN uploader_id INT DEFAULT NULL COMMENT '上传用户ID' AFTER is_public;
    END IF;
END //

-- 清理 file_resource 表已废弃的 AI 字段（AI 文件解析链路已停用）
CREATE PROCEDURE IF NOT EXISTS migrate_drop_file_resource_ai()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'file_resource'
                 AND column_name = 'ai_tags') THEN
        ALTER TABLE file_resource DROP COLUMN ai_tags;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'file_resource'
                 AND column_name = 'extracted_text') THEN
        ALTER TABLE file_resource DROP COLUMN extracted_text;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'file_resource'
                 AND column_name = 'is_ai_processed') THEN
        ALTER TABLE file_resource DROP COLUMN is_ai_processed;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'file_resource'
                 AND column_name = 'processed_at') THEN
        ALTER TABLE file_resource DROP COLUMN processed_at;
    END IF;
END //

-- 修正 project_member 表唯一索引（兼容逻辑删除）
-- 旧索引 (project_id, user_id, deleted) 导致同一成员只能被删除一次
-- 改为函数索引，仅约束 deleted = 0 的未删除记录
CREATE PROCEDURE IF NOT EXISTS migrate_project_member_index()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'project_member'
                 AND index_name = 'uk_project_user'
                 AND column_name = 'deleted') THEN
        ALTER TABLE project_member DROP INDEX uk_project_user;

        DELETE pm1 FROM project_member pm1
        JOIN project_member pm2
          ON pm1.project_id = pm2.project_id
         AND pm1.user_id = pm2.user_id
         AND pm1.deleted = 0 AND pm2.deleted = 0
         AND pm1.id > pm2.id;

        ALTER TABLE project_member ADD UNIQUE INDEX uk_project_user
            ((CASE WHEN deleted = 0 THEN project_id END), (CASE WHEN deleted = 0 THEN user_id END));
    END IF;
END //

-- 修正 task_dependency 表唯一索引（兼容逻辑删除）
CREATE PROCEDURE IF NOT EXISTS migrate_task_dependency_index()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'task_dependency'
                 AND index_name = 'uk_task_dependency'
                 AND column_name = 'deleted') THEN
        ALTER TABLE task_dependency DROP INDEX uk_task_dependency;

        DELETE td1 FROM task_dependency td1
        JOIN task_dependency td2
          ON td1.task_id = td2.task_id
         AND td1.depends_on_task_id = td2.depends_on_task_id
         AND td1.deleted = 0 AND td2.deleted = 0
         AND td1.id > td2.id;

        ALTER TABLE task_dependency ADD UNIQUE INDEX uk_task_dependency
            ((CASE WHEN deleted = 0 THEN task_id END), (CASE WHEN deleted = 0 THEN depends_on_task_id END));
    END IF;
END //

-- 修正 ai_call_log 表添加 ai_type/scene 字段并移除 prompt_template_id（兼容旧版本数据库）
CREATE PROCEDURE IF NOT EXISTS migrate_ai_call_log_type()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'ai_call_log'
                     AND column_name = 'ai_type') THEN
        ALTER TABLE ai_call_log ADD COLUMN ai_type VARCHAR(20) DEFAULT NULL COMMENT 'AI类型：DEEPSEEK' AFTER project_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'ai_call_log'
                     AND index_name = 'idx_ai_type') THEN
        ALTER TABLE ai_call_log ADD INDEX idx_ai_type (ai_type);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'ai_call_log'
                 AND column_name = 'prompt_template_id') THEN
        ALTER TABLE ai_call_log DROP COLUMN prompt_template_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'ai_call_log'
                     AND column_name = 'scene') THEN
        ALTER TABLE ai_call_log ADD COLUMN scene VARCHAR(50) DEFAULT NULL COMMENT 'AI场景：TASK_SPLIT/RISK_ANALYZ/DOCUMENT_EXTRACT' AFTER ai_type;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'ai_call_log'
                     AND index_name = 'idx_scene') THEN
        ALTER TABLE ai_call_log ADD INDEX idx_scene (scene);
    END IF;
END //

-- 清理已废弃的 prompt_template 表（模板已迁移为代码常量）
CREATE PROCEDURE IF NOT EXISTS migrate_drop_prompt_template()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE()
                 AND table_name = 'prompt_template') THEN
        DROP TABLE prompt_template;
    END IF;
END //

-- 修正 task_comment 表添加 edited_at 字段（兼容旧版本数据库）
CREATE PROCEDURE IF NOT EXISTS migrate_task_comment_edit()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'task_comment'
                     AND column_name = 'edited_at') THEN
        ALTER TABLE task_comment ADD COLUMN edited_at DATETIME DEFAULT NULL COMMENT '最后编辑时间，NULL表示未编辑' AFTER is_resolved;
    END IF;
END //

-- 创建任务变更历史表（兼容旧版本数据库）
CREATE PROCEDURE IF NOT EXISTS migrate_task_change_log()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE()
                     AND table_name = 'task_change_log') THEN
        CREATE TABLE task_change_log (
            id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
            task_id INT NOT NULL COMMENT '任务ID',
            field_name VARCHAR(50) NOT NULL COMMENT '变更字段名称',
            field_label VARCHAR(50) DEFAULT NULL COMMENT '字段中文标签',
            old_value TEXT DEFAULT NULL COMMENT '旧值',
            new_value TEXT DEFAULT NULL COMMENT '新值',
            changed_by INT NOT NULL COMMENT '操作人ID',
            changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
            change_type VARCHAR(20) NOT NULL COMMENT '变更类型：UPDATE/STATUS/ASSIGN/PROGRESS/REVIEW',
            comment TEXT DEFAULT NULL COMMENT '变更备注',
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
            INDEX idx_task_id (task_id),
            INDEX idx_changed_by (changed_by),
            INDEX idx_changed_at (changed_at),
            INDEX idx_field_name (field_name),
            CONSTRAINT chk_change_type CHECK (change_type IN ('UPDATE','STATUS','ASSIGN','PROGRESS','REVIEW'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务变更历史表';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'task_change_log'
                     AND column_name = 'created_at') THEN
        ALTER TABLE task_change_log ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER comment;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'task_change_log'
                     AND column_name = 'updated_at') THEN
        ALTER TABLE task_change_log ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'task_change_log'
                     AND column_name = 'deleted') THEN
        ALTER TABLE task_change_log ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除' AFTER updated_at;
    END IF;
END //

-- 修正 operation_log 表添加 project_id 字段，并迁移 project_log 历史数据
CREATE PROCEDURE IF NOT EXISTS migrate_operation_log_project_id()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'operation_log'
                     AND column_name = 'project_id') THEN
        ALTER TABLE operation_log ADD COLUMN project_id INT DEFAULT NULL COMMENT '关联项目ID' AFTER username;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'operation_log'
                     AND index_name = 'idx_project_id') THEN
        ALTER TABLE operation_log ADD INDEX idx_project_id (project_id);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE()
                 AND table_name = 'project_log') THEN
        INSERT INTO operation_log (user_id, username, project_id, operation_type, method_name, params, status, created_at)
        SELECT operator_id, NULL, project_id, action, 'ProjectLog', detail, 1, operated_at
        FROM project_log WHERE deleted = 0;
        DROP TABLE project_log;
    END IF;
END //

-- 修正 task_info 表删除 review_status 字段（状态机简化）
CREATE PROCEDURE IF NOT EXISTS migrate_task_info_review_status()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'task_info'
                 AND column_name = 'review_status') THEN
        ALTER TABLE task_info DROP COLUMN review_status;
    END IF;
END //

-- 清理已删除的任务移交相关字段（兼容旧版本数据库）
CREATE PROCEDURE IF NOT EXISTS migrate_task_info_drop_handover()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'task_info'
                 AND column_name = 'handover_reason') THEN
        SET @sql = 'ALTER TABLE task_info DROP COLUMN handover_reason';
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'task_info'
                 AND column_name = 'handover_status') THEN
        SET @sql = 'ALTER TABLE task_info DROP COLUMN handover_status';
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER //

-- 修正 task_info 表工时字段为计划/实际学时（幂等执行）
CREATE PROCEDURE IF NOT EXISTS migrate_task_info_study_hours()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'task_info'
                 AND column_name = 'estimated_hours') THEN
        SET @sql = 'ALTER TABLE task_info CHANGE COLUMN estimated_hours planned_hours DOUBLE DEFAULT NULL COMMENT ''计划学时''';
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'task_info'
                 AND column_name = 'actual_hours') THEN
        SET @sql = 'ALTER TABLE task_info MODIFY COLUMN actual_hours DOUBLE DEFAULT NULL COMMENT ''实际学时''';
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL migrate_file_resource();
CALL migrate_task_info_study_hours();
CALL migrate_drop_file_resource_ai();
CALL migrate_project_member_index();
CALL migrate_task_dependency_index();
CALL migrate_ai_call_log_type();
CALL migrate_drop_prompt_template();
CALL migrate_task_comment_edit();
CALL migrate_task_change_log();
CALL migrate_operation_log_project_id();
CALL migrate_task_info_review_status();
CALL migrate_task_info_drop_handover();

DROP PROCEDURE IF EXISTS migrate_file_resource;
DROP PROCEDURE IF EXISTS migrate_drop_file_resource_ai;
DROP PROCEDURE IF EXISTS migrate_project_member_index;
DROP PROCEDURE IF EXISTS migrate_task_dependency_index;
DROP PROCEDURE IF EXISTS migrate_ai_call_log_type;
DROP PROCEDURE IF EXISTS migrate_drop_prompt_template;
DROP PROCEDURE IF EXISTS migrate_task_comment_edit;
DROP PROCEDURE IF EXISTS migrate_task_change_log;
DROP PROCEDURE IF EXISTS migrate_operation_log_project_id;
DROP PROCEDURE IF EXISTS migrate_task_info_review_status;
DROP PROCEDURE IF EXISTS migrate_task_info_drop_handover;
DROP PROCEDURE IF EXISTS migrate_task_info_study_hours;

-- 阶段七：添加物理外键约束，保障数据引用完整性（幂等执行）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS migrate_foreign_keys()
BEGIN
    DECLARE fk_exists INT DEFAULT 0;

    -- project_info 引用 sys_user
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'project_info' AND constraint_name = 'fk_project_creator';
    IF fk_exists = 0 THEN
        ALTER TABLE project_info ADD CONSTRAINT fk_project_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE RESTRICT;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'project_info' AND constraint_name = 'fk_project_owner';
    IF fk_exists = 0 THEN
        ALTER TABLE project_info ADD CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES sys_user(id) ON DELETE RESTRICT;
    END IF;

    -- project_member 引用 project_info / sys_user
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'project_member' AND constraint_name = 'fk_member_project';
    IF fk_exists = 0 THEN
        ALTER TABLE project_member ADD CONSTRAINT fk_member_project FOREIGN KEY (project_id) REFERENCES project_info(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'project_member' AND constraint_name = 'fk_member_user';
    IF fk_exists = 0 THEN
        ALTER TABLE project_member ADD CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;
    END IF;

    -- task_info 引用 project_info / task_info(自关联) / sys_user
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_info' AND constraint_name = 'fk_task_project';
    IF fk_exists = 0 THEN
        ALTER TABLE task_info ADD CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES project_info(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_info' AND constraint_name = 'fk_task_parent';
    IF fk_exists = 0 THEN
        ALTER TABLE task_info ADD CONSTRAINT fk_task_parent FOREIGN KEY (parent_task_id) REFERENCES task_info(id) ON DELETE SET NULL;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_info' AND constraint_name = 'fk_task_assignee';
    IF fk_exists = 0 THEN
        ALTER TABLE task_info ADD CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id) ON DELETE SET NULL;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_info' AND constraint_name = 'fk_task_creator';
    IF fk_exists = 0 THEN
        ALTER TABLE task_info ADD CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE RESTRICT;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_info' AND constraint_name = 'fk_task_reviewer';
    IF fk_exists = 0 THEN
        ALTER TABLE task_info ADD CONSTRAINT fk_task_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id) ON DELETE SET NULL;
    END IF;

    -- task_dependency 引用 task_info
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_dependency' AND constraint_name = 'fk_dep_task';
    IF fk_exists = 0 THEN
        ALTER TABLE task_dependency ADD CONSTRAINT fk_dep_task FOREIGN KEY (task_id) REFERENCES task_info(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_dependency' AND constraint_name = 'fk_dep_depends_on';
    IF fk_exists = 0 THEN
        ALTER TABLE task_dependency ADD CONSTRAINT fk_dep_depends_on FOREIGN KEY (depends_on_task_id) REFERENCES task_info(id) ON DELETE CASCADE;
    END IF;

    -- task_comment 引用 task_info / sys_user / task_comment(自关联)
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_comment' AND constraint_name = 'fk_comment_task';
    IF fk_exists = 0 THEN
        ALTER TABLE task_comment ADD CONSTRAINT fk_comment_task FOREIGN KEY (task_id) REFERENCES task_info(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_comment' AND constraint_name = 'fk_comment_user';
    IF fk_exists = 0 THEN
        ALTER TABLE task_comment ADD CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_comment' AND constraint_name = 'fk_comment_parent';
    IF fk_exists = 0 THEN
        ALTER TABLE task_comment ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES task_comment(id) ON DELETE CASCADE;
    END IF;

    -- task_change_log 引用 task_info / sys_user
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_change_log' AND constraint_name = 'fk_change_task';
    IF fk_exists = 0 THEN
        ALTER TABLE task_change_log ADD CONSTRAINT fk_change_task FOREIGN KEY (task_id) REFERENCES task_info(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_change_log' AND constraint_name = 'fk_change_user';
    IF fk_exists = 0 THEN
        ALTER TABLE task_change_log ADD CONSTRAINT fk_change_user FOREIGN KEY (changed_by) REFERENCES sys_user(id) ON DELETE RESTRICT;
    END IF;

    -- file_resource 引用 project_info / sys_user
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'file_resource' AND constraint_name = 'fk_file_project';
    IF fk_exists = 0 THEN
        ALTER TABLE file_resource ADD CONSTRAINT fk_file_project FOREIGN KEY (project_id) REFERENCES project_info(id) ON DELETE SET NULL;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'file_resource' AND constraint_name = 'fk_file_uploader';
    IF fk_exists = 0 THEN
        ALTER TABLE file_resource ADD CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user(id) ON DELETE SET NULL;
    END IF;

    -- notification 引用 sys_user
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'notification' AND constraint_name = 'fk_notification_user';
    IF fk_exists = 0 THEN
        ALTER TABLE notification ADD CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;
    END IF;

    -- ai_call_log 引用 sys_user / project_info
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'ai_call_log' AND constraint_name = 'fk_ai_user';
    IF fk_exists = 0 THEN
        ALTER TABLE ai_call_log ADD CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'ai_call_log' AND constraint_name = 'fk_ai_project';
    IF fk_exists = 0 THEN
        ALTER TABLE ai_call_log ADD CONSTRAINT fk_ai_project FOREIGN KEY (project_id) REFERENCES project_info(id) ON DELETE SET NULL;
    END IF;

    -- operation_log 引用 sys_user
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'operation_log' AND constraint_name = 'fk_op_user';
    IF fk_exists = 0 THEN
        ALTER TABLE operation_log ADD CONSTRAINT fk_op_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL;
    END IF;

    -- task_attachment 引用 task_info / file_resource
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_attachment' AND constraint_name = 'fk_attachment_task';
    IF fk_exists = 0 THEN
        ALTER TABLE task_attachment ADD CONSTRAINT fk_attachment_task FOREIGN KEY (task_id) REFERENCES task_info(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'task_attachment' AND constraint_name = 'fk_attachment_file';
    IF fk_exists = 0 THEN
        ALTER TABLE task_attachment ADD CONSTRAINT fk_attachment_file FOREIGN KEY (file_id) REFERENCES file_resource(id) ON DELETE CASCADE;
    END IF;

    -- sys_role_permission 引用 sys_permission
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'sys_role_permission' AND constraint_name = 'fk_role_perm_permission';
    IF fk_exists = 0 THEN
        ALTER TABLE sys_role_permission ADD CONSTRAINT fk_role_perm_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE;
    END IF;

END //
DELIMITER ;

CALL migrate_foreign_keys();
DROP PROCEDURE IF EXISTS migrate_foreign_keys;

-- =====================================================================
-- 阶段八：高校实训协同改造（幂等执行）
-- 聚焦期末课程实训任务分配，见《高校实训协同系统改造设计文档 v5.0》
-- 新增：学院/专业/班级/分组/分组成员 5 张表 + 现有表扩展实训字段
-- =====================================================================

-- 8.1 组织架构与实训分组（新增 5 张表）
CREATE TABLE IF NOT EXISTS school_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '学院名称',
    code VARCHAR(30) DEFAULT NULL COMMENT '学院编码',
    dean VARCHAR(50) DEFAULT NULL COMMENT '院长',
    contact_phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    description VARCHAR(500) DEFAULT NULL COMMENT '学院简介',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    -- 仅约束未删除记录，避免逻辑删除后同名学院只能删除一次（MySQL 8.0.13+ 函数索引）
    UNIQUE KEY uk_school_name ((CASE WHEN deleted = 0 THEN name END)),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院表';

CREATE TABLE IF NOT EXISTS major_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    school_id INT NOT NULL COMMENT '所属学院ID',
    name VARCHAR(100) NOT NULL COMMENT '专业名称',
    code VARCHAR(30) DEFAULT NULL COMMENT '专业编码',
    degree_type VARCHAR(20) DEFAULT NULL COMMENT '培养层次：本科/专科',
    description VARCHAR(500) DEFAULT NULL COMMENT '专业简介',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_major_name ((CASE WHEN deleted = 0 THEN school_id END), (CASE WHEN deleted = 0 THEN name END)),
    INDEX idx_school_id (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';

CREATE TABLE IF NOT EXISTS class_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    major_id INT NOT NULL COMMENT '所属专业ID',
    name VARCHAR(100) NOT NULL COMMENT '班级名称',
    grade VARCHAR(20) NOT NULL COMMENT '年级，如 2023',
    class_size INT DEFAULT 0 COMMENT '班级人数',
    head_teacher_id INT DEFAULT NULL COMMENT '班主任ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_class_name ((CASE WHEN deleted = 0 THEN major_id END), (CASE WHEN deleted = 0 THEN name END)),
    INDEX idx_major_id (major_id),
    INDEX idx_grade (grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- 教学班（课程）表：不支持合班，一个教学班对应一个班级
CREATE TABLE IF NOT EXISTS teaching_class (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '课程/教学班名称',
    term VARCHAR(20) DEFAULT NULL COMMENT '学期，如 2025-2026-1',
    teacher_id INT DEFAULT NULL COMMENT '授课教师ID',
    class_id INT NOT NULL COMMENT '班级ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_teaching_class_name ((CASE WHEN deleted = 0 THEN class_id END), (CASE WHEN deleted = 0 THEN name END)),
    INDEX idx_class_id (class_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_term (term)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学班（课程）表';

CREATE TABLE IF NOT EXISTS student_group (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id INT NOT NULL COMMENT '所属实训项目ID',
    name VARCHAR(100) NOT NULL COMMENT '小组名称',
    group_no VARCHAR(20) DEFAULT NULL COMMENT '组号',
    leader_id INT DEFAULT NULL COMMENT '组长用户ID',
    description VARCHAR(255) DEFAULT NULL COMMENT '小组说明',
    max_members INT NOT NULL DEFAULT 0 COMMENT '人数上限，0表示不限',
    join_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许学生自选加入：1允许 0关闭',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_project_group ((CASE WHEN deleted = 0 THEN project_id END), (CASE WHEN deleted = 0 THEN name END)),
    INDEX idx_project_id (project_id),
    INDEX idx_leader_id (leader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实训分组表';

CREATE TABLE IF NOT EXISTS group_member (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id INT NOT NULL COMMENT '分组ID',
    student_id INT NOT NULL COMMENT '学生用户ID',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_group_student ((CASE WHEN deleted = 0 THEN group_id END), (CASE WHEN deleted = 0 THEN student_id END)),
    INDEX idx_group_id (group_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分组成员表';

-- 8.2 现有表扩展实训字段（幂等）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS migrate_training_columns()
BEGIN
    -- sys_user：实训身份
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'student_no') THEN
        ALTER TABLE sys_user ADD COLUMN student_no VARCHAR(30) DEFAULT NULL COMMENT '学号' AFTER role;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'teacher_no') THEN
        ALTER TABLE sys_user ADD COLUMN teacher_no VARCHAR(30) DEFAULT NULL COMMENT '工号' AFTER student_no;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'school_id') THEN
        ALTER TABLE sys_user ADD COLUMN school_id INT DEFAULT NULL COMMENT '所属学院ID' AFTER teacher_no;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'major_id') THEN
        ALTER TABLE sys_user ADD COLUMN major_id INT DEFAULT NULL COMMENT '所属专业ID' AFTER school_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'class_id') THEN
        ALTER TABLE sys_user ADD COLUMN class_id INT DEFAULT NULL COMMENT '所属班级ID' AFTER major_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'grade') THEN
        ALTER TABLE sys_user ADD COLUMN grade VARCHAR(20) DEFAULT NULL COMMENT '年级，如 2023' AFTER class_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_school_id') THEN
        ALTER TABLE sys_user ADD INDEX idx_school_id (school_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_class_id') THEN
        ALTER TABLE sys_user ADD INDEX idx_class_id (class_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'uk_student_no') THEN
        ALTER TABLE sys_user ADD UNIQUE KEY uk_student_no ((CASE WHEN deleted = 0 THEN student_no END));
    END IF;

    -- project_info：课程实训项目
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_info' AND column_name = 'project_type') THEN
        ALTER TABLE project_info ADD COLUMN project_type VARCHAR(30) DEFAULT 'COURSE_TRAINING' COMMENT '类型：COURSE_TRAINING课程实训/COURSE_DESIGN课程设计/GRADUATION_DESIGN毕业设计' AFTER tags;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_info' AND column_name = 'course_name') THEN
        ALTER TABLE project_info ADD COLUMN course_name VARCHAR(100) DEFAULT NULL COMMENT '实训课程名称' AFTER project_type;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_info' AND column_name = 'school_id') THEN
        ALTER TABLE project_info ADD COLUMN school_id INT DEFAULT NULL COMMENT '学院ID' AFTER course_name;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_info' AND column_name = 'major_id') THEN
        ALTER TABLE project_info ADD COLUMN major_id INT DEFAULT NULL COMMENT '专业ID' AFTER school_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_info' AND column_name = 'class_id') THEN
        ALTER TABLE project_info ADD COLUMN class_id INT DEFAULT NULL COMMENT '班级ID' AFTER major_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_info' AND column_name = 'course_id') THEN
        ALTER TABLE project_info ADD COLUMN course_id INT DEFAULT NULL COMMENT '所属教学班（课程）ID，可为空' AFTER class_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_info' AND column_name = 'archived') THEN
        ALTER TABLE project_info ADD COLUMN archived TINYINT NOT NULL DEFAULT 0 COMMENT '是否归档：0否 1是' AFTER teacher_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_info' AND column_name = 'teacher_id') THEN
        ALTER TABLE project_info ADD COLUMN teacher_id INT DEFAULT NULL COMMENT '指导教师ID' AFTER owner_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'project_info' AND index_name = 'idx_school_id') THEN
        ALTER TABLE project_info ADD INDEX idx_school_id (school_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'project_info' AND index_name = 'idx_teacher_id') THEN
        ALTER TABLE project_info ADD INDEX idx_teacher_id (teacher_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'project_info' AND index_name = 'idx_course_id') THEN
        ALTER TABLE project_info ADD INDEX idx_course_id (course_id);
    END IF;

    -- task_info：任务类型 / 难度 / 评分
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'task_info' AND column_name = 'task_type') THEN
        ALTER TABLE task_info ADD COLUMN task_type VARCHAR(30) DEFAULT 'SKILL' COMMENT '任务类型：COGNITIVE认知/SKILL实操/COMPREHENSIVE综合/DELIVERABLE成果' AFTER task_level;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'task_info' AND column_name = 'difficulty') THEN
        ALTER TABLE task_info ADD COLUMN difficulty VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '任务难度：EASY简单/MEDIUM中等/HARD困难' AFTER task_type;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'task_info' AND column_name = 'score') THEN
        ALTER TABLE task_info ADD COLUMN score DECIMAL(5,2) DEFAULT NULL COMMENT '评分（0-100）' AFTER review_comment;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'task_info' AND index_name = 'idx_task_type') THEN
        ALTER TABLE task_info ADD INDEX idx_task_type (task_type);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'task_info' AND index_name = 'idx_difficulty') THEN
        ALTER TABLE task_info ADD INDEX idx_difficulty (difficulty);
    END IF;

    -- 数据迁移：平台角色合并（合并原 role 与身份字段为单一字段，取值 ADMIN/TEACHER/STUDENT）
    UPDATE sys_user SET role = 'TEACHER' WHERE role = 'MANAGER';
    UPDATE sys_user SET role = 'STUDENT' WHERE role = 'MEMBER';
    -- 清理权限表中旧角色行（权限已由上方种子按 TEACHER/STUDENT 重新授予）
    DELETE FROM sys_role_permission WHERE role IN ('MANAGER', 'MEMBER');

    -- 数据回填：指导教师历史空值默认为创建人（仅指导教师可创建项目）
    UPDATE project_info SET teacher_id = creator_id WHERE teacher_id IS NULL AND creator_id IS NOT NULL;

    -- 数据迁移：成员表只放学生，硬删历史教师成员行
    -- （project_member 含软删列，软删行会被"恢复成员"重新拉回，故必须硬删）
    DELETE FROM project_member
    WHERE user_id IN (SELECT id FROM sys_user WHERE role = 'TEACHER');
END //
DELIMITER ;

CALL migrate_training_columns();
DROP PROCEDURE IF EXISTS migrate_training_columns;

-- 8.3 新增实训权限项
INSERT INTO sys_permission (permission_key, permission_name, module, description) VALUES
('school.manage', '学院管理', '组织架构', '管理学院信息'),
('major.manage', '专业管理', '组织架构', '管理专业信息'),
('class.manage', '班级管理', '组织架构', '管理班级信息'),
('course.manage', '课程管理', '组织架构', '管理教学班（课程）信息'),
('group.manage', '分组管理', '实训管理', '管理实训分组与组长'),
('task.score_view', '查看任务评分', '任务管理', '查看任务审核得分'),
('score.export', '导出实训成绩', '成绩管理', '导出实训任务成绩报表'),
('dashboard.teacher', '教师看板', '仪表盘', '查看教师数据看板'),
('task.review', '审核任务', '任务管理', '审核任务并评分')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), module = VALUES(module),
    description = VALUES(description), updated_at = NOW();

-- 8.4 新增权限的角色授权（ADMIN 全量，TEACHER/指导教师，STUDENT/学生）
INSERT IGNORE INTO sys_role_permission (role, permission_id)
SELECT 'ADMIN', id FROM sys_permission WHERE deleted = 0 AND permission_key IN (
    'school.manage', 'major.manage', 'class.manage', 'course.manage', 'group.manage',
    'task.score_view', 'score.export', 'dashboard.teacher'
);

INSERT IGNORE INTO sys_role_permission (role, permission_id)
SELECT 'TEACHER', id FROM sys_permission WHERE deleted = 0 AND permission_key IN (
    'group.manage', 'task.score_view', 'score.export', 'dashboard.teacher'
);

INSERT IGNORE INTO sys_role_permission (role, permission_id)
SELECT 'STUDENT', id FROM sys_permission WHERE deleted = 0 AND permission_key IN (
    'task.score_view'
);

-- 8.5 新增表与扩展字段的物理外键约束（幂等）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS migrate_training_foreign_keys()
BEGIN
    DECLARE fk_exists INT DEFAULT 0;

    -- 组织架构：专业→学院，班级→专业
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'major_info' AND constraint_name = 'fk_major_school';
    IF fk_exists = 0 THEN
        ALTER TABLE major_info ADD CONSTRAINT fk_major_school FOREIGN KEY (school_id) REFERENCES school_info(id) ON DELETE RESTRICT;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'class_info' AND constraint_name = 'fk_class_major';
    IF fk_exists = 0 THEN
        ALTER TABLE class_info ADD CONSTRAINT fk_class_major FOREIGN KEY (major_id) REFERENCES major_info(id) ON DELETE RESTRICT;
    END IF;

    -- 分组：组→项目，成员→组/学生
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'student_group' AND constraint_name = 'fk_group_project';
    IF fk_exists = 0 THEN
        ALTER TABLE student_group ADD CONSTRAINT fk_group_project FOREIGN KEY (project_id) REFERENCES project_info(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'group_member' AND constraint_name = 'fk_gm_group';
    IF fk_exists = 0 THEN
        ALTER TABLE group_member ADD CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES student_group(id) ON DELETE CASCADE;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'group_member' AND constraint_name = 'fk_gm_student';
    IF fk_exists = 0 THEN
        ALTER TABLE group_member ADD CONSTRAINT fk_gm_student FOREIGN KEY (student_id) REFERENCES sys_user(id) ON DELETE CASCADE;
    END IF;

    -- 项目上下文：项目→学院/专业/班级
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'project_info' AND constraint_name = 'fk_project_school';
    IF fk_exists = 0 THEN
        ALTER TABLE project_info ADD CONSTRAINT fk_project_school FOREIGN KEY (school_id) REFERENCES school_info(id) ON DELETE SET NULL;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'project_info' AND constraint_name = 'fk_project_major';
    IF fk_exists = 0 THEN
        ALTER TABLE project_info ADD CONSTRAINT fk_project_major FOREIGN KEY (major_id) REFERENCES major_info(id) ON DELETE SET NULL;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'project_info' AND constraint_name = 'fk_project_class';
    IF fk_exists = 0 THEN
        ALTER TABLE project_info ADD CONSTRAINT fk_project_class FOREIGN KEY (class_id) REFERENCES class_info(id) ON DELETE SET NULL;
    END IF;

    -- 用户上下文：用户→学院/专业/班级
    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND constraint_name = 'fk_user_school';
    IF fk_exists = 0 THEN
        ALTER TABLE sys_user ADD CONSTRAINT fk_user_school FOREIGN KEY (school_id) REFERENCES school_info(id) ON DELETE SET NULL;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND constraint_name = 'fk_user_major';
    IF fk_exists = 0 THEN
        ALTER TABLE sys_user ADD CONSTRAINT fk_user_major FOREIGN KEY (major_id) REFERENCES major_info(id) ON DELETE SET NULL;
    END IF;

    SET fk_exists = 0;
    SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND constraint_name = 'fk_user_class';
    IF fk_exists = 0 THEN
        ALTER TABLE sys_user ADD CONSTRAINT fk_user_class FOREIGN KEY (class_id) REFERENCES class_info(id) ON DELETE SET NULL;
    END IF;
END //
DELIMITER ;

CALL migrate_training_foreign_keys();
DROP PROCEDURE IF EXISTS migrate_training_foreign_keys;

-- =====================================================================
-- 9. 存量数据修复：组长职责下沉到小组
-- 说明：项目级不再有「项目组长」，审核路由改为「组员任务→小组组长」。
--      早期版本「指定组长」会把 project_info.owner_id 写成学生，
--      导致教师项目列表、管理权限判断出现偏差，此处回滚该字段。
-- 幂等：可重复执行，仅处理不符合当前模型的数据。
-- =====================================================================

-- 9.1 项目负责人字段回归指导教师（无指导教师时退回创建人）
UPDATE project_info p
JOIN sys_user u ON u.id = p.owner_id
SET p.owner_id = COALESCE(p.teacher_id, p.creator_id)
WHERE p.deleted = 0
  AND u.role <> 'TEACHER';

-- 9.2 审核中的任务：审核人若仍指向非教师账号，重算为负责人所在小组的组长，
--     组长本人或无小组时回退指导教师
UPDATE task_info t
JOIN project_info p ON p.id = t.project_id
JOIN sys_user ru ON ru.id = t.reviewer_id
SET t.reviewer_id = COALESCE(
        (SELECT sg.leader_id
         FROM group_member gm
         JOIN student_group sg ON sg.id = gm.group_id
         WHERE gm.student_id = t.assignee_id
           AND gm.deleted = 0
           AND sg.project_id = p.id
           AND sg.deleted = 0
           AND sg.leader_id IS NOT NULL
           AND sg.leader_id <> t.assignee_id
         ORDER BY sg.id
         LIMIT 1),
        p.teacher_id,
        t.reviewer_id
    )
WHERE t.deleted = 0
  AND t.status = 'REVIEW'
  AND ru.role <> 'TEACHER'
  AND p.teacher_id IS NOT NULL;

-- =====================================================================
-- 10. 清理冗余字段：组长唯一来源为 student_group.leader_id
-- 说明：project_member.role_in_project 与 group_member.is_leader 与
--      student_group.leader_id 重复表达组长身份，统一删除，避免双写不一致。
-- 幂等：列存在才删除，可重复执行。
-- =====================================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS migrate_drop_redundant_leader_columns()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'project_member'
                 AND column_name = 'role_in_project') THEN
        ALTER TABLE project_member DROP COLUMN role_in_project;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'group_member'
                 AND column_name = 'is_leader') THEN
        ALTER TABLE group_member DROP COLUMN is_leader;
    END IF;
    -- student_group.teacher_id 只写不读，指导教师以 project_info.teacher_id 为准
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'student_group'
                 AND column_name = 'teacher_id') THEN
        ALTER TABLE student_group DROP COLUMN teacher_id;
    END IF;
END //
DELIMITER ;

CALL migrate_drop_redundant_leader_columns();
DROP PROCEDURE IF EXISTS migrate_drop_redundant_leader_columns;

-- =====================================================================
-- 11. 修复学院名称唯一索引（逻辑删除兼容）
-- 旧索引 UNIQUE(name, deleted) 导致同名学院被逻辑删除后再次删除时报
-- Duplicate entry 'xxx-1' for key 'uk_school_name'。
-- 改为函数索引，仅约束 deleted = 0 的未删除记录；
-- 已删除记录表达式结果为 NULL，不参与唯一性比较。
-- 幂等：可重复执行。
-- =====================================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS migrate_school_name_unique()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'school_info'
                 AND index_name = 'uk_school_name'
                 AND column_name = 'deleted') THEN
        ALTER TABLE school_info DROP INDEX uk_school_name;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'school_info'
                     AND index_name = 'uk_school_name') THEN
        ALTER TABLE school_info ADD UNIQUE KEY uk_school_name ((CASE WHEN deleted = 0 THEN name END));
    END IF;
END //
DELIMITER ;

CALL migrate_school_name_unique();
DROP PROCEDURE IF EXISTS migrate_school_name_unique;

-- =====================================================================
-- 11.1 其余唯一索引统一改为函数索引（逻辑删除兼容）
-- 旧索引包含 deleted 字段，导致同名/同关系记录只能被逻辑删除一次；
-- 改为仅约束 deleted = 0 的记录，已删除记录表达式为 NULL，不参与唯一性比较。
-- 幂等：可重复执行。
-- =====================================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS migrate_logical_delete_unique_indexes()
BEGIN
    -- class_info：同一专业下班级名称
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'class_info'
                 AND index_name = 'uk_class_name' AND column_name = 'deleted') THEN
        ALTER TABLE class_info DROP INDEX uk_class_name;
        DELETE c1 FROM class_info c1 JOIN class_info c2
          ON c1.major_id = c2.major_id AND c1.name = c2.name
         AND c1.deleted = 0 AND c2.deleted = 0 AND c1.id > c2.id;
        ALTER TABLE class_info ADD UNIQUE KEY uk_class_name
            ((CASE WHEN deleted = 0 THEN major_id END), (CASE WHEN deleted = 0 THEN name END));
    END IF;

    -- major_info：同一学院下专业名称
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'major_info'
                 AND index_name = 'uk_major_name' AND column_name = 'deleted') THEN
        ALTER TABLE major_info DROP INDEX uk_major_name;
        DELETE m1 FROM major_info m1 JOIN major_info m2
          ON m1.school_id = m2.school_id AND m1.name = m2.name
         AND m1.deleted = 0 AND m2.deleted = 0 AND m1.id > m2.id;
        ALTER TABLE major_info ADD UNIQUE KEY uk_major_name
            ((CASE WHEN deleted = 0 THEN school_id END), (CASE WHEN deleted = 0 THEN name END));
    END IF;

    -- student_group：同一项目下小组名称
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'student_group'
                 AND index_name = 'uk_project_group' AND column_name = 'deleted') THEN
        ALTER TABLE student_group DROP INDEX uk_project_group;
        DELETE g1 FROM student_group g1 JOIN student_group g2
          ON g1.project_id = g2.project_id AND g1.name = g2.name
         AND g1.deleted = 0 AND g2.deleted = 0 AND g1.id > g2.id;
        ALTER TABLE student_group ADD UNIQUE KEY uk_project_group
            ((CASE WHEN deleted = 0 THEN project_id END), (CASE WHEN deleted = 0 THEN name END));
    END IF;

    -- group_member：小组成员
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'group_member'
                 AND index_name = 'uk_group_student' AND column_name = 'deleted') THEN
        ALTER TABLE group_member DROP INDEX uk_group_student;
        DELETE gm1 FROM group_member gm1 JOIN group_member gm2
          ON gm1.group_id = gm2.group_id AND gm1.student_id = gm2.student_id
         AND gm1.deleted = 0 AND gm2.deleted = 0 AND gm1.id > gm2.id;
        ALTER TABLE group_member ADD UNIQUE KEY uk_group_student
            ((CASE WHEN deleted = 0 THEN group_id END), (CASE WHEN deleted = 0 THEN student_id END));
    END IF;

    -- task_attachment：任务附件关联
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'task_attachment'
                 AND index_name = 'uk_task_file' AND column_name = 'deleted') THEN
        ALTER TABLE task_attachment DROP INDEX uk_task_file;
        DELETE ta1 FROM task_attachment ta1 JOIN task_attachment ta2
          ON ta1.task_id = ta2.task_id AND ta1.file_id = ta2.file_id
         AND ta1.deleted = 0 AND ta2.deleted = 0 AND ta1.id > ta2.id;
        ALTER TABLE task_attachment ADD UNIQUE KEY uk_task_file
            ((CASE WHEN deleted = 0 THEN task_id END), (CASE WHEN deleted = 0 THEN file_id END));
    END IF;

    -- sys_role_permission：角色权限关联
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'sys_role_permission'
                 AND index_name = 'uk_role_permission' AND column_name = 'deleted') THEN
        ALTER TABLE sys_role_permission DROP INDEX uk_role_permission;
        DELETE rp1 FROM sys_role_permission rp1 JOIN sys_role_permission rp2
          ON rp1.role = rp2.role AND rp1.permission_id = rp2.permission_id
         AND rp1.deleted = 0 AND rp2.deleted = 0 AND rp1.id > rp2.id;
        ALTER TABLE sys_role_permission ADD UNIQUE KEY uk_role_permission
            ((CASE WHEN deleted = 0 THEN role END), (CASE WHEN deleted = 0 THEN permission_id END));
    END IF;

    -- sys_user：登录用户名
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'sys_user'
                 AND index_name = 'uk_username_deleted' AND column_name = 'deleted') THEN
        ALTER TABLE sys_user DROP INDEX uk_username_deleted;
        DELETE u1 FROM sys_user u1 JOIN sys_user u2
          ON u1.username = u2.username
         AND u1.deleted = 0 AND u2.deleted = 0 AND u1.id > u2.id;
        ALTER TABLE sys_user ADD UNIQUE KEY uk_username_deleted
            ((CASE WHEN deleted = 0 THEN username END));
    END IF;

    -- sys_user：学号（旧索引为普通唯一索引，删除学生后学号无法复用）
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'sys_user'
                 AND index_name = 'uk_student_no' AND expression IS NULL) THEN
        ALTER TABLE sys_user DROP INDEX uk_student_no;
        DELETE u1 FROM sys_user u1 JOIN sys_user u2
          ON u1.student_no = u2.student_no AND u1.student_no IS NOT NULL
         AND u1.deleted = 0 AND u2.deleted = 0 AND u1.id > u2.id;
        ALTER TABLE sys_user ADD UNIQUE KEY uk_student_no
            ((CASE WHEN deleted = 0 THEN student_no END));
    END IF;
END //
DELIMITER ;

CALL migrate_logical_delete_unique_indexes();
DROP PROCEDURE IF EXISTS migrate_logical_delete_unique_indexes;

-- =====================================================================
-- 12. 分组支持人数上限与学生自选加入
-- =====================================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS migrate_group_join_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'student_group'
                     AND column_name = 'max_members') THEN
        ALTER TABLE student_group ADD COLUMN max_members INT NOT NULL DEFAULT 0
            COMMENT '人数上限，0表示不限' AFTER description;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'student_group'
                     AND column_name = 'join_enabled') THEN
        ALTER TABLE student_group ADD COLUMN join_enabled TINYINT NOT NULL DEFAULT 1
            COMMENT '是否允许学生自选加入：1允许 0关闭' AFTER max_members;
    END IF;
END //
DELIMITER ;

CALL migrate_group_join_columns();
DROP PROCEDURE IF EXISTS migrate_group_join_columns;
