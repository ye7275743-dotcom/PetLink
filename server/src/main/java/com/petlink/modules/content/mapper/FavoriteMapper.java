package com.petlink.modules.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.content.entity.Favorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
    @Select("SELECT * FROM favorite WHERE user_id=#{userId} AND animal_id=#{animalId}")
    Favorite selectByUserAndAnimal(@Param("userId") Long userId,@Param("animalId") Long animalId);

    @Delete("DELETE FROM favorite WHERE user_id=#{userId} AND animal_id=#{animalId}")
    int deleteByUserAndAnimal(@Param("userId") Long userId,@Param("animalId") Long animalId);

    @Select("SELECT * FROM favorite WHERE user_id=#{userId} ORDER BY created_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Favorite> selectUserPage(@Param("userId") Long userId,@Param("limit") int limit,@Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id=#{userId}")
    long countByUser(@Param("userId") Long userId);
}
