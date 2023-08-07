package org.teacherbot;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NER {


    private String modelFileName;

    public NER(String modelFileName) {
        this.modelFileName = modelFileName;
    }

    public void SentenceTest() throws IOException {

        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize("John is 26 years old. His best friend's name is Leonard. He has a sister named Penny.");

        InputStream inputStreamNameFinder = getClass().getResourceAsStream("/models/en-ner-person.bin");
        TokenNameFinderModel model = new TokenNameFinderModel(inputStreamNameFinder);
        NameFinderME nameFinderME = new NameFinderME(model);
        List<Span> spans = Arrays.asList(nameFinderME.find(tokens));
        List<String> names = new ArrayList<String>();
        int k = 0;
        for (Span s : spans) {
            names.add("");
            for (int index = s.getStart(); index < s.getEnd(); index++) {
                names.set(k, names.get(k) + tokens[index]);
            }
            k++;
        }
        for (String name : names) {
            System.out.println("Name = " + name.toString());
        }
    }




    //class collect nouns
    public List<String> collectNouns(String sentence) throws IOException {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(sentence);

        InputStream inputStreamNameFinder = getClass().getResourceAsStream(modelFileName);
        TokenNameFinderModel model = new TokenNameFinderModel(inputStreamNameFinder);
        NameFinderME nameFinderME = new NameFinderME(model);
        List<Span> spans = Arrays.asList(nameFinderME.find(tokens));
        List<String> names = new ArrayList<>();
        int k = 0;
        for (Span s : spans) {
            names.add("");
            for (int index = s.getStart(); index < s.getEnd(); index++) {
                names.set(k, names.get(k) + tokens[index]);
            }
            k++;
        }
        inputStreamNameFinder.close();
        return names;
    }


}
