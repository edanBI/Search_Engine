package sample.Models;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class City {
    private String city;
    private String country;
    private String currency;
    private String population;

    public City(String city) {
        this.city = city;
        ArrayList<String> info = infoByCity(city);
        this.country = info.get(0);
        this.currency = info.get(1);
        this.population = info.get(2);
    }

    private ArrayList<String> infoByCity(String capital) {
        String inputLine;
        String url = "https://restcountries.eu/rest/v2/capital/" + capital;
        try {
            URL oracle = new URL(url);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(oracle.openStream()));
            StringBuilder x = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                x.append(inputLine);
            //System.out.println(inputLine);
            in.close();
            x = new StringBuilder(x.toString().replaceAll("\"", ""));
            String[] s = x.toString().split(",");
            //population:1315944
            String country = (s[0].split(":"))[1];
            String population = population((s[12].split(":"))[1]);
            String currency = (s[23].split(":"))[2];
            ArrayList<String> h = new ArrayList<>();
            h.add(country);
            h.add(currency);
            h.add(population);
            return h;
        } catch (java.io.IOException e) {
            System.out.println(e.getStackTrace());
        }
        return null;
    }

    public static String population(String number) {
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