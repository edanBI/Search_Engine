package sample.Models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Searcher {
    private Parser parser;
    private Ranker ranker;
    private String pathOfPostingFolder;
    private TreeMap<String, DictionaryRecord> dictionary;
    private HashMap<Character, String> postingFiles = new HashMap<>();

    public Searcher(Parser parser, TreeMap<String, DictionaryRecord> dictionary, Ranker ranker, String pathOfPostingFolder) {
        this.parser = parser;
        this.ranker = ranker;
        this.dictionary = dictionary;
        this.pathOfPostingFolder = pathOfPostingFolder;
        initPostingFiles(postingFiles);
    }

    //Finds the relevant documents for the given query
    public ArrayList<sample.Models.Document> parseFromQuery(String query, HashSet<City> cities, boolean toSemanticTreatment) {
        HashSet<String> docsByCities = docsByCities(cities);
        //HashMap for all the terms by docs (K- docId, V- term name ,TermData)
        HashMap<String, HashMap<String, TermData>> docsAndTerms = new HashMap<>();
        //return all the Terms of the query, after parse
        List<String> queryTerms = new ArrayList<>(parser.Parsing(query).keySet());
        //add to the list words with a meaning similar for the given query
        if (toSemanticTreatment && semanticTreatment(queryTerms) != null)
            queryTerms.addAll(semanticTreatment(queryTerms));
        for (String term : queryTerms) {
            //check if the term is in the dictionary
            if (dictionary.containsKey(term) || dictionary.containsKey(term.toUpperCase()) ||
                    dictionary.containsKey(term.toLowerCase())) {
                String termToAdd = term;
                if (dictionary.containsKey(term.toUpperCase()))
                    termToAdd = term.toUpperCase();
                else if (dictionary.containsKey(term.toLowerCase()))
                    termToAdd = term.toLowerCase();
                //find the pointer for the line in the posting line
                int pointer = dictionary.ceilingEntry(termToAdd).getValue().getPtr();
                //how much lines to read from the posting line
                int df = dictionary.ceilingEntry(termToAdd).getValue().getDF();
                //double idf = dictionary.ceilingEntry(term).getValue().getIdf();

                //all the lines of the term from the posting file
                HashSet<String> allLineFromPostingFiles = postingLines(pointer, df, termToAdd);

                if (!allLineFromPostingFiles.isEmpty()) {
                    for (String line : allLineFromPostingFiles) {
                        String docId = line.substring(line.indexOf("| docId=") + 8, line.indexOf(", tf="));

                        //check if the docId is in the Hash of the docsByCity
                        if (docsByCities != null && !docId.isEmpty() && !docsByCities.contains(docId))
                            continue;

                        int tF = Integer.parseInt(line.substring(line.indexOf(", tf=") + 5, line.indexOf(", positions=")));
                        String positions = line.substring(line.indexOf(", positions=") + 12).trim();
                        //add to docsAndTerms
                        if (docsAndTerms.containsKey(docId)) {
                            docsAndTerms.get(docId).put(termToAdd, new TermData(tF, positions));
                        } else {
                            HashMap<String, TermData> hashMap = new HashMap<>();
                            hashMap.put(termToAdd, new TermData(tF, positions));
                            docsAndTerms.put(docId, hashMap);
                        }
                    }
                }
            }
        }
        // return an arraylist containing the ranked documents by descending order of relevancy
        return ranker.rank(queryTerms, docsAndTerms);
    }

    //Finds the relevant documents for each query in the query file and write the result to the disk
    public void parseFromQueryFile(File file, HashSet<City> cities, boolean toSemanticTreatment, String resPath) {
        HashMap<String, String> readQueryFile = readQueryFile(file);
        ArrayList<String> ans = new ArrayList<>();
        int i = 0;
        for (String query : readQueryFile.keySet()) {
            ArrayList<sample.Models.Document> docs = parseFromQuery(readQueryFile.get(query), cities, toSemanticTreatment);
            //send to write to file function format:" query + " 0 " + DocNum +" " + relevanc 1/0 + " 42.38 mt"
            for (sample.Models.Document document : docs) {
                if (i < 50)
                    ans.add(query + " 0 " + document.getDoc_id() + " 1 42.38 mt");
                else
                    ans.add(query + " 0 " + document.getDoc_id() + " 0 42.38 mt");
                i++;
            }
        }
        writeQueryResultToFile(ans, resPath);
    }

    //Write the query result file to disk
    private void writeQueryResultToFile(ArrayList<String> toPrint, String resPath) {
        try {
            File query_file;
            if (resPath.length() > 0)
                query_file = new File(resPath);
            else
                query_file = new File(pathOfPostingFolder + "/qrels.txt");
            BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(query_file), StandardCharsets.UTF_8));
            toPrint.forEach((s) -> {
                try {
                    br.write(s);
                    br.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Read the query file and split it to query number and the query
    private HashMap<String, String> readQueryFile(File file) {
        HashMap<String, String> QueryById = new HashMap<>();
        try {
            Document document = Jsoup.parse(file, "UTF-8");
            Elements docs = document.getElementsByTag("top");
            Iterator<Element> iterator = docs.iterator();
            while (iterator.hasNext()) {
                Element element = iterator.next();

                //Query ID by <num> tag
                String qId = element.getElementsByTag("num").toString();
                qId = qId.substring(qId.indexOf("Number:") + 7, qId.indexOf("<title>"));
                qId = qId.trim();

                //Query by <title> tag
                String queryText = element.getElementsByTag("title").toString();
                queryText = queryText.substring(7, queryText.indexOf("</title>"));
                queryText = queryText.trim();

                QueryById.put(qId, queryText);
            }
        } catch (IOException e) {
            e.getStackTrace();
        }
        return QueryById;
    }

    //Return specific lines from the posting file
    private HashSet<String> postingLines(int firstLine, int numOfLines, String firstWordCharacter) {
        String filePath;
        if (Character.isLetter(firstWordCharacter.charAt(0)))
            filePath = pathOfPostingFolder + "/Posting Files/" + postingFiles.get(firstWordCharacter.toUpperCase().charAt(0));
        else filePath = pathOfPostingFolder + "/Posting Files/$-9.txt";

        HashSet<String> allHisLines = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            for (int i = 0; i < firstLine; i++)
                br.readLine();
            for (int i = 0; i < numOfLines; i++)
                allHisLines.add(br.readLine());
        } catch (IOException e) {
            e.getStackTrace();
        }
        return allHisLines;
    }

    //Return all the docs that the cities the func get are in their "104" tag or in the text
    private HashSet<String> docsByCities(HashSet<City> cities) {
        if (cities==null || cities.isEmpty()) {
            return null;
        }
        HashSet<String> docs = new HashSet<>();
        for (City c : cities) {
            docs.addAll(c.getDocsRepresent());
            if (dictionary.containsKey(c.getCity())) {
                int pointer = dictionary.ceilingEntry(c.getCity()).getValue().getPtr();
                int df = dictionary.ceilingEntry(c.getCity()).getValue().getDF();
                HashSet<String> allLineFromPostingFiles = postingLines(pointer, df, c.getCity());
                for (String line : allLineFromPostingFiles) {
                    docs.add(line.substring(line.indexOf("| docId=") + 8, line.indexOf(", tf=")));
                }
            }
        }
        return docs;
    }

    //Find with Datamuse API words with a meaning similar for the given query
    private List<String> semanticTreatment(List<String> queryWords) {
        if (queryWords.isEmpty()) {
            return null;
        }
        List<String> similarWord = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        for (String s : queryWords) {
            query.append(s + "+");
        }
        query.deleteCharAt(query.length() - 1);
        String word;
        int numOfWordToImport = 2 * queryWords.size();
        if (numOfWordToImport > 100)
            numOfWordToImport = 100;
        else if (numOfWordToImport == 0)
            return null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            URL oracle1 = new URL("https://api.datamuse.com/words?ml=" + query);
            String inputLine;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(oracle1.openStream()));
            while ((inputLine = in.readLine()) != null)
                stringBuilder.append(inputLine);
            in.close();
            if (stringBuilder.length() > 0) {
                for (int i = 0; i < numOfWordToImport; i++) {
                    word = stringBuilder.substring(stringBuilder.toString().indexOf("\"word\":\"") + 8, stringBuilder.toString().indexOf("\",\""));
                    similarWord.add(word);
                    stringBuilder.delete(0, stringBuilder.indexOf("},{\"word\":") + 3);
                }
                stringBuilder.delete(0, stringBuilder.length());
            } else
                return null;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return similarWord;
    }

    /**
     * initialize the postingFiles
     */
    private void initPostingFiles(HashMap<Character, String> postingFiles) {
        postingFiles.put('A', "A-B.txt");
        postingFiles.put('B', "A-B.txt");
        postingFiles.put('C', "C-D.txt");
        postingFiles.put('D', "C-D.txt");
        postingFiles.put('E', "E-F.txt");
        postingFiles.put('F', "E-F.txt");
        postingFiles.put('G', "G-H.txt");
        postingFiles.put('H', "G-H.txt");
        postingFiles.put('I', "I-J.txt");
        postingFiles.put('J', "I-J.txt");
        postingFiles.put('K', "K-L.txt");
        postingFiles.put('L', "K-L.txt");
        postingFiles.put('M', "M-N.txt");
        postingFiles.put('N', "M-N.txt");
        postingFiles.put('O', "O-P.txt");
        postingFiles.put('P', "O-P.txt");
        postingFiles.put('Q', "Q-R.txt");
        postingFiles.put('R', "Q-R.txt");
        postingFiles.put('S', "S-T.txt");
        postingFiles.put('T', "S-T.txt");
        postingFiles.put('U', "U-V.txt");
        postingFiles.put('V', "U-V.txt");
        postingFiles.put('W', "W-X.txt");
        postingFiles.put('X', "W-X.txt");
        postingFiles.put('Y', "Y-Z.txt");
        postingFiles.put('Z', "Y-Z.txt");
    }
}