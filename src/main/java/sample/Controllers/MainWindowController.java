package sample.Controllers;

import javafx.application.Platform;
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
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.CheckListView;
import sample.Models.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindowController implements Initializable
{
    private boolean toStem;
    private long startTime, endTime;
    private String corpus_path, postings_path, queries_path;
    private HashSet<City> hs_citiesSelected;

    private ReadFile readFile;
    private Indexer indexer;
    private Searcher searcher;
    private Ranker ranker;

    public MainWindowController() { toStem = false; }

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
    public TextArea txt_queryEntry;
    public TextField txt_corpus_path;
    public TextField txt_postings_path;
    public TextField txt_queryPath;
    public CheckBox chbx_stemming;
    public CheckBox chbx_semantic;
    public ImageView imageView_logo;
    public MenuButton m_languages;

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
        try { FileUtils.deleteDirectory(new File(postings_path+"/Temporary Postings")); } // delete all temporary files
        catch (IOException e) { e.printStackTrace(); }
        endTime = System.currentTimeMillis();
        alert.close();
        displaySummary();

        // init for the search phase
        ranker = new Ranker(indexer.getDictionary(), indexer.getDocsSet());
        searcher = new Searcher(parser, indexer, ranker, postings_path);
    }

    /**
     * Open the file explorer window, Enable the user to choose a directory.
     * @param actionEvent when one of the 'Browse' button is selected.
     */
    @FXML
    public void openFileExplorer(ActionEvent actionEvent)
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
    }

    @FXML
    public void setToStem()
    {
        toStem = chbx_stemming.isSelected();
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
        String query = txt_queryEntry.getText();
        if (query.length() == 0) {
            new Alert(Alert.AlertType.ERROR, "Please Enter Query").showAndWait();
            return;
        }

        // display the retrieved documents in new stage
        Stage window = new Stage();
        window.setTitle("Retrieved Documents");

        ObservableList<Document> retrievedDocumentsList = FXCollections.observableArrayList(searcher.parseFromQuery(query, hs_citiesSelected));

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
    public void runQueryFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("TEXT files (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("All files (*)", "*")
        );
        File qFile = fileChooser.showOpenDialog(vBox_mainWindows.getScene().getWindow());
        if (qFile!=null)
            txt_queryPath.setText(qFile.getAbsolutePath());

        // run the searcher on the query file
        searcher.parseFromQueryFile(qFile, hs_citiesSelected);
    }
}