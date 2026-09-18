package com.atguigu.java.ai.langchain4j.coach.model;

public enum CoachScene {

    GENERAL_CHAT("prompts/hbti/scenes/general-chat.txt"),
    PLAN_GENERATION("prompts/hbti/scenes/plan-generation.txt"),
    DAILY_CHECKIN("prompts/hbti/scenes/daily-checkin.txt"),
    WEEKLY_REVIEW("prompts/hbti/scenes/weekly-review.txt"),
    HBTI_INTERPRETATION("prompts/hbti/scenes/hbti-interpretation.txt"),
    SAFETY_SCREENING("prompts/hbti/scenes/safety-screening.txt");

    private final String resourcePath;

    CoachScene(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String resourcePath() {
        return resourcePath;
    }
}
