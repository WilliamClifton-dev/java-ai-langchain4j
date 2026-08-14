package com.atguigu.java.ai.langchain4j.coach.api;

import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatResult;
import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coach/messages")
public class CoachController {

    private final CoachChatService coachChatService;

    public CoachController(CoachChatService coachChatService) {
        this.coachChatService = coachChatService;
    }

    @PostMapping
    public CoachChatResult chat(@Valid @RequestBody CoachChatRequest request) {
        return coachChatService.chat(new CoachChatCommand(
                request.conversationId(),
                request.scene(),
                request.message()
        ));
    }
}
