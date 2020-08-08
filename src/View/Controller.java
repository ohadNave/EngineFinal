package View;

import Model.Indexer;
import Model.Searcher;
import Model.Term;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static Model.Indexer.deletePostingFilesAndDictionary;

public class Controller {


    public static boolean wantToStem;
    private boolean wantEntity;
    private boolean wantSemantical;
    private static String queryFilePathString;
    private static String savePathString;
    private static String theSingleQuery;
    private static Map<String, Term> dictionary;
    private static Searcher searcher;

    @FXML
    public TextField corpusPath;
    public TextField postingPath;
    public TextField singleQuery;
    public TextField queryFilePath;
    public TextField savePath;
    public Button corpusButton;
    public Button postingButton;
    public Button loadDicButton;
    public Button searchButton;
    public Button displayDicButton;
    public Button queryFilePathButton;
    public Button chooseSavePathButton;
    public Button savePathButton;
    public Button indexButton;
    public Button displayResult;
    public Button resetButton;
    public CheckBox stem;
    public CheckBox entity;
    public CheckBox treat;

    public static String stringCorpusPath = "";
    public static String stringostingPath = "";
    private NumberFormat numFormatter;


    private void showAlert(String alertMessage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(alertMessage);
        alert.show();
    }

    public void createIndex(javafx.event.ActionEvent actionEvent) throws Exception {
        numFormatter = NumberFormat.getNumberInstance(Locale.US);
        try {
            if (!stringostingPath.isEmpty() && !stringCorpusPath.isEmpty()) {
                long start = System.currentTimeMillis();
                Indexer indexer = new Indexer(stringostingPath, stringCorpusPath, wantToStem);
                indexer.createInvertedIndex();
                StringJoiner sj = new StringJoiner("\n");
                String dicSize = "Amount of uniqe words: " + numFormatter.format(indexer.getDicSize());
                sj.add(dicSize);
                sj.add("Amount of documents that has been indexed: " + indexer.getDocCounter());
                long finish = System.currentTimeMillis();
                sj.add("Total SECONDS for indexing: " + TimeUnit.MILLISECONDS.toSeconds(finish - start));
                sj.add("Total MINUTES for indexing: " + TimeUnit.MILLISECONDS.toMinutes(finish - start));
                showAlert(sj.toString());
                resetButton.setDisable(false);
                dictionary = indexer.getDictionary();
                displayDicButton.setDisable(false);
            }
            else {
                showAlert("Please enter valid path for the corpus and stop-words");
            }
        } catch (IOException e) {
            showAlert("Please enter valid path for the corpus and stop-words");
        }
    }

