package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("select count(*) from notification where user_id = #{userId} and is_read = 0 and deleted = 0")
    long countUnread(@Param("userId") Integer userId);

    @Update("update notification set is_read = 1, updated_at = now() where user_id = #{userId} and is_read = 0 and deleted = 0")
    int markAllRead(@Param("userId") Integer userId);

    @Select("select type, count(*) as count from notification where user_id = #{userId} and deleted = 0 group by type")
    java.util.List<java.util.Map<String, Object>> countByType(@Param("userId") Integer userId);
}
