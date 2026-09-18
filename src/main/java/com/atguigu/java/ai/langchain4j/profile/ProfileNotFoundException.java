package com.atguigu.java.ai.langchain4j.profile;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException() {
        super("Profile was not found");
    }
}
