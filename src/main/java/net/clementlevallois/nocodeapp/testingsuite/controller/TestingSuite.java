/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeapp.testingsuite.controller;

import net.clementlevallois.nocodeapp.testingsuite.functions.TestInterface;
import com.slack.api.methods.SlackApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.clementlevallois.nocodeapp.testingsuite.functions.TestUmigon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.clementlevallois.nocodeapp.testingsuite.functions.TestTopics;
import net.clementlevallois.nocodeapp.testingsuite.utils.SlackAPI;
import net.clementlevallois.utils.Clock;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 *
 * @author LEVALLOIS
 */
public class TestingSuite {

    private static final boolean SEND_MESAGES_TO_SLACK = true;
    private static final boolean TESTING_LOCALLY_DEPLOYED = false;
    private static final boolean HEADLESS = true;
    private static final boolean EXIT_AFTER_MESSAGE = true;
    private static final boolean SILENT_LOGGING = true;
    private static SlackAPI slackAPI;

    public static void main(String[] args) throws IOException, SlackApiException {
        slackAPI = new SlackAPI(SEND_MESAGES_TO_SLACK);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        slackAPI.sendMessage("initializing the test suite", false);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                runTests();
                scheduler.schedule(this, 1, TimeUnit.HOURS);
            }
        };

        scheduler.execute(task);
    }

    private static void runTests() {

        List<WebDriver> webDrivers = new ArrayList();
        ChromeOptions chromeOptions = new ChromeOptions();
        String userAgent = "--user-agent=Mozilla/5.0 (compatible; MyRobot/1.0; +http://www.example.com/robot) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
        String headlessParam = "--headless";
        String headlessParamForGoogle = "--headless=new";
        if (HEADLESS) {
            List<String> arguments = List.of(userAgent, headlessParamForGoogle);
            chromeOptions.addArguments(arguments);
        } else {
            List<String> arguments = List.of(userAgent);
            chromeOptions.addArguments(arguments);
        }
        Map<String, Object> prefs = new HashMap();
        prefs.put("intl.accept_languages", "fr");
        prefs.put("intl.selected_languages", "fr");
        boolean testingFromWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Properties properties = loadProperties();
        String downloadDirectory;
        if (testingFromWindows) {
            downloadDirectory = properties.getProperty("local-path-download");
        } else {
            downloadDirectory = properties.getProperty("local-path-download");
        }
        prefs.put("download.default_directory", downloadDirectory);
        chromeOptions.setExperimentalOption("prefs", prefs);
        Clock clock = new Clock("initializing the Chrome Driver", SILENT_LOGGING);
        WebDriver chromeDriver = new ChromeDriver(chromeOptions);
        webDrivers.add(chromeDriver);
        clock.closeAndPrintClock();
        TestInterface testUmigon = new TestUmigon(slackAPI, EXIT_AFTER_MESSAGE);
        TestInterface testTopics = new TestTopics(slackAPI, EXIT_AFTER_MESSAGE);
        List<TestInterface> tests = List.of(testUmigon, testTopics);
        //            List<TestInterface> tests = List.of(testTopics);
//        List<TestInterface> tests = List.of(testUmigon);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        System.out.print(formattedDateTime + ": testing ");
        for (TestInterface test : tests) {
            System.out.print(test.getName());
            deleteFilesInDownloadFolder();
            test.conductTests(webDrivers);
        }
        System.out.println();
    }

    public static String domain() {
        boolean testingFromWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String domain;
        if (testingFromWindows & TESTING_LOCALLY_DEPLOYED) {
            domain = "http://localhost:8080/nocode-app-web-front/";
        } else {
            domain = "https://nocodefunctions.com";
        }
        return domain;
    }

    private static Properties loadProperties() {
        String resourcePath = "/private/read.txt";
        Properties properties = new Properties();
        try (InputStream inputStream = TestingSuite.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static Path rootFolder() {
        Properties properties = loadProperties();
        boolean testingFromWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Path rootFolder;
        if (testingFromWindows) {
            String pathLocal = properties.getProperty("local-path");
            rootFolder = Path.of(pathLocal);
        } else {
            String pathServer = properties.getProperty("server-path");
            rootFolder = Path.of(pathServer);
        }
        return rootFolder;
    }

    public static Path downloadFolder() {
        boolean testingFromWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Properties properties = loadProperties();
        Path rootFolder;
        if (testingFromWindows) {
            String pathLocal = properties.getProperty("local-path-download");
            rootFolder = Path.of(pathLocal);
        } else {
            String pathServer = properties.getProperty("server-path-download");
            rootFolder = Path.of(pathServer);
        }
        return rootFolder;
    }

    private static void deleteFilesInDownloadFolder() {
        Path downloadFolder = downloadFolder();
        final long oneHourAgo = Instant.now().minusSeconds(3600).toEpochMilli();
        try (Stream<Path> paths = Files.walk(downloadFolder)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileExtension = path.toFile().getName();
                        boolean shouldBeDeleted = fileExtension.endsWith(".gexf") || fileExtension.endsWith(".xlsx");
                        return shouldBeDeleted;
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                        }
                    });
        } catch (IOException ex) {
            Logger.getLogger(TestingSuite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static boolean isSILENT_LOGGING() {
        return SILENT_LOGGING;
    }

}
