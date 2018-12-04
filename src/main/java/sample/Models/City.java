package sample.Models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

public class City {
    private String country;
    private String currency;
    private String population;
    private LinkedList<String> docsRepresent;

    public City(String city, String initDocId) {
        docsRepresent = new LinkedList<>();
        docsRepresent.addFirst(initDocId);
        infoByCity(city);
    }
    public void addDocId(String id) {
        this.docsRepresent.addLast(id);
    }

    public LinkedList<String> getDocsRepresent() {
        return docsRepresent;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency()
    {
        return currency;
    }

    public String getPopulation()
    {
        return population;
    }

    private void infoByCity(String city)
    {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder country = new StringBuilder();
        StringBuilder currency = new StringBuilder();
        StringBuilder population = new StringBuilder();
        String inputLine;
        try {
            URL oracle = new URL("https://restcountries.eu/rest/v2/capital/" + city);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(oracle.openStream()));
            while ((inputLine = in.readLine()) != null)
                stringBuilder.append(inputLine);
            in.close();
            country.append(stringBuilder.toString().substring(10, stringBuilder.indexOf(",") - 1));
            if (country.toString().isEmpty()) {
                stringBuilder.delete(0,stringBuilder.length());
                this.country = null;
            } else {
                stringBuilder.delete(0, stringBuilder.toString().indexOf("population"));
                population.append(stringBuilder.toString().substring(12, stringBuilder.indexOf(",") - 1));
                population.replace(0, population.length(), population(stringBuilder.toString().substring(12, stringBuilder.indexOf(",") - 1)));
                stringBuilder.delete(0, stringBuilder.toString().indexOf("currencies"));
                currency.append(stringBuilder.toString().substring(22, stringBuilder.indexOf(",") - 1));
                this.country = country.toString();
                this.currency = currency.toString();
                this.population = population.toString();
            }
        } catch (java.io.IOException e) {
            try {
                URL oracle1 = new URL("http://getcitydetails.geobytes.com/GetCityDetails?fqcn=" + city);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(oracle1.openStream()));
                while ((inputLine = in.readLine()) != null)
                    stringBuilder.append(inputLine);
                in.close();
                stringBuilder.delete(0, stringBuilder.toString().indexOf("country"));
                country.append(stringBuilder.toString().substring(10, stringBuilder.indexOf(",") - 1));
                if (country.toString().isEmpty()) {
                    stringBuilder.delete(0, stringBuilder.length());
                    this.country = null;
                } else {
                    stringBuilder.delete(0, stringBuilder.toString().indexOf("population"));
                    population.append(population(stringBuilder.toString().substring(13, stringBuilder.indexOf(",") - 1)));
                    stringBuilder.delete(0, stringBuilder.toString().indexOf("currencycode"));
                    currency.append(stringBuilder.toString().substring(15, stringBuilder.indexOf(",") - 1));
                }
                this.currency = currency.toString();
                this.country = country.toString();
                this.population = population.toString();
            } catch (IOException e1) { e.printStackTrace(); }
        }
    }

    public static String population(String number)
    {
        String x = number.replaceAll(",", "");
        double num = Double.parseDouble(x);
        if (num < 1000)
            return number;
        if (num < 1000000) {
            num /= 1000;
            num = Math.round(num * 100);
            return num / 100 + "K";
        }
        if (num < 1000000000) {
            num /= 1000000;
            num = Math.round(num * 100);
            return num / 100 + "M";
        } else {
            num /= 1000000000;
            num = Math.round(num * 100);
            return num / 100 + "B";
        }
    }
}