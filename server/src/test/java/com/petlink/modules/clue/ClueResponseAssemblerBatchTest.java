package com.petlink.modules.clue;

import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.entity.RescueClueImage;
import com.petlink.modules.clue.mapper.RescueClueImageMapper;
import com.petlink.modules.clue.service.ClueResponseAssembler;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class ClueResponseAssemblerBatchTest {
    @Test void pageSummariesLoadCoversInOneQuery() {
        RescueClueImageMapper images=mock(RescueClueImageMapper.class);
        ClueResponseAssembler assembler=new ClueResponseAssembler(images);
        RescueClue first=clue(1L); RescueClue second=clue(2L);
        RescueClueImage cover=new RescueClueImage(); cover.setId(9L); cover.setClueId(2L); cover.setSortOrder(1);
        when(images.selectCovers(List.of(1L,2L))).thenReturn(List.of(cover));
        var result=assembler.summaries(List.of(first,second));
        assertEquals(2,result.size()); assertNull(result.get(0).getCoverImageUrl()); assertEquals("/api/media/rescue-clue-images/9",result.get(1).getCoverImageUrl());
        verify(images).selectCovers(List.of(1L,2L)); verify(images,never()).selectCover(anyLong());
    }

    private RescueClue clue(Long id) {
        RescueClue clue=new RescueClue(); clue.setId(id); clue.setLocation("地点"); clue.setAnimalDescription("描述"); clue.setStatus("PENDING_REVIEW");
        clue.setFoundTime(LocalDateTime.now()); clue.setCreatedAt(LocalDateTime.now()); clue.setUpdatedAt(LocalDateTime.now()); return clue;
    }
}
