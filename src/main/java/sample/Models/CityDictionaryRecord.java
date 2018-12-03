package sample.Models;

public class CityDictionaryRecord extends DictionaryRecord {
    private String country;
    private String currency;
    private int population;

    public CityDictionaryRecord(String term, int initFreq, boolean isCapital, String country, String currency, int population) {
        super(term, initFreq, isCapital);
        this.country = country;
        this.currency = currency;
        this.population = population;
    }

    public CityDictionaryRecord(DictionaryRecord other, String country, String currency, int population) {
        super(other);
        this.country = country;
        this.currency = currency;
        this.population = population;
    }

    public CityDictionaryRecord(String term, int df, int freq, String country, String currency, int population) {
        super(term, df, freq);
        this.country = country;
        this.currency = currency;
        this.population = population;
    }

    public CityDictionaryRecord(String term, int df, int totalFreq, int ptr, double idf, String country, String currency, int population) {
        super(term, df, totalFreq, ptr, idf);
        this.country = country;
        this.currency = currency;
        this.population = population;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency() {
        return currency;
    }

    public int getPopulation() {
        return population;
    }
}
