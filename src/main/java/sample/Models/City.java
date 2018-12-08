package sample.Models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;

public class City {
    private static HashMap<String, String> restCountriesAPI = new HashMap<>();
    private String country;
    private String currency;
    private String population;
    private LinkedList<String> docsRepresent;

    public City(String city, String initDocId) {
        docsRepresent = new LinkedList<>();
        docsRepresent.addFirst(initDocId);
        infoByCity(city);
    }
    void addDocId(String id) {
        this.docsRepresent.addLast(id);
    }

    LinkedList<String> getDocsRepresent() {
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

    static {
        String inputLine;
        String path = "src/main/resources/city1.txt";
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            StringBuilder x = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                x.append(inputLine);
            in.close();
            x.deleteCharAt(0);
            while (!x.toString().isEmpty() && x.toString().contains("@")) {
                restCountriesAPI.put(x.substring(0, x.toString().indexOf(":")).toUpperCase(), x.substring(x.toString().indexOf(":") + 1, x.toString().indexOf("@")));
                x.delete(0, x.indexOf("@") + 1);
            }
            restCountriesAPI.put(x.substring(0, x.toString().indexOf(":")), x.substring(x.toString().indexOf(":") + 1, x.toString().length()));
        } catch (Exception e) {
            e.getStackTrace();
        }
    }


    private void infoByCity(String city) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder country = new StringBuilder();
        StringBuilder currency = new StringBuilder();
        StringBuilder population = new StringBuilder();
        String inputLine;
        if (restCountriesAPI.containsKey(city.toUpperCase())) {
            String cityInfo = restCountriesAPI.get(city.toUpperCase());
            this.country = cityInfo.substring(cityInfo.indexOf(":") + 1, cityInfo.indexOf("*"));
            this.population = population(cityInfo.substring(cityInfo.indexOf("*") + 1, cityInfo.indexOf("+")));
            this.currency = cityInfo.substring(cityInfo.indexOf("+") + 1, cityInfo.length());
        } else {
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
                    this.country = "";
                } else {
                    stringBuilder.delete(0, stringBuilder.toString().indexOf("population"));
                    population.append(population(stringBuilder.toString().substring(13, stringBuilder.indexOf(",") - 1)));
                    stringBuilder.delete(0, stringBuilder.toString().indexOf("currencycode"));
                    currency.append(stringBuilder.toString().substring(15, stringBuilder.indexOf(",") - 1));
                }
                this.currency = currency.toString();
                this.country = country.toString();
                this.population = population.toString();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

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