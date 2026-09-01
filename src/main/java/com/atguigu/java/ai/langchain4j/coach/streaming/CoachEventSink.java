package com.atguigu.java.ai.langchain4j.coach.streaming;

import com.atguigu.java.ai.langchain4j.coach.model.CoachScene;

public interface CoachEventSink {
    void metadata(String conversationId, CoachScene scene);

    void token(long sequence, String text);

    void completion(String conversationId);

    void error(String code, boolean retryable);
}
