package sample.Models;

public class Document {
    private String doc_id;
    private int max_tf;
    private int unique_words;
    private int length;

    public Document(String doc_id, int max_tf, int unique_words) {
        this.doc_id = doc_id;
        this.max_tf = max_tf;
        this.unique_words = unique_words;
        this.length = 0;
    }

    String getDoc_id() {
        return doc_id;
    }

    public int getMax_tf() {
        return max_tf;
    }

    public int getUnique_words() {
        return unique_words;
    }

    void updateLength(int val) {
        this.length += val;
    }

    void setMax_tf(int max_tf) {
        this.max_tf = max_tf;
    }

    void setUnique_words(int unique_words) {
        this.unique_words = unique_words;
    }
}