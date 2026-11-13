package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.StudentGroup;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentGroupMapper extends BaseMapper<StudentGroup> {

    @Delete("delete from student_group where id = #{id}")
    int physicalDeleteById(@Param("id") Integer id);
}