package com.petlink.modules.animal;

import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.HealthRecord;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.HealthRecordMapper;
import com.petlink.modules.animal.service.AnimalResponseAssembler;
import com.petlink.modules.animal.vo.HealthRecordResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnimalResponseAssemblerTest {
    @Test void publicDetailHidesRescueTaskAndRecorderId() {
        AnimalImageMapper images=mock(AnimalImageMapper.class); HealthRecordMapper records=mock(HealthRecordMapper.class);
        AnimalResponseAssembler assembler=new AnimalResponseAssembler(images,records); Animal animal=animal();
        HealthRecord record=record(); when(images.selectByAnimalId(1L)).thenReturn(List.of()); when(records.selectByAnimalId(1L)).thenReturn(List.of(record));
        var detail=assembler.detail(animal,false);
        assertNull(detail.getRescueTaskId()); assertEquals(1,detail.getHealthRecords().size());
        assertFalse(detail.getHealthRecords().get(0) instanceof HealthRecordResponse);
    }
    @Test void privilegedDetailIncludesRescueTaskAndRecorderId() {
        AnimalImageMapper images=mock(AnimalImageMapper.class); HealthRecordMapper records=mock(HealthRecordMapper.class);
        AnimalResponseAssembler assembler=new AnimalResponseAssembler(images,records); Animal animal=animal();
        when(images.selectByAnimalId(1L)).thenReturn(List.of()); when(records.selectByAnimalId(1L)).thenReturn(List.of(record()));
        var detail=assembler.detail(animal,true);
        assertEquals("10",detail.getRescueTaskId()); assertTrue(detail.getHealthRecords().get(0) instanceof HealthRecordResponse);
        assertEquals("100",((HealthRecordResponse)detail.getHealthRecords().get(0)).getRecorderId());
    }
    private Animal animal(){Animal a=new Animal();a.setId(1L);a.setRescueTaskId(10L);a.setName("A");a.setSpecies("CAT");a.setSex("UNKNOWN");a.setHealthCondition("ok");a.setStatus("AVAILABLE");a.setVersion(1);a.setCreatedAt(LocalDateTime.now());a.setUpdatedAt(LocalDateTime.now());return a;}
    private HealthRecord record(){HealthRecord r=new HealthRecord();r.setId(2L);r.setAnimalId(1L);r.setRecorderId(100L);r.setContent("record");r.setCreatedAt(LocalDateTime.now());return r;}
}
