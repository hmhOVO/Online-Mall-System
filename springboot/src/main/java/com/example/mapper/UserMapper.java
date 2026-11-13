package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    @Select("select * from sys_user where username = #{username} and deleted = 0 limit 1")
    User selectByUsername(@Param("username") String username);

    @Select("select count(*) from project_info where deleted = 0 and owner_id = #{userId} and status != 'COMPLETED'")
    long countManagingProjects(@Param("userId") Integer userId);
}
