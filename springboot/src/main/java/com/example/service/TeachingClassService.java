package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.ClassInfo;
import com.example.entity.Project;
import com.example.entity.TeachingClass;
import com.example.entity.User;
import com.example.exception.CustomerException;
import com.example.mapper.ClassInfoMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.TeachingClassMapper;
import com.example.mapper.UserMapper;
import com.example.common.CurrentUserContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeachingClassService {

    private final TeachingClassMapper teachingClassMapper;
    private final ClassInfoMapper classInfoMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

    public TeachingClassService(TeachingClassMapper teachingClassMapper, ClassInfoMapper classInfoMapper,
                                ProjectMapper projectMapper, UserMapper userMapper) {
        this.teachingClassMapper = teachingClassMapper;
        this.classInfoMapper = classInfoMapper;
        this.projectMapper = projectMapper;
        this.userMapper = userMapper;
    }

    /**
     * 教师端「我的课程」：返回本人授课的课程，并附带项目数与班级学生数。
     */
    public List<Map<String, Object>> listForCurrentTeacher() {
        User current = CurrentUserContext.get();
        if (current == null) {
            throw new CustomerException("未登录");
        }
        List<TeachingClass> courses = teachingClassMapper.selectList(Wrappers.lambdaQuery(TeachingClass.class)
                .eq(TeachingClass::getTeacherId, current.getId())
                .orderByDesc(TeachingClass::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeachingClass course : courses) {
            ClassInfo classInfo = classInfoMapper.selectById(course.getClassId());
            Long projectCount = projectMapper.selectCount(Wrappers.lambdaQuery(Project.class)
                    .eq(Project::getCourseId, course.getId())
                    .eq(Project::getDeleted, 0));
            Long studentCount = userMapper.selectCount(Wrappers.lambdaQuery(User.class)
                    .eq(User::getClassId, course.getClassId())
                    .eq(User::getRole, "STUDENT")
                    .eq(User::getStatus, "ENABLED"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", course.getId());
            item.put("name", course.getName());
            item.put("term", course.getTerm());
            item.put("classId", course.getClassId());
            item.put("className", classInfo == null ? "" : classInfo.getName());
            item.put("status", course.getStatus());
            item.put("projectCount", projectCount);
            item.put("studentCount", studentCount);
            result.add(item);
        }
        return result;
    }

    public Page<TeachingClass> list(Integer pageNum, Integer pageSize, String keyword, Integer classId,
                                    String status, String term, Integer teacherId) {
        LambdaQueryWrapper<TeachingClass> wrapper = Wrappers.lambdaQuery();
        wrapper.like(keyword != null && !keyword.isBlank(), TeachingClass::getName, keyword)
                .eq(classId != null, TeachingClass::getClassId, classId)
                .eq(status != null && !status.isBlank(), TeachingClass::getStatus, status)
                .like(term != null && !term.isBlank(), TeachingClass::getTerm, term)
                .eq(teacherId != null, TeachingClass::getTeacherId, teacherId)
                .orderByDesc(TeachingClass::getCreatedAt);
        return teachingClassMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 下拉框用：仅返回启用状态的教学班。
     */
    public List<TeachingClass> listAll(Integer teacherId) {
        return teachingClassMapper.selectList(Wrappers.lambdaQuery(TeachingClass.class)
                .eq(teacherId != null, TeachingClass::getTeacherId, teacherId)
                .eq(TeachingClass::getStatus, "ENABLED")
                .orderByDesc(TeachingClass::getCreatedAt));
    }

    public TeachingClass create(TeachingClass teachingClass) {
        validate(teachingClass);
        if (teachingClass.getStatus() == null || teachingClass.getStatus().isBlank()) {
            teachingClass.setStatus("ENABLED");
        }
        teachingClassMapper.insert(teachingClass);
        return teachingClass;
    }

    public void update(Integer id, TeachingClass teachingClass) {
        if (teachingClassMapper.selectById(id) == null) {
            throw new CustomerException("课程不存在");
        }
        validate(teachingClass);
        teachingClass.setId(id);
        teachingClassMapper.updateById(teachingClass);
    }

    public void delete(Integer id) {
        if (teachingClassMapper.selectById(id) == null) {
            throw new CustomerException("课程不存在");
        }
        teachingClassMapper.deleteById(id);
    }

    private void validate(TeachingClass teachingClass) {
        if (teachingClass.getName() == null || teachingClass.getName().isBlank()) {
            throw new CustomerException("课程名称不能为空");
        }
        if (teachingClass.getClassId() == null) {
            throw new CustomerException("所属班级不能为空");
        }
    }
}
