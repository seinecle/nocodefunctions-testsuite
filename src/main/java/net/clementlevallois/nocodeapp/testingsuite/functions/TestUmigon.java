/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeapp.testingsuite.functions;

import com.slack.api.methods.SlackApiException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.clementlevallois.nocodeapp.testingsuite.controller.TestInterface;
import net.clementlevallois.nocodeapp.testingsuite.controller.TestingSuite;
import net.clementlevallois.nocodeapp.testingsuite.slack.SlackAPI;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author LEVALLOIS
 */
public class TestUmigon implements TestInterface {

    private final SlackAPI slackAPI;
    private static final String NAME = "umigon";
    private static final String TEST_FILE = "2_sentences_in_English.txt";
    private final String domain;
    private final Path rootFolder;

    @Override
    public String getName() {
        return NAME;
    }

    public TestUmigon(boolean sendMessagesToSlack) {
        slackAPI = new SlackAPI(sendMessagesToSlack);
        domain = TestingSuite.domain();
        rootFolder = TestingSuite.rootFolder();
    }

    @Override
    public void conductTests(List<WebDriver> webDrivers) {
        for (WebDriver webDriver : webDrivers) {
            try {
                // visit first page of the function
                webDriver.get(domain + "/" + NAME + "/sentiment_analysis_tool.html");
                Thread.sleep(Duration.ofSeconds(2));
                String urlOfCurrentPage = webDriver.getCurrentUrl();
                boolean isTitleFirstPageOK = urlOfCurrentPage.contains(NAME + "/sentiment_analysis_tool.html");
                if (!isTitleFirstPageOK) {
                    String errorMessage = "error when loading first page";
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                }

                // loading page for txt file upload
                WebElement button = webDriver.findElement(By.id("textInBulkButton_1"));
                button.click();
                Thread.sleep(Duration.ofSeconds(2));
                urlOfCurrentPage = webDriver.getCurrentUrl();
                boolean isUrlUploadPageOK = urlOfCurrentPage.contains("import_your_data_bulk_text.html") && urlOfCurrentPage.contains("function=" + NAME);
                if (!isUrlUploadPageOK) {
                    String errorMessage = "error when loading import data in bulk text page";
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                }

                // we are now on the text file upload page
                WebElement webElement = webDriver.findElement(By.id("launchButtons:fileUploadButton_input"));
                webElement.sendKeys(rootFolder.toString() + File.separator + NAME + File.separator + TEST_FILE);
                WebElement uploadButton = webDriver.findElement(By.className("ui-fileupload-upload"));
                if (!uploadButton.getAttribute("class").contains("ui-state-disabled")) {
                    uploadButton.click();
                }
                WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));
                wait.until(ExpectedConditions.elementToBeClickable(By.id("launchButtons:readFileBtn")));
                button = webDriver.findElement(By.id("launchButtons:readFileBtn"));
                button.click();

                // click on the "compute" button of the import txt file page
                wait.until(ExpectedConditions.elementToBeClickable(By.id("formComputeButton:computeButton")));
                button = webDriver.findElement(By.id("formComputeButton:computeButton"));
                button.click();
                Thread.sleep(Duration.ofSeconds(2));

                // we are now on the param page for the function
                urlOfCurrentPage = webDriver.getCurrentUrl();
                boolean isUrlParamPageOK = urlOfCurrentPage.contains("/" + NAME + "/" + NAME + ".html");
                if (!isUrlParamPageOK) {
                    String errorMessage = "error loading " + NAME + ".html";
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                }

                // Click on the "compute" button
                WebElement computeButton = webDriver.findElement(By.id("formComputeButton:computeButton"));
                computeButton.click();

                Thread.sleep(Duration.ofSeconds(2));
                // we are on the result page
                urlOfCurrentPage = webDriver.getCurrentUrl();
                boolean isUrlresultPageOK = urlOfCurrentPage.contains(NAME + "/results.html");
                if (!isUrlresultPageOK) {
                    String errorMessage = "error loading " + NAME + "/results.html";
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                }

                List<WebElement> rows = webDriver.findElements(By.cssSelector("[class='ui-datatable-data ui-widget-content'] tr"));

                if (rows.size() != 3) {
                    String errorMessage = "error on " + NAME + "/results.html, there should be exactly 2 rows in the table of results on the page";
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                }

                // Click on the "download" button
                WebElement downloadButton = webDriver.findElement(By.id("formDownloadButton:downloadButton"));
                downloadButton.click();

            } catch (InterruptedException | IOException | SlackApiException ex) {
                try {
                    String errorMessage = "unspecified error: " + ex.getMessage();
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                } catch (IOException | SlackApiException ex1) {
                    System.out.println("error sending message to Slack");
                    Logger.getLogger(TestUmigon.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }

        }

    }

}
