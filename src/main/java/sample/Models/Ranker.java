package sample.Models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Ranker {
    private TreeMap<String, DictionaryRecord> dictionary;
    private HashSet<Document> documents;
    private double avgdl;
    private String postingPath;
    private boolean stem;

    public Ranker(TreeMap<String, DictionaryRecord> dictionary, HashSet<Document> documents, String postingPath, boolean stem) {
        this.dictionary = dictionary;
        this.documents = documents;
        this.postingPath = postingPath;
        this.stem = stem;

        // calc the average document length
        this.documents.forEach(doc -> avgdl+=doc.getLength());
        avgdl = avgdl / this.documents.size();
    }

    /**
     * calc the ranking score foreach document
     * @param queryTerms is the query
     * @return 50 documents with the highest score
     */
    public String[] rank(HashMap<String, TermData> queryTerms) {
        // BM25 constants
        final double k1 = 0.75;
        final double b = 1.2;

        Hashtable<String, Double> hash_scores = new Hashtable<>();
        double score, w;
        int c_w_d;

        for (Document d : documents) {
            score = 0; c_w_d = 0;
            for (String q : queryTerms.keySet()) {
                try { c_w_d = docTF(q.charAt(0), dictionary.get(q).getPtr(), d.getDoc_id()); }
                catch (IOException e) { e.printStackTrace(); }

                //w = queryTerms.get(q).gettF();
                w = dictionary.get(d.getDoc_id()).getIdf();
                w *= ((k1 + 1) * c_w_d) / (c_w_d + k1*(1-b+b*(d.getLength()/avgdl)));
                score += w;
                if (queryTerms.get(q).getImportant()) // if the word is important then it will have more weight
                    score *= 2;
            }
            hash_scores.put(d.getDoc_id(), score);
        }

        //insert the top 50 into the array in descending order. cell 0 is the most relevant document
        String[] rankedDocs = new String[50];
        double max;
        for (int i=0; i<50; i++){
            max = Collections.max(hash_scores.values());
            Iterator<String> it = hash_scores.keySet().iterator();
            String curr;
            while ((curr=it.next()) != null) {
                if (hash_scores.get(curr) == max) {
                    rankedDocs[i] = curr;
                    hash_scores.remove(curr);
                    break;
                }
            }
        }
        return rankedDocs;
    }

    // read the specific line of the term from the posting file to get the value of the TF in the document.
    private int docTF(char c, int idx, String docId) throws IOException {
        c = Character.toLowerCase(c);
        BufferedReader in;
        if      (c == 'a' || c=='b') in = new BufferedReader(new FileReader(postingPath + "/A-B.txt"));
        else if (c == 'c' || c=='d') in = new BufferedReader(new FileReader(postingPath + "/C-D.txt"));
        else if (c == 'e' || c=='f') in = new BufferedReader(new FileReader(postingPath + "/E-F.txt"));
        else if (c == 'g' || c=='h') in = new BufferedReader(new FileReader(postingPath + "/G-H.txt"));
        else if (c == 'i' || c=='j') in = new BufferedReader(new FileReader(postingPath + "/I-J.txt"));
        else if (c == 'k' || c=='l') in = new BufferedReader(new FileReader(postingPath + "/K-L.txt"));
        else if (c == 'm' || c=='n') in = new BufferedReader(new FileReader(postingPath + "/M-N.txt"));
        else if (c == 'o' || c=='p') in = new BufferedReader(new FileReader(postingPath + "/O-P.txt"));
        else if (c == 'q' || c=='r') in = new BufferedReader(new FileReader(postingPath + "/Q-R.txt"));
        else if (c == 's' || c=='t') in = new BufferedReader(new FileReader(postingPath + "/S-T.txt"));
        else if (c == 'u' || c=='v') in = new BufferedReader(new FileReader(postingPath + "/U-V.txt"));
        else if (c == 'w' || c=='x') in = new BufferedReader(new FileReader(postingPath + "/W-X.txt"));
        else if (c == 'y' || c=='z') in = new BufferedReader(new FileReader(postingPath + "/Y-Z.txt"));
        else                         in = new BufferedReader(new FileReader(postingPath + "/$-9.txt"));

        // ignore all the lines before the index
        for (int i=0; i<idx; i++)
            in.readLine();
        // once index has been reached, start searching for the line with the right docId
        String line = in.readLine();
        while (!line.substring(line.indexOf("docId")+6, line.indexOf(", tf=")).equals(docId))
            line = in.readLine();
        in.close();
        // return the TF value from that line
        return Integer.parseInt(line.substring(line.indexOf("tf=")+3, line.indexOf(", positions=")));
    }
}
