package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    @Select("select gm.*, u.username as sender_username, u.nickname as sender_name " +
            "from group_message gm left join sys_user u on gm.sender_id = u.id " +
            "where gm.group_id = #{groupId} and gm.deleted = 0 " +
            "order by gm.created_at desc, gm.id desc limit #{limit}")
    List<GroupMessage> selectRecentByGroupId(@Param("groupId") Integer groupId, @Param("limit") Integer limit);
}
