package com.petlink.modules.rescue;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.mapper.RescueClueMapper;
import com.petlink.modules.clue.service.ClueResponseAssembler;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.mapper.RescueRecordMapper;
import com.petlink.modules.rescue.mapper.RescueTaskMapper;
import com.petlink.modules.rescue.service.RescueTaskQueryService;
import com.petlink.modules.rescue.service.RescueTaskResponseAssembler;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RescueTaskQueryServiceTest {
    @Test void rescuerCannotSeeOtherRescuersTask(){
        RescueTaskMapper taskMapper=mock(RescueTaskMapper.class); RescueTask t=new RescueTask(); t.setId(4001L); t.setRescuerId(2002L); when(taskMapper.selectById(4001L)).thenReturn(t);
        RescueTaskQueryService service=new RescueTaskQueryService(mock(RescueClueMapper.class),taskMapper,mock(RescueRecordMapper.class),mock(ClueResponseAssembler.class),mock(RescueTaskResponseAssembler.class));
        BusinessException ex=assertThrows(BusinessException.class,()->service.detail(new UserPrincipal(2001L,"RESCUER"),4001L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,ex.getErrorCode());
    }

    @Test void adminMaySeeAnyTask(){
        RescueTaskMapper taskMapper=mock(RescueTaskMapper.class); RescueClueMapper clueMapper=mock(RescueClueMapper.class); RescueTaskResponseAssembler assembler=mock(RescueTaskResponseAssembler.class);
        RescueTask t=new RescueTask(); t.setId(4001L); t.setClueId(3001L); t.setRescuerId(2002L); when(taskMapper.selectById(4001L)).thenReturn(t); RescueClue c=new RescueClue(); c.setId(3001L); when(clueMapper.selectById(3001L)).thenReturn(c);
        RescueTaskQueryService service=new RescueTaskQueryService(clueMapper,taskMapper,mock(RescueRecordMapper.class),mock(ClueResponseAssembler.class),assembler);
        service.detail(new UserPrincipal(1L,"ADMIN"),4001L); verify(assembler).detail(t,c);
    }
}
