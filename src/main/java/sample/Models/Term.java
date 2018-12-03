package sample.Models;

public class Term {
    private Boolean isImportant ;
    //private Boolean toStem;
    private int tF;

    public Term(Boolean isImportant, /*Boolean toStem,*/ int tF) {
        this.isImportant = isImportant;
        //this.toStem = toStem;
        this.tF = tF;
    }

    public int gettF() {
        return tF;
    }

    public void settF(int tF) {
        this.tF = tF;
    }

    public Boolean getImportant() {
        return isImportant;
    }

    public void setImportant(Boolean important) {
        isImportant = important;
    }
}