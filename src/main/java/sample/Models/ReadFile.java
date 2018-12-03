package sample.Models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ReadFile {
    private String path;
    private ArrayList<String> listOfFilePath;

    public ArrayList<String> getListOfFilePath() {
        return listOfFilePath;
    }

    public ReadFile(String path) {
        this.path = path;
        this.listOfFilePath = new ArrayList<String>();
        filesForFolder(path);
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

    public HashMap<String,String> read(String path){
        HashMap<String,String> textById = new HashMap<>();
        StringBuilder text = new StringBuilder();
        StringBuilder id = new StringBuilder();
        File file = new File(path);
        try {
            Document document = Jsoup.parse(file, "UTF-8");
            Elements docs = document.getElementsByTag("DOC");
            Iterator<Element> iterator = docs.iterator();
            while (iterator.hasNext()){
                Element element = iterator.next();
                id.append(element.getElementsByTag("DOCNO"));
                id.delete(0,7);
                id.delete(id.length()-8,id.length());
                text.append(element.getElementsByTag("TEXT"));
                if(text.toString().contains("<text>"))
                    text.delete(text.indexOf("<text>"),text.indexOf("<text>") + 6);
                if(text.toString().contains("</text>"))
                    text.delete(text.indexOf("</text>"),text.indexOf("</text>") + 7);
                /*String city = element.getElementsByTag("F").toString();
                if (city.contains("<f p=\"104\">")) {
                    city = city.substring(city.indexOf("<f p=\"104\">", city.indexOf("</f>")));
                    if (city.length() > 15) {
                        city = city.substring(city.indexOf("\n "), city.indexOf(" \n"));
                        city = city.replaceAll("\n", "");
                        city = city.replace("(", "");
                        city = city.replace(")", "");
                        city = city.trim();
                        city = city.split(" ")[0];
                        city = city.toUpperCase();
                    }
                }
                else city = "";*/
                textById.put(id.toString().trim(),text.toString());
                text.delete(0,text.length());
                id.delete(0,id.length());
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return textById;
    }

}