package Model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ReadQuery {

    private String queryFilePath;
    ArrayList<Query> Queries;

    /**
     *
     * @param queryFilePath - queries file path
     */
    public ReadQuery(String queryFilePath){
        this.queryFilePath = queryFilePath;
        Queries = new ArrayList<>();
    }


    /**
     * starts the query reader operation - splits each <top> element that is in the queries file
     */
    public void start() {
        ArrayList<Element> queryBeforeLabeling = null;
        try {
                queryBeforeLabeling = new ArrayList<>();
                Document queryFile = Jsoup.parse(new File(queryFilePath), "UTF-8");
                Elements elements = queryFile.getElementsByTag("top");
                for (Element element : elements){
                    queryBeforeLabeling.add(element);
                }
        }
        catch (Exception e){ e.getCause(); }
        labelEachQuery(queryBeforeLabeling);
    }


    /**
     * gets each query data and assign relevant query fields.
     * @param before
     */
    public void labelEachQuery(ArrayList<Element> before ){
        for ( Element e : before ){
            String allContent = e.text();
            String number = allContent.split(("\\s\\s"))[0].split("Number:")[1].trim();
            String title =  allContent.split(("\\s\\s"))[1];
            allContent = allContent.split("Description:")[1];
            String desc = allContent.split("Narrative:")[0];
            allContent = allContent.split("Narrative:")[1];
            String narr = allContent;
            Query query = new Query(number,title,desc,narr);
            Queries.add(query);
        }
    }

    public ArrayList<Query> getQueries() {
        return Queries;
    }


}
