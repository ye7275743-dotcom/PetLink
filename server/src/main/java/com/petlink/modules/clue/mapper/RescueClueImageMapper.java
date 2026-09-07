package com.petlink.modules.clue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.clue.entity.RescueClueImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RescueClueImageMapper extends BaseMapper<RescueClueImage> {
    @Select("SELECT * FROM rescue_clue_image WHERE clue_id=#{clueId} ORDER BY sort_order ASC, id ASC")
    List<RescueClueImage> selectByClueId(@Param("clueId") Long clueId);

    @Select("SELECT * FROM rescue_clue_image WHERE clue_id=#{clueId} ORDER BY sort_order ASC, id ASC LIMIT 1")
    RescueClueImage selectCover(@Param("clueId") Long clueId);

    @Select({"<script>",
            "SELECT image.* FROM rescue_clue_image image",
            "WHERE image.clue_id IN",
            "<foreach collection='clueIds' item='clueId' open='(' separator=',' close=')'>#{clueId}</foreach>",
            "AND NOT EXISTS (",
            " SELECT 1 FROM rescue_clue_image earlier",
            " WHERE earlier.clue_id=image.clue_id",
            " AND (earlier.sort_order&lt;image.sort_order OR (earlier.sort_order=image.sort_order AND earlier.id&lt;image.id))",
            ") ORDER BY image.clue_id ASC",
            "</script>"})
    List<RescueClueImage> selectCovers(@Param("clueIds") List<Long> clueIds);

    @Select("SELECT COUNT(*) FROM rescue_clue_image WHERE clue_id=#{clueId}")
    int countByClueId(@Param("clueId") Long clueId);

    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM rescue_clue_image WHERE clue_id=#{clueId}")
    int selectMaxSortOrder(@Param("clueId") Long clueId);

    @Select("SELECT * FROM rescue_clue_image WHERE clue_id=#{clueId} AND id=#{imageId} LIMIT 1")
    RescueClueImage selectOwnedImage(@Param("clueId") Long clueId, @Param("imageId") Long imageId);

    @Select("SELECT * FROM rescue_clue_image WHERE clue_id=#{clueId} AND sort_order > #{sortOrder} ORDER BY sort_order ASC, id ASC")
    List<RescueClueImage> selectAfter(@Param("clueId") Long clueId, @Param("sortOrder") int sortOrder);

    @Update("UPDATE rescue_clue_image SET sort_order = sort_order - 1 WHERE id=#{id}")
    int shiftOneForward(@Param("id") Long id);
}
