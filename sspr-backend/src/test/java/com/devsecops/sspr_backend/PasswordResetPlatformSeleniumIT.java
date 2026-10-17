package com.devsecops.sspr_backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetPlatformSeleniumIT {

    private static final String GENERIC_RESET_MESSAGE =
            "If the email exists in our system, a password reset link has been sent.";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);

    private WebDriver driver;
    private WebDriverWait wait;

    @RegisterExtension
    final FailureScreenshotExtension failureScreenshots =
            new FailureScreenshotExtension(() -> driver);

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--window-size=1440,1000", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        driver.manage().window().setSize(new Dimension(1440, 1000));
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void existingUserCanSubmitPasswordResetRequest() {
        openUserPortal();

        driver.findElement(testId("reset-email")).sendKeys("devsecops@test.com");
        driver.findElement(testId("submit-reset")).click();

        assertEquals(GENERIC_RESET_MESSAGE,
                wait.until(ExpectedConditions.visibilityOfElementLocated(testId("reset-message"))).getText());
    }

    @Test
    void unknownUserReceivesPrivacyPreservingResetResponse() {
        openUserPortal();

        driver.findElement(testId("reset-email")).sendKeys("missing-" + uniqueSuffix() + "@example.com");
        driver.findElement(testId("submit-reset")).click();

        assertEquals(GENERIC_RESET_MESSAGE,
                wait.until(ExpectedConditions.visibilityOfElementLocated(testId("reset-message"))).getText());
    }

    @Test
    void administratorCanCreateUserAndSeeDashboardCount() {
        openAdminDashboard();
        String email = "selenium-user-" + uniqueSuffix() + "@example.com";

        driver.findElement(testId("new-username")).sendKeys("Selenium User");
        driver.findElement(testId("new-user-email")).sendKeys(email);
        driver.findElement(testId("create-user")).click();
        acceptAlert();

        int totalUsers = Integer.parseInt(wait.until(
                ExpectedConditions.visibilityOfElementLocated(testId("total-users"))).getText());
        assertTrue(totalUsers > 0, "The dashboard should show at least one user after creation");
    }

    @Test
    void administratorCanSearchAndCompleteResetWorkflow() {
        String email = "workflow-" + uniqueSuffix() + "@example.com";
        createUserThroughUi(email);
        requestResetThroughUi(email);
        openAdminDashboard();

        driver.findElement(testId("request-search")).sendKeys(email);
        driver.findElement(testId("search-requests")).click();

        By matchingRow = By.xpath("//tr[starts-with(@data-testid, 'request-row-')]"
            + "[.//td[normalize-space()=\"" + email + "\"]]");
        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(matchingRow));
        String requestId = row.findElements(By.tagName("td")).get(0).getText();
        By status = testId("request-status-" + requestId);

        driver.findElement(testId("approve-request-" + requestId)).click();
        wait.until(ExpectedConditions.textToBe(status, "APPROVED"));

        driver.findElement(testId("complete-request-" + requestId)).click();
        wait.until(ExpectedConditions.textToBe(status, "COMPLETED"));
    }

    private void openUserPortal() {
        driver.get(baseUrl());
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId("user-tab")));
    }

    private void openAdminDashboard() {
        openUserPortal();
        driver.findElement(testId("admin-tab")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId("total-users")));
    }

    private void createUserThroughUi(String email) {
        openAdminDashboard();
        driver.findElement(testId("new-username")).sendKeys("Workflow User");
        driver.findElement(testId("new-user-email")).sendKeys(email);
        driver.findElement(testId("create-user")).click();
        acceptAlert();
    }

    private void requestResetThroughUi(String email) {
        driver.findElement(testId("user-tab")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId("reset-email")));
        driver.findElement(testId("reset-email")).sendKeys(email);
        driver.findElement(testId("submit-reset")).click();
        assertEquals(GENERIC_RESET_MESSAGE, waitForText(testId("reset-message")));
    }

    private void acceptAlert() {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        assertEquals("User created successfully!", alert.getText());
        alert.accept();
    }

    private String waitForText(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText();
    }

    private String baseUrl() {
        return System.getProperty("selenium.baseUrl", "http://localhost:5173");
    }

    private String uniqueSuffix() {
        return String.valueOf(System.currentTimeMillis());
    }

    private By testId(String value) {
        return By.cssSelector("[data-testid='" + value + "']");
    }

    static class FailureScreenshotExtension implements AfterTestExecutionCallback {

        private final Supplier<WebDriver> driverSupplier;

        FailureScreenshotExtension(Supplier<WebDriver> driverSupplier) {
            this.driverSupplier = driverSupplier;
        }

        @Override
        public void afterTestExecution(ExtensionContext context) {
            if (context.getExecutionException().isPresent()) {
                saveScreenshot(context);
            }
        }

        private void saveScreenshot(ExtensionContext context) {
            WebDriver currentDriver = driverSupplier.get();
            if (currentDriver == null) {
                return;
            }

            try {
                Path directory = Path.of("target", "selenium-screenshots");
                Files.createDirectories(directory);
                String fileName = context.getRequiredTestClass().getSimpleName()
                        + "-" + context.getRequiredTestMethod().getName() + ".png";
                Files.write(directory.resolve(fileName),
                        ((org.openqa.selenium.TakesScreenshot) currentDriver)
                                .getScreenshotAs(org.openqa.selenium.OutputType.BYTES));
            } catch (IOException | RuntimeException screenshotError) {
                System.err.println("Unable to save Selenium failure screenshot: " + screenshotError.getMessage());
            }
        }
    }
}