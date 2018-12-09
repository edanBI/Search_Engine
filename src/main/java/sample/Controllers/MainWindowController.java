package sample.Controllers;

import javafx.collections.FXCollections;
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
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import sample.Models.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindowController implements Initializable
{
    private boolean toStem;
    private long startTime, endTime;
    private String corpus_path, postings_path;
    private Indexer indexer;
    private ReadFile readFile;

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
    public TextField txt_corpus_path;
    public TextField txt_postings_path;
    public Button btn_corpus_browse;
    public Button btn_postings_browse;
    public Button btn_generate;
    public CheckBox chbx_stemming;
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
        try { FileUtils.deleteDirectory(new File("DB Files")); } // delete all temporary files
        catch (IOException e) { e.printStackTrace(); }
        endTime = System.currentTimeMillis();
        alert.close();
        displaySummary();
    }

    /**
     * Open the file explorer window, Enable the user to choose a directory.
     * @param actionEvent when one of the 'Browse' button is selected.
     */
    @FXML
    public void openFileExplorer(ActionEvent actionEvent)
    {
        DirectoryChooser dc = new DirectoryChooser();

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
            if (!toStem) {
                new File(postings_path + "/dictionary.txt").delete();
                FileUtils.deleteDirectory(new File(postings_path + "/Posting Files"));
            } else {
                new File(postings_path + "/dictionary_stemmer.txt").delete();
                FileUtils.deleteDirectory(new File(postings_path + "/Posting Files_stemmer"));
            }
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
        try {
            Stage d_window = new Stage();
            d_window.setTitle("Dictionary");
            String dic_path;
            if (this.toStem)
                dic_path = postings_path + "\\dictionary_stemmer.txt";
            else
                dic_path = postings_path + "\\dictionary.txt";
            Collection<DictionaryRecord> list = Files.readAllLines(new File(dic_path).toPath()).stream()
                    .map(line -> {
                        String[] row = line.split("---");
                        String term = row[0];
                        int df = Integer.parseInt(row[1]);
                        int freq = Integer.parseInt(row[2]);
                        if (term.length()==0)
                            term = "-";
                        return new DictionaryRecord(term, df, freq);
                    }).collect(Collectors.toList());
            ObservableList<DictionaryRecord> details = FXCollections.observableArrayList(list);

            TableView<DictionaryRecord> tbl_dictionary = new TableView<>();
            TableColumn<DictionaryRecord, String> clmn_terms = new TableColumn<>();
            TableColumn<DictionaryRecord, Integer> clmn_totalFreq = new TableColumn<>();
            tbl_dictionary.setMinWidth(500.0);
            tbl_dictionary.setMinHeight(702.0);
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
            //e.printStackTrace();
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
}