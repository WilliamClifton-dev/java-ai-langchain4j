package com.atguigu.java.ai.langchain4j.profile;

public class ProfileRequiredException extends RuntimeException {

    public ProfileRequiredException() {
        super("A profile is required before safety screening");
    }
}
