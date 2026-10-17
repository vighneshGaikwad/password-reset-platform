package com.devsecops.selenium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Admin Dashboard and Workflow UI Tests")
public class AdminDashboardUITest extends BaseTest {

    @BeforeEach
    void setUp() {
        openBaseUrl();
        // Switch to Admin Dashboard & Workflow tab
        clickTestId("admin-tab");
        waitForTestId("total-users");
    }

    @Test
    @DisplayName("Verify admin dashboard loads and all metric summary cards are displayed")
    void testDashboardLoads() {
        WebElement totalUsers = waitForTestId("total-users");
        WebElement pendingRequests = waitForTestId("pending-requests");
        WebElement completedRequests = waitForTestId("completed-requests");
        WebElement rejectedRequests = waitForTestId("rejected-requests");

        assertTrue(totalUsers.isDisplayed(), "Total Users metric card must be visible");
        assertTrue(pendingRequests.isDisplayed(), "Pending Requests metric card must be visible");
        assertTrue(completedRequests.isDisplayed(), "Completed Requests metric card must be visible");
        assertTrue(rejectedRequests.isDisplayed(), "Rejected Requests metric card must be visible");
    }

    @Test
    @DisplayName("Verify dashboard displays accurate non-null metrics and responds to backend data")
    void testDashboardMetrics() {
        // Create a user to guarantee totalUsers > 0
        String email = "metric-user-" + System.currentTimeMillis() + "@example.com";
        typeTestId("new-username", "Metric Admin User");
        typeTestId("new-user-email", email);
        clickTestId("create-user");
        getAlertTextAndAccept();

        // Verify total users is updated and > 0
        WebElement totalUsersEl = waitForTestId("total-users");
        int totalUsers = Integer.parseInt(totalUsersEl.getText().trim());
        assertTrue(totalUsers > 0, "Dashboard totalUsers metric should be greater than 0, got: " + totalUsers);

        // Verify other metrics parse as valid integer numbers
        WebElement pendingEl = waitForTestId("pending-requests");
        assertNotNull(pendingEl.getText(), "Pending requests text must not be null");
        assertDoesNotThrow(() -> Integer.parseInt(pendingEl.getText().trim()),
                "Pending requests should be a valid integer");

        WebElement completedEl = waitForTestId("completed-requests");
        assertNotNull(completedEl.getText(), "Completed requests text must not be null");
        assertDoesNotThrow(() -> Integer.parseInt(completedEl.getText().trim()),
                "Completed requests should be a valid integer");

        WebElement rejectedEl = waitForTestId("rejected-requests");
        assertNotNull(rejectedEl.getText(), "Rejected requests text must not be null");
        assertDoesNotThrow(() -> Integer.parseInt(rejectedEl.getText().trim()),
                "Rejected requests should be a valid integer");
    }

    @Test
    @DisplayName("Verify the requests and workflow table renders with proper columns")
    void testRequestsTableExists() {
        WebElement table = driver.findElement(By.tagName("table"));
        assertTrue(table.isDisplayed(), "Workflow requests table must be rendered on admin dashboard");

        List<WebElement> headers = table.findElements(By.tagName("th"));
        assertFalse(headers.isEmpty(), "Table must have header columns");

        List<String> headerTexts = headers.stream().map(WebElement::getText).toList();
        assertTrue(headerTexts.stream().anyMatch(h -> h.contains("ID")), "Header should include ID");
        assertTrue(headerTexts.stream().anyMatch(h -> h.contains("Email")), "Header should include Email");
        assertTrue(headerTexts.stream().anyMatch(h -> h.contains("Status")), "Header should include Status");
        assertTrue(headerTexts.stream().anyMatch(h -> h.contains("Actions")), "Header should include Actions");
    }

    @Test
    @DisplayName("Verify search input exists and triggers search query")
    void testSearchFunctionality() {
        WebElement searchInput = waitForTestId("request-search");
        WebElement searchButton = waitForTestId("search-requests");

        assertTrue(searchInput.isDisplayed(), "Request search input should be visible");
        assertTrue(searchButton.isDisplayed(), "Search requests button should be visible");

        String searchTerm = "admin@example.com";
        typeTestId("request-search", searchTerm);
        assertEquals(searchTerm, searchInput.getAttribute("value"), "Search input value should match entered text");

        clickTestId("search-requests");
        // Verify table remains stable and rendered after executing search
        WebElement table = driver.findElement(By.tagName("table"));
        assertTrue(table.isDisplayed(), "Requests table should remain visible after search execution");
    }
}
