package com.petlink.modules.rescue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.rescue.entity.RescueTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RescueTaskMapper extends BaseMapper<RescueTask> {
    @Select("SELECT * FROM rescue_task WHERE id=#{id} FOR UPDATE")
    RescueTask selectForUpdate(@Param("id") Long id);

    @Select({"<script>",
            "SELECT * FROM rescue_task WHERE rescuer_id=#{rescuerId}",
            "<if test='status != null'> AND status=#{status}</if>",
            " ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<RescueTask> selectMinePage(@Param("rescuerId") Long rescuerId,
                                    @Param("status") String status,
                                    @Param("limit") int limit,
                                    @Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM rescue_task WHERE rescuer_id=#{rescuerId}",
            "<if test='status != null'> AND status=#{status}</if>",
            "</script>"})
    long countMine(@Param("rescuerId") Long rescuerId, @Param("status") String status);

    @Select("SELECT * FROM rescue_task WHERE clue_id=#{clueId} ORDER BY created_at DESC, id DESC LIMIT 1 FOR UPDATE")
    RescueTask selectLatestForUpdateByClue(@Param("clueId") Long clueId);

    @Select("SELECT * FROM rescue_task WHERE clue_id=#{clueId} AND status IN ('WAITING_START','IN_PROGRESS') ORDER BY created_at DESC, id DESC LIMIT 1 FOR UPDATE")
    RescueTask selectActiveForUpdateByClue(@Param("clueId") Long clueId);

    @Update("UPDATE rescue_task SET status='IN_PROGRESS', started_at=#{startedAt} WHERE id=#{id} AND rescuer_id=#{rescuerId} AND status='WAITING_START'")
    int start(@Param("id") Long id, @Param("rescuerId") Long rescuerId, @Param("startedAt") LocalDateTime startedAt);

    @Update("UPDATE rescue_task SET status='FAILED', failure_reason=#{reason}, finished_at=#{finishedAt} WHERE id=#{id} AND rescuer_id=#{rescuerId} AND status='IN_PROGRESS'")
    int fail(@Param("id") Long id, @Param("rescuerId") Long rescuerId,
             @Param("reason") String reason, @Param("finishedAt") LocalDateTime finishedAt);

    @Update("UPDATE rescue_task SET status='SUCCESS', finished_at=#{finishedAt} WHERE id=#{id} AND rescuer_id=#{rescuerId} AND status='IN_PROGRESS'")
    int succeed(@Param("id") Long id, @Param("rescuerId") Long rescuerId,
                @Param("finishedAt") LocalDateTime finishedAt);

    @Update("UPDATE rescue_task SET status='CANCELED', cancel_reason=#{reason}, finished_at=#{finishedAt} WHERE id=#{id} AND status IN ('WAITING_START','IN_PROGRESS')")
    int cancel(@Param("id") Long id, @Param("reason") String reason, @Param("finishedAt") LocalDateTime finishedAt);
}
