package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Permission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("select p.* from sys_permission p " +
            "join sys_role_permission rp on p.id = rp.permission_id " +
            "where rp.role = #{role} and p.deleted = 0")
    List<Permission> selectByRole(@Param("role") String role);
}
