package com.atguigu.java.ai.langchain4j.assessment;

import java.util.List;

public record SubmitHbtiAssessmentCommand(String definitionVersion, List<HbtiAnswer> answers) {
    public SubmitHbtiAssessmentCommand {
        answers = answers == null ? null : List.copyOf(answers);
    }
}
