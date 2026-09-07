package com.petlink.modules.admin.mapper;

import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.auth.entity.SysUser;
import com.petlink.modules.rescue.entity.RescueTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminMapper {
    @Update("UPDATE sys_user SET deleted=1,status='DISABLED',updated_at=#{now} WHERE id=#{id} AND deleted=0 AND role_code!='ADMIN'")
    int softDeleteUser(@Param("id") Long id,@Param("now") LocalDateTime now);

    @Select("SELECT * FROM sys_user WHERE deleted=0 AND id=#{id} FOR UPDATE")
    SysUser lockUser(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM rescue_task WHERE rescuer_id=#{id} AND status IN ('WAITING_START','IN_PROGRESS')")
    long countActiveTasks(@Param("id") Long id);

    @Update("UPDATE sys_user SET role_code=#{role},updated_at=#{now} WHERE deleted=0 AND id=#{id} AND role_code=#{previous}")
    int changeRole(@Param("id") Long id,@Param("previous") String previous,@Param("role") String role,@Param("now") LocalDateTime now);

    @Select({"<script>",
            "SELECT * FROM sys_user WHERE deleted=0 AND 1=1",
            "<if test='roleCode != null'> AND role_code=#{roleCode}</if>",
            "<if test='status != null'> AND status=#{status}</if>",
            "<if test='keyword != null'> AND (account LIKE CONCAT('%',#{keyword},'%') OR nickname LIKE CONCAT('%',#{keyword},'%'))</if>",
            " ORDER BY created_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<SysUser> selectUsersPage(@Param("roleCode") String roleCode,@Param("status") String status,
                                  @Param("keyword") String keyword,@Param("limit") int limit,@Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM sys_user WHERE deleted=0 AND 1=1",
            "<if test='roleCode != null'> AND role_code=#{roleCode}</if>",
            "<if test='status != null'> AND status=#{status}</if>",
            "<if test='keyword != null'> AND (account LIKE CONCAT('%',#{keyword},'%') OR nickname LIKE CONCAT('%',#{keyword},'%'))</if>",
            "</script>"})
    long countUsers(@Param("roleCode") String roleCode,@Param("status") String status,@Param("keyword") String keyword);

    @Select("SELECT * FROM sys_user WHERE deleted=0 AND id=#{id}")
    SysUser selectUser(@Param("id") Long id);

    @Update("UPDATE sys_user SET status='ENABLED',updated_at=#{now} WHERE deleted=0 AND id=#{id} AND status='DISABLED'")
    int enableUser(@Param("id") Long id,@Param("now") LocalDateTime now);

    @Update("UPDATE sys_user SET status='DISABLED',updated_at=#{now} WHERE deleted=0 AND id=#{id} AND status='ENABLED'")
    int disableUser(@Param("id") Long id,@Param("now") LocalDateTime now);

    @Update("UPDATE sys_user SET role_code='RESCUER',updated_at=#{now} WHERE deleted=0 AND id=#{id} AND role_code='USER'")
    int promoteRescuer(@Param("id") Long id,@Param("now") LocalDateTime now);

    @Select("SELECT COUNT(*) FROM rescue_clue WHERE publisher_id=#{userId}")
    long countPublishedClues(@Param("userId") Long userId);
    @Select("SELECT COUNT(*) FROM rescue_task WHERE rescuer_id=#{userId}")
    long countRescueTasks(@Param("userId") Long userId);
    @Select("SELECT COUNT(*) FROM adoption_application WHERE user_id=#{userId}")
    long countAdoptionApplications(@Param("userId") Long userId);
    @Select("SELECT COUNT(*) FROM adoption_record WHERE user_id=#{userId}")
    long countAdoptionRecordsByUser(@Param("userId") Long userId);

    @Select({"<script>",
            "SELECT * FROM rescue_task WHERE 1=1",
            "<if test='status != null'> AND status=#{status}</if>",
            "<if test='rescuerId != null'> AND rescuer_id=#{rescuerId}</if>",
            "<if test='clueId != null'> AND clue_id=#{clueId}</if>",
            " ORDER BY created_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<RescueTask> selectTasksPage(@Param("status") String status,@Param("rescuerId") Long rescuerId,
                                     @Param("clueId") Long clueId,@Param("limit") int limit,@Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM rescue_task WHERE 1=1",
            "<if test='status != null'> AND status=#{status}</if>",
            "<if test='rescuerId != null'> AND rescuer_id=#{rescuerId}</if>",
            "<if test='clueId != null'> AND clue_id=#{clueId}</if>",
            "</script>"})
    long countTasks(@Param("status") String status,@Param("rescuerId") Long rescuerId,@Param("clueId") Long clueId);

    @Select({"<script>",
            "SELECT * FROM animal WHERE 1=1",
            "<if test='status != null'> AND status=#{status}</if>",
            "<if test='species != null'> AND species=#{species}</if>",
            "<if test='rescueTaskId != null'> AND rescue_task_id=#{rescueTaskId}</if>",
            " ORDER BY created_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<Animal> selectAnimalsPage(@Param("status") String status,@Param("species") String species,
                                   @Param("rescueTaskId") Long rescueTaskId,@Param("limit") int limit,@Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM animal WHERE 1=1",
            "<if test='status != null'> AND status=#{status}</if>",
            "<if test='species != null'> AND species=#{species}</if>",
            "<if test='rescueTaskId != null'> AND rescue_task_id=#{rescueTaskId}</if>",
            "</script>"})
    long countAnimals(@Param("status") String status,@Param("species") String species,@Param("rescueTaskId") Long rescueTaskId);

    @Select({"<script>",
            "SELECT * FROM adoption_record WHERE 1=1",
            "<if test='userId != null'> AND user_id=#{userId}</if>",
            "<if test='animalId != null'> AND animal_id=#{animalId}</if>",
            "<if test='applicationId != null'> AND application_id=#{applicationId}</if>",
            " ORDER BY adopted_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<AdoptionRecord> selectAdoptionRecordsPage(@Param("userId") Long userId,@Param("animalId") Long animalId,
                                                   @Param("applicationId") Long applicationId,@Param("limit") int limit,@Param("offset") long offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM adoption_record WHERE 1=1",
            "<if test='userId != null'> AND user_id=#{userId}</if>",
            "<if test='animalId != null'> AND animal_id=#{animalId}</if>",
            "<if test='applicationId != null'> AND application_id=#{applicationId}</if>",
            "</script>"})
    long countAdoptionRecords(@Param("userId") Long userId,@Param("animalId") Long animalId,@Param("applicationId") Long applicationId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted=0") long countAllUsers();
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted=0 AND status=#{status}") long countUsersByStatus(@Param("status") String status);
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted=0 AND role_code=#{roleCode}") long countUsersByRole(@Param("roleCode") String roleCode);
    @Select("SELECT status,COUNT(*) AS count FROM rescue_clue GROUP BY status") List<StatusCountRow> countClueStatuses();
    @Select("SELECT status,COUNT(*) AS count FROM rescue_task GROUP BY status") List<StatusCountRow> countTaskStatuses();
    @Select("SELECT status,COUNT(*) AS count FROM animal GROUP BY status") List<StatusCountRow> countAnimalStatuses();
    @Select("SELECT status,COUNT(*) AS count FROM adoption_application GROUP BY status") List<StatusCountRow> countApplicationStatuses();
    @Select("SELECT COUNT(*) FROM adoption_record") long countAllAdoptionRecords();
    @Select("SELECT COUNT(*) FROM follow_up_record") long countAllFollowUps();

    @Select("SELECT DATE_FORMAT(finished_at,'%Y-%m-%d') AS date,COUNT(*) AS count FROM rescue_task WHERE status='SUCCESS' AND finished_at>=#{from} AND finished_at<#{toExclusive} GROUP BY DATE_FORMAT(finished_at,'%Y-%m-%d') ORDER BY DATE_FORMAT(finished_at,'%Y-%m-%d')")
    List<TrendCountRow> countRescueSuccessByDay(@Param("from") LocalDateTime from,@Param("toExclusive") LocalDateTime toExclusive);

    @Select("SELECT DATE_FORMAT(adopted_at,'%Y-%m-%d') AS date,COUNT(*) AS count FROM adoption_record WHERE adopted_at>=#{from} AND adopted_at<#{toExclusive} GROUP BY DATE_FORMAT(adopted_at,'%Y-%m-%d') ORDER BY DATE_FORMAT(adopted_at,'%Y-%m-%d')")
    List<TrendCountRow> countAdoptionsByDay(@Param("from") LocalDateTime from,@Param("toExclusive") LocalDateTime toExclusive);
}
