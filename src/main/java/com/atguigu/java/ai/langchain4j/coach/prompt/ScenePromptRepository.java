package com.atguigu.java.ai.langchain4j.coach.prompt;

import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Component
public class ScenePromptRepository {

    private final Map<CoachScene, String> prompts = new EnumMap<>(CoachScene.class);

    public ScenePromptRepository() {
        for (CoachScene scene : CoachScene.values()) {
            prompts.put(scene, readNonBlank(scene.resourcePath()));
        }
    }

    public String get(CoachScene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("Coach scene is required");
        }
        return prompts.get(scene);
    }

    private String readNonBlank(String path) {
        ClassPathResource resource = new ClassPathResource(path);

        try (InputStream input = resource.getInputStream()) {
            String prompt = StreamUtils.copyToString(input, StandardCharsets.UTF_8).trim();
            if (prompt.isEmpty()) {
                throw new IllegalStateException("Scene prompt must not be empty: " + path);
            }
            return prompt;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load scene prompt: " + path, exception);
        }
    }
}
