package View;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Map;

public class DictionaryItem {

    private SimpleStringProperty term;
    private SimpleStringProperty tf;

    public DictionaryItem(String termString, String tfString) {
        term = new SimpleStringProperty(termString);
        tf = new SimpleStringProperty(tfString);
    }


    public String getTerm() {
        return term.get();
    }

    public SimpleStringProperty termProperty() {
        return term;
    }

    public void setTerm(String term) {
        this.term.set(term);
    }

    public String getTf() {
        return tf.get();
    }

    public SimpleStringProperty tfProperty() {
        return tf;
    }

    public void setTf(String tf) {
        this.tf.set(tf);
    }
}
