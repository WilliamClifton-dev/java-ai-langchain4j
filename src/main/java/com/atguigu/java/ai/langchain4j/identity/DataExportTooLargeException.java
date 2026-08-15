package com.atguigu.java.ai.langchain4j.identity;

public class DataExportTooLargeException extends RuntimeException {
    public DataExportTooLargeException() {
        super("Account data export exceeds the bounded limit");
    }
}
