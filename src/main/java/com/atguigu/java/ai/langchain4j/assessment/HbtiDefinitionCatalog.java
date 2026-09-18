package com.atguigu.java.ai.langchain4j.assessment;

import java.util.Optional;

public interface HbtiDefinitionCatalog {

    Optional<HbtiDefinition> findPublished(String assessmentKey, String version);
}
