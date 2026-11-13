package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.FileResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FileResourceMapper extends BaseMapper<FileResource> {

    @Select("select * from file_resource where project_id = #{projectId} and deleted = 0 order by created_at desc")
    List<FileResource> selectByProjectId(@Param("projectId") Integer projectId);

    @Select("select f.*, coalesce(u.nickname, u.username) as uploader_name " +
            "from file_resource f left join sys_user u on u.id = f.uploader_id " +
            "where f.project_id = #{projectId} and f.business_type = #{businessType} and f.deleted = 0 " +
            "order by f.created_at desc")
    List<FileResource> selectProjectFiles(@Param("projectId") Integer projectId,
                                          @Param("businessType") String businessType);

    @Update("update file_resource set ref_count = ref_count + #{delta}, updated_at = now() where id = #{id}")
    int updateRefCount(@Param("id") Integer id, @Param("delta") int delta);
}
