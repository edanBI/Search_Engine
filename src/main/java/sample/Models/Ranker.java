package sample.Models;

import javax.print.Doc;
import java.util.*;

public class Ranker {
    private TreeMap<String, DictionaryRecord> dictionary;
    private HashMap<String, Document> documents;
    private double avgdl;

    public Ranker(TreeMap<String, DictionaryRecord> dictionary, HashMap<String, Document> documents) {
        this.dictionary = dictionary;
        this.documents = documents;

        // calc the average document length
        this.documents.forEach((id, doc) -> avgdl += doc.getLength());
        avgdl = avgdl / this.documents.size();
    }

    /**
     * calc the ranking score foreach document
     * @param queryTerms is the query
     * @return 50 documents with the highest score
     */
    ArrayList<Document> rank(List<String> queryTerms, HashMap<String, HashMap<String, TermData>> docsAndTerms) {
        //      <docId, rankVal>
        double score, bm25, tf_idf_cosine;
        HashMap<String, Double> hash_scores = new HashMap<>();
        HashMap<String, TermData> intersectionSet;

        // calculate each document score
        for (Map.Entry<String, Document> d : documents.entrySet()) {
            intersectionSet = new HashMap<>();
            for (String q : queryTerms){
                if (docsAndTerms.get(d.getKey()).containsKey((q)))
                    intersectionSet.put(q, docsAndTerms.get(d.getKey()).get(q));
            }

            bm25 = BM25(intersectionSet, d.getValue());
            tf_idf_cosine = TF_IDF_Cosine(docsAndTerms.get(d.getKey()), d.getKey(), queryTerms);

            score = 0.6*bm25 + 0.4*tf_idf_cosine;
            hash_scores.put(d.getKey(), score);
        }

        // get the top 50's in hash_scores
        ArrayList<Document> ranked_arr = new ArrayList<>(50);
        Map<Document, Double> map = sortedMap(hash_scores);
        Object[] sorted = map.keySet().toArray();
//        Object[] sorted = sortedMap(hash_scores).keySet().toArray();
        for (int i = 0; i < 50 && i < sorted.length; i++) {
            ranked_arr.add((Document) sorted[i]);
        }
        return ranked_arr;
    }

    private double TF_IDF_Cosine(HashMap<String, TermData> docTerms, String docId, List<String> query) {
        double w_ij, w_iq, lowerSum, sum2_wij = 0.0, sum2_wiq = 0.0, upperSum = 0.0;

        for(Map.Entry<String, TermData> docTerm : docTerms.entrySet()){
            w_ij = (docTerm.getValue().gettF() / (double)documents.get(docId).getMax_tf()) * (dictionary.get(docTerm.getKey()).getIdf());
            w_iq = query.contains(docTerm.getKey()) ? 1 : 0;
            upperSum += w_ij * w_iq;

            sum2_wij += Math.pow(w_ij, 2);
            sum2_wiq += Math.pow(w_iq, 2);
        }

        lowerSum = Math.sqrt(sum2_wij * sum2_wiq);

        return upperSum / lowerSum;
    }

    private double BM25(HashMap<String, TermData> intersectionSet, Document d) {
        final double k1 = 0.75; final double b = 1.2; // bm25 constants
        double score = 0.0, tmp;

        for(Map.Entry<String, TermData> w : intersectionSet.entrySet()) {
            tmp = dictionary.get(w.getKey()).getIdf() * (k1+1) * w.getValue().gettF();
            tmp /= w.getValue().gettF() + k1 * (1 - b + b * (d.getLength() / avgdl));
            score = w.getValue().getImportant() ? (tmp*2) : tmp; // if it's an important term then it's weight will double
        }
        return score;
    }

    // return a map sorted by VALUE
    private Map<Document, Double> sortedMap(HashMap<String, Double> unsorted) {
        List<Map.Entry<String, Double>> list = new ArrayList<>(unsorted.entrySet());
        list.sort(new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                if (o1.getValue() > o2.getValue()) return -1;
                else if (o1.getValue().equals(o2.getValue())) return 0;
                else return 1;
            }
        });

        Map<Document, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : list) {
            sorted.put(documents.get(entry.getKey()), entry.getValue());
        }
        return sorted;
    }
}
