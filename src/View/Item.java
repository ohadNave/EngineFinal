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

public class Item {


    private SimpleStringProperty queryId;
    private Button showDocuments;
    private ArrayList<String> documentsForQuery;
    private Map<String,Map<String,Double>> entityMap;
    private boolean isEntity;

    public Item(String queryNO, ArrayList<String> docs,Map<String,Map<String,Double>> mapRankedEntity,boolean wantEntity) {
        queryId = new SimpleStringProperty(queryNO);
        showDocuments = new Button("Show Docs");
        documentsForQuery = docs;
        entityMap = mapRankedEntity;
        isEntity = wantEntity;


        showDocuments.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                showTheDocuments();
            }
        });

    }
    public void showTheDocuments(){
        Stage docStage = new Stage();
        docStage.setTitle("Doc For Queries");
        TableView tw = new TableView();
        tw.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
        tw.setPrefSize( 600, 800 );
        TableColumn<String, DocItem> firstCol = new TableColumn<>("Doc Id");
        firstCol.setCellValueFactory(new PropertyValueFactory<>("docId"));
        TableColumn<String,DocItem> secCol = new TableColumn<>("Show Docs");
        secCol.setCellValueFactory(new PropertyValueFactory<>("show"));
        tw.getColumns().addAll(firstCol,secCol);


        for (String docNo : documentsForQuery) {
            Map<String,Double> rankedMap = entityMap.get(docNo);
            DocItem docItem = new DocItem(queryId.getValue(),docNo,rankedMap,isEntity);
            tw.getItems().add(docItem);

        }

        VBox vBox = new VBox(tw);
        Scene scene = new Scene(vBox);
        docStage.setScene(scene);
        docStage.show();
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

    public Button getShowDocuments() {
        return showDocuments;
    }

    public void setShowDocuments(Button showDocuments) {
        this.showDocuments = showDocuments;
    }

    public ArrayList<String> getDocumentsForQuery() {
        return documentsForQuery;
    }

    public void setDocumentsForQuery(ArrayList<String> documentsForQuery) {
        this.documentsForQuery = documentsForQuery;
    }

    public Map<String, Map<String, Double>> getEntityMap() {
        return entityMap;
    }

    public void setEntityMap(Map<String, Map<String, Double>> entityMap) {
        this.entityMap = entityMap;
    }
}
