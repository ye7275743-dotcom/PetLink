package com.petlink.modules.followup;
import com.petlink.modules.followup.entity.*;
import com.petlink.modules.followup.mapper.FollowUpImageMapper;
import com.petlink.modules.followup.service.FollowUpResponseAssembler;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FollowUpBatchAssemblerTest{
 @Test void oneImageQueryForWholePage(){var mapper=mock(FollowUpImageMapper.class);var assembler=new FollowUpResponseAssembler(mapper);var a=new FollowUpRecord();a.setId(1L);var b=new FollowUpRecord();b.setId(2L);var image=new FollowUpImage();image.setId(9L);image.setFollowUpId(2L);when(mapper.selectByFollowUpIds(List.of(1L,2L))).thenReturn(List.of(image));var rows=assembler.records(List.of(a,b));assertEquals(List.of("1","2"),rows.stream().map(r->r.getId()).toList());assertTrue(rows.get(0).getImages().isEmpty());assertEquals("9",rows.get(1).getImages().get(0).getId());verify(mapper).selectByFollowUpIds(List.of(1L,2L));verifyNoMoreInteractions(mapper);}
 @Test void emptyPageDoesNotQueryImages(){var mapper=mock(FollowUpImageMapper.class);assertTrue(new FollowUpResponseAssembler(mapper).records(List.of()).isEmpty());verifyNoInteractions(mapper);}
}
