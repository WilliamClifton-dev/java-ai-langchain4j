package com.atguigu.java.ai.langchain4j.tracking;

public record TrackingWrite<T>(T record, boolean replayed) { }
