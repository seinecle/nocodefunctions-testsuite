/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeapp.testingsuite.functions;

import net.clementlevallois.nocodeapp.testingsuite.utils.ExcelReader;
import com.slack.api.methods.SlackApiException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeapp.testingsuite.controller.TestInterface;
import net.clementlevallois.nocodeapp.testingsuite.controller.TestingSuite;
import net.clementlevallois.nocodeapp.testingsuite.utils.SlackAPI;
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

                List<String> testFiles = List.of("file_1.txt");

                System.out.print(" (");
                int indexTestFiles = 0;
                for (String testFile : testFiles) {
                    Properties descriptorForOneTestFile = new Properties();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(TestUmigon.class.getResourceAsStream("/" + NAME + "/" + testFile), "UTF-8"))) {
                        descriptorForOneTestFile.load(reader);
                    } catch (IOException e) {
                        throw new IOException("Test file descriptor not found: " + File.separator + NAME + File.separator + testFile);
                    }

                    String testFileName = descriptorForOneTestFile.getProperty("name");

                    System.out.print(testFileName);
                    if (++indexTestFiles < testFiles.size()) {
                        System.out.print(", ");
                    }
                    int nbEntries = Integer.parseInt(descriptorForOneTestFile.getProperty("nb_entries"));

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
                    webElement.sendKeys(rootFolder.toString() + File.separator + NAME + File.separator + testFileName);
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

                    if (rows.size() != (nbEntries + 1)) {
                        String errorMessage = "error on " + NAME + "/results.html, there should be exactly " + (nbEntries + 1) + " rows in the table of results on the page";
                        System.out.println(NAME + ": " + errorMessage);
                        slackAPI.sendMessage(NAME, errorMessage);
                    }

                    // Click on the "download" button
                    WebElement downloadButton = webDriver.findElement(By.id("formDownloadButton:downloadButton"));
                    downloadButton.click();

                    // after a while to let the download complete, check that the results are as expected
                    Thread.sleep(Duration.ofSeconds(5));
                    checkingCorrectnessResults(descriptorForOneTestFile);
                }
                System.out.print(") ");

            } catch (InterruptedException | IOException | SlackApiException ex) {
                String errorMessage = "unspecified error: " + ex.getMessage();
                System.out.println(NAME + ": " + errorMessage);
                try {
                    slackAPI.sendMessage(NAME, errorMessage);
                } catch (IOException | SlackApiException ex1) {
                    System.out.println("error sending message to Slack");
                    Logger.getLogger(TestUmigon.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }

    private void checkingCorrectnessResults(Properties descriptorForOneTestFile) {
        Path pathExcelResults = null;
        try {
            // opening Excel file with results and checking correctness
            Path downloadFolder = TestingSuite.downloadFolder();
            final long thirtySecondsAgo = System.currentTimeMillis() - 30 * 1000;
            List<Path> allExcelFiles = Files.walk(downloadFolder)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xlsx"))
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis() > thirtySecondsAgo
                                    || Files.readAttributes(p, BasicFileAttributes.class).creationTime().toMillis() > thirtySecondsAgo;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            if (allExcelFiles == null || allExcelFiles.size() != 1) {
                String errorMessage = "error on " + NAME + " downloaded results: not exactly one Excel result file found";
                System.out.println(NAME + ": " + errorMessage);
                slackAPI.sendMessage(NAME, errorMessage);
            } else {
                pathExcelResults = allExcelFiles.get(0);
                List<SheetModel> sheetsModels = ExcelReader.readExcelFile(pathExcelResults);
                SheetModel firstSheet = sheetsModels.get(0);
                Map<Integer, List<CellRecord>> rowIndexToCellRecords = firstSheet.getRowIndexToCellRecords();

                // checking values on row index 0
                List<CellRecord> cellsRow = rowIndexToCellRecords.get(0);
                boolean correct0 = cellsRow.get(0).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_1_col_1"));
                boolean correct1 = cellsRow.get(1).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_1_col_2"));
                boolean correct2 = cellsRow.get(2).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_1_col_3"));
                if (!correct0 || !correct1 || !correct2) {
                    String errorMessage = "error on first row of results for test file " + descriptorForOneTestFile.getProperty("name");
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                }

                // checking values on row index 1
                cellsRow = rowIndexToCellRecords.get(1);
                correct0 = cellsRow.get(0).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_2_col_1"));
                correct1 = cellsRow.get(1).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_2_col_2"));
                correct2 = cellsRow.get(2).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_2_col_3"));
                if (!correct0 || !correct1 || !correct2) {
                    String errorMessage = "error on second row of results for test file " + descriptorForOneTestFile.getProperty("name");
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                }

                // checking values on row index 2
                cellsRow = rowIndexToCellRecords.get(2);
                correct0 = cellsRow.get(0).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_3_col_1"));
                correct1 = cellsRow.get(1).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_3_col_2"));
                correct2 = cellsRow.get(2).getRawValue().equals(descriptorForOneTestFile.getProperty("expected_result_row_3_col_3"));
                if (!correct0 || !correct1 || !correct2) {
                    String errorMessage = "error on third row of results for test file " + descriptorForOneTestFile.getProperty("name");
                    System.out.println(NAME + ": " + errorMessage);
                    slackAPI.sendMessage(NAME, errorMessage);
                }
                Files.deleteIfExists(pathExcelResults);

            }
        } catch (IOException | SlackApiException ex) {
            Logger.getLogger(TestUmigon.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (pathExcelResults != null) {
            try {
                Files.deleteIfExists(pathExcelResults);
            } catch (IOException ex) {
                Logger.getLogger(TestUmigon.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
