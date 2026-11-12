package com.mentormatching.mentor.controller;

import java.math.BigDecimal;
import java.util.List;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.query.SearchMentorsQuery;
import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.dto.response.MentorDetailResponse;
import com.mentormatching.mentor.dto.response.MentorOptionResponse;
import com.mentormatching.mentor.dto.response.MentorSearchItemResponse;
import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.dto.response.MentorTraitsDetailResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.GetHighlightOptionsUseCase;
import com.mentormatching.mentor.service.GetPersonalityOptionsUseCase;
import com.mentormatching.mentor.usecase.GetMentorDetailUseCase;
import com.mentormatching.mentor.usecase.GetPublicMentorAchievementsUseCase;
import com.mentormatching.mentor.usecase.GetPublicMentorSubjectsUseCase;
import com.mentormatching.mentor.usecase.GetPublicMentorTraitsUseCase;
import com.mentormatching.mentor.usecase.SearchMentorsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MentorController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class MentorControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    SearchMentorsUseCase searchMentorsUseCase;

    @MockitoBean
    GetMentorDetailUseCase getMentorDetailUseCase;

    @MockitoBean
    GetPersonalityOptionsUseCase getPersonalityOptionsUseCase;

    @MockitoBean
    GetHighlightOptionsUseCase getHighlightOptionsUseCase;

    @MockitoBean
    GetPublicMentorSubjectsUseCase getPublicMentorSubjectsUseCase;

    @MockitoBean
    GetPublicMentorTraitsUseCase getPublicMentorTraitsUseCase;

    @MockitoBean
    GetPublicMentorAchievementsUseCase getPublicMentorAchievementsUseCase;

    @Test
    void searchMentors_publicAccess() throws Exception {
        MentorSearchItemResponse item = MentorSearchItemResponse.builder()
                .id(1L)
                .fullName("John Doe")
                .meetingType(MeetingType.ONLINE)
                .build();
        PageResponse<MentorSearchItemResponse> page = new PageResponse<>(1, 10, 1, 1L, List.of(item));

        when(searchMentorsUseCase.execute(any(SearchMentorsQuery.class))).thenReturn(page);

        mvc.perform(get("/api/v1/mentors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].fullName", is("John Doe")));
    }

    @Test
    void getOptions_publicAccess() throws Exception {
        when(getPersonalityOptionsUseCase.execute()).thenReturn(List.of(new MentorOptionResponse(1L, "Patient", "Desc")));
        when(getHighlightOptionsUseCase.execute()).thenReturn(List.of(new MentorOptionResponse(10L, "Olympic", "Desc")));

        mvc.perform(get("/api/v1/mentors/personality-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        mvc.perform(get("/api/v1/mentors/highlight-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void getMentorDetail_publicAccess() throws Exception {
        MentorDetailResponse response = MentorDetailResponse.builder()
                .id(1L)
                .fullName("John Doe")
                .meetingType(MeetingType.ONLINE)
                .build();

        when(getMentorDetailUseCase.execute(1L)).thenReturn(response);

        mvc.perform(get("/api/v1/mentors/{mentorId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")));
    }

    @Test
    void getPublicSubjects_publicAccess() throws Exception {
        MentorSubjectDetails details = MentorSubjectDetails.builder()
                .id(10L)
                .subjectGradeId(100L)
                .pricePerHour(new BigDecimal("200000.00"))
                .active(true)
                .build();

        when(getPublicMentorSubjectsUseCase.execute(1L)).thenReturn(List.of(details));

        mvc.perform(get("/api/v1/mentors/{mentorId}/subjects", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void getPublicTraits_publicAccess() throws Exception {
        MentorTraitsDetailResponse response = MentorTraitsDetailResponse.builder()
                .personalities(List.of(new MentorOptionResponse(1L, "Patient", "Desc")))
                .highlights(List.of())
                .build();

        when(getPublicMentorTraitsUseCase.execute(1L)).thenReturn(response);

        mvc.perform(get("/api/v1/mentors/{mentorId}/traits", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.personalities", hasSize(1)));
    }

    @Test
    void getPublicAchievements_publicAccess() throws Exception {
        MentorAchievementDetails details = MentorAchievementDetails.builder()
                .id(1L)
                .title("IELTS 8.5")
                .build();

        when(getPublicMentorAchievementsUseCase.execute(1L)).thenReturn(List.of(details));

        mvc.perform(get("/api/v1/mentors/{mentorId}/achievements", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
