package com.devsecops.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

/**
 * BaseTest provides common setup, teardown, and utility helper methods
 * for all Selenium browser-based and integration test cases.
 */
public abstract class BaseTest {

    protected static WebDriver driver;
    protected static WebDriverWait wait;

    // Configurable URLs with production-ready defaults
    public static final String BASE_URL = System.getProperty(
            "selenium.baseUrl",
            System.getProperty("base.url", "http://localhost:5173")
    );

    public static final String API_URL = System.getProperty(
            "api.base.url",
            System.getProperty("api.url", "http://localhost:8080")
    );

    // Standard timeouts
    protected static final Duration IMPLICIT_WAIT_TIMEOUT = Duration.ofSeconds(5);
    protected static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);
    protected static final Duration SCRIPT_TIMEOUT = Duration.ofSeconds(15);
    protected static final Duration EXPLICIT_WAIT_TIMEOUT = Duration.ofSeconds(10);

    @BeforeAll
    public static void setUpDriver() {
        if (driver == null) {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--remote-allow-origins=*");

            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT_TIMEOUT);
            driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
            driver.manage().timeouts().scriptTimeout(SCRIPT_TIMEOUT);

            wait = new WebDriverWait(driver, EXPLICIT_WAIT_TIMEOUT);
        }
    }

    @AfterAll
    public static void tearDownDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error closing WebDriver: " + e.getMessage());
            } finally {
                driver = null;
            }
        }
    }

    // Navigation helpers
    protected void openBaseUrl() {
        driver.get(BASE_URL);
    }

    protected void navigateTo(String path) {
        String cleanBase = BASE_URL.replaceAll("/+$", "");
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        driver.get(cleanBase + cleanPath);
    }

    // Element location helpers
    protected By testId(String value) {
        return By.cssSelector("[data-testid='" + value + "']");
    }

    protected WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    protected WebElement findElementByTestId(String testId) {
        return driver.findElement(testId(testId));
    }

    // Explicit wait helpers
    protected WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForVisibility(By locator, Duration timeout) {
        return new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForTestId(String testIdValue) {
        return waitForVisibility(testId(testIdValue));
    }

    protected WebElement waitForTestId(String testIdValue, Duration timeout) {
        return waitForVisibility(testId(testIdValue), timeout);
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected WebElement waitForTestIdClickable(String testIdValue) {
        return wait.until(ExpectedConditions.elementToBeClickable(testId(testIdValue)));
    }

    protected boolean waitForText(By locator, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    protected boolean waitForTestIdText(String testIdValue, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(testId(testIdValue), text));
    }

    // Interaction helpers
    protected void click(By locator) {
        waitForClickable(locator).click();
    }

    protected void clickTestId(String testIdValue) {
        waitForTestIdClickable(testIdValue).click();
    }

    protected void type(By locator, String text) {
        WebElement element = waitForVisibility(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected void typeTestId(String testIdValue, String text) {
        type(testId(testIdValue), text);
    }

    // Alert helpers
    protected Alert waitForAlert() {
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    protected String getAlertTextAndAccept() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.accept();
        return text;
    }

    protected void dismissAlert() {
        Alert alert = waitForAlert();
        alert.dismiss();
    }

    // Health check helper
    public static boolean isServiceAvailable(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int code = connection.getResponseCode();
            return code > 0;
        } catch (IOException e) {
            return false;
        }
    }
}
