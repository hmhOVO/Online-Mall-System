package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.School;
import com.example.exception.CustomerException;
import com.example.mapper.SchoolMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolService {

    private final SchoolMapper schoolMapper;

    public SchoolService(SchoolMapper schoolMapper) {
        this.schoolMapper = schoolMapper;
    }

    public Page<School> list(Integer pageNum, Integer pageSize, String keyword, String status) {
        LambdaQueryWrapper<School> wrapper = Wrappers.lambdaQuery();
        wrapper.like(keyword != null && !keyword.isBlank(), School::getName, keyword)
                .eq(status != null && !status.isBlank(), School::getStatus, status)
                .orderByAsc(School::getSortOrder)
                .orderByDesc(School::getCreatedAt);
        return schoolMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<School> listAll() {
        return schoolMapper.selectList(Wrappers.lambdaQuery(School.class)
                .eq(School::getStatus, "ENABLED")
                .orderByAsc(School::getSortOrder));
    }

    public School create(School school) {
        if (school.getName() == null || school.getName().isBlank()) {
            throw new CustomerException("学院名称不能为空");
        }
        if (school.getSortOrder() == null) {
            school.setSortOrder(0);
        }
        if (school.getStatus() == null || school.getStatus().isBlank()) {
            school.setStatus("ENABLED");
        }
        schoolMapper.insert(school);
        return school;
    }

    public void update(Integer id, School school) {
        School existing = schoolMapper.selectById(id);
        if (existing == null) {
            throw new CustomerException("学院不存在");
        }
        school.setId(id);
        schoolMapper.updateById(school);
    }

    public void delete(Integer id) {
        if (schoolMapper.selectById(id) == null) {
            throw new CustomerException("学院不存在");
        }
        schoolMapper.deleteById(id);
    }
}