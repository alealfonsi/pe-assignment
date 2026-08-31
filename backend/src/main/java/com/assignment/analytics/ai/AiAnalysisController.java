package com.assignment.analytics.ai;

import com.assignment.analytics.ai.AnalysisDtos.AnalysisResponse;
import com.assignment.analytics.auth.AuthenticatedOperator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers/{customerId}/analyses")
public class AiAnalysisController {

    private final AiAnalysisService analysisService;

    public AiAnalysisController(AiAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /** Runs the AI pipeline for this customer; the persisted result is returned. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse run(@PathVariable UUID customerId,
                                @AuthenticationPrincipal AuthenticatedOperator operator) {
        return analysisService.run(customerId, operator);
    }

    @GetMapping
    public List<AnalysisResponse> history(@PathVariable UUID customerId) {
        return analysisService.history(customerId);
    }

    @GetMapping("/{analysisId}")
    public AnalysisResponse get(@PathVariable UUID customerId, @PathVariable UUID analysisId) {
        return analysisService.get(customerId, analysisId);
    }
}
