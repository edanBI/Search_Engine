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
import java.util.Collection;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MainWindowController implements Initializable
{
    private boolean toStem;
    private long startTime, endTime;
    private String corpus_path, postings_path;
    private Indexer indexer;

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

    @FXML
    public void initDataSet()
    {
        if (corpus_path==null || postings_path==null)
        {
            showAlert();
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Parsing and Indexing...");
        alert.show();
        startTime = System.currentTimeMillis();
        ReadFile readFile = new ReadFile(corpus_path);
        Parser parser = new Parser(corpus_path+"/stop_words.txt", toStem);
        indexer = new Indexer(postings_path);
        HashMap<String,String> files;//will be fill by docid and text of the files
        for (int i = 0 ; i<readFile.getListOfFilePath().size() ; i++){
            files = readFile.read(readFile.getListOfFilePath().get(i));//filling the docs file by file
            System.out.println(i);
            for (String id:files.keySet()) {
                indexer.processTerms(parser.Parsing(files.get(id)), id);
            }
        }
        indexer.flushTmpPosting(); //clear all the remaining terms which haven't written to disk
        indexer.mergeTmpPostingFiles(); // build final posting files
        indexer.writeDictionaryToDisk(); // store all the dictionary in disk
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
        if (chbx_stemming.isSelected()) toStem = true;
        if (!chbx_stemming.isSelected()) toStem = false;
    }

    @FXML
    public void reset()
    {
        try {
            new File(postings_path + "/dictionary.txt").delete();
            FileUtils.deleteDirectory(new File(postings_path + "/Posting Files"));
        }
        catch (IOException e) { e.printStackTrace(); }
        txt_corpus_path.clear();
        txt_postings_path.clear();
        corpus_path = null;
        postings_path = null;
        chbx_stemming.setSelected(false);
        chbx_stemming.setIndeterminate(false);
        toStem = false;
    }

    @FXML
    public void displayDictionary()
    {
        try {
            Stage d_window = new Stage();
            d_window.setTitle("Dictionary");
            Collection<DictionaryRecord> list = Files.readAllLines(new File(postings_path + "/dictionary.txt").toPath()).stream()
                    .map(line -> {
                        String[] row = line.split("---");
                        String term = row[0];
                        int df = Integer.parseInt(row[1]);
                        int freq = Integer.parseInt(row[2]);
                        return new DictionaryRecord(term, df, freq);
                    }).collect(Collectors.toList());
            ObservableList<DictionaryRecord> details = FXCollections.observableArrayList(list);

            TableView<DictionaryRecord> tbl_dictionary = new TableView<>();
            TableColumn<DictionaryRecord, String> clmn_terms = new TableColumn<>();
            //TableColumn<DictionaryRecord, Integer> clmn_df = new TableColumn<>();
            TableColumn<DictionaryRecord, Integer> clmn_totalFreq = new TableColumn<>();
            tbl_dictionary.setMinWidth(500.0);
            tbl_dictionary.setMinHeight(702.0);
            clmn_terms.setText("Terms");
            clmn_terms.setMinWidth(300.0);
            //clmn_df.setText("DF");
            //clmn_df.setMinWidth(100.0);
            clmn_totalFreq.setText("Total Frequency");
            clmn_totalFreq.setMinWidth(200.0);

            //tbl_dictionary.getColumns().addAll(clmn_terms, clmn_df, clmn_totalFreq);
            tbl_dictionary.getColumns().addAll(clmn_terms, clmn_totalFreq);

            clmn_terms.setCellValueFactory(data -> data.getValue().getTermProperty());
            //clmn_df.setCellValueFactory(data -> data.getValue().getDfProperty());
            clmn_totalFreq.setCellValueFactory(data -> data.getValue().getTotalFreqProperty());
            tbl_dictionary.setItems(details);

            ScrollPane scrollPane = new ScrollPane(tbl_dictionary);
            scrollPane.setFitToWidth(true);
            Scene scene = new Scene(scrollPane, 515, 705);
            d_window.setScene(scene);
            d_window.setResizable(false);
            d_window.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Dictionary is unavailable");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        }
    }

    @FXML
    public void loadDictionary()
    {
        /*FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt"));
        File f_dictionary = fc.showOpenDialog(vBox_mainWindows.getScene().getWindow());*/
        try {
            String str = txt_postings_path.getText();
            TreeMap<String, DictionaryRecord> loadedDictionary = Indexer.readDictionaryFromFile(str);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Missing Posting Path");
            java.awt.Toolkit.getDefaultToolkit().beep();
            alert.showAndWait();
        }
    }

    private void displaySummary()
    {
        /*Stage win = new Stage();
        win.setTitle("Summary");
        TextField txt_numDocs = new TextField("#Docs Indexed: " + this.indexer.getNumDocsIndexed());
        TextField txt_uniqueTerms = new TextField("#Unique Terms: " + this.indexer.getNumUniqueTerms());
        TextField txt_time = new TextField("Time: " + ((double)(endTime - startTime)/60000) + " min.");
        //TextField txt_time = new TextField("Time: " + ((double)(endTime - startTime)/1000) + " seconds.");

        VBox vBox = new VBox(txt_numDocs, txt_uniqueTerms, txt_time);
        Scene scene = new Scene(vBox, 300, 300);
        win.setScene(scene);
        win.showAndWait();*/

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Indexing Summary");
        alert.setHeaderText(null);
        alert.setContentText("#Docs Indexed: " + this.indexer.getNumDocsIndexed() + '\n' +
                            "#Unique Terms: " + this.indexer.getNumUniqueTerms() + '\n' +
                            "Time: " + ((double)(endTime - startTime)/60000) + " min.");
        alert.showAndWait();
    }
}
