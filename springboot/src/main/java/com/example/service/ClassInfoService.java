package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.ClassInfo;
import com.example.exception.CustomerException;
import com.example.mapper.ClassInfoMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassInfoService {

    private final ClassInfoMapper classInfoMapper;

    public ClassInfoService(ClassInfoMapper classInfoMapper) {
        this.classInfoMapper = classInfoMapper;
    }

    public Page<ClassInfo> list(Integer pageNum, Integer pageSize, String keyword, Integer majorId, String status) {
        LambdaQueryWrapper<ClassInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.like(keyword != null && !keyword.isBlank(), ClassInfo::getName, keyword)
                .eq(majorId != null, ClassInfo::getMajorId, majorId)
                .eq(status != null && !status.isBlank(), ClassInfo::getStatus, status)
                .orderByAsc(ClassInfo::getSortOrder)
                .orderByDesc(ClassInfo::getCreatedAt);
        return classInfoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<ClassInfo> listAll(Integer majorId) {
        return classInfoMapper.selectList(Wrappers.lambdaQuery(ClassInfo.class)
                .eq(majorId != null, ClassInfo::getMajorId, majorId)
                .eq(ClassInfo::getStatus, "ENABLED")
                .orderByAsc(ClassInfo::getSortOrder));
    }

    public ClassInfo create(ClassInfo classInfo) {
        if (classInfo.getName() == null || classInfo.getName().isBlank()) {
            throw new CustomerException("班级名称不能为空");
        }
        if (classInfo.getMajorId() == null) {
            throw new CustomerException("所属专业不能为空");
        }
        if (classInfo.getSortOrder() == null) {
            classInfo.setSortOrder(0);
        }
        if (classInfo.getStatus() == null || classInfo.getStatus().isBlank()) {
            classInfo.setStatus("ENABLED");
        }
        classInfoMapper.insert(classInfo);
        return classInfo;
    }

    public void update(Integer id, ClassInfo classInfo) {
        if (classInfoMapper.selectById(id) == null) {
            throw new CustomerException("班级不存在");
        }
        classInfo.setId(id);
        classInfoMapper.updateById(classInfo);
    }

    public void delete(Integer id) {
        if (classInfoMapper.selectById(id) == null) {
            throw new CustomerException("班级不存在");
        }
        classInfoMapper.deleteById(id);
    }
}