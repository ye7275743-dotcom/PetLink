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

    @Select("SELECT COALESCE(MAX(sort_order),0) FROM animal_image WHERE animal_id=#{animalId}")
    int maxSortOrder(@Param("animalId") Long animalId);

    @Select("SELECT * FROM animal_image WHERE animal_id=#{animalId} AND sort_order>#{sortOrder} ORDER BY sort_order ASC, id ASC")
    List<AnimalImage> selectAfter(@Param("animalId") Long animalId, @Param("sortOrder") Integer sortOrder);

    @Update("UPDATE animal_image SET sort_order=#{newOrder} WHERE id=#{id} AND sort_order=#{oldOrder}")
    int moveSortOrder(@Param("id") Long id, @Param("oldOrder") Integer oldOrder, @Param("newOrder") Integer newOrder);
}
