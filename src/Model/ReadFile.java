package Model;

import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadFile  {

    private static final int FILES_PER_CHUNK = 10 ;
    private int totalFilesToRead;
    private int filesCounter;
    private String corpusPath;
    private  List<String> filePaths;
    protected static Map <Pair<String,String>,String> docsBeforeParse;
    //protected static Map <String,String> docsBeforeParse;



    public ReadFile(String path ){
        if (path != null){
            corpusPath = path;
            filesCounter = 0;
            buildPathsArray();
        }
    }


    /**
     * with the corpus path, gets all the file paths and puts them in data structore.
     */
    public void buildPathsArray() {
        filePaths = new ArrayList<>();
        File corpusFile = new File(corpusPath);
        for (File subFile : corpusFile.listFiles() ) {
            if(subFile.getName().equals("stop_words.txt"))
                continue;
            filePaths.add(subFile.getPath() + "//" + subFile.getName() );
        }
        totalFilesToRead = filePaths.size();
    }


    //reades 8 files each time (for condition)

    /**
     * Seperetas docunemts in each doc with the assistance of the <DOC> </DOC> tags.
     */
    public void seperateDocsInFile() {
        try {
            docsBeforeParse = new HashMap<>();
            for (int i = 0; i < FILES_PER_CHUNK && filesCounter < totalFilesToRead; i++) {
                Document document = Jsoup.parse(new File(filePaths.get(filesCounter)), "UTF-8");
                Elements documents = document.getElementsByTag("DOC");
                filesCounter++;
                splitDoc_Header_Text(documents);
            }
        }
        catch (Exception e){ e.getCause(); }
    }

    //takes entire Document and seperates all docsBeforeParse in it into data base including doc NO & text Header & text content
    /**
     * Takes relevant data in each doc with the assitance of the <TI></TI> & <TEXT></TEXT> tags.
     */
    private void splitDoc_Header_Text(Elements documents ){
        for (Element document : documents) {
            String docNO = document.getElementsByTag("DOCNO").text().trim();
            String docText = document.getElementsByTag("Text").text();
            String docTitle = document.getElementsByTag("TI").text().trim();
            if(docTitle == null || docTitle.length() == 0)
                docTitle = "noTitle";
            Pair<String,String> keyValue = new Pair<>(docNO,docTitle);
            //String textToParse = docTitle+" "+ docText;
            docsBeforeParse.put(keyValue ,docTitle + " " +  docText); ////

        }
    }



    /**
     * let the indexer know if there are any files left to read
     * @return
     */
    public boolean hasMoreToRead(){ return filesCounter < totalFilesToRead; }

    public Map<Pair<String,String>, String> getDocsBeforeParse() { return docsBeforeParse; }
}
