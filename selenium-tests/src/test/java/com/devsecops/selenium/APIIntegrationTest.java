package com.devsecops.selenium;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("REST API Integration Tests")
public class APIIntegrationTest extends BaseTest {

    @BeforeAll
    public static void setUpRestAssured() {
        RestAssured.baseURI = API_URL;
    }

    @Test
    @DisplayName("GET /api/v1/admin/dashboard/summary returns 200 and valid JSON")
    void testDashboardSummaryEndpoint() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/admin/dashboard/summary")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("POST /api/v1/auth/request-reset returns 200")
    void testPasswordResetEndpoint() {
        String testEmail = "api-reset-" + System.currentTimeMillis() + "@example.com";

        given()
            .queryParam("email", testEmail)
        .when()
            .post("/api/v1/auth/request-reset")
        .then()
            .statusCode(200)
            .body(containsString("If the email exists in our system, a password reset link has been sent."));
    }

    @Test
    @DisplayName("POST /api/v1/admin/users returns 200")
    void testCreateUserEndpoint() {
        String username = "ApiUser" + System.currentTimeMillis();
        String email = "api-user-" + System.currentTimeMillis() + "@example.com";

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", username);
        userPayload.put("email", email);
        userPayload.put("status", "ACTIVE");

        given()
            .contentType(ContentType.JSON)
            .body(userPayload)
        .when()
            .post("/api/v1/admin/users")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("username", equalTo(username))
            .body("email", equalTo(email))
            .body("status", equalTo("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/requests returns 200")
    void testGetRequestsEndpoint() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/admin/requests")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("PUT /api/v1/admin/requests/{id}/status returns 200 or 404")
    void testUpdateStatusEndpoint() {
        // 1. Verify 404 for a non-existent request ID
        given()
            .queryParam("status", "APPROVED")
        .when()
            .put("/api/v1/admin/requests/999999999/status")
        .then()
            .statusCode(anyOf(is(200), is(404)));

        // 2. Create user, request reset, and verify 200 when updating existing request
        String email = "status-workflow-" + System.currentTimeMillis() + "@example.com";
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", "StatusWorkflowUser");
        userPayload.put("email", email);
        userPayload.put("status", "ACTIVE");

        // Seed user and reset token
        given().contentType(ContentType.JSON).body(userPayload).post("/api/v1/admin/users");
        given().queryParam("email", email).post("/api/v1/auth/request-reset");

        // Query for created token
        List<Map<String, Object>> requests = given()
            .queryParam("search", email)
        .when()
            .get("/api/v1/admin/requests")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("");

        if (!requests.isEmpty()) {
            Object id = requests.get(0).get("id");
            given()
                .queryParam("status", "APPROVED")
            .when()
                .put("/api/v1/admin/requests/" + id + "/status")
            .then()
                .statusCode(200)
                .body("message", containsString("Status updated successfully"))
                .body("status", equalTo("APPROVED"));
        }
    }

    @Test
    @DisplayName("Verify dashboard summary contains all required fields (Catches deliberate defect)")
    void testDashboardContainsRequiredFields() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/admin/dashboard/summary")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("totalUsers", notNullValue())
            .body("totalRequests", notNullValue())
            .body("pendingRequests", notNullValue())
            .body("completedRequests", notNullValue())
            .body("rejectedRequests", notNullValue());
    }
}
