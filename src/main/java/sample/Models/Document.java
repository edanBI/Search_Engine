package sample.Models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.util.*;

public class Document {
    private String doc_id;
    private int max_tf;
    private int unique_words;
    private int length;
    private String entities;
    private Button entitiesButton;
    public double weight;

    Document(String doc_id, int max_tf, int unique_words) {
        this.doc_id = doc_id;
        this.max_tf = max_tf;
        this.unique_words = unique_words;
        this.length = 0;
//        entitiesButton = new Button("Display Entities");
//        entitiesButton.setOnAction(this::displayEntities);
        this.entities = "";
    }

    public Document(String doc_id, int max_tf, int unique_words, int length) {
        this.doc_id = doc_id;
        this.max_tf = max_tf;
        this.unique_words = unique_words;
        this.length = length;
//        entitiesButton = new Button("Display Entities");
//        entitiesButton.setOnAction(this::displayEntities);
        this.entities = "";
    }

    public String getDoc_id() {
        return doc_id;
    }

    public StringProperty getPropertyDoc_id() {
        return new SimpleStringProperty(doc_id);
    }

    public int getMax_tf() {
        return max_tf;
    }

    public int getUnique_words() {
        return unique_words;
    }

    int getLength() {
        return length;
    }

    void updateLength(int val) {
        this.length += val;
    }

    void setMax_tf(int max_tf) {
        this.max_tf = max_tf;
    }

    void setUnique_words(int unique_words) {
        this.unique_words = unique_words;
    }

    public String getEntities() {
        return this.entities;
    }

    void setEntities(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        list.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int oo1 = Integer.parseInt(o1.substring(o1.indexOf("_") + 1));
                int oo2 = Integer.parseInt(o2.substring(o2.indexOf("_") + 1));
                if (oo1 < oo2) return 1;
                else if (oo1 > oo2) return -1;
                else
                    return o1.substring(0, o1.indexOf("_")).compareTo(o2.substring(0, o2.indexOf("_")));
            }
        });
        for (String s : list
        ) {
            stringBuilder.append(s.substring(0, s.indexOf("_")) + "@");
        }
        if (stringBuilder != null && !stringBuilder.toString().isEmpty())
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        this.entities = stringBuilder.toString();
    }

    public void setEntities(String arg) {
        entities = arg;
    }

    @Override
    public String toString() {
        return "Document{" +
                "doc_id=" + doc_id +
                ", max_tf=" + max_tf +
                ", unique_words=" + unique_words +
                ", length=" + length +
                ", entities=" + entities +
                '}';
    }

    public Button getEntitiesButton() {
        return entitiesButton;
    }

    public void setEntitiesButton(Button entitiesButton) {
        this.entitiesButton = entitiesButton;
        this.entitiesButton.setOnAction(this::displayEntities);
    }

    private void displayEntities(ActionEvent event) {
        ListView<String> list = new ListView<>(FXCollections.observableArrayList(Arrays.asList(getEntities())));
        Scene scene = new Scene(list);
        Stage window = new Stage();
        window.setScene(scene);
        window.showAndWait();
    }
}