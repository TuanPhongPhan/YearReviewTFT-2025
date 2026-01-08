package com.tuanphong.yearreviewtft.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanphong.yearreviewtft.api.dto.WrappedRequest;
import com.tuanphong.yearreviewtft.service.WrappedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WrappedController.class)
class WrappedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WrappedService wrappedService;

    @Test
    void requestRejectsBlankRiotId() throws Exception {
        WrappedRequest request = new WrappedRequest(" ", "EUW1", 2024);

        mockMvc.perform(post("/api/wrapped/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestRejectsBlankPlatform() throws Exception {
        WrappedRequest request = new WrappedRequest("Name#TAG", " ", 2024);

        mockMvc.perform(post("/api/wrapped/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestRejectsYearBelowRange() throws Exception {
        WrappedRequest request = new WrappedRequest("Name#TAG", "EUW1", 2019);

        mockMvc.perform(post("/api/wrapped/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestRejectsYearAboveRange() throws Exception {
        WrappedRequest request = new WrappedRequest("Name#TAG", "EUW1", 2031);

        mockMvc.perform(post("/api/wrapped/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusRejectsBlankPuuid() throws Exception {
        mockMvc.perform(get("/api/wrapped/status")
                        .param("puuid", " ")
                        .param("year", "2024"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusRejectsMissingYear() throws Exception {
        mockMvc.perform(get("/api/wrapped/status")
                        .param("puuid", "puuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsSummaryMap() throws Exception {
        Map<String, Object> summary = Map.of("games", 42, "rank", "Gold");
        when(wrappedService.getSummary("puuid", 2024)).thenReturn(summary);

        mockMvc.perform(get("/api/wrapped")
                        .param("puuid", "puuid")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").value(42))
                .andExpect(jsonPath("$.rank").value("Gold"));

        verify(wrappedService).getSummary("puuid", 2024);
    }
}
