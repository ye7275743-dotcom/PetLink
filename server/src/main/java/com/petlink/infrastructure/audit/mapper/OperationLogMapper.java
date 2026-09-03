package com.petlink.infrastructure.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petlink.infrastructure.audit.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
