package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Project;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("<script>" +
            "select p.*, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id) as totalTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id and t.status = 'DONE') as completedTasks " +
            "from project_info p " +
            "where p.deleted = 0 " +
            "<if test='archived != null'>and p.archived = #{archived} </if>" +
            "<if test='name != null and name != \"\"'>and p.name like concat('%', #{name}, '%') </if>" +
            "<if test='status != null and status != \"\"'>and p.status = #{status} </if>" +
            "<if test='startDateBegin != null'>and p.start_date &gt;= #{startDateBegin} </if>" +
            "<if test='startDateEnd != null'>and p.end_date &lt;= #{startDateEnd} </if>" +
            "<if test='courseId != null'>and p.course_id = #{courseId} </if>" +
            "<if test='projectIds != null and projectIds.size() > 0'>and p.id in <foreach collection='projectIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> </if>" +
            "order by p.created_at desc " +
            "limit #{offset}, #{size}" +
            "</script>")
    List<Project> selectProjectsWithTaskStats(@Param("name") String name,
                                               @Param("status") String status,
                                               @Param("startDateBegin") LocalDate startDateBegin,
                                               @Param("startDateEnd") LocalDate startDateEnd,
                                               @Param("courseId") Integer courseId,
                                               @Param("projectIds") List<Integer> projectIds,
                                               @Param("archived") Integer archived,
                                               @Param("offset") Integer offset,
                                               @Param("size") Integer size);

    @Select("<script>" +
            "select count(*) from project_info p " +
            "where p.deleted = 0 " +
            "<if test='archived != null'>and p.archived = #{archived} </if>" +
            "<if test='name != null and name != \"\"'>and p.name like concat('%', #{name}, '%') </if>" +
            "<if test='status != null and status != \"\"'>and p.status = #{status} </if>" +
            "<if test='startDateBegin != null'>and p.start_date &gt;= #{startDateBegin} </if>" +
            "<if test='startDateEnd != null'>and p.end_date &lt;= #{startDateEnd} </if>" +
            "<if test='courseId != null'>and p.course_id = #{courseId} </if>" +
            "<if test='projectIds != null and projectIds.size() > 0'>and p.id in <foreach collection='projectIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> </if>" +
            "</script>")
    long countProjectsWithTaskStats(@Param("name") String name,
                                     @Param("status") String status,
                                     @Param("startDateBegin") LocalDate startDateBegin,
                                     @Param("startDateEnd") LocalDate startDateEnd,
                                     @Param("courseId") Integer courseId,
                                     @Param("projectIds") List<Integer> projectIds,
                                     @Param("archived") Integer archived);

    @Select("<script>" +
            "select p.*, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id) as totalTasks, " +
            "  (select count(*) from task_info t where t.deleted = 0 and t.project_id = p.id and t.status = 'DONE') as completedTasks " +
            "from project_info p " +
            "where p.deleted = 1 " +
            "<if test='name != null and name != \"\"'>and p.name like concat('%', #{name}, '%') </if>" +
            "<if test='status != null and status != \"\"'>and p.status = #{status} </if>" +
            "<if test='projectIds != null and projectIds.size() > 0'>and p.id in <foreach collection='projectIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> </if>" +
            "order by p.updated_at desc " +
            "limit #{offset}, #{size}" +
            "</script>")
    List<Project> selectDeletedProjectsWithTaskStats(@Param("name") String name,
                                                      @Param("status") String status,
                                                      @Param("projectIds") List<Integer> projectIds,
                                                      @Param("offset") Integer offset,
                                                      @Param("size") Integer size);

    @Select("<script>" +
            "select count(*) from project_info p " +
            "where p.deleted = 1 " +
            "<if test='name != null and name != \"\"'>and p.name like concat('%', #{name}, '%') </if>" +
            "<if test='status != null and status != \"\"'>and p.status = #{status} </if>" +
            "<if test='projectIds != null and projectIds.size() > 0'>and p.id in <foreach collection='projectIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> </if>" +
            "</script>")
    long countDeletedProjectsWithTaskStats(@Param("name") String name,
                                            @Param("status") String status,
                                            @Param("projectIds") List<Integer> projectIds);

    @Select("select * from project_info where id = #{id} and deleted = 1")
    Project selectDeletedById(Integer id);

    @Select("select id from project_info where deleted = 1 and (owner_id = #{userId} or creator_id = #{userId} or teacher_id = #{userId})")
    List<Integer> selectOwnDeletedProjectIds(Integer userId);

    @Update("update project_info set deleted = 0 where id = #{id} and deleted = 1")
    int restoreProjectById(Integer id);

    @Select("select id from project_info where deleted = 1")
    List<Integer> selectAllDeletedProjectIds();

    @Delete("delete from task_dependency where task_id in (select id from task_info where project_id = #{projectId}) " +
            "or depends_on_task_id in (select id from task_info where project_id = #{projectId})")
    int deleteTaskDependenciesByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from task_comment where task_id in (select id from task_info where project_id = #{projectId})")
    int deleteTaskCommentsByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from task_attachment where task_id in (select id from task_info where project_id = #{projectId})")
    int deleteTaskAttachmentsByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from task_change_log where task_id in (select id from task_info where project_id = #{projectId})")
    int deleteTaskChangeLogsByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from group_member where group_id in (select id from student_group where project_id = #{projectId})")
    int deleteGroupMembersByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from student_group where project_id = #{projectId}")
    int deleteStudentGroupsByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from project_member where project_id = #{projectId}")
    int deleteMembersByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from file_resource where project_id = #{projectId}")
    int deleteFilesByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from task_info where project_id = #{projectId}")
    int deleteTasksByProjectId(@Param("projectId") Integer projectId);

    @Delete("delete from project_info where id = #{id} and deleted = 1")
    int hardDeleteById(Integer id);
}
