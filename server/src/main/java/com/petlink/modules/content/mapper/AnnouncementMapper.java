package com.petlink.modules.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.content.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
    @Select("SELECT * FROM announcement WHERE status='PUBLISHED' ORDER BY published_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Announcement> selectPublicPage(@Param("limit") int limit,@Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM announcement WHERE status='PUBLISHED'")
    long countPublic();

    @Select("SELECT * FROM announcement WHERE id=#{id} AND status='PUBLISHED'")
    Announcement selectPublishedById(@Param("id") Long id);

    @Select({"<script>",
            "SELECT * FROM announcement WHERE 1=1",
            "<if test='status != null'> AND status=#{status}</if>",
            " ORDER BY created_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<Announcement> selectAdminPage(@Param("status") String status,@Param("limit") int limit,@Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM announcement WHERE 1=1",
            "<if test='status != null'> AND status=#{status}</if>",
            "</script>"})
    long countAdmin(@Param("status") String status);

    @Update({"<script>",
            "UPDATE announcement SET",
            "<if test='titlePresent'> title=#{title},</if>",
            "<if test='contentPresent'> content=#{content},</if>",
            " updated_by=#{updatedBy},updated_at=#{now},version=version+1",
            " WHERE id=#{id} AND status IN ('DRAFT','PUBLISHED') AND version=#{version}",
            "</script>"})
    int updateContent(@Param("id") Long id,@Param("title") String title,@Param("titlePresent") boolean titlePresent,
                      @Param("content") String content,@Param("contentPresent") boolean contentPresent,
                      @Param("updatedBy") Long updatedBy,@Param("version") Integer version,@Param("now") LocalDateTime now);

    @Update("UPDATE announcement SET status='PUBLISHED',published_at=#{now},updated_by=#{updatedBy},updated_at=#{now},version=version+1 WHERE id=#{id} AND status='DRAFT' AND version=#{version}")
    int publish(@Param("id") Long id,@Param("updatedBy") Long updatedBy,@Param("version") Integer version,@Param("now") LocalDateTime now);

    @Update("UPDATE announcement SET status='WITHDRAWN',updated_by=#{updatedBy},updated_at=#{now},version=version+1 WHERE id=#{id} AND status='PUBLISHED' AND version=#{version}")
    int withdraw(@Param("id") Long id,@Param("updatedBy") Long updatedBy,@Param("version") Integer version,@Param("now") LocalDateTime now);
}
