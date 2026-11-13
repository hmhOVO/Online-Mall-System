package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.TaskAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskAttachmentMapper extends BaseMapper<TaskAttachment> {

    @Select("SELECT ta.*, fr.original_name AS file_name, fr.file_path, fr.file_size, fr.mime_type, fr.uploader_id " +
            "FROM task_attachment ta " +
            "JOIN file_resource fr ON ta.file_id = fr.id " +
            "WHERE ta.task_id = #{taskId} AND ta.deleted = 0 AND fr.deleted = 0 " +
            "ORDER BY ta.created_at DESC")
    List<TaskAttachment> selectWithFileByTaskId(@Param("taskId") Integer taskId);

    @Select("SELECT COUNT(*) FROM task_attachment WHERE file_id = #{fileId} AND deleted = 0")
    int countByFileId(@Param("fileId") Integer fileId);
}