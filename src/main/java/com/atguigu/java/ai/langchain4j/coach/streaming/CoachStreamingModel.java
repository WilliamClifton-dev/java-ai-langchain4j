package com.atguigu.java.ai.langchain4j.coach.streaming;

public interface CoachStreamingModel {
    CoachStreamHandle start(CoachModelRequest request, CoachModelListener listener);
}
