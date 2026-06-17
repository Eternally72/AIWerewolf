package com.example.aiwerewolf.aiinfra.eval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationController.class)
class EvaluationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluationRunner evaluationRunner;

    @Test
    void runEvaluationReturnsUnifiedResponse() throws Exception {
        when(evaluationRunner.run(any())).thenReturn(new EvaluationReport(
                "eval-1", "7-standard", 1, 1, 0, 1.0,
                2, 1, 0.5, 1, 0, 15.0, 3.0,
                100, Instant.now(), List.of()));

        mockMvc.perform(post("/api/evaluations/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameCount\":1,\"templateId\":\"7-standard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationId").value("eval-1"))
                .andExpect(jsonPath("$.data.completionRate").value(1.0))
                .andExpect(jsonPath("$.data.fallbackRate").value(0.5));
    }
}
