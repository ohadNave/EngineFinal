package Model;

import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class Document implements Serializable {

    private String docNO;
    private String docHeader;
    private String mostFrequentTerm;
    private int mostFrequentTermNum;
    private int uniqueTermsNum;
    private int docLength;
    private Map<String, Integer> docTerms;


    public Document(String docNO) {
        this.docNO = docNO;
        this.docTerms = new HashMap<>();
        docLength = 0;
    }


    public Document(String docNO , String docHeader) {
        this.docNO = docNO;
        this.docHeader = docHeader;
        this.docTerms = new HashMap<>();
        docLength = 0;
    }

    public void setDocHeader(String docHeader) {
        this.docHeader = docHeader;
    }

    /**
     * updates the most frequent term value(String)
     */
    public void updateMostFreq() {
        int max = 0;
        for (String term : docTerms.keySet()) {
            if (docTerms.get(term) > max) {
                max = docTerms.get(term);
                mostFrequentTerm = term;
            }
        }
        mostFrequentTermNum = max;
    }


    /**
     * The Last station for each term - adds the param term into this document terms map.
     * @param term
     */
    public void addWordToDic(String term) {
        if(!term.isEmpty()){
            int df = 1;
            String allUpper = term.toUpperCase();
            String allLower = term.toLowerCase();

            if(term.equals(allLower) || term.equals(allUpper)){
                if(term.equals(allLower)){              //i am lower
                    if(docTerms.containsKey(allLower)){
                        df = docTerms.get(allLower);
                        docTerms.put(allLower,df + 1);
                    }
                    else if(docTerms.containsKey(allUpper)){
                        df = docTerms.get(allUpper);
                        docTerms.put(allLower,df + 1);
                        docTerms.remove(allUpper);
                    }
                    else{
                        docTerms.put(term,df);
                    }
                }
                else{            //i am upper
                    if(docTerms.containsKey(allLower)){
                        df = docTerms.get(allLower);
                        docTerms.replace(allLower,df + 1);
                    }
                    else if(docTerms.containsKey(allUpper)){
                        df = docTerms.get(allUpper);
                        docTerms.replace(allUpper,df + 1);
                    }
                    else{ // i am upper & not exist
                        docTerms.put(allUpper,df);
                    }
                }
            }
            else{
                if(docTerms.containsKey(term)){
                    df = docTerms.get(term);
                    docTerms.replace(term,df + 1);
                }
                else{
                    docTerms.put(term,df);
                }
            }
        }
    }

    /**
     * clears doc terms map it has been posted in indexer.
     */
    public void clearDocTermsMap() { this.docTerms.clear(); }

    public Map<String, Integer> getDocTerms() { return docTerms; }

    public String getDocNO() { return docNO; }

    public String getMostFrequentTerm() { return mostFrequentTerm; }

    public int getMostFrequentTermNum() { return mostFrequentTermNum; }

    public int getUniqueTermsNum() { return uniqueTermsNum; }

    public void setUniqueTermsNum() { uniqueTermsNum = docTerms.size(); }

    public int getDocLength() { return docLength; }

    public void setDocLength(int docLength) { this.docLength = docLength; }

    public String getDocHeader() {
        return docHeader;
    }

}