    public void resetProgram(javafx.event.ActionEvent actionEvent) {
        if(stringostingPath.isEmpty()){
            showAlert("Please enter posting path first");
        }
        if(checkIfPostingDirIsEmpty()){
            showAlert("The directory is empty");
        }
        resetButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
                    if(mouseEvent.getClickCount() == 2){
                        System.out.println("Double clicked");
                        showAlert("You Double clicked the button");
                    }
                    else {
                        deletePostingFilesAndDictionary(stringostingPath);
                    }
                }
            }
        });
    }

    public void selectDir(javafx.event.ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        try {
            if (selectedDirectory != null) {
                String chosenPath = selectedDirectory.getAbsolutePath();
                if (actionEvent.getSource() == corpusButton) {
                    corpusPath.setText(chosenPath);
                    stringCorpusPath = chosenPath;
                } else {
                    showAlert("Please enter valid path for the corpus and stop-words");
                }
            } else {
                showAlert("Please enter valid path for the corpus and stop-words");
            }
        } catch (Exception e) {
            showAlert("Please enter valid path for the corpus and stop-words");
        }
    }

    public void selectPostingDir(javafx.event.ActionEvent actionEvent) {
        DirectoryChooser postDirChooser = new DirectoryChooser();
        File postingDirectory = postDirChooser.showDialog(null);
        try {
            if (postingDirectory != null) {
                String chosenPath = postingDirectory.getAbsolutePath();
                if (actionEvent.getSource() == postingButton) {
                    postingPath.setText(chosenPath);
                    stringostingPath = chosenPath;
                    if(!stringCorpusPath.isEmpty() && !stringostingPath.isEmpty()){
                        indexButton.setDisable(false);
                    }
                } else {
                    showAlert("Please enter valid path for posthing");
                }
            } else {
                showAlert("Please enter valid path for posting");
            }
        } catch (Exception e) {
            showAlert("Please enter valid path for posting");
        }
    }
    public void selectSavingDir(javafx.event.ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);
        try {
            if (!stringostingPath.isEmpty() && selectedDirectory != null) {
                String chosenPath = selectedDirectory.getAbsolutePath();
                if (actionEvent.getSource() == chooseSavePathButton) {
                    savePath.setText(chosenPath);
                    savePathString = chosenPath;
                } else {
                    showAlert("Please enter valid path to save results and posting path");
                }
            } else {
                showAlert("Please enter valid path to save results and posting path");
            }
        } catch (Exception e) {
            showAlert("Please enter valid path to save results and posting path");
        }
    }


    public void save(javafx.event.ActionEvent actionEvent) throws IOException {
        try {
            if(!savePathString.isEmpty()) {
                if (actionEvent.getSource() == savePathButton) {
                    File saveTheFileForUser = new File(savePathString + "/UserQueryResults.txt");
                    File readTheFile = new File(stringostingPath + "/results.txt");
                    if(readTheFile.exists()){
                        BufferedReader reader = new BufferedReader(new FileReader(readTheFile));
                        BufferedWriter writer = new BufferedWriter(new FileWriter(saveTheFileForUser));
                        String line = "";
                        while (reader.readLine() != null) {
                            line = reader.readLine();
                            writer.write(line);
                            writer.newLine();
                        }
                        writer.flush();
                        reader.close();
                        writer.close();
                        showAlert("The file saved");
                    }
                    else showAlert("Error Please run search again.");

                }
            }
            else showAlert("Please enter valid path to save results");

        }catch (Exception e){
            showAlert("Please enter valid path to save results");
        }

    }

    public void createSearcher(javafx.event.ActionEvent actionEvent) {
        try {
            boolean flag = true;
            System.out.println("Start searching");
            if (actionEvent.getSource() == searchButton) {
                queryFilePathString = queryFilePath.getText();
                theSingleQuery = singleQuery.getText();
                if(!stringostingPath.isEmpty()) {
                    if (dictionary != null) {
                        if (theSingleQuery == null || theSingleQuery.isEmpty()) {
                            if (queryFilePathString == null || queryFilePathString.isEmpty()) {
                                showAlert("Please enter valid path for queries");
                                flag = false;
                            } else {
                                // the user choose file
                                searcher = new Searcher(wantToStem, wantSemantical, wantEntity, dictionary, false, queryFilePathString, stringostingPath);
                                searcher.start("");
                            }
                        } else {
                            // the user choose single query
                            // check that there is no other path in the file too
                            if (!queryFilePathString.isEmpty()) {
                                showAlert("Please enter only one type for query");
                                flag = false;
                            } else {
                                searcher = new Searcher(wantToStem, wantSemantical, wantEntity, dictionary, true, "", stringostingPath);
                                searcher.start(theSingleQuery);
                            }
                        }//else
                    }//
                    else {
                        showAlert("You can't search without loading the dictionary. Please load the dictionary first or run the index again");
                        flag = false;
                    }
                }
                else {
                    showAlert("You can't search without posting path. Please enter posting path");
                    flag = false;
                }
            }
            if(flag){
                createQueryResultToDisplay();
            }
            System.out.println("Finish searching");
        }catch (Exception e){
            showAlert("Somthing went wrong. Please try again");
        }

    }

    public void runFileOfQuery(javafx.event.ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Queries Files", "*.txt"));
        File loadFile = fileChooser.showOpenDialog(null);
        if (loadFile != null) {
            String chosenPath = loadFile.getAbsolutePath();
            if (actionEvent.getSource() == queryFilePathButton) {
                queryFilePath.setText(chosenPath);
                queryFilePathString = chosenPath;
            }
        } else {
            showAlert("Please enter path to queries file again");
        }

    }

    public void checkBoxStem(javafx.event.ActionEvent actionEvent) throws Exception {
        if (actionEvent.getSource() == stem) {
            if(wantToStem){
                wantToStem = false;
            }
            else {
                wantToStem = true;
            }
        }
    }

    public void checkBoxs(javafx.event.ActionEvent actionEvent) throws Exception {
        if (actionEvent.getSource() == entity) {
            // remove entity option
            if(wantEntity){
                wantEntity = false;
            }
            else{
                wantEntity = true;
            }
        }
        if (actionEvent.getSource() == treat) {
            if(wantSemantical){
                wantSemantical = false;
            }
            else{
                wantSemantical = true;
            }

        }
    }

    public void displayDic(javafx.event.ActionEvent actionEvent){
        try{
            if(!checkIfPostingDirIsEmpty() || dictionary != null){
                if(actionEvent.getSource() == displayDicButton){
                    Stage stage = new Stage();
                    stage.setTitle("The Dictionary");
                    TableView tw = new TableView();
                    tw.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
                    tw.setPrefSize( 600, 800 );

                    TableColumn<String,Item> firstCol = new TableColumn<>("The Term");
                    firstCol.setCellValueFactory(new PropertyValueFactory<>("term"));
                    TableColumn<String,Item> secCol = new TableColumn<>("The Tf");
                    secCol.setCellValueFactory(new PropertyValueFactory<>("tf"));
                    tw.getColumns().addAll(firstCol,secCol);


                    Object[] keys = dictionary.keySet().toArray();
                    Arrays.sort(keys);
                    for (Object term : keys) {
                        Term currTerm = dictionary.get(term);
                        int termCounter = currTerm.getTotalCounter();
                        DictionaryItem dictionaryItem = new DictionaryItem((String)term,"" + termCounter);
                        tw.getItems().add(dictionaryItem);
                    }
                    VBox vBox = new VBox(tw);
                    Scene scene = new Scene(vBox);
                    stage.setScene(scene);
                    stage.show();
                }
            }
            else {
                showAlert("You need to load the dictionary first");
            }

        }
        catch (Exception e){
            showAlert("Something went wrong");
        }
    }



    public void loadDictionary(javafx.event.ActionEvent actionEvent) throws Exception {
        try {
            if (!stringostingPath.isEmpty() && !checkIfPostingDirIsEmpty()) {
                dictionary = Indexer.loadDictionary(stringostingPath, wantToStem);
                resetButton.setDisable(false);
                System.out.println("Dictionary is loaded");
                if (wantToStem) System.out.println("Stemmed dictionary has been loaded");
                else showAlert("Regular dictionary has been loaded");
                displayDicButton.setDisable(false);
            } else {
                showAlert("There is no dictionary file to load");
            }
        } catch (Exception e) {
            showAlert("There is no dictionary file to load");
        }
    }

    public void displayQueryResult(javafx.event.ActionEvent actionEvent) {
        try{
            if(actionEvent.getSource() == displayResult){
                if(searcher != null){
                    createQueryResultToDisplay();
                }else {
                    showAlert("You need to search first. Please try again");
                }

            }
        }catch (Exception e){
            showAlert("There is no nothing to show");
        }


    }
    private void createQueryResultToDisplay(){
        Stage stage = new Stage();
        Map<Integer, ArrayList<String>> docs = searcher.getRelevantDocs();
        Map<String,Map<String,Double>> entityMap = searcher.getDocEntityMap();
        stage.setTitle("All Docs For Queries");
        TableView tw = new TableView();
        tw.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
        tw.setPrefSize( 600, 800 );


        TableColumn<String,Item> firstCol = new TableColumn<>("QueryId");
        firstCol.setCellValueFactory(new PropertyValueFactory<>("queryId"));
        TableColumn<String,Item> secCol = new TableColumn<>("Show Relevet Docs");
        secCol.setCellValueFactory(new PropertyValueFactory<>("showDocuments"));
        tw.getColumns().addAll(firstCol,secCol);


        for (Integer queryNumber : docs.keySet()) {
            ArrayList<String> doclList = docs.get(queryNumber);
            Item item = new Item( Integer.toString(queryNumber), doclList,entityMap,wantEntity);
            tw.getItems().add(item);
        }

        VBox vBox = new VBox(tw);
        Scene scene = new Scene(vBox);
        stage.setScene(scene);
        stage.show();
    }

    private boolean checkIfPostingDirIsEmpty(){
        File file = new File(stringostingPath);
        if(file.list().length > 0){
            return false;
        }else {
            return true;
        }
    }

}
