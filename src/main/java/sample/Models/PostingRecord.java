package sample.Models;

public class PostingRecord {
    private String docId;
    private int tf;

    public PostingRecord(String docId, int tf) {
        this.docId = docId;
        this.tf = tf;
    }

    public String getDocId() {
        return docId;
    }

    public int getTf() {
        return tf;
    }

    @Override
    public String toString()
    {
        return "docId=" + docId + ", tf=" + tf;
    }
}
