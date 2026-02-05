package com.test.dosa_backend.dto;

import com.test.dosa_backend.domain.Model;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModelSliceDto {

    private List<ModelSummaryDto> models;
    private boolean hasNext;       // 다음 페이지가 있는지 여부
    private int pageNumber;        // 현재 페이지 번호

    public ModelSliceDto(Slice<Model> slice) {
        // Model 엔티티를 ModelDto로 변환
        this.models = slice.getContent().stream()
                .map(a -> new ModelSummaryDto(a.getModelId(), a.getOverview()))
                .collect(Collectors.toList());

        this.hasNext = slice.hasNext();
        this.pageNumber = slice.getNumber();
    }
}
