package sample.Models;

import com.google.common.math.DoubleMath;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Indexer {
    private TreeMap<String, DictionaryRecord> dictionary; // [term | df ]
    private TreeMap<String, LinkedList<PostingRecord>> tmpPosting; // [ docId | tf ]
    private HashMap<String, Document> docsSet;
    private int numDocsCached;
    private int cachedDocsLimit;
    private int fileCounter;
    private int numDocsIndexed;
    private int numUniqueTerms;
    private BufferedWriter bw_tmpPosting;
    private String postingDir;
    private String tmpPostPath;

    private TreeMap<String, City> idxCities;

    public Indexer(String finalPostingPath)
    {
        this.postingDir = finalPostingPath;
        tmpPostPath = finalPostingPath + "/Temporary Postings";
        new File(tmpPostPath).mkdirs();
        this.dictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.tmpPosting = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.docsSet = new HashMap<>();
        this.numDocsCached = 0;
        this.fileCounter = 0;
        this.cachedDocsLimit = 10000;
        this.numDocsIndexed = 0;
        this.numUniqueTerms = 0;
    }

    /**
     * Process the terms which come from the parser. All terms are written into files from which the
     * posting list files will construct from later.
     * @param d_args - terms hashmap
     * @param docId - Document which all the terms belong to.
     */
    public void processTerms(HashMap<String, TermData> d_args, String docId)
    {
        int max_tf = -1;
        Set<String> terms = d_args.keySet();
        List<String> listOfEntities = new ArrayList<>();
        numDocsCached++;
        numDocsIndexed++;

        // update documents set
        if (docsSet.containsKey(docId)) {
            CityDocument tmp = (CityDocument) docsSet.get(docId);
            tmp.setMax_tf(max_tf);
            tmp.setUnique_words(d_args.size());
        } else {// adds the current document to the documents hashset.
            docsSet.put(docId, new Document(docId, max_tf, d_args.size()));
        }

        // index each term in the document
        for (String t : terms)
        {
            // update the document length
            docsSet.get(docId).updateLength(d_args.get(t).gettF());

            if (d_args.get(t).gettF() > max_tf)
                max_tf = d_args.get(t).gettF();

            // checks whether the term is already inside the dictionary, else add it
            DictionaryRecord tmp;
            if (dictionary.containsKey(t))
            {
                if (dictionary.get(t).isCapital() && Character.isLowerCase(t.charAt(0)))
                {
                    DictionaryRecord dr = dictionary.remove(t);
                    dictionary.put(t, new DictionaryRecord(dr));
                }
                tmp = dictionary.get(t);
                tmp.updateTotalFreq(d_args.get(t).gettF());
                tmp.updateDF();
                dictionary.replace(t, tmp);
            } else {
                if (Character.isUpperCase(t.charAt(0))) {
                    dictionary.put(t, new DictionaryRecord(t, d_args.get(t).gettF(), true));
                    // adds the uppercase term to the listOfEntities
                    listOfEntities.add(t+"_"+d_args.get(t).gettF());
                }
                else
                    dictionary.put(t, new DictionaryRecord(t, d_args.get(t).gettF(), false));
            }
            // checks whether the term is already inside the posting list
            if (tmpPosting.containsKey(t))
                tmpPosting.get(t).add(new PostingRecord(docId, d_args.get(t).gettF(), d_args.get(t).getPlaces()));
            else {
                tmpPosting.put(t, new LinkedList<>());
                tmpPosting.get(t).add(new PostingRecord(docId, d_args.get(t).gettF(), d_args.get(t).getPlaces()));
            }
        }
        // set the listOfEntities in this current document
        docsSet.get(docId).setMax_tf(max_tf);
        docsSet.get(docId).setEntities(listOfEntities);

        if (numDocsCached == cachedDocsLimit)
        {
            System.out.println("*** write posting file"+ fileCounter +" ***");
            numDocsCached = 0;
            writeTmpPostingToDisk();
            fileCounter++;
            tmpPosting.clear(); //clears the temporary posting list
        }
    }

    /**
     * Clears the remaining terms in the tmpPosting treemap onto the disk.
     * this method complements 'processTerms'.
     */
    public void flushTmpPosting()
    {
        if (tmpPosting.size() > 0)
        {
            System.out.println("*** write posting file"+ fileCounter +" ***");
            numDocsCached = 0;
            writeTmpPostingToDisk();
            tmpPosting.clear();
            fileCounter++;
        }
    }

    /**
     * Write the i'th temp posting file into the disk at folder 'DB Files/Temporary Postings'
     */
    private void writeTmpPostingToDisk()
    {
        File dir = new File(tmpPostPath);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(tmpPostPath + "/posting" + fileCounter + ".txt");
        try {
            bw_tmpPosting = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), cachedDocsLimit*29);
            tmpPosting.forEach((key, value) ->  {
                try {
                    Iterator<PostingRecord> it = value.iterator();
                    PostingRecord pr;
                    while (it.hasNext())
                    {
                        pr = it.next();
                        bw_tmpPosting.write(key + " | " + pr.toString());
                        bw_tmpPosting.newLine();
                    }
                } catch (IOException e) { e.printStackTrace(); }
            });
            bw_tmpPosting.close();
        } catch (IOException e) {
            System.out.println("Unable to write file -- posting"+ fileCounter);
            e.printStackTrace();
        }
    }

    /**
     * writes the entire dictionary into the disk.
     */
    public void writeDictionaryToDisk(boolean stem)
    {
        numUniqueTerms = dictionary.size();
        updateIDFs();
        try {
            File dictionary_file;
            if (stem)
                dictionary_file = new File(postingDir + "/dictionary_stemmer.txt");
            else
                dictionary_file = new File(postingDir + "/dictionary.txt");
            //BufferedWriter br = new BufferedWriter(new FileWriter(dictionary_file));
            BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dictionary_file), StandardCharsets.UTF_8));
            dictionary.forEach((term, record) -> {
                try {
                    br.write(term +
                            "---DF=" + record.getDF() +
                            "---TF=" + record.getTotalFreq() +
                            "---PTR=" + record.getPtr() +
                            "---IDF=" + Double.toString(record.getIdf()).substring(0, 7)
                    );
                    br.newLine();
                } catch (IOException e) { e.printStackTrace(); }
            });
            br.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * this is a wrapper for the merge sorting method.
     */
    public void mergeTmpPostingFiles(boolean stem)
    {
        ArrayList<String> filesList = new ArrayList<>(getFiles());
        filesList.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.compare(
                        Integer.parseInt(o1.substring(7, o1.indexOf("."))),
                        Integer.parseInt(o2.substring(7, o2.indexOf(".")))
                );
            }
        });
        try {
            while (filesList.size() > 2)
            {
                mergeSort(filesList.remove(0), filesList.remove(0));
                filesList.add(filesList.size(), "posting"+fileCounter+".txt");
                fileCounter++;
            }
            if (filesList.size()==1) createPostings(tmpPostPath + "/" + filesList.remove(0), null, stem); //CREATE THE POSTING FILES
            else createPostings(tmpPostPath + "/" + filesList.remove(0), tmpPostPath + "/" + filesList.remove(0), stem); //CREATE THE POSTING FILES
        }
        catch (NullPointerException ptr) { ptr.toString(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * merging the temporary posting files using merge sort algorithm.
     * @param file1 - left file
     * @param file2 - right file
     * @throws IOException in the event of an IOException
     */
    private void mergeSort(String file1, String file2) throws IOException, StringIndexOutOfBoundsException
    {
        File f1 = new File(tmpPostPath + "/" + file1);
        File f2 = new File(tmpPostPath + "/" + file2);
        BufferedReader br1 = new BufferedReader(new FileReader(f1), cachedDocsLimit*29);
        BufferedReader br2 = new BufferedReader(new FileReader(f2), cachedDocsLimit*29);
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new
                        FileOutputStream(tmpPostPath + "/posting" + fileCounter + ".txt"), StandardCharsets.UTF_8),
                cachedDocsLimit*29);
        String line1 = br1.readLine();
        String line2 = br2.readLine();
        String term1 = line1.substring(0, line1.indexOf(" | "));
        String term2 = line2.substring(0, line2.indexOf(" | "));
        while (line1!=null || line2!=null)
        {
            if (line1!=null && line2!=null)
            {
                if (term1.compareToIgnoreCase(term2) < 0) //term1 is alphabetically before term2
                {
                    bw.write(line1);
                    bw.newLine();
                    line1 = br1.readLine();
                    if (line1!=null)
                        term1 = line1.substring(0, line1.indexOf(" | "));
                }
                else if (term1.compareToIgnoreCase(term2) == 0) //term1 == term2
                {
                    /*if ( idCompare(
                            line1.substring(line1.indexOf("=")+1, line1.indexOf(", tf=")),
                            line2.substring(line2.indexOf("=")+1, line2.indexOf(", tf="))) < 0)
                    {
                        bw.write(line1);
                        bw.newLine();
                        line1 = br1.readLine();
                        if (line1!=null)
                            term1 = line1.substring(0, line1.indexOf(" | "));
                    }
                    else {
                        bw.write(line2);
                        bw.newLine();
                        line2 = br2.readLine();
                        if (line2!=null)
                            term2 = line2.substring(0, line2.indexOf(" | "));
                    }*/

                    bw.write(line1);
                    bw.newLine();
                    line1 = br1.readLine();
                    if (line1!=null)
                        term1 = line1.substring(0, line1.indexOf(" | "));
                }
                else { // term2 is alphabetically before term1 OR term1==term2
                    bw.write(line2);
                    bw.newLine();
                    line2 = br2.readLine();
                    if (line2!=null)
                        term2 = line2.substring(0, line2.indexOf(" | "));
                }
            }
            else if (line1!=null)
            {
                bw.write(line1);
                bw.newLine();
                line1 = br1.readLine();
                if (line1 != null)
                    term1 = line1.substring(0, line1.indexOf(" | "));
            }
            else {
                bw.write(line2);
                bw.newLine();
                line2 = br2.readLine();
                if (line2 != null)
                    term2 = line2.substring(0, line2.indexOf(" | "));
            }
        }
        br1.close();
        br2.close();
        bw.close();
        if (f1.delete())
            System.out.println(f1.getName()+" DELETED");
        if (f2.delete())
            System.out.println(f2.getName()+" DELETED");
    }

    /**
     * Creates the final posting files in the disk. All the symbols and number has their own posting file and
     * each pair of letters has his own posting file.
     * @param left is the first file path.
     * @param right is the second file path.
     * @param stem is true if stemmer check box is selected.
     * @throws IOException in the event of an IO exception thrown, if unable to read or write.
     */
    private void createPostings(String left, String right, boolean stem) throws IOException, NullPointerException
    {
        String postingDir_path;
        File postingDir;
        // create the directory
        if (stem) {
            postingDir_path = this.postingDir + "/Posting Files_stemmer";
            postingDir = new File(postingDir_path);
        }
        else {
            postingDir_path = this.postingDir + "/Posting Files";
            postingDir = new File(postingDir_path);
        }
        if (!postingDir.exists()) postingDir.mkdirs();
        BufferedReader brLeft = new BufferedReader(new FileReader(left));
        BufferedReader brRight = null;
        if (right!=null) brRight = new BufferedReader(new FileReader(right));

        BufferedWriter bw_$9 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/$-9.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_AB = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/A-B.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_CD = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/C-D.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_EF = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/E-F.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_GH = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/G-H.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_IJ = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/I-J.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_KL = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/K-L.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_MN = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/M-N.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_OP = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/O-P.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_QR = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/Q-R.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_ST = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/S-T.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_UV = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/U-V.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_WX = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/W-X.txt"), StandardCharsets.UTF_8), 30000);
        BufferedWriter bw_YZ = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(postingDir_path + "/Y-Z.txt"), StandardCharsets.UTF_8), 30000);

        String leftLine = brLeft.readLine();
        String rightLine = null;
        if (brRight!=null)
            rightLine = brRight.readLine();
        char first;
        int i$9 = 0, iAB = 0, iCD = 0, iEF = 0, iGH = 0, iIJ = 0, iKL = 0, iMN = 0, iOP = 0, iQR = 0, iST = 0, iUV = 0, iWX = 0, iYZ = 0;
        String leftTerm, rightTerm, curr_term = null, curr_line, prev_term;
        boolean leftChosen;

        while (leftLine!= null || rightLine!=null) {
            // get just the terms value
            leftTerm = leftLine!=null ? leftLine.substring(0, leftLine.indexOf(" | ")) : null;
            rightTerm = rightLine!=null ? rightLine.substring(0, rightLine.indexOf(" | ")) : null;
            //compare the terms
            if (leftTerm!=null && rightTerm!=null) {
                if (leftTerm.compareToIgnoreCase(rightTerm) < 0) {
                    curr_line = leftLine;
                    leftChosen = true;
                } else {
                    curr_line = rightLine;
                    leftChosen = false;
                }
            } else if (leftTerm!=null && rightTerm==null) {
                curr_line = leftLine;
                leftChosen = true;
            } else {
                curr_line = rightLine;
                leftChosen = false;
            }
            prev_term = curr_term;
            curr_term = curr_line.substring(0, curr_line.indexOf(" | "));
            first = curr_line.charAt(0);

            if (first < 65 || (first > 90 && first < 97) || first > 122) {
                /*if (dictionary.get(curr_line)==null)
                    System.out.println(curr_line);*/
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(i$9);
                i$9++;
                bw_$9.write(curr_line);
                bw_$9.newLine();
            } else if (first < 67 || (first > 96 && first < 99)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iAB);
                iAB++;
                bw_AB.write(curr_line);
                bw_AB.newLine();
            } else if (first < 69 || (first > 98 && first < 101)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iCD);
                iCD++;
                bw_CD.write(curr_line);
                bw_CD.newLine();
            } else if (first < 71 || (first > 100 && first < 103)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iEF);
                iEF++;
                bw_EF.write(curr_line);
                bw_EF.newLine();
            } else if (first < 73 || (first > 102 && first < 105)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iGH);
                iGH++;
                bw_GH.write(curr_line);
                bw_GH.newLine();
            } else if (first < 75 || (first > 104 && first < 107)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iIJ);
                iIJ++;
                bw_IJ.write(curr_line);
                bw_IJ.newLine();
            } else if (first < 77 || (first > 106 && first < 109)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iKL);
                iKL++;
                bw_KL.write(curr_line);
                bw_KL.newLine();
            } else if (first < 79 || (first > 108 && first < 111)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iMN);
                iMN++;
                bw_MN.write(curr_line);
                bw_MN.newLine();
            } else if (first < 81 || (first > 110 && first < 113)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iOP);
                iOP++;
                bw_OP.write(curr_line);
                bw_OP.newLine();
            } else if (first < 83 || (first > 112 && first < 115)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iQR);
                iQR++;
                bw_QR.write(curr_line);
                bw_QR.newLine();
            } else if (first < 85 || (first > 114 && first < 117)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iST);
                iST++;
                bw_ST.write(curr_line);
                bw_ST.newLine();
            } else if (first < 87 || (first > 116 && first < 119)) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iUV);
                iUV++;
                bw_UV.write(curr_line);
                bw_UV.newLine();
            } else if (first < 89 || first < 121) {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iWX);
                iWX++;
                bw_WX.write(curr_line);
                bw_WX.newLine();
            } else {
                if (!curr_term.equals(prev_term)) dictionary.get(curr_term).setPtr(iYZ);
                iYZ++;
                bw_YZ.write(curr_line);
                bw_YZ.newLine();
            }

            // read new line
            if (leftChosen) leftLine = brLeft.readLine();
            else rightLine = brRight.readLine();
        }
        // close all
        brLeft.close();
        if (brRight!=null) brRight.close();
        bw_$9.close();
        bw_AB.close();
        bw_CD.close();
        bw_EF.close();
        bw_GH.close();
        bw_IJ.close();
        bw_KL.close();
        bw_MN.close();
        bw_OP.close();
        bw_QR.close();
        bw_ST.close();
        bw_UV.close();
        bw_WX.close();
        bw_YZ.close();
    }

    /**
     * @return the amount of document indexed.
     */
    public int getNumDocsIndexed()
    {
        return numDocsIndexed;
    }

    /**
     * @return the amount of unique terms identified in the corpus.
     */
    public int getNumUniqueTerms()
    {
        return numUniqueTerms;
    }

    /**
     * @return the Documents Set.
     */
    public HashMap<String, Document> getDocsSet()
    {
        return docsSet;
    }

    /**
     * @return an arraylist containing the files names
     */
    private ArrayList<String> getFiles()
    {
        File dir = new File(tmpPostPath);
        File[] list = dir.listFiles();
        ArrayList<String> ans = new ArrayList<>();
        for (File file : list)
            ans.add(file.getName());
        return ans;
    }

    /**
     * calculate all terms idf's
     */
    private void updateIDFs()
    {
        dictionary.forEach((t, record) -> record.setIdf(DoubleMath.log2((double) numDocsIndexed / (double) record.getDF())));
    }

    public void indexCities(HashMap<String, City> cities)
    {
        idxCities = new TreeMap<>(cities);
        idxCities.forEach((name, city) -> city.getDocsRepresent().forEach(docid -> docsSet
                .put(docid, new CityDocument(docid, -1, -1, city))));
    }

    public TreeMap<String, City> getIdxCities() {
        return idxCities;
    }

    public TreeMap<String, DictionaryRecord> getDictionary() {
        return dictionary;
    }

    /**
     * write the document set to the disk. this is done for later use when retrieving docs
     */
    public void writeDocumentsToDisk()
    {
        try {
            File progData = new File(postingDir + "/ProgramData");
            if (!progData.exists()) progData.mkdirs();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(progData + "/Documents.txt"), StandardCharsets.UTF_8));
            for (Document doc : docsSet.values()) {
                bw.write(doc.toString());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * @param path to the postings directory
     * @return a TreeMap of the dictionary read from the file.
     * @throws IOException if the dictionary file is not found.
     */
    public static TreeMap<String, DictionaryRecord> readDictionaryFromFile(String path) throws IOException
    {
        int len = 0;
        TreeMap<String, DictionaryRecord> dictionary = new TreeMap<>();
        BufferedReader br = new BufferedReader(new FileReader(path + "/dictionary.txt"));
        String curr = br.readLine();
        while (curr != null)
        {
            String term = curr.substring(0, curr.indexOf("---DF"));
            if (term.length()==0)
                term = "-";
            len += term.length() + 6;
            int df = Integer.parseInt(curr.substring(len, curr.indexOf("---TF=", len)));
            len += Integer.toString(df).length() + 6;
            int totalFreq = Integer.parseInt(curr.substring(len, curr.indexOf("---PTR=", len)));
            len += Integer.toString(totalFreq).length() + 7;
            int ptr = Integer.parseInt(curr.substring(len, curr.indexOf("---IDF=", len)));
            len += Integer.toString(ptr).length() + 7;
            double idf = Double.parseDouble(curr.substring(len));

            dictionary.put(term, new DictionaryRecord(term, df, totalFreq, ptr, idf));
            curr = br.readLine();
            len=0;
        }
        return dictionary;
    }
}
