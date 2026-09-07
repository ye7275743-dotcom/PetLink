package com.petlink.modules.followup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.followup.entity.FollowUpImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowUpImageMapper extends BaseMapper<FollowUpImage> {
    @Select({"<script>","SELECT * FROM follow_up_image WHERE follow_up_id IN","<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>","ORDER BY follow_up_id,sort_order,id","</script>"})
    List<FollowUpImage> selectByFollowUpIds(@Param("ids") List<Long> ids);

    @Select("SELECT * FROM follow_up_image WHERE follow_up_id=#{followUpId} ORDER BY sort_order ASC,id ASC")
    List<FollowUpImage> selectByFollowUpId(@Param("followUpId") Long followUpId);
}
