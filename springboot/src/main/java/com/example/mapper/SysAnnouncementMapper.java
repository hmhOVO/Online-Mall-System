package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.SysAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysAnnouncementMapper extends BaseMapper<SysAnnouncement> {

    @Select("SELECT a.*, u.username as publisher_name FROM sys_announcement a " +
            "LEFT JOIN sys_user u ON a.publisher_id = u.id " +
            "WHERE a.deleted = 0 " +
            "AND (#{status} IS NULL OR a.status = #{status}) " +
            "AND (#{type} IS NULL OR a.type = #{type}) " +
            "AND (#{keyword} IS NULL OR a.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY a.is_top DESC, a.publish_time DESC, a.created_at DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<SysAnnouncement> selectAnnouncements(
            @Param("status") String status,
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM sys_announcement a " +
            "WHERE a.deleted = 0 " +
            "AND (#{status} IS NULL OR a.status = #{status}) " +
            "AND (#{type} IS NULL OR a.type = #{type}) " +
            "AND (#{keyword} IS NULL OR a.title LIKE CONCAT('%', #{keyword}, '%'))")
    int countAnnouncements(
            @Param("status") String status,
            @Param("type") String type,
            @Param("keyword") String keyword);

    @Select("SELECT a.*, u.username as publisher_name FROM sys_announcement a " +
            "LEFT JOIN sys_user u ON a.publisher_id = u.id " +
            "WHERE a.deleted = 0 AND a.status = 'PUBLISHED' " +
            "ORDER BY a.is_top DESC, a.publish_time DESC, a.created_at DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<SysAnnouncement> selectPublishedAnnouncements(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM sys_announcement a " +
            "WHERE a.deleted = 0 AND a.status = 'PUBLISHED'")
    int countPublishedAnnouncements();
}
