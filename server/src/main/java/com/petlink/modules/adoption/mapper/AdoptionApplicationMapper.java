package com.petlink.modules.adoption.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.adoption.entity.AdoptionApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdoptionApplicationMapper extends BaseMapper<AdoptionApplication> {
    @Select("SELECT animal_id FROM adoption_application WHERE id=#{id}")
    Long selectAnimalIdById(@Param("id") Long id);

    @Select("SELECT * FROM adoption_application WHERE id=#{id} FOR UPDATE")
    AdoptionApplication selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM adoption_application WHERE animal_id=#{animalId} AND status='PENDING' AND id<>#{excludeId} ORDER BY id ASC FOR UPDATE")
    List<AdoptionApplication> selectOtherPendingForUpdate(@Param("animalId") Long animalId,@Param("excludeId") Long excludeId);

    @Select({"<script>",
            "SELECT * FROM adoption_application WHERE user_id=#{userId}",
            "<if test='status != null'> AND status=#{status}</if>",
            " ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<AdoptionApplication> selectUserPage(@Param("userId") Long userId,@Param("status") String status,
                                             @Param("limit") int limit,@Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM adoption_application WHERE user_id=#{userId}",
            "<if test='status != null'> AND status=#{status}</if>",
            "</script>"})
    long countUser(@Param("userId") Long userId,@Param("status") String status);

    @Select({"<script>",
            "SELECT * FROM adoption_application WHERE status=#{status}",
            "<if test='animalId != null'> AND animal_id=#{animalId}</if>",
            "<if test='userId != null'> AND user_id=#{userId}</if>",
            " ORDER BY created_at ASC,id ASC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<AdoptionApplication> selectAdminPage(@Param("status") String status,
                                               @Param("animalId") Long animalId,
                                               @Param("userId") Long userId,
                                               @Param("limit") int limit,
                                               @Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM adoption_application WHERE status=#{status}",
            "<if test='animalId != null'> AND animal_id=#{animalId}</if>",
            "<if test='userId != null'> AND user_id=#{userId}</if>",
            "</script>"})
    long countAdmin(@Param("status") String status,
                    @Param("animalId") Long animalId,
                    @Param("userId") Long userId);

    @Update("UPDATE adoption_application SET status='WITHDRAWN',updated_at=#{now} WHERE id=#{id} AND user_id=#{userId} AND status='PENDING'")
    int withdraw(@Param("id") Long id,@Param("userId") Long userId,@Param("now") LocalDateTime now);

    @Update("UPDATE adoption_application SET status='REJECTED',reviewer_id=#{reviewerId},reviewed_at=#{now},reject_reason=#{reason},updated_at=#{now} WHERE id=#{id} AND status='PENDING'")
    int rejectPending(@Param("id") Long id,@Param("reviewerId") Long reviewerId,@Param("reason") String reason,@Param("now") LocalDateTime now);

    @Update("UPDATE adoption_application SET status='APPROVED',reviewer_id=#{reviewerId},reviewed_at=#{now},reject_reason=NULL,updated_at=#{now} WHERE id=#{id} AND status='PENDING'")
    int approvePending(@Param("id") Long id,@Param("reviewerId") Long reviewerId,@Param("now") LocalDateTime now);

    @Update("UPDATE adoption_application SET status='INVALIDATED',reviewer_id=NULL,reviewed_at=NULL,reject_reason=NULL,updated_at=#{now} WHERE id=#{id} AND status='PENDING'")
    int invalidatePending(@Param("id") Long id,@Param("now") LocalDateTime now);

    @Select("SELECT COUNT(*) FROM adoption_application WHERE animal_id=#{animalId} AND status='PENDING'")
    long countPendingByAnimal(@Param("animalId") Long animalId);

    @Select("SELECT * FROM adoption_application WHERE animal_id=#{animalId} AND status='APPROVED' ORDER BY id ASC LIMIT 1")
    AdoptionApplication selectApprovedByAnimal(@Param("animalId") Long animalId);
}
