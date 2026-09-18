package com.atguigu.java.ai.langchain4j.assessment;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class HbtiDefinitionRepository implements HbtiDefinitionCatalog {

    private final HbtiDefinitionMapper mapper;

    public HbtiDefinitionRepository(HbtiDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<HbtiDefinition> findPublished(String assessmentKey, String version) {
        if (assessmentKey == null || assessmentKey.isBlank() || version == null || version.isBlank()) {
            return Optional.empty();
        }
        return mapper.findPublishedMetadata(assessmentKey, version)
                .map(metadata -> HbtiDefinition.from(
                        metadata,
                        mapper.findDimensions(metadata.id()),
                        mapper.findItems(metadata.id())
                ));
    }
}
