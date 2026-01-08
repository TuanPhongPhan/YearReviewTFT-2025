package com.tuanphong.yearreviewtft.api;

import com.tuanphong.yearreviewtft.api.dto.RiotAccountResponse;
import com.tuanphong.yearreviewtft.riot.TftMatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResolveController.class)
class ResolveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TftMatchService tftMatchService;

    @Test
    void resolveReturnsRiotAccount() throws Exception {
        RiotAccountResponse response = new RiotAccountResponse("puuid", "Name", "TAG");
        when(tftMatchService.resolveRiotId("Name#TAG")).thenReturn(response);

        mockMvc.perform(get("/api/resolve")
                        .param("riotId", "Name#TAG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puuid").value("puuid"))
                .andExpect(jsonPath("$.gameName").value("Name"))
                .andExpect(jsonPath("$.tagLine").value("TAG"));
    }

    @Test
    void resolveRejectsMissingRiotId() throws Exception {
        mockMvc.perform(get("/api/resolve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolveRejectsBlankRiotId() throws Exception {
        mockMvc.perform(get("/api/resolve")
                        .param("riotId", " "))
                .andExpect(status().isBadRequest());
    }
}
