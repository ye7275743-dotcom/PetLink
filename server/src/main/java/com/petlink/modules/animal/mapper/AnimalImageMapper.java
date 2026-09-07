package com.petlink.modules.animal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.animal.entity.AnimalImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AnimalImageMapper extends BaseMapper<AnimalImage> {
    @Select("SELECT * FROM animal_image WHERE animal_id=#{animalId} ORDER BY sort_order ASC, id ASC")
    List<AnimalImage> selectByAnimalId(@Param("animalId") Long animalId);

    @Select("SELECT * FROM animal_image WHERE animal_id=#{animalId} ORDER BY sort_order ASC, id ASC LIMIT 1")
    AnimalImage selectCover(@Param("animalId") Long animalId);

    @Select({"<script>",
            "SELECT ai.* FROM animal_image ai",
            "WHERE ai.animal_id IN",
            "<foreach collection='animalIds' item='animalId' open='(' separator=',' close=')'>#{animalId}</foreach>",
            "AND NOT EXISTS (",
            " SELECT 1 FROM animal_image earlier",
            " WHERE earlier.animal_id=ai.animal_id",
            " AND (earlier.sort_order&lt;ai.sort_order OR (earlier.sort_order=ai.sort_order AND earlier.id&lt;ai.id))",
            ") ORDER BY ai.animal_id ASC",
            "</script>"})
    List<AnimalImage> selectCovers(@Param("animalIds") List<Long> animalIds);

    @Select("SELECT COALESCE(MAX(sort_order),0) FROM animal_image WHERE animal_id=#{animalId}")
    int maxSortOrder(@Param("animalId") Long animalId);

    @Select("SELECT * FROM animal_image WHERE animal_id=#{animalId} AND sort_order>#{sortOrder} ORDER BY sort_order ASC, id ASC")
    List<AnimalImage> selectAfter(@Param("animalId") Long animalId, @Param("sortOrder") Integer sortOrder);

    @Update("UPDATE animal_image SET sort_order=#{newOrder} WHERE id=#{id} AND sort_order=#{oldOrder}")
    int moveSortOrder(@Param("id") Long id, @Param("oldOrder") Integer oldOrder, @Param("newOrder") Integer newOrder);
}
