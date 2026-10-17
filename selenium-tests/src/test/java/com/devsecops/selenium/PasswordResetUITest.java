package com.devsecops.selenium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Password Reset Portal UI Tests")
public class PasswordResetUITest extends BaseTest {

    private static final String EXPECTED_SUCCESS_MESSAGE =
            "If the email exists in our system, a password reset link has been sent.";

    @BeforeEach
    void setUp() {
        openBaseUrl();
        // Ensure User Reset Portal tab is active
        WebElement userTab = waitForTestId("user-tab");
        userTab.click();
    }

    @Test
    @DisplayName("Verify that the application page loads with the expected title")
    void testPageTitle() {
        String title = driver.getTitle();
        assertNotNull(title, "Page title should not be null");
        assertFalse(title.trim().isEmpty(), "Page title should not be empty");
        assertTrue(
                title.contains("Password Reset") || title.equalsIgnoreCase("frontend"),
                "Page title should contain 'Password Reset' or 'frontend'. Actual title: " + title
        );
    }

    @Test
    @DisplayName("Verify that the reset portal form components exist and are visible")
    void testResetPortalFormExists() {
        WebElement emailInput = waitForTestId("reset-email");
        WebElement submitButton = waitForTestId("submit-reset");

        assertTrue(emailInput.isDisplayed(), "Email input field must be displayed");
        assertTrue(submitButton.isDisplayed(), "Submit button must be displayed");
        assertEquals("email", emailInput.getAttribute("type"), "Email input should be of type 'email'");
        assertEquals("true", emailInput.getAttribute("required"), "Email input should have 'required' attribute");
    }

    @Test
    @DisplayName("Verify password reset submission displays the generic privacy-preserving success message")
    void testPasswordResetSubmission() {
        String testEmail = "reset-test-" + System.currentTimeMillis() + "@example.com";

        typeTestId("reset-email", testEmail);
        clickTestId("submit-reset");

        WebElement messageElement = waitForTestId("reset-message");
        assertNotNull(messageElement, "Reset response message element must be present");
        assertTrue(messageElement.isDisplayed(), "Reset response message must be visible");

        String actualMessage = messageElement.getText();
        assertEquals(EXPECTED_SUCCESS_MESSAGE, actualMessage,
                "Expected generic reset message for anti-enumeration compliance");
    }

    @Test
    @DisplayName("Verify HTML5 client-side validation when attempting submission with an empty email")
    void testEmptyEmailValidation() {
        WebElement emailInput = waitForTestId("reset-email");
        emailInput.clear();

        WebElement submitButton = waitForTestId("submit-reset");
        submitButton.click();

        // Check HTML5 validity using JavascriptExecutor
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Boolean isValid = (Boolean) js.executeScript("return arguments[0].checkValidity();", emailInput);
        assertFalse(isValid, "Empty required email input must fail browser HTML5 form validation");

        // Verify that browser populates validationMessage
        String validationMessage = (String) js.executeScript("return arguments[0].validationMessage;", emailInput);
        assertNotNull(validationMessage, "Validation message must be non-null");
        assertFalse(validationMessage.trim().isEmpty(), "Validation message must not be empty when field is invalid");

        // Verify that submission did not proceed to render a reset response message
        boolean messageAppeared = driver.findElements(testId("reset-message")).size() > 0;
        assertFalse(messageAppeared, "Reset message element should not appear when validation fails");
    }
}
