package sample.Models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Searcher {
    private Parser parser;
    private Indexer indexer;
    private Ranker ranker;
    private String pathOfPosthingFolder;
    private HashMap<Character, String> postingFiles = new HashMap<>();

    public Searcher(Parser parser, Indexer indexer, Ranker ranker, String pathOfPosthingFolder) {
        this.parser = parser;
        this.indexer = indexer;
        // this.ranker = ranker;
        this.pathOfPosthingFolder = pathOfPosthingFolder;
        initpostingFiles(postingFiles);
    }

    //Finds the relevant documents for the given query
    public ArrayList<sample.Models.Document> parseFromQuery(String query, HashSet<City> cities) {
        HashSet<String> docsByCities = docsByCities(cities);
        //HashMap for all the terms by docs (K- docId, V- term name ,TermData)
        HashMap<String, HashMap<String, TermData>> docsAndTerms = new HashMap<>();
        //return all the Terms of the query, after parse
        List<String> queryTerms = new ArrayList<String>(parser.Parsing(query).keySet());
        for (String term : queryTerms) {
            //check if the term is in the dictionary
            if (indexer.getDictionary().containsKey(term) || indexer.getDictionary().containsKey(term.toUpperCase()) || indexer.getDictionary().containsKey(term.toLowerCase())) {
                String termToAdd = term;
                if (indexer.getDictionary().containsKey(term.toUpperCase()))
                    termToAdd = term.toUpperCase();
                else if (indexer.getDictionary().containsKey(term.toLowerCase()))
                    termToAdd = term.toLowerCase();
                //find the pointer for the line in the posting line
                int pointer = indexer.getDictionary().ceilingEntry(termToAdd).getValue().getPtr();
                //how much lines to read from the posting line
                int df = indexer.getDictionary().ceilingEntry(termToAdd).getValue().getDF();
                //double idf = indexer.getDictionary().ceilingEntry(term).getValue().getIdf();

                //all the lines of the term from the posting file
                HashSet<String> allLineFromPostingFiles = postingLines(this.pathOfPosthingFolder, pointer, df, termToAdd);

                if (!allLineFromPostingFiles.isEmpty()) {
                    for (String line : allLineFromPostingFiles) {
                        String docId = line.substring(line.indexOf("| docId=") + 8, line.indexOf(", tf="));

                        //check if the docId is in the Hash of the docsByCity
                        if (docsByCities != null && !docId.isEmpty() && !docsByCities.contains(docId))
                            continue;

                        int tF = Integer.parseInt(line.substring(line.indexOf(", tf=") + 5, line.indexOf(", positions=")));
                        String positions = line.substring(line.indexOf(", positions=") + 12, line.length()).trim();
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
        //something that sended to ranker
    }

    //Finds the relevant documents for each query in the query file and write the result to the disk
    public void parseFromQueryFile(String path, HashSet<City> cities) {
        HashMap<String, String> readQueryFile = readQueryFile(path);
        ArrayList<String> ans = new ArrayList<>();
        for (String query:readQueryFile.keySet()) {
            ArrayList<sample.Models.Document> docs = parseFromQuery(readQueryFile.get(query),cities);
            //send to write to file function format:" query + " 0 " + DocNum +" " + relevanc 1/0 + " 42.38 mt"
            for (sample.Models.Document document: docs) {
                ans.add(query + " 0 " + document.getDoc_id() /* + " " + relevance */ + " 42.38 mt");
            }
        }
        writeQueryResultToFile(ans);
    }

    //Write the query result file to disk
    public void writeQueryResultToFile(ArrayList<String> toPrint) {
        try {
            File query_file = new File(pathOfPosthingFolder + "/qrels.txt");
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
    private HashMap<String, String> readQueryFile(String path) {
        HashMap<String, String> QueryById = new HashMap<>();
        File file = new File(path);
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
    private HashSet<String> postingLines(String postings_path, int firstLine, int numOfLines, String firstWordCharacter) {
        String fileName;
        if (Character.isLetter(firstWordCharacter.charAt(0)))
            fileName = postingFiles.get(firstWordCharacter.toUpperCase().charAt(0));
        else fileName = "$-9.txt";

        HashSet<String> allHisLines = new HashSet<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(postings_path + "/Posting Files/" + fileName))) {
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
        if (cities.isEmpty()){
            return null;
        }
        HashSet<String> docs = new HashSet<>();
        for (City c : cities) {
            docs.addAll(c.getDocsRepresent());
            if (indexer.getDictionary().containsKey(c.getCity())) {
                int pointer = indexer.getDictionary().ceilingEntry(c.getCity()).getValue().getPtr();
                int df = indexer.getDictionary().ceilingEntry(c.getCity()).getValue().getDF();
                HashSet<String> allLineFromPostingFiles = postingLines(this.pathOfPosthingFolder, pointer, df, c.getCity());
                for (String line : allLineFromPostingFiles) {
                    docs.add(line.substring(line.indexOf("| docId=") + 8, line.indexOf(", tf=")));
                }
            }
        }
        return docs;
    }

    /**
     * initialize the postingFiles
     */
    private void initpostingFiles(HashMap<Character, String> postingFiles) {
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