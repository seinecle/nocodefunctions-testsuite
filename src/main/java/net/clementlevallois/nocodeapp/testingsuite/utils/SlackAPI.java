/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeapp.testingsuite.utils;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author LEVALLOIS
 */
public class SlackAPI {

    private String apiKey = "";
    private Slack slack = null;
    private MethodsClient methods = null;
    private boolean sendMessages = false;

    public static void main(String args[]) throws IOException, SlackApiException {
        SlackAPI slackAPI = new SlackAPI(false);
        String message = ":wave: Hi from a bot written in Java!";
        slackAPI.sendMessage(message, true);
    }

    public SlackAPI(boolean sendessages) {
        this.apiKey = getApiKey();
        this.slack = Slack.getInstance();
        this.methods = slack.methods(apiKey);
        this.sendMessages = sendessages;
    }

    private static String getApiKey() {
        String resourcePath = "/private/read.txt";
        String apiKey = "";
        Properties properties = new Properties();
        try (InputStream inputStream = SlackAPI.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            properties.load(inputStream);
            apiKey = properties.getProperty("slack-key");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return apiKey;
    }

    public void sendMessage(String message, boolean exitAfterMessage) throws IOException, SlackApiException {
        System.out.println(message);
        if (!sendMessages) {
            return;
        }
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel("#robot-alerts") // Use a channel ID `C1234567` is preferable
                .text(message)
                .build();
        ChatPostMessageResponse response = methods.chatPostMessage(request);
        if (!response.isOk()) {
            System.out.println("error: " + response.getError());
        }

        if (exitAfterMessage) {
            // EXITING TO AVOID AN ACCUMULATION OF MSG
            System.exit(-1);
        }
    }

    public void sendMessage(String function, String message, boolean exitAfterMessage) throws IOException, SlackApiException {
        System.out.println(function + ": " + message);
        if (!sendMessages) {
            return;
        }
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel("#robot-alerts") // Use a channel ID `C1234567` is preferable
                .text("function: " + function + ", message: " + message)
                .build();

        ChatPostMessageResponse response = methods.chatPostMessage(request);
        if (!response.isOk()) {
            System.out.println("error: " + response.getError());
        }

        if (exitAfterMessage) {
            // EXITING TO AVOID AN ACCUMULATION OF MSG
            System.exit(-1);
        }
    }
}
