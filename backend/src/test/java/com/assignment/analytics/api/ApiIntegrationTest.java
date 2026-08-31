package com.assignment.analytics.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests on the seeded H2 database, running the full stub AI
 * pipeline (digest -> triage -> BM25 retrieval -> analysis -> persistence).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    // Deterministic seed ids (see V2__seed_data.sql)
    private static final String DANIEL_OSEI = "f64a8ac6-baf4-5ffd-b7ce-3256ae699248";
    private static final String MARCO_DELUCA = "3968f964-895e-53af-880b-db8ad20bd114";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    @Order(1)
    void loginRejectsBadCredentials() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void searchFindsCustomersByNameFragmentAndById() throws Exception {
        String token = login("alice", "operator1");

        mvc.perform(get("/api/customers").param("query", "sorokina")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Yulia Sorokina"));

        mvc.perform(get("/api/customers").param("query", DANIEL_OSEI.substring(0, 8))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Daniel Osei"));

        // no query -> all seeded customers listed
        mvc.perform(get("/api/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    @Order(4)
    void overviewAggregatesActivityAndRiskSignals() throws Exception {
        String token = login("alice", "operator1");
        mvc.perform(get("/api/customers/" + DANIEL_OSEI).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Daniel Osei"))
                .andExpect(jsonPath("$.riskScore").value(190.0))
                .andExpect(jsonPath("$.firedRules[0].ruleName").value("Rapid crypto outflow"))
                .andExpect(jsonPath("$.activity[?(@.type=='CRYPTO')].count").value(8));
    }

    @Test
    @Order(5)
    void transactionsArePagedFilteredAndCarrySubtypeDetails() throws Exception {
        String token = login("alice", "operator1");
        String body = mvc.perform(get("/api/customers/" + DANIEL_OSEI + "/transactions")
                        .param("type", "CRYPTO").param("size", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(8))
                .andExpect(jsonPath("$.items.length()").value(5))
                .andReturn().getResponse().getContentAsString();

        JsonNode items = objectMapper.readTree(body).get("items");
        for (JsonNode item : items) {
            assertThat(item.get("activityType").asText()).isEqualTo("CRYPTO");
            assertThat(item.get("crypto").isObject()).isTrue();
            assertThat(item.get("card").isNull()).isTrue();
        }
    }

    @Test
    @Order(6)
    void unknownCustomerReturns404() throws Exception {
        String token = login("alice", "operator1");
        mvc.perform(get("/api/customers/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void aiAnalysisRunsThroughTheFullPipelineAndIsPersisted() throws Exception {
        String token = login("bob", "operator2");

        String body = mvc.perform(post("/api/customers/" + DANIEL_OSEI + "/analyses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.llmMode").value("STUB"))
                .andExpect(jsonPath("$.operatorName").value("Bob Lindqvist"))
                .andExpect(jsonPath("$.findings").isNotEmpty())
                .andExpect(jsonPath("$.recommendations").isNotEmpty())
                .andExpect(jsonPath("$.sources").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode analysis = objectMapper.readTree(body);
        String analysisId = analysis.get("analysisId").asText();

        // RAG really happened: the mixer policy section was retrieved and cited.
        assertThat(body).contains("Mixing and tumbling services");

        // Persisted and visible in history for later review.
        mvc.perform(get("/api/customers/" + DANIEL_OSEI + "/analyses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].analysisId").value(analysisId));

        mvc.perform(get("/api/customers/" + DANIEL_OSEI + "/analyses/" + analysisId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"));

        // The same analysis id does not leak under another customer.
        mvc.perform(get("/api/customers/" + MARCO_DELUCA + "/analyses/" + analysisId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    void lowRiskCustomerGetsLowGrade() throws Exception {
        String token = login("alice", "operator1");
        mvc.perform(post("/api/customers/" + MARCO_DELUCA + "/analyses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }
}
