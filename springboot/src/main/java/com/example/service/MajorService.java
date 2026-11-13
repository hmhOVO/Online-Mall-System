package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.Major;
import com.example.exception.CustomerException;
import com.example.mapper.MajorMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MajorService {

    private final MajorMapper majorMapper;

    public MajorService(MajorMapper majorMapper) {
        this.majorMapper = majorMapper;
    }

    public Page<Major> list(Integer pageNum, Integer pageSize, String keyword, Integer schoolId, String status) {
        LambdaQueryWrapper<Major> wrapper = Wrappers.lambdaQuery();
        wrapper.like(keyword != null && !keyword.isBlank(), Major::getName, keyword)
                .eq(schoolId != null, Major::getSchoolId, schoolId)
                .eq(status != null && !status.isBlank(), Major::getStatus, status)
                .orderByAsc(Major::getSortOrder)
                .orderByDesc(Major::getCreatedAt);
        return majorMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<Major> listAll(Integer schoolId) {
        return majorMapper.selectList(Wrappers.lambdaQuery(Major.class)
                .eq(schoolId != null, Major::getSchoolId, schoolId)
                .eq(Major::getStatus, "ENABLED")
                .orderByAsc(Major::getSortOrder));
    }

    public Major create(Major major) {
        if (major.getName() == null || major.getName().isBlank()) {
            throw new CustomerException("专业名称不能为空");
        }
        if (major.getSchoolId() == null) {
            throw new CustomerException("所属学院不能为空");
        }
        if (major.getSortOrder() == null) {
            major.setSortOrder(0);
        }
        if (major.getStatus() == null || major.getStatus().isBlank()) {
            major.setStatus("ENABLED");
        }
        majorMapper.insert(major);
        return major;
    }

    public void update(Integer id, Major major) {
        if (majorMapper.selectById(id) == null) {
            throw new CustomerException("专业不存在");
        }
        major.setId(id);
        majorMapper.updateById(major);
    }

    public void delete(Integer id) {
        if (majorMapper.selectById(id) == null) {
            throw new CustomerException("专业不存在");
        }
        majorMapper.deleteById(id);
    }
}