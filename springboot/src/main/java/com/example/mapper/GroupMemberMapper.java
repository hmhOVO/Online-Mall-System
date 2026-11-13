package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.GroupMember;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    @Delete("delete from group_member where group_id = #{groupId} and student_id = #{studentId}")
    int physicalDeleteByGroupAndStudent(@Param("groupId") Integer groupId, @Param("studentId") Integer studentId);

    /**
     * 查询学生在指定实训项目中所属小组的组长。
     */
    @Select("select sg.leader_id from group_member gm " +
            "join student_group sg on gm.group_id = sg.id " +
            "where gm.student_id = #{studentId} and gm.deleted = 0 " +
            "and sg.project_id = #{projectId} and sg.deleted = 0 " +
            "order by sg.id limit 1")
    Integer selectLeaderIdByStudent(@Param("projectId") Integer projectId, @Param("studentId") Integer studentId);

    /**
     * 查询学生在指定实训项目中所属小组的 ID。
     */
    @Select("select sg.id from group_member gm " +
            "join student_group sg on gm.group_id = sg.id " +
            "where gm.student_id = #{studentId} and gm.deleted = 0 " +
            "and sg.project_id = #{projectId} and sg.deleted = 0 " +
            "order by sg.id limit 1")
    Integer selectGroupIdByStudent(@Param("projectId") Integer projectId, @Param("studentId") Integer studentId);

    /**
     * 查询用户在指定实训项目中担任组长的小组 ID（每个项目取一个）。
     */
    @Select("select sg.id from student_group sg " +
            "where sg.project_id = #{projectId} and sg.leader_id = #{leaderId} and sg.deleted = 0 " +
            "order by sg.id limit 1")
    Integer selectLedGroupId(@Param("projectId") Integer projectId, @Param("leaderId") Integer leaderId);
}