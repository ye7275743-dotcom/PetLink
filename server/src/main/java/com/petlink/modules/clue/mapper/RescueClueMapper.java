package com.petlink.modules.clue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.clue.entity.RescueClue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RescueClueMapper extends BaseMapper<RescueClue> {
    @Select("SELECT * FROM rescue_clue WHERE id = #{id} FOR UPDATE")
    RescueClue selectForUpdate(@Param("id") Long id);

    @Select({"<script>",
            "SELECT * FROM rescue_clue WHERE publisher_id = #{publisherId}",
            "<if test='status != null'> AND status = #{status}</if>",
            " ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<RescueClue> selectMinePage(@Param("publisherId") Long publisherId,
                                    @Param("status") String status,
                                    @Param("limit") int limit,
                                    @Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM rescue_clue WHERE publisher_id = #{publisherId}",
            "<if test='status != null'> AND status = #{status}</if>",
            "</script>"})
    long countMine(@Param("publisherId") Long publisherId, @Param("status") String status);

    @Select({"<script>",
            "SELECT * FROM rescue_clue WHERE 1=1",
            "<if test='status != null'> AND status = #{status}</if>",
            "<if test='keyword != null'> AND (location LIKE CONCAT('%',#{keyword},'%') OR animal_description LIKE CONCAT('%',#{keyword},'%') OR scene_description LIKE CONCAT('%',#{keyword},'%') OR contact LIKE CONCAT('%',#{keyword},'%'))</if>",
            " ORDER BY created_at ASC, id ASC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<RescueClue> selectAdminPage(@Param("status") String status,
                                     @Param("keyword") String keyword,
                                     @Param("limit") int limit,
                                     @Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM rescue_clue WHERE 1=1",
            "<if test='status != null'> AND status = #{status}</if>",
            "<if test='keyword != null'> AND (location LIKE CONCAT('%',#{keyword},'%') OR animal_description LIKE CONCAT('%',#{keyword},'%') OR scene_description LIKE CONCAT('%',#{keyword},'%') OR contact LIKE CONCAT('%',#{keyword},'%'))</if>",
            "</script>"})
    long countAdmin(@Param("status") String status,@Param("keyword") String keyword);

    @Select("SELECT CASE WHEN EXISTS (SELECT 1 FROM rescue_task WHERE clue_id = #{clueId} AND rescuer_id = #{rescuerId}) THEN 1 ELSE 0 END")
    int existsTaskForRescuer(@Param("clueId") Long clueId, @Param("rescuerId") Long rescuerId);

    @Update("UPDATE rescue_clue SET status='WITHDRAWN' WHERE id=#{id} AND publisher_id=#{publisherId} AND status='PENDING_REVIEW'")
    int withdrawPending(@Param("id") Long id, @Param("publisherId") Long publisherId);

    @Update("UPDATE rescue_clue SET status='WAITING_ACCEPT', reviewer_id=#{reviewerId}, reviewed_at=#{reviewedAt}, reject_reason=NULL " +
            "WHERE id=#{id} AND status='PENDING_REVIEW'")
    int auditApprove(@Param("id") Long id, @Param("reviewerId") Long reviewerId,
                     @Param("reviewedAt") LocalDateTime reviewedAt);

    @Update("UPDATE rescue_clue SET status='REJECTED', reviewer_id=#{reviewerId}, reviewed_at=#{reviewedAt}, reject_reason=#{rejectReason} " +
            "WHERE id=#{id} AND status='PENDING_REVIEW'")
    int auditReject(@Param("id") Long id, @Param("reviewerId") Long reviewerId,
                    @Param("reviewedAt") LocalDateTime reviewedAt,
                    @Param("rejectReason") String rejectReason);

    @Select("SELECT * FROM rescue_clue WHERE status='WAITING_ACCEPT' ORDER BY created_at ASC, id ASC LIMIT #{limit} OFFSET #{offset}")
    List<RescueClue> selectWaitingAcceptancePage(@Param("limit") int limit, @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM rescue_clue WHERE status='WAITING_ACCEPT'")
    long countWaitingAcceptance();

    @Update("UPDATE rescue_clue SET status='CONVERTED' WHERE id=#{id} AND status='WAITING_ACCEPT'")
    int acceptForRescue(@Param("id") Long id);

    @Update("UPDATE rescue_clue SET status='CLOSED' WHERE id=#{id} AND status='CONVERTED'")
    int closeConverted(@Param("id") Long id);

    @Update("UPDATE rescue_clue SET status='WAITING_ACCEPT' WHERE id=#{id} AND status='CONVERTED'")
    int reopenConverted(@Param("id") Long id);
}
