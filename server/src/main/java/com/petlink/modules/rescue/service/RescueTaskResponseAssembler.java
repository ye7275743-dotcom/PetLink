package com.petlink.modules.rescue.service;

import com.petlink.common.TimeUtils;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.service.ClueResponseAssembler;
import com.petlink.modules.rescue.entity.RescueRecord;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.mapper.RescueRecordMapper;
import com.petlink.modules.rescue.vo.AnimalSummaryResponse;
import com.petlink.modules.rescue.vo.RescueRecordResponse;
import com.petlink.modules.rescue.vo.TaskDetailResponse;
import com.petlink.modules.rescue.vo.TaskSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RescueTaskResponseAssembler {
    private final ClueResponseAssembler clueAssembler;
    private final RescueRecordMapper recordMapper;
    private final AnimalMapper animalMapper;
    public RescueTaskResponseAssembler(ClueResponseAssembler clueAssembler, RescueRecordMapper recordMapper, AnimalMapper animalMapper) {
        this.clueAssembler=clueAssembler; this.recordMapper=recordMapper; this.animalMapper=animalMapper;
    }
    public TaskSummaryResponse summary(RescueTask task) {
        return new TaskSummaryResponse(String.valueOf(task.getId()), String.valueOf(task.getClueId()), task.getStatus(),
                TimeUtils.toOffset(task.getStartedAt()), TimeUtils.toOffset(task.getFinishedAt()),
                TimeUtils.toOffset(task.getCreatedAt()), TimeUtils.toOffset(task.getUpdatedAt()));
    }
    public RescueRecordResponse record(RescueRecord record) {
        return new RescueRecordResponse(String.valueOf(record.getId()), String.valueOf(record.getTaskId()), record.getContent(),
                TimeUtils.toOffset(record.getCreatedAt()));
    }
    public TaskDetailResponse detail(RescueTask task, RescueClue clue) {
        List<RescueRecordResponse> records=recordMapper.selectByTaskId(task.getId()).stream().map(this::record).collect(Collectors.toList());
        List<AnimalSummaryResponse> animals=animalMapper.selectByRescueTaskId(task.getId()).stream().map(this::animal).collect(Collectors.toList());
        TaskSummaryResponse base=summary(task);
        return new TaskDetailResponse(base.getId(), base.getClueId(), base.getStatus(), base.getStartedAt(), base.getFinishedAt(),
                base.getCreatedAt(), base.getUpdatedAt(), clueAssembler.summary(clue), task.getFailureReason(), task.getCancelReason(), records, animals);
    }
    private AnimalSummaryResponse animal(Animal animal) {
        return new AnimalSummaryResponse(String.valueOf(animal.getId()), animal.getName(), animal.getSpecies(), animal.getSex(), animal.getStatus());
    }
}
