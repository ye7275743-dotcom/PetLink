package com.petlink.modules.animal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.modules.animal.entity.Animal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AnimalMapper extends BaseMapper<Animal> {
    @Select("SELECT * FROM animal WHERE id=#{id} FOR UPDATE")
    Animal selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM animal WHERE id=#{id} FOR SHARE")
    Animal selectForShare(@Param("id") Long id);

    @Select("SELECT * FROM animal WHERE rescue_task_id=#{taskId} ORDER BY created_at ASC, id ASC")
    List<Animal> selectByRescueTaskId(@Param("taskId") Long taskId);

    @Select({"<script>",
            "SELECT * FROM animal WHERE status='AVAILABLE'",
            "<if test='species != null'> AND species=#{species}</if>",
            "<if test='sex != null'> AND sex=#{sex}</if>",
            " ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<Animal> selectPublicPage(@Param("species") String species, @Param("sex") String sex,
                                  @Param("limit") int limit, @Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM animal WHERE status='AVAILABLE'",
            "<if test='species != null'> AND species=#{species}</if>",
            "<if test='sex != null'> AND sex=#{sex}</if>",
            "</script>"})
    long countPublic(@Param("species") String species, @Param("sex") String sex);

    @Select({"<script>",
            "SELECT a.* FROM animal a JOIN rescue_task t ON t.id=a.rescue_task_id",
            " WHERE t.rescuer_id=#{rescuerId}",
            "<if test='status != null'> AND a.status=#{status}</if>",
            " ORDER BY a.created_at DESC, a.id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<Animal> selectResponsiblePage(@Param("rescuerId") Long rescuerId, @Param("status") String status,
                                       @Param("limit") int limit, @Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM animal a JOIN rescue_task t ON t.id=a.rescue_task_id",
            " WHERE t.rescuer_id=#{rescuerId}",
            "<if test='status != null'> AND a.status=#{status}</if>",
            "</script>"})
    long countResponsible(@Param("rescuerId") Long rescuerId, @Param("status") String status);

    @Update("UPDATE animal SET status='ADOPTED',suspend_reason=NULL,version=version+1,updated_at=#{now} WHERE id=#{id} AND status='AVAILABLE'")
    int adoptAvailable(@Param("id") Long id, @Param("now") LocalDateTime now);
}
