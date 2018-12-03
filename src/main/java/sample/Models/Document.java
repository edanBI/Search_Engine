package sample.Models;

public class Document {
    private String doc_id;
    private String city;
    private int max_tf;
    private int unique_words;


    public Document(String doc_id, int max_tf, int unique_words) {
        this.doc_id = doc_id;
        this.max_tf = max_tf;
        this.unique_words = unique_words;

    }

    public String getDoc_id() {
        return doc_id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}