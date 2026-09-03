package com.petlink.modules.animal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.animal.entity.HealthRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {
    @Select("SELECT * FROM health_record WHERE animal_id=#{animalId} ORDER BY created_at ASC, id ASC")
    List<HealthRecord> selectByAnimalId(@Param("animalId") Long animalId);
}
