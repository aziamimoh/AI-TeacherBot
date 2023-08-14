package org.teacherbot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class OpenAI {
    final String endpointUrl = "https://api.openai.com/v1/chat/completions";
    String apiKey;

    public OpenAI() throws Exception {
        apiKey = System.getenv("OPENAI_TOKEN");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("API key not found in environment variable OPENAI_API_KEY");
        }
    }

    public String sendQueryToOpenAI(String query) {
        String response = "";

        String requestBody = "{\n" +
                "    \"model\": \"gpt-3.5-turbo-16k-0613\",\n" +
                "    \"messages\": [\n" +
                "        {\n" +
                "            \"role\": \"system\",\n" +
                "            \"content\": \"You are a sympathetic teacher.\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"role\": \"user\",\n" +
                "            \"content\": \"" + query + "\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        try {
            response = sendPostRequest(requestBody);
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(response);

            // Extract the "id" from the response
            String id = (String) jsonObject.get("id");
            //System.out.println("ID: " + id);

            JSONArray choicesArray = (JSONArray) jsonObject.get("choices");
            String responseText = "";
            for (int i = 0; i < choicesArray.size(); i++) {
                JSONObject choiceObject = (JSONObject) choicesArray.get(i);
                HashMap<String, JSONObject> message = (HashMap<String, JSONObject>) choiceObject.get("message");
                responseText = String.valueOf(message.get("content"));
            }

            //System.out.println("Response: " + response);
            //System.out.println("Response: " + responseText);
            response = responseText;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public String sendPostRequest(String requestBody) throws IOException, URISyntaxException {
        URI uri = new URI(endpointUrl);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] requestBodyBytes = requestBody.getBytes();
            outputStream.write(requestBodyBytes);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                return responseBuilder.toString();
            }
        } else {
            throw new IOException("Request failed with response code: " + responseCode);
        }
    }
}
