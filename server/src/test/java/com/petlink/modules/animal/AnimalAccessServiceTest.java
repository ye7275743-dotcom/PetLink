package com.petlink.modules.animal;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.mapper.RescueTaskMapper;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnimalAccessServiceTest {
    @Test void visitorCanOnlySeeAvailableAnimal() {
        AnimalMapper animals=mock(AnimalMapper.class); RescueTaskMapper tasks=mock(RescueTaskMapper.class);
        AnimalAccessService service=new AnimalAccessService(animals,tasks);
        Animal available=animal(1L,"AVAILABLE",10L); Animal treating=animal(2L,"TREATING",10L);
        assertTrue(service.isVisible(null,available)); assertFalse(service.isVisible(null,treating));
    }
    @Test void responsibleRescuerCanSeeNonPublicAnimal() {
        RescueTaskMapper tasks=mock(RescueTaskMapper.class); AnimalAccessService service=new AnimalAccessService(mock(AnimalMapper.class),tasks);
        RescueTask task=new RescueTask(); task.setId(10L); task.setRescuerId(100L); when(tasks.selectById(10L)).thenReturn(task);
        UserPrincipal rescuer=new UserPrincipal(100L,"RESCUER");
        assertTrue(service.isVisible(rescuer,animal(2L,"TREATING",10L)));
        assertTrue(service.isPrivileged(rescuer,animal(2L,"TREATING",10L)));
    }
    @Test void otherRescuerCannotSeeNonPublicAnimal() {
        RescueTaskMapper tasks=mock(RescueTaskMapper.class); AnimalAccessService service=new AnimalAccessService(mock(AnimalMapper.class),tasks);
        RescueTask task=new RescueTask(); task.setId(10L); task.setRescuerId(200L); when(tasks.selectById(10L)).thenReturn(task);
        assertFalse(service.isVisible(new UserPrincipal(100L,"RESCUER"),animal(2L,"TREATING",10L)));
    }
    @Test void adminCanSeeAnyAnimal() {
        AnimalAccessService service=new AnimalAccessService(mock(AnimalMapper.class),mock(RescueTaskMapper.class));
        assertTrue(service.isVisible(new UserPrincipal(1L,"ADMIN"),animal(2L,"ADOPTED",10L)));
    }
    @Test void hiddenResourceUses404() {
        AnimalMapper animals=mock(AnimalMapper.class); when(animals.selectById(2L)).thenReturn(animal(2L,"TREATING",10L));
        AnimalAccessService service=new AnimalAccessService(animals,mock(RescueTaskMapper.class));
        BusinessException ex=assertThrows(BusinessException.class,()->service.requireVisible(new UserPrincipal(100L,"USER"),2L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,ex.getErrorCode());
    }
    private Animal animal(Long id,String status,Long taskId){Animal a=new Animal();a.setId(id);a.setStatus(status);a.setRescueTaskId(taskId);return a;}
}
