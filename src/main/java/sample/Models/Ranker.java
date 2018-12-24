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
        avgdl = avgdl / this.documents.size();
    }

    // TODO need to add cosine formula
    /**
     * calc the ranking score foreach document
     * @param queryTerms is the query
     * @return 50 documents with the highest score
     */
    ArrayList<Document> rank(List<String> queryTerms, HashMap<String, HashMap<String, TermData>> docsAndTerms) {
        // BM25 constants
        final double k1 = 0.75;
        final double b = 1.2;

        //       <docId , rankVal>
        HashMap<String, Double> hash_scores = new HashMap<>();
        Set<String> intersect;
        double score, w_score;
        int c_w_d;

        for (Map.Entry<String, HashMap<String, TermData>> docEntry : docsAndTerms.entrySet()) {
            intersect = new HashSet<>();
            score = 0.0;

            // create the set of all the word which are both in the document and the query
            for (String q : queryTerms){
                if (docEntry.getValue().containsKey(q))
                    intersect.add(q);
            }

            // calc BM25 formula foreach word w in the intersection set
            for (String w : intersect) {
                c_w_d = docEntry.getValue().get(w).gettF();
                w_score = (k1+1) * c_w_d * dictionary.get(w).getIdf();
                w_score /= c_w_d + k1*(1-b+b*(getDocumentByID(docEntry.getKey()).getLength() / avgdl));
                // give the word a bigger weight if it was tagged as important while parsing
                if (docEntry.getValue().get(w).getImportant())
                    w_score *= 1.5;
                // update the total sum
                score += w_score;
            }
            hash_scores.put(docEntry.getKey(), score);
        }

        return new ArrayList<>(sortedMap(hash_scores).keySet());
    }

    private Document getDocumentByID(String id) {
        Iterator<Map.Entry<String, Document>> it = documents.entrySet().iterator();
        Map.Entry<String, Document> entry;
        while ((entry = it.next()) != null) {
            if (entry.getKey().equals(id))
                return entry.getValue();
        }
        return null;
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
            sorted.put(getDocumentByID(entry.getKey()), entry.getValue());
        }
        return sorted;
    }
}
