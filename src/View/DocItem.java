package View;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;


public class DocItem {
    private SimpleStringProperty queryId;
    private SimpleStringProperty docId;
    private Button show;
    private Map<String,Double> entityRankMap;
    private boolean entity;

    public DocItem(String queryNum, String docNo, Map<String,Double> innetMap,boolean wantEntity){
        queryId = new SimpleStringProperty(queryNum);
        docId = new SimpleStringProperty(docNo);
        show = new Button("Show 5 Entities");
        entityRankMap = innetMap;
        entity = wantEntity;

        show.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(!entity){
                    showAlert("Entities selection box wasnt checked, please mark entities selection box and try again ");
                }
                else{
                    showEntity();
                }


            }
        });
    }
    public void showEntity(){
        Stage stage = new Stage();
        stage.setTitle("Enitiys and rank for doc" + docId.getValue());
        TableView tw = new TableView();
        tw.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
        tw.setPrefSize( 600, 800 );
        TableColumn<String, EntityItem> firstCol = new TableColumn<>("Entity");
        firstCol.setCellValueFactory(new PropertyValueFactory<>("entityName"));
        TableColumn<String,EntityItem> secCol = new TableColumn<>("Rank");
        secCol.setCellValueFactory(new PropertyValueFactory<>("entityRank"));
        tw.getColumns().addAll(firstCol,secCol);
        if(entityRankMap != null && entityRankMap.size() > 0) {
            for (String entity : entityRankMap.keySet()) {
                Double theRank = entityRankMap.get(entity);
                EntityItem item = new EntityItem(entity, theRank.toString());
                tw.getItems().add(item);
            }
        }
        else {
            showAlert("There is no entities to display at this moment ");
        }
        VBox vBox = new VBox(tw);
        Scene scene = new Scene(vBox);
        stage.setScene(scene);
        stage.show();
    }

    public String getQueryId() {
        return queryId.get();
    }

    public SimpleStringProperty queryIdProperty() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId.set(queryId);
    }

    public String getDocId() {
        return docId.get();
    }

    public SimpleStringProperty docIdProperty() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId.set(docId);
    }

    public Button getShow() {
        return show;
    }

    public void setShow(Button show) {
        this.show = show;
    }

    public Map<String, Double> getEntityRankMap() {
        return entityRankMap;
    }

    public void setEntityRankMap(Map<String, Double> entityRankMap) {
        this.entityRankMap = entityRankMap;
    }

    private void showAlert(String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.show();
    }
}
