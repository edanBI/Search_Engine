package sample.Models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class ReadFile {
    private String path;
    private ArrayList<String> listOfFilePath;
    private HashSet<String> allDocsLanguage;
    private HashMap<String, City> allDocsCity;

    public ArrayList<String> getListOfFilePath() {
        return listOfFilePath;
    }

    public ReadFile(String path) {
        this.path = path;
        this.listOfFilePath = new ArrayList<>();
        this.allDocsLanguage = new HashSet<>();
        this.allDocsCity = new HashMap<>();
        filesForFolder(path);
    }

    public HashSet<String> getAllDocsLanguage() {
        return allDocsLanguage;
    }

    public HashMap<String, City> getAllDocsCity() {
        return allDocsCity;
    }

    private void filesForFolder(String path) {
        final File folder = new File(path);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                filesForFolder(fileEntry.getPath());
            } else {
                if (!fileEntry.getAbsolutePath().contains("stop_words.txt"))
                    this.listOfFilePath.add(fileEntry.getAbsolutePath());
            }
        }
    }

    public HashMap<String, String> read(String path) {
        HashMap<String, String> textById = new HashMap<>();
        StringBuilder text = new StringBuilder();
        StringBuilder id = new StringBuilder();
        StringBuilder city = new StringBuilder();
        StringBuilder language = new StringBuilder();
        File file = new File(path);
        try {
            Document document = Jsoup.parse(file, "UTF-8");
            Elements docs = document.getElementsByTag("DOC");
            Iterator<Element> iterator = docs.iterator();
            while (iterator.hasNext()) {
                Element element = iterator.next();

                //Document ID by <docno> tag
                id.append(element.getElementsByTag("DOCNO"));
                id.delete(0, 7);
                id.delete(id.length() - 8, id.length());

                //Text of the document by <text> tag
                text.append(element.getElementsByTag("TEXT"));
                if (text.toString().contains("<text>"))
                    text.delete(text.indexOf("<text>"), text.indexOf("<text>") + 6);
                if (text.toString().contains("</text>"))
                    text.delete(text.indexOf("</text>"), text.indexOf("</text>") + 7);
                if (text.toString().toLowerCase().contains("[text]"))
                    text.delete(0, text.toString().toLowerCase().indexOf("[text]") + 6);

                //City of the text by <f p=104> tag
                city.append(element.getElementsByTag("F").toString());
                if (city.toString().contains("<f p=\"104\">")) {
                    city.delete(0, city.indexOf("<f p=\"104\">") + 14);
                    if (city.toString().contains("</f>"))
                        city.delete(city.indexOf("</f>"), city.length());
                    while (city.toString().contains("\n"))
                        city.deleteCharAt(city.indexOf("\n"));
                    if (!city.toString().isEmpty() &&
                            (!Character.isUpperCase(city.toString().charAt(0)) || city.toString().charAt(0) == '>' || city.toString().charAt(0) == '<'
                                    || city.toString().charAt(0) == '-' || city.toString().charAt(0) == '['
                                    || city.toString().charAt(0) == ']' || city.toString().charAt(0) == '('
                                    || city.toString().charAt(0) == '\'' || Character.isDigit(city.toString().charAt(0))))
                        city.delete(0, city.length());
                    else {
                        city.delete(city.indexOf(" "), city.length());
                        city.replace(0, city.length(), city.toString().toUpperCase());
                        while (!city.toString().isEmpty() && (city.toString().contains("/") || city.length() < 3 || Character.isDigit(city.charAt(city.length() - 1)) || city.charAt(city.length() - 1) == ',' || city.charAt(city.length() - 1) == '-'))// || city.charAt(city.length()-1)==';' || || city.charAt(city.length()-1)=='3')
                            city.deleteCharAt(city.length() - 1);
                        if (!city.toString().isEmpty() && city.charAt(city.length() - 1) == ']')
                            city.delete(0, city.length());
                    }
                } else city.delete(0, city.length());

                //Language of the text by <f p=105> tag
                language.append(element.getElementsByTag("F").toString());
                if (language.toString().contains("<f p=\"105\">")) {
                    language.delete(0, language.indexOf("<f p=\"105\">") + 14);
                    if (language.toString().contains("</f>"))
                        language.delete(language.indexOf("</f>"), language.length());
                    while (language.toString().contains("\n"))
                        language.deleteCharAt(language.indexOf("\n"));
                    if (!language.toString().isEmpty() &&
                            (!Character.isUpperCase(language.toString().charAt(0)) || language.toString().charAt(0) == '>' || language.toString().charAt(0) == '<'
                                    || language.toString().charAt(0) == '-' || language.toString().charAt(0) == '['
                                    || language.toString().charAt(0) == ']' || language.toString().charAt(0) == '('
                                    || language.toString().charAt(0) == '\'' || Character.isDigit(language.toString().charAt(0))))
                        language.delete(0, language.length());
                    else {
                        language.delete(language.indexOf(" "), language.length());
                        if (language.charAt(language.length() - 1) == ',' || language.charAt(language.length() - 1) == ';' || language.charAt(language.length() - 1) == '-' || language.charAt(language.length() - 1) == '3')
                            language.deleteCharAt(language.length() - 1);
                    }
                } else language.delete(0, language.length());

                if (!language.toString().isEmpty())
                    allDocsLanguage.add(language.toString());
                if (!allDocsCity.containsKey(city.toString())) {
                    City city1 = new City(city.toString(), id.toString());
                    if (city1.getCountry() != null)
                        allDocsCity.put(city.toString(), city1);
                } else {
                    allDocsCity.get(city.toString()).addDocId(id.toString());
                }
                textById.put(id.toString().trim(), text.toString());
                text.delete(0, text.length());
                id.delete(0, id.length());
                city.delete(0, city.length());
                language.delete(0, language.length());

            }
        } catch (IOException e) {
            e.getStackTrace();
        }
        return textById;
    }
}