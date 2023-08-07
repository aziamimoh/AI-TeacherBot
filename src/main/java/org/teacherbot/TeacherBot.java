package org.teacherbot;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TeacherBot {

    public static String execCmd(String cmd) throws Exception {
        Process process = null;
        StringBuilder output;

        try {
            process = Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new Exception("Input error");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            output = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        }
        process.waitFor();

        return output.toString();
    }

    public static String removeHtmlTags(String htmlString) {
        Pattern pattern = Pattern.compile("<[^>]*>");
        Matcher matcher = pattern.matcher(htmlString);
        return matcher.replaceAll("");
    }

    private static String convertPluralToSingular(String pluralNoun) {
        String singularNoun = pluralNoun;

        // Basic plural forms
        if (pluralNoun.endsWith("ies")) {
            singularNoun = pluralNoun.substring(0, pluralNoun.length() - 3) + "y";
        } else if (pluralNoun.endsWith("s")) {
            singularNoun = pluralNoun.substring(0, pluralNoun.length() - 1);
        }

        return singularNoun;
    }

    public static void main(String[] args) throws Exception {
        // 1 - Ask user for a filename with text
        Scanner scanner = new Scanner(System.in);
        String defaultFileName = "input.txt";

        System.out.print("Enter input text file (default ./input.txt): ");
        String filename = scanner.nextLine();
        boolean fileNameExists = false;
        if (filename.isEmpty()) {
            filename = "./target/classes/input.txt";
            if (Files.exists(Path.of(filename))) {
                fileNameExists = true;
            } else {
                filename = defaultFileName;
            }
        }

        if (!fileNameExists) {
            throw new Exception("Input file " + filename + "does not exist");
        }

        // Load text file into string

        Path path = Paths.get(filename);
        String fileContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        String newline = "\\r?\\n";
        fileContent = removeHtmlTags(fileContent).replaceAll(newline, " ");

        NER ner = new NER("/models/en-ner-person.bin");
        try {
            System.out.println("My working directory is " + execCmd("pwd"));
            InputStream inputStream = new FileInputStream("./target/classes/models/en-sent.bin");
            SentenceModel model = new SentenceModel(inputStream);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);

            // Read the text file
            String content = new String(fileContent.getBytes(), StandardCharsets.UTF_8);

            String[] sentences = sentenceDetector.sentDetect(content);

            // Close the input stream
            inputStream.close();

            // For each sentence, invoke NER to collect nouns
            InputStream posModelStream = new FileInputStream("./target/classes/models/en-pos-perceptron.bin");
            POSModel posModel = new POSModel(posModelStream);
            POSTaggerME posTagger = new POSTaggerME(posModel);

            // Process each sentence and collect nouns using NER
            List<String> sentencesWithNouns = new ArrayList<>();
            HashMap<String, Integer> nouns = new HashMap<String, Integer>();
            // Process each sentence for nouns
            for (String sentence : sentences) {
                // Tokenize the sentence
                String[] tokens = sentence.split("\\s+");

                // Perform part-of-speech tagging
                String[] tags = posTagger.tag(tokens);

                // Identify nouns (tags starting with "NN" are nouns)
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].contains("openstax") ||
                            tokens[i].contains("(") ||
                            tokens[i].contains(")") ||
                            tokens[i].equals("it") ||
                            tokens[i].equals("others") ||
                            tokens[i].equals("them")) {
                        continue;
                    }
                    if (tags[i].startsWith("NNS")) {
                        String singularForm = convertPluralToSingular(tokens[i]);
                        tokens[i] = singularForm;
                    }
                    if (tags[i].startsWith("NN")) {
                        //System.out.println("Noun: " + tokens[i]);
                        if (!nouns.containsKey(tokens[i])) {
                            nouns.put(tokens[i], 0);
                        }
                        Integer numOccurences = nouns.get(tokens[i]);
                        numOccurences++;
                        nouns.put(tokens[i], numOccurences);
                    }
                }
            }

            // Send query to openai with sentences in 7)
            Set<String> keys = nouns.keySet();
            List<String> nounList = new ArrayList<>();
            // Print all keys
            for (String key : keys) {
                key = key.replace(".", "");
                key = key.replace(",", "");
                if (!key.contains("openstax")) {
                    nounList.add(key);
                }
            }

            String[] nounArray = nounList.toArray(new String[0]);
            int rand = (int) (Math.random() * (nounArray.length - 1));
            String pickNoun = nounArray[rand];
            System.out.print("Please write about what " + pickNoun + " means, as mentioned in the reference: ");
            String userInput = scanner.nextLine();

            String matchedSentences = "";
            // Find which lines have the pickNoun
            for (String s : sentences) {
                if (s.contains(pickNoun)) {
                    matchedSentences = matchedSentences + " " + s;
                }
            }

            // Send the query to OpenAI API
            String query = "Please find the difference between the user's note and the reference not. The user's note is as follows:" + userInput + ".";
            if (matchedSentences.length() > 4096) {
                matchedSentences = matchedSentences.substring(0, 2048);
            }
            query = query + "The reference note is as follows: " + matchedSentences + ".";
            String response = sendQueryToOpenAI(query);

            // Process the response from OpenAI
            // Handle the results according to your application's needs
            //System.out.println("Response from OpenAI:");
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String sendQueryToOpenAI(String query) {

        String response = "";

        ///////
        String apiKey = System.getenv("OPENAI_TOKEN");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("API key not found in environment variable OPENAI_API_KEY");
            return response;
        }
        String endpointUrl = "https://api.openai.com/v1/chat/completions";

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
            response = sendPostRequest(apiKey, endpointUrl, requestBody);
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


    public static String sendPostRequest(String apiKey, String endpointUrl, String requestBody) throws IOException, URISyntaxException {
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
