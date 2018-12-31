package sample.Models;
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
        avgdl /= this.documents.size();
    }

    /**
     * calc the ranking score foreach document
     * @param queryTerms is the query
     * @return 50 documents with the highest score
     */
    ArrayList<Document> rank(/*List<String> restTerms, */List<String> queryTerms, HashMap<String, HashMap<String, TermData>> docsAndTerms) {
        HashMap<String, Double> hash_scores = new HashMap<>();

        // calculate each document score
        for (Map.Entry<String, Document> d : documents.entrySet()) {
            if (!docsAndTerms.containsKey(d.getKey())) {
                hash_scores.put(d.getKey(), 0.0); // because Count(w,d)==0
            }
            else {
                //bm25 = BM25(docsAndTerms.get(d.getKey()), d.getValue()/*,restTerms*/);

                // BM25 calculationx
                final double k1 = 1.2; final double b = 0.75; // bm25 constants
                double score, bm25=0.0, important=0.0, notImportant=0.0, sumImportant, tmp;
                for(Map.Entry<String, TermData> w : docsAndTerms.get(d.getKey()).entrySet()) {
                    double wTf = w.getValue().gettF();
                    double idf = dictionary.get(w.getKey()).getIdf();
                    tmp =  (k1+1) * wTf * idf;
                    tmp /= wTf + k1 * ( (1 - b) + b * ( (double)d.getValue().getLength() / avgdl));
                    bm25 += tmp;
                    important++;
/*                    if (w.getValue().getImportant()) important++;
                    else notImportant++;*/
                }

                sumImportant = important /*+ notImportant*/;

                score = 0.7*bm25 + 0.3*sumImportant;
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
}