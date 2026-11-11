package com.mentormatching.mentor.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.command.UpsertCurrentMentorAchievementCommand;
import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.entity.AchievementType;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.DeleteCurrentMentorAchievementUseCase;
import com.mentormatching.mentor.service.UpsertCurrentMentorAchievementUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorAchievementsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MentorAchievementController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class MentorAchievementControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetCurrentMentorAchievementsUseCase getCurrentMentorAchievementsUseCase;

    @MockitoBean
    UpsertCurrentMentorAchievementUseCase upsertCurrentMentorAchievementUseCase;

    @MockitoBean
    DeleteCurrentMentorAchievementUseCase deleteCurrentMentorAchievementUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getAchievements_success() throws Exception {
        MentorAchievementDetails details = MentorAchievementDetails.builder()
                .id(1L)
                .title("IELTS 8.5")
                .description("Overall band 8.5")
                .achievementType(AchievementType.CERTIFICATE)
                .achievedAt(LocalDate.of(2023, 5, 15))
                .proofUrl("https://example.com/cert.png")
                .proofMediaId(101L)
                .build();

        when(getCurrentMentorAchievementsUseCase.execute(userId)).thenReturn(List.of(details));

        mvc.perform(get("/api/v1/mentors/me/achievements")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("IELTS 8.5")))
                .andExpect(jsonPath("$.data[0].proofMediaId", is(101)));
    }

    @Test
    void postAchievement_success() throws Exception {
        MentorAchievementDetails details = MentorAchievementDetails.builder()
                .id(1L)
                .title("IELTS 8.5")
                .description("Overall band 8.5")
                .achievementType(AchievementType.CERTIFICATE)
                .achievedAt(LocalDate.of(2023, 5, 15))
                .proofUrl("https://example.com/cert.png")
                .proofMediaId(101L)
                .build();

        when(upsertCurrentMentorAchievementUseCase.execute(eq(userId), any(UpsertCurrentMentorAchievementCommand.class)))
                .thenReturn(details);

        mvc.perform(post("/api/v1/mentors/me/achievements")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "IELTS 8.5",
                                    "description": "Overall band 8.5",
                                    "achievementType": "CERTIFICATE",
                                    "achievedAt": "2023-05-15",
                                    "proofUrl": "https://example.com/cert.png",
                                    "proofMediaId": 101
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.title", is("IELTS 8.5")));
    }

    @Test
    void putAchievement_success() throws Exception {
        MentorAchievementDetails details = MentorAchievementDetails.builder()
                .id(1L)
                .title("IELTS 9.0")
                .achievementType(AchievementType.CERTIFICATE)
                .achievedAt(LocalDate.of(2024, 1, 10))
                .build();

        when(upsertCurrentMentorAchievementUseCase.execute(eq(userId), any(UpsertCurrentMentorAchievementCommand.class)))
                .thenReturn(details);

        mvc.perform(put("/api/v1/mentors/me/achievements/{achievementId}", 1L)
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "IELTS 9.0",
                                    "achievementType": "CERTIFICATE",
                                    "achievedAt": "2024-01-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("IELTS 9.0")));
    }

    @Test
    void deleteAchievement_success() throws Exception {
        doNothing().when(deleteCurrentMentorAchievementUseCase).execute(userId, 1L);

        mvc.perform(delete("/api/v1/mentors/me/achievements/{achievementId}", 1L)
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
