package com.example.aiwerewolf.aiinfra.eval;

import com.example.aiwerewolf.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
@Profile({"dev", "test"})
public class EvaluationController {
    private final EvaluationRunner evaluationRunner;

    public EvaluationController(EvaluationRunner evaluationRunner) {
        this.evaluationRunner = evaluationRunner;
    }

    @PostMapping("/run")
    public ApiResponse<EvaluationReport> run(@Valid @RequestBody EvaluationRunRequest request) {
        return ApiResponse.ok(evaluationRunner.run(request));
    }
}
