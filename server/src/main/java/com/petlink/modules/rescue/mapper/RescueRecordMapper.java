package com.petlink.modules.rescue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.rescue.entity.RescueRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RescueRecordMapper extends BaseMapper<RescueRecord> {
    @Select("SELECT * FROM rescue_record WHERE task_id=#{taskId} ORDER BY created_at ASC, id ASC")
    List<RescueRecord> selectByTaskId(@Param("taskId") Long taskId);
}
