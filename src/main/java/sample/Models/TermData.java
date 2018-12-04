package sample.Models;

import java.util.LinkedList;

public class TermData {
    private Boolean isImportant ;
    private int tF;
    private StringBuilder place;

    public TermData(Boolean isImportant, int tF, int theFirstPlace) {
        this.isImportant = isImportant;
        this.tF = tF;
        this.place = new StringBuilder(Integer.toString(theFirstPlace));
    }

    public TermData(Boolean isImportant, int tF, String place, int firstIndex) {
        this.isImportant = isImportant;
        this.tF = tF;
        this.place = new StringBuilder(place);
        this.place.append(","+Integer.toString(firstIndex));
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

    public String getPlaces() {
        return place.toString();
    }

    public void setPlace(int place) {
        this.place.append(","+Integer.toString(place));
    }
}