package com.atguigu.java.ai.langchain4j.coach.streaming;

public interface CoachModelListener {
    void onToken(String text);

    void onComplete();

    void onError(Throwable failure);
}
