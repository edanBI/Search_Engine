package sample.Models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Document {
    private String doc_id;
    private int max_tf;
    private int unique_words;
    private int length;
    private String entities;

    Document(String doc_id, int max_tf, int unique_words) {
        this.doc_id = doc_id;
        this.max_tf = max_tf;
        this.unique_words = unique_words;
        this.length = 0;
        this.entities = "";
    }

    public Document(String doc_id, int max_tf, int unique_words, int length) {
        this.doc_id = doc_id;
        this.max_tf = max_tf;
        this.unique_words = unique_words;
        this.length = length;
        this.entities = "";
    }

    String getDoc_id() {
        return doc_id;
    }

    public StringProperty getPropertyDoc_id() {
        return new SimpleStringProperty(doc_id);
    }

    public int getMax_tf() {
        return max_tf;
    }

    public int getUnique_words() {
        return unique_words;
    }

    int getLength() {
        return length;
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

    public String getEntities() {
        return entities;
    }

    public void setEntities(String arg) {
        entities = arg;
    }

    @Override
    public String toString() {
        return "Document{" +
                "doc_id='" + doc_id +
                ", max_tf=" + max_tf +
                ", unique_words=" + unique_words +
                ", length=" + length +
                ", entities=" + entities +
                '}';
    }
}