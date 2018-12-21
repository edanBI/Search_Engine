package sample.Models;

import java.util.HashSet;
import java.util.TreeMap;

public class Ranker {
    private TreeMap<String, DictionaryRecord> dictionary;

    public Ranker(TreeMap<String, DictionaryRecord> dictionary) {
        this.dictionary = dictionary;
    }

    public Document[] rank(HashSet<Document> docs, String q) {
        Document[] rankedDocs = new Document[50]; // ranked answer
        double b = 0.75, k = 1.2; // k = [1.2, 2.0]
        int numDocs = docs.size();
        double avgdl = 0.0;


        return rankedDocs;
    }
}
