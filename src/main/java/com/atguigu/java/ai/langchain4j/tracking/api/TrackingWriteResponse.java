package com.atguigu.java.ai.langchain4j.tracking.api;

import com.atguigu.java.ai.langchain4j.tracking.TrackingWrite;

public record TrackingWriteResponse<T>(T record, boolean replayed) {
    static <T> TrackingWriteResponse<T> from(TrackingWrite<T> write) {
        return new TrackingWriteResponse<>(write.record(), write.replayed());
    }
}
