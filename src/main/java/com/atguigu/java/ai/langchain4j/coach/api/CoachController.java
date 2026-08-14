package com.atguigu.java.ai.langchain4j.coach.api;

import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatCommand;
import com.atguigu.java.ai.langchain4j.coach.dto.CoachChatResult;
import com.atguigu.java.ai.langchain4j.coach.service.CoachChatService;
import com.atguigu.java.ai.langchain4j.coach.streaming.CoachStreamingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/coach/messages")
public class CoachController {

    private final CoachChatService coachChatService;
    private final CoachStreamingService coachStreamingService;

    public CoachController(CoachChatService coachChatService,
                           CoachStreamingService coachStreamingService) {
        this.coachChatService = coachChatService;
        this.coachStreamingService = coachStreamingService;
    }

    @PostMapping
    public CoachChatResult chat(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody CoachChatRequest request) {
        return coachChatService.chat(new CoachChatCommand(
                jwt.getSubject(),
                request.conversationId(),
                request.scene(),
                request.message()
        ));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal Jwt jwt,
                             @Valid @RequestBody CoachChatRequest request) {
        SseEmitterCoachEventSink sink = new SseEmitterCoachEventSink();
        sink.attach(coachStreamingService.open(new CoachChatCommand(
                jwt.getSubject(), request.conversationId(), request.scene(), request.message()
        ), sink));
        return sink.emitter();
    }
}
