package com.petlink.modules.rescue.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("rescue_task")
public class RescueTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clueId;
    private Long rescuerId;
    private String status;
    @TableField(value = "active_clue_id", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long activeClueId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String failureReason;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClueId() { return clueId; }
    public void setClueId(Long clueId) { this.clueId = clueId; }
    public Long getRescuerId() { return rescuerId; }
    public void setRescuerId(Long rescuerId) { this.rescuerId = rescuerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getActiveClueId() { return activeClueId; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
