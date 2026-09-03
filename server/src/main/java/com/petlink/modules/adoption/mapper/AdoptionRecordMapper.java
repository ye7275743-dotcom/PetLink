package com.petlink.modules.adoption.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdoptionRecordMapper extends BaseMapper<AdoptionRecord> {
    @Select("SELECT EXISTS(SELECT 1 FROM adoption_record WHERE animal_id=#{animalId} AND user_id=#{userId})")
    int existsByAnimalAndUser(@Param("animalId") Long animalId, @Param("userId") Long userId);

    @Select("SELECT * FROM adoption_record WHERE animal_id=#{animalId} LIMIT 1")
    AdoptionRecord selectByAnimalId(@Param("animalId") Long animalId);

    @Select("SELECT * FROM adoption_record WHERE user_id=#{userId} ORDER BY adopted_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}")
    List<AdoptionRecord> selectUserPage(@Param("userId") Long userId,
                                        @Param("limit") int limit,
                                        @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM adoption_record WHERE user_id=#{userId}")
    long countUser(@Param("userId") Long userId);
}
