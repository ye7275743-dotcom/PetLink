package com.petlink.modules.followup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.followup.entity.FollowUpRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowUpRecordMapper extends BaseMapper<FollowUpRecord> {
    @Select("SELECT * FROM follow_up_record WHERE submitter_id=#{submitterId} AND idempotency_key=#{key} LIMIT 1")
    FollowUpRecord selectBySubmitterAndKey(@Param("submitterId") Long submitterId, @Param("key") String key);

    @Select("SELECT * FROM follow_up_record WHERE adoption_record_id=#{adoptionRecordId} ORDER BY created_at ASC,id ASC")
    List<FollowUpRecord> selectByAdoptionRecordAsc(@Param("adoptionRecordId") Long adoptionRecordId);

    @Select({"<script>",
            "SELECT f.* FROM follow_up_record f JOIN adoption_record ar ON ar.id=f.adoption_record_id",
            " WHERE 1=1",
            "<if test='animalId != null'> AND ar.animal_id=#{animalId}</if>",
            "<if test='userId != null'> AND ar.user_id=#{userId}</if>",
            "<if test='adoptionRecordId != null'> AND ar.id=#{adoptionRecordId}</if>",
            " ORDER BY f.created_at DESC,f.id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<FollowUpRecord> selectAdminPage(@Param("animalId") Long animalId,
                                         @Param("userId") Long userId,
                                         @Param("adoptionRecordId") Long adoptionRecordId,
                                         @Param("limit") int limit,
                                         @Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM follow_up_record f JOIN adoption_record ar ON ar.id=f.adoption_record_id",
            " WHERE 1=1",
            "<if test='animalId != null'> AND ar.animal_id=#{animalId}</if>",
            "<if test='userId != null'> AND ar.user_id=#{userId}</if>",
            "<if test='adoptionRecordId != null'> AND ar.id=#{adoptionRecordId}</if>",
            "</script>"})
    long countAdmin(@Param("animalId") Long animalId,
                    @Param("userId") Long userId,
                    @Param("adoptionRecordId") Long adoptionRecordId);

    @Select({"<script>",
            "SELECT f.* FROM follow_up_record f",
            " JOIN adoption_record ar ON ar.id=f.adoption_record_id",
            " JOIN animal a ON a.id=ar.animal_id",
            " JOIN rescue_task t ON t.id=a.rescue_task_id",
            " WHERE t.rescuer_id=#{rescuerId}",
            "<if test='animalId != null'> AND ar.animal_id=#{animalId}</if>",
            " ORDER BY f.created_at DESC,f.id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<FollowUpRecord> selectRescuerPage(@Param("rescuerId") Long rescuerId,
                                           @Param("animalId") Long animalId,
                                           @Param("limit") int limit,
                                           @Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM follow_up_record f",
            " JOIN adoption_record ar ON ar.id=f.adoption_record_id",
            " JOIN animal a ON a.id=ar.animal_id",
            " JOIN rescue_task t ON t.id=a.rescue_task_id",
            " WHERE t.rescuer_id=#{rescuerId}",
            "<if test='animalId != null'> AND ar.animal_id=#{animalId}</if>",
            "</script>"})
    long countRescuer(@Param("rescuerId") Long rescuerId, @Param("animalId") Long animalId);
}
