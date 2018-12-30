package sample.Models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Ranker {
    private TreeMap<String, DictionaryRecord> dictionary;
    private HashMap<String, Document> documents;
    private String docsTermTfPath;
    private double avgdl;

    public Ranker(TreeMap<String, DictionaryRecord> dictionary, HashMap<String, Document> documents, String postingPath) {
        this.dictionary = dictionary;
        this.documents = documents;
        this.docsTermTfPath = postingPath + "/ProgramData/Documents-Term-TF/";

        // calc the average document length
        this.documents.forEach((id, doc) -> avgdl += doc.getLength());
        avgdl /= this.documents.size();
    }

    /**
     * calc the ranking score foreach document
     * @param queryTerms is the query
     * @return 50 documents with the highest score
     */
    ArrayList<Document> rank(/*List<String> restTerms, */List<String> queryTerms, HashMap<String, HashMap<String, TermData>> docsAndTerms) {
        //      <docId, rankVal>
        double score, bm25, tf_idf_cosine;
        HashMap<String, Double> hash_scores = new HashMap<>();

        // calculate each document score
        for (Map.Entry<String, Document> d : documents.entrySet()) {
            if (!docsAndTerms.containsKey(d.getKey())) {
                hash_scores.put(d.getKey(), 0.0); // because Count(w,d)==0
            }
            else {
                bm25 = BM25(docsAndTerms.get(d.getKey()), d.getValue()/*,restTerms*/);
                tf_idf_cosine = TF_IDF_Cosine(docsAndTerms.get(d.getKey()), d.getKey(), queryTerms);

                score = 0.6*(bm25) + 0.4*(tf_idf_cosine);
                //score = bm25;
                //score = tf_idf_cosine;
                hash_scores.put(d.getKey(), score);
            }
        }

        // get the top 50's in hash_scores
        ArrayList<Document> ranked_arr = new ArrayList<>(50);
        Map<Document, Double> map = sortedMap(hash_scores);
        Object[] sorted = map.keySet().toArray();
        int size = sorted.length > 50 ? 50 : sorted.length;
        for (int i = 0; i < size; i++) {
            ranked_arr.add((Document) sorted[i]);
        }
        return ranked_arr;
    }

    private double TF_IDF_Cosine(HashMap<String, TermData> docTerms, String docId, List<String> query) {
        double lowerSum, upperSum = 0.0, idf, tf, max_tf;

        for (String q : docTerms.keySet()) {
            idf = dictionary.get(q).getIdf();
            tf = (double) docTerms.get(q).gettF();
            max_tf = (double) documents.get(docId).getMax_tf();
            upperSum += (tf/ max_tf) * idf;
        }

        lowerSum = Math.sqrt(getDocWeight(docId) * query.size());

        return (upperSum / lowerSum);
    }

    private double BM25(HashMap<String, TermData> intersectionSet,/* List<String> restTerms, */Document d) {
        final double k1 = 1.3; final double b = 0.75; // bm25 constants
        double score = 0.0, tmp;


        for(Map.Entry<String, TermData> w : intersectionSet.entrySet()) {
            double idf = dictionary.get(w.getKey()).getIdf();
            tmp =  (k1+1) * w.getValue().gettF() * idf;
            tmp /= w.getValue().gettF() + k1 * ( (1 - b) + b * ( (double)d.getLength() / avgdl));
            if (w.getValue().getImportant()) // if it's an important term then it's weight will double
                tmp *= 2;
            /*if (restTerms.contains(w.getKey()))
                tmp*=0.3 ;*/
            score += tmp;
        }
        return score;
    }

    // return a map sorted by VALUE
    private Map<Document, Double> sortedMap(HashMap<String, Double> unsorted) {
        List<Map.Entry<String, Double>> list = new ArrayList<>(unsorted.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map.Entry<String, Double> curr;
        Map<Document, Double> sorted = new LinkedHashMap<>();
        for (int i=list.size()-1; i>0; i--) {
            curr = list.remove(i);
            sorted.put(documents.get(curr.getKey()), curr.getValue());
        }
        return sorted;
    }

    private double getDocWeight(String id) {
        ArrayList<String> term_tf_arr = new ArrayList<>(150);
        Document document = documents.get(id);
        String currTerm;
        int max_tf = document.getMax_tf(), tf;
        double totalWeight = 0.0, idf=0.0, docWeight;

        try {
            BufferedReader br = new BufferedReader(new FileReader(docsTermTfPath + id + ".txt"));
            String line;
            while ((line = br.readLine()) != null) {
                term_tf_arr.add(line);
            }

            for (String term_tf : term_tf_arr) {
                currTerm = term_tf.substring(0, term_tf.lastIndexOf('_'));
                tf = Integer.parseInt(term_tf.substring(term_tf.lastIndexOf('_')+1));
                if (dictionary.containsKey(currTerm)) {
                    idf = dictionary.get(currTerm).getIdf();
                }
                else if (dictionary.containsKey(currTerm.toUpperCase())) {
                    idf = dictionary.get(currTerm.toUpperCase()).getIdf();
                }
                else if (dictionary.containsKey(currTerm.toLowerCase())) {
                    idf = dictionary.get(currTerm.toLowerCase()).getIdf();
                }
                docWeight = ((double)tf / (double)max_tf) * idf;
                totalWeight += Math.pow(docWeight, 2);
            }
        }
        catch (IOException e) { e.printStackTrace(); }

        return totalWeight;
    }
}