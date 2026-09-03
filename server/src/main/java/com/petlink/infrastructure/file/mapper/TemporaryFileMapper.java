package com.petlink.infrastructure.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.infrastructure.file.entity.TemporaryFile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TemporaryFileMapper extends BaseMapper<TemporaryFile> {
    @Select({"<script>",
            "SELECT * FROM temporary_file WHERE token IN",
            "<foreach collection='tokens' item='token' open='(' separator=',' close=')'>#{token}</foreach>",
            " ORDER BY id ASC FOR UPDATE",
            "</script>"})
    List<TemporaryFile> selectForUpdateByTokens(@Param("tokens") List<String> tokens);

    @Update("UPDATE temporary_file SET status='BOUND', business_type=#{businessType}, business_id=#{businessId}, " +
            "formal_path=#{formalPath}, bound_at=#{boundAt} " +
            "WHERE id=#{id} AND owner_id=#{ownerId} AND status='UPLOADED' " +
            "AND business_type IS NULL AND business_id IS NULL AND formal_path IS NULL AND bound_at IS NULL " +
            "AND expires_at > #{boundAt}")
    int bindUploaded(@Param("id") Long id,
                     @Param("ownerId") Long ownerId,
                     @Param("businessType") String businessType,
                     @Param("businessId") Long businessId,
                     @Param("formalPath") String formalPath,
                     @Param("boundAt") LocalDateTime boundAt);

    @Delete("DELETE FROM temporary_file WHERE id=#{id} AND status='BOUND' AND business_type=#{businessType} " +
            "AND business_id=#{businessId} AND formal_path=#{formalPath}")
    int deleteBoundRecord(@Param("id") Long id,
                          @Param("businessType") String businessType,
                          @Param("businessId") Long businessId,
                          @Param("formalPath") String formalPath);
}
