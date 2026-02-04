package com.test.dosa_backend.service;

import com.test.dosa_backend.dto.ModelInfoDto;
import com.test.dosa_backend.dto.ModelResponseDto;
import com.test.dosa_backend.dto.NodeInfoDto;
import com.test.dosa_backend.dto.PartInfoDto;
import com.test.dosa_backend.repository.AssemblyNodeRepository;
import com.test.dosa_backend.repository.ModelRepository;
import com.test.dosa_backend.repository.PartRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;
    private final PartRepository partRepository;
    private final AssemblyNodeRepository assemblyNodeRepository;

    @Transactional(readOnly = true)
    public ModelResponseDto getModelDetail(String modelId) {
        ModelInfoDto modelInfo = modelRepository.findById(modelId)
                .map(ModelInfoDto::fromModel)
                .orElseThrow(() -> new EntityNotFoundException("모델 없음"));

        List<PartInfoDto> parts = partRepository.findAllByModelId(modelId).stream()
                .map(PartInfoDto::fromPart)
                .toList();

        List<NodeInfoDto> nodes = assemblyNodeRepository.findAllByModelId(modelId).stream()
                .map(NodeInfoDto::fromNode)
                .toList();

        return ModelResponseDto.of(modelInfo, parts, nodes);
    }

}
