package sample.Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.CheckListView;
import sample.Models.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class MainWindowController implements Initializable
{
    private boolean toStem, toSemantic;
    private long startTime, endTime;
    private String corpus_path, postings_path, resQueries_path;
    private HashSet<City> hs_citiesSelected;

    private ReadFile readFile;
    private Indexer indexer;
    private Searcher searcher;

    private File qFile;

    public MainWindowController()
    {
        toStem = false; toSemantic = false;

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Image image = new Image(getClass().getResource("/black_white.jpg").toURI().toString());
            imageView_logo.setImage(image);
            imageView_logo.setCache(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert()
    {
        Alert alert;
        if (corpus_path == null && postings_path==null)
        {
            alert = new Alert(Alert.AlertType.ERROR, "Please Enter Corpus and Posting Paths");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        } else if (corpus_path == null)
        {
            alert = new Alert(Alert.AlertType.ERROR, "Missing Corpus Path");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        } else {
            alert = new Alert(Alert.AlertType.ERROR, "Missing Posting Path");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        }
    }

    @FXML
    public VBox vBox_mainWindows;
    public Button btn_corpus_browse;
    public Button btn_postings_browse;
    public Button btn_generate;
    public Button btn_browseQuery;
    public Button btn_cities;
    public Button btn_run;
    public Button btn_resPath;
    public TextArea txt_queryEntry;
    public TextField txt_corpus_path;
    public TextField txt_postings_path;
    public TextField txt_queryPath;
    public CheckBox chbx_stemming;
    public CheckBox chbx_semantic;
    public ImageView imageView_logo;
    public MenuButton m_languages;
    public Label lbl_resPath;

    @FXML
    public void initDataSet()
    {
        if (corpus_path==null || postings_path==null) {
            showAlert();
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Parsing and Indexing...");
        alert.show();
        startTime = System.currentTimeMillis();
        readFile = new ReadFile(corpus_path);
        Parser parser = new Parser(corpus_path+"/stop_words.txt", toStem);
        indexer = new Indexer(postings_path);
        indexer.indexCities(readFile.getAllDocsCity());
        HashMap<String,String> files;//will be fill by docid and text of the files
        for (int i = 0 ; i<readFile.getListOfFilePath().size() ; i++){
            files = readFile.read(readFile.getListOfFilePath().get(i));//filling the docs file by file
            System.out.println(i);
            for (String id:files.keySet()) {
                indexer.processTerms(parser.Parsing(files.get(id)), id);
            }
        }
        indexer.flushTmpPosting(); //clear all the remaining terms which haven't written to disk
        indexer.mergeTmpPostingFiles(toStem); // build final posting files
        indexer.writeDictionaryToDisk(toStem); // store all the dictionary in disk
        indexer.writeDocumentsToDisk(); // store the documents in disk
        readFile.writeCitiesToDisk(postings_path); // store the cities in disk
        try { FileUtils.deleteDirectory(new File(postings_path+"/Temporary Postings")); } // delete all temporary files
        catch (IOException e) { e.printStackTrace(); }
        endTime = System.currentTimeMillis();
        alert.close();
        displaySummary();

        // init for the search phase
        Ranker ranker = new Ranker(indexer.getDictionary(), indexer.getDocsSet());
        searcher = new Searcher(parser, indexer, ranker, postings_path);
    }

    /**
     * Open the file explorer window, Enable the user to choose a directory.
     * @param actionEvent when one of the 'Browse' button is selected.
     */
    @FXML
    public void openDirectoryFileExplorer(ActionEvent actionEvent)
    {
        DirectoryChooser dc = new DirectoryChooser();
        //dc.setInitialDirectory(new File("D:\\documents\\users\\benivre\\Downloads"));
        //dc.setInitialDirectory(new File("C:\\Users\\user\\Desktop\\Engine misc"));

        File selectedDir = dc.showDialog(vBox_mainWindows.getScene().getWindow());
        if (actionEvent.getSource().equals(btn_corpus_browse) && selectedDir!=null)
        {
            corpus_path = selectedDir.getPath();
            txt_corpus_path.setText(selectedDir.getAbsolutePath());
            txt_corpus_path.setDisable(true);
        }
        else if (actionEvent.getSource().equals(btn_postings_browse) && selectedDir!=null)
        {
            postings_path = selectedDir.getPath();
            txt_postings_path.setText(selectedDir.getAbsolutePath());
            txt_postings_path.setDisable(true);
        }
        else if (actionEvent.getSource().equals(btn_resPath) && selectedDir!=null) {
            lbl_resPath.setText("  Results File Path: " + selectedDir.getAbsolutePath() + "\\qrels.txt");
            resQueries_path = selectedDir.getAbsolutePath();
        }
    }

    @FXML
    public void setToStem()
    {
        toStem = chbx_stemming.isSelected();
    }

    @FXML
    public void setToSemantic()
    {
        toSemantic = chbx_semantic.isSelected();
    }

    @FXML
    public void reset()
    {
        try {
            File noStemmer = new File(postings_path + "/dictionary.txt");
            File noStemmerDir = new File(postings_path + "/Posting Files");
            File stemmer = new File(postings_path + "/dictionary_stemmer.txt");
            File stemmerDir = new File(postings_path + "/Posting Files_stemmer");
            if (noStemmer.exists())
                noStemmer.delete();
            if (stemmer.exists())
                stemmer.delete();
            if (noStemmerDir.exists())
                FileUtils.deleteDirectory(noStemmerDir);
            if (stemmerDir.exists())
                FileUtils.deleteDirectory(stemmerDir);
        }
        catch (IOException e) { e.printStackTrace(); }
        txt_corpus_path.clear();
        txt_postings_path.clear();
        corpus_path = null;
        postings_path = null;
        chbx_stemming.setSelected(false);
        chbx_stemming.setIndeterminate(false);
        toStem = false;
        m_languages.getItems().clear();
    }

    @FXML
    public void displayDictionary()
    {
        if (indexer == null) {
            new Alert(Alert.AlertType.ERROR, "Dictionary N/A").showAndWait();
            return;
        }
        try {
            Stage d_window = new Stage();
            d_window.setTitle("Dictionary");
            String dic_path =
                    this.toStem ? postings_path + "\\dictionary_stemmer.txt" : postings_path + "\\dictionary.txt";

            Collection<DictionaryRecord> list = indexer.getDictionary().values();
            ObservableList<DictionaryRecord> details = FXCollections.observableArrayList(list);

            TableView<DictionaryRecord> tbl_dictionary = new TableView<>();
            TableColumn<DictionaryRecord, String> clmn_terms = new TableColumn<>();
            TableColumn<DictionaryRecord, Integer> clmn_totalFreq = new TableColumn<>();
            tbl_dictionary.setMinWidth(500.0);
            tbl_dictionary.setMinHeight(802.0);
            clmn_terms.setText("Terms");
            clmn_terms.setMinWidth(300.0);
            clmn_totalFreq.setText("Total Frequency");
            clmn_totalFreq.setMinWidth(200.0);

            tbl_dictionary.getColumns().addAll(clmn_terms, clmn_totalFreq);

            clmn_terms.setCellValueFactory(data -> data.getValue().getTermProperty());
            clmn_totalFreq.setCellValueFactory(data -> data.getValue().getTotalFreqProperty());
            tbl_dictionary.setItems(details);

            ScrollPane scrollPane = new ScrollPane(tbl_dictionary);
            scrollPane.setFitToWidth(true);
            Scene scene = new Scene(scrollPane, 515, 705);
            d_window.setScene(scene);
            //d_window.setResizable(false);
            d_window.show();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Dictionary is unavailable");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        }
    }

    @FXML
    public void loadDictionary()
    {
        try {
            String str = txt_postings_path.getText();
            TreeMap<String, DictionaryRecord> loadedDictionary = Indexer.readDictionaryFromFile(str);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Dictionary Loaded");
            alert.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Missing Posting Path");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        }
    }

    @FXML
    public void addLanguages()
    {
        if (readFile == null)
            return;
        m_languages.maxHeight(100.0);
        m_languages.getItems().clear();
        HashSet<String> languages = readFile.getAllDocsLanguage();
        languages.forEach((lang) -> {
            MenuItem menuItem = new MenuItem(lang);
            m_languages.getItems().add(menuItem);
        });
    }

    private void displaySummary()
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Indexing Summary");
        alert.setHeaderText(null);
        alert.setContentText("#Docs Indexed: " + this.indexer.getNumDocsIndexed() + '\n' +
                            "#Unique Terms: " + this.indexer.getNumUniqueTerms() + '\n' +
                            "Time(min): " + ((double)(endTime - startTime)/60000) + " min." +
                            "Time(sec): " + ((double)(endTime - startTime)/1000) + " sec."
        );
        alert.showAndWait();
    }

    /**
     * display the user a list of cities from the documents in the corpus.
     */
    public void displayCities()
    {
        if (readFile == null) {
            new Alert(Alert.AlertType.ERROR, "No Cities to display").showAndWait();
            return;
        }

        Set<String> cities = readFile.getAllDocsCity().keySet();
        ObservableList<String> list = FXCollections.observableArrayList();
        list.addAll(cities);
        list = list.sorted();

        Stage window = new Stage();
        CheckListView<String> checkListView = new CheckListView<>(list);
        checkListView.getCheckModel().getCheckedItems().addListener(new ListChangeListener<String>() {
            public void onChanged(ListChangeListener.Change<? extends String> c) {
                hs_citiesSelected = new HashSet<>();
                ObservableList<String> selected = checkListView.getCheckModel().getCheckedItems();
                for (String city : selected) {
                    hs_citiesSelected.add(readFile.getAllDocsCity().get(city));
                }
            }
        });

        ScrollPane scrollPane = new ScrollPane(checkListView);
        Scene layout = new Scene(scrollPane);
        window.setScene(layout);
        window.show();
    }

    /**
     * run the query from the text area which the user had typed in.
     */
    public void runQuery() {
        if (txt_queryEntry.getText().length()==0 && txt_queryPath.getText().length()==0) {
            new Alert(Alert.AlertType.ERROR, "Please Enter Query").showAndWait();
            return;
        }
        if (txt_queryEntry.getText().length()>0 && txt_queryPath.getText().length()>0) {
            new Alert(Alert.AlertType.ERROR, "Unable to run two queries. Please Choose only one!").showAndWait();
            return;
        }

        // get the ranked documents
        if (searcher == null) {
            //searcher = new Searcher(new Parser(corpus_path, toStem), restoreDocuments(), res)
        }
        ObservableList<Document> retrievedDocumentsList;
        if (txt_queryEntry.getText().length() > 0) {
            retrievedDocumentsList = FXCollections.observableArrayList(searcher.parseFromQuery(txt_queryEntry.getText(), hs_citiesSelected, toSemantic));
        } else {
            if (resQueries_path != null)
                searcher.parseFromQueryFile(qFile, hs_citiesSelected, toSemantic, resQueries_path);
            else
                searcher.parseFromQueryFile(qFile, hs_citiesSelected, toSemantic, "");
            return;
        }

        // display the retrieved documents in new stage
        Stage window = new Stage();
        window.setTitle("Retrieved Documents");

        TableView<Document> tbl_documents = new TableView<>();
        TableColumn<Document, String> col_ids = new TableColumn<>();

        tbl_documents.setMinWidth(500.0);
        tbl_documents.setMinHeight(802.0);
        col_ids.setText("Document ID");
        col_ids.setMinWidth(300.0);
        tbl_documents.getColumns().add(col_ids);
        col_ids.setCellValueFactory(data -> data.getValue().getPropertyDoc_id());
        tbl_documents.setItems(retrievedDocumentsList);

        ScrollPane scrollPane = new ScrollPane(tbl_documents);
        scrollPane.setFitToWidth(true);
        Scene scene = new Scene(scrollPane, 515, 705);
        window.setScene(scene);
        window.show();
    }

    /**
     * run the query from the file in the path the user had entered.
     */
    public void browseQueryFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("TEXT files (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("All files (*)", "*")
        );
        qFile = fileChooser.showOpenDialog(vBox_mainWindows.getScene().getWindow());
        if (qFile!=null) {
            txt_queryPath.setText(qFile.getAbsolutePath());
        }
        // run the searcher on the query file
        //searcher.parseFromQueryFile(qFile, hs_citiesSelected, toSemantic);
    }

    private HashMap<String, Document> restoreDocuments() throws IOException
    {
        HashMap<String, Document> docSet = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(postings_path + "/ProgramData/Documents.txt"));
        String line, docId;
        int max_tf, unique_words, length;
        while ((line = br.readLine()) != null) {
            docId = line.substring(7, line.indexOf(", max_tf="));
            max_tf = Integer.parseInt(line.substring(line.indexOf(", max_tf=")+9, line.indexOf(", unique_words=")));
            unique_words = Integer.parseInt(line.substring(line.indexOf(", unique_words=")+15, line.indexOf(", length=")));
            length = Integer.parseInt(line.substring(line.indexOf(", length=")+9, line.indexOf(", entities=")));

            docSet.put(docId, new Document(docId, max_tf, unique_words, length));
            docSet.get(docId).setEntities( line.substring(line.indexOf(", entities=") + 11, line.lastIndexOf('}')));
        }

        return docSet;
    }

    private HashMap<String, City> restoreCities() throws IOException
    {
        HashMap<String, City> cities = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(postings_path + "/ProgramData/Cities.txt"));
        String line, city, country, currency, population, docs[];
        LinkedList<String> list;

        while ((line = br.readLine()) != null) {
            city = line.substring(10, line.indexOf(", country='"));
            country = line.substring(line.indexOf(", country='")+10, line.indexOf(", currency='"));
            currency = line.substring(line.indexOf(", currency='")+11, line.indexOf(", population='"));
            population = line.substring(line.indexOf(", population='")+13, line.indexOf(", docsRepresent="));

            docs = line.substring(line.indexOf(", docsRepresent=")+17, line.length()-1).split(",");
            list = new LinkedList<>(Arrays.asList(docs));

            cities.put(city, new City(city, country, currency, population, list));
        }

        return cities;
    }
}