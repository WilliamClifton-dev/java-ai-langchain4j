package com.atguigu.java.ai.langchain4j.profile.api;

import com.atguigu.java.ai.langchain4j.profile.ProfileService;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreening;
import com.atguigu.java.ai.langchain4j.profile.SafetyScreeningAnswers;
import com.atguigu.java.ai.langchain4j.profile.SaveProfileCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(profileService.getRequired(jwt.getSubject()));
    }

    @PutMapping
    public ProfileResponse save(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ProfileResponse.from(profileService.save(jwt.getSubject(), new SaveProfileCommand(
                request.dateOfBirth(), request.calculationSex(), request.heightCm(),
                request.currentWeightKg(), request.targetWeightKg(),
                request.activityLevel(), request.timeZone()
        )));
    }

    @PostMapping("/screenings")
    @ResponseStatus(HttpStatus.CREATED)
    public SafetyScreeningResponse screen(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SafetyScreeningRequest request
    ) {
        SafetyScreening screening = profileService.screen(jwt.getSubject(), new SafetyScreeningAnswers(
                request.pregnantOrBreastfeeding(), request.eatingDisorderHistory(),
                request.medicalGuidanceRequired(), request.weightAffectingMedication(),
                request.concerningSymptoms()
        ));
        return SafetyScreeningResponse.from(screening);
    }

    @GetMapping("/screenings/current")
    public SafetyScreeningResponse currentScreening(@AuthenticationPrincipal Jwt jwt) {
        return profileService.findCurrentScreening(jwt.getSubject())
                .map(SafetyScreeningResponse::from)
                .orElseThrow(com.atguigu.java.ai.langchain4j.profile.ProfileNotFoundException::new);
    }
}
