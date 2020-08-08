package Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;

public class Ranker {

    private String postingPath ;
    private boolean useStem;
    private HashMap<String, Map<String, Integer>> simWordsQuery; //  hold for each synonyms of a queryWord - inner map of the documents it appears in with the tf in each doc.
    private HashMap<String, Map<String, Integer>> titleDocs; //  hold for each "original" queryWord - inner map of the documents it appears in with the tf in each doc.
    private HashMap<String, Map<String, Integer>> descDocs; //  hold for each "original" queryWord - inner map of the documents it appears in with the tf in each doc.
    private HashMap <String, Double> rankedQueryDocs;//  holds the rank for each doc that received as part of the potential docs.
    private Map<String, Document > docsData; //  holds the whole information for each document in the entire corpus (should be read from the disk - docsInfo).
    private double avgDocSize;
    private int totalDocsNum;
    private boolean semanticPref;


    public Ranker(String path, boolean sementicBool , boolean useStem) {
        postingPath = path;
        this.useStem = useStem;
        unserializeDocs();
        avgDocSize = calculateAvgSize();
        rankedQueryDocs =new HashMap<>();
        totalDocsNum = docsData.size();
        semanticPref = sementicBool;

    }

    /**
     * gets the data for a given query and ranks all the his potential docs.
     * @param queryNumber - query indentifier
     * @param titleTerms - parsed terms set of query title.
     * @param descTerms -  parsed terms set of query description.
     * @param potentialDocsForQuery - list of all potential docs.
     * @return
     */
    public ArrayList<String> rankDocuments(String queryNumber ,Set <String> titleTerms , Set<String> descTerms , Set<String> potentialDocsForQuery){
        rankedQueryDocs = new HashMap<>();
        titleDocs  = setWordsMap(Searcher.termsData, titleTerms);
        descDocs= setWordsMap(Searcher.termsData, descTerms);

        if (Searcher.semanticPref){
            Set<String> simQueryTerms = Searcher.simQueryTerms.get(queryNumber);
            simWordsQuery = setWordsMap(Searcher.simTermsData, simQueryTerms);
        }

        for (String docId : potentialDocsForQuery ){
            double BM25Score = BM25(docId);
            double docTitleRank = rankDocTitle(docId,titleTerms, descTerms);
            double rank = BM25Score + 2 * docTitleRank;
            rankedQueryDocs.put(docId,rank);
        }
        rankedQueryDocs = sortByValue(rankedQueryDocs);
        return convertToList(rankedQueryDocs);
    }

    /**
     * a formula which helps us to give a rank to a given doc.
     * @param docId
     * @return
     */
    public double BM25( String docId){
        Document document = docsData.get(docId);
        int docLength= document.getDocLength();
        double titleScore = 0, descScore = 0, finalScore , similarScore = 0;

        // Title Score
        for (String term:titleDocs.keySet()){
            double tf;
            if(titleDocs.get(term).get(docId)!=null){
                tf = titleDocs.get(term).get(docId);
            }
            else {
                tf = 0;
            }
            titleScore+=calculateBM25inputs(tf,titleDocs.get(term).size(), docLength,  docId);
        }

        // Description Score
        for (String term:descDocs.keySet()){
            double tf;
            if(descDocs.get(term).get(docId)!=null){
                tf = descDocs.get(term).get(docId);
            }
            else {
                tf = 0;
            }
            descScore+=calculateBM25inputs(tf,descDocs.get(term).size(), docLength,  docId);
        }
        if (semanticPref){
            for(String similarQueryTerm: simWordsQuery.keySet()){
                double tf;
                if(simWordsQuery.get(similarQueryTerm).get(docId)!=null){
                    tf = simWordsQuery.get(similarQueryTerm).get(docId);
                }
                else {
                    tf = 0;
                }
                similarScore += calculateBM25inputs(tf, simWordsQuery.get(similarQueryTerm).size() , docLength ,docId);
            }
        }

        finalScore =  2 * titleScore + 3 * descScore + similarScore * 1;
        return finalScore;
    }

    /**
     * general ranking algorithm based on a document header and given query terms.
     * increase document rank if at least on of his header terms are equal to some query term.
     * @param DocId
     * @param titleTerms
     * @param descTerms
     * @return
     */
    public double rankDocTitle(String DocId, Set<String> titleTerms,Set<String> descTerms ){
        int counter = 0;
        for (String titleTerm:titleTerms){
            String docTitle = docsData.get(DocId).getDocHeader();
            if(docTitle.toLowerCase().contains(titleTerm.toLowerCase())){
                counter++;
            }
        }
        for (String descTerm:descTerms){
            String docTitle = docsData.get(DocId).getDocHeader();
            if(docTitle.toLowerCase().contains(descTerm.toLowerCase())){
                counter++;
            }
        }
        return counter;
    }

    /**
     * returns map of maps which contains document and tf information for each term.
     * @param allTerms
     * @param queryTerms
     * @return
     */
    public HashMap<String, Map<String, Integer>> setWordsMap(Map<String, Map<String, Integer>> allTerms, Set<String> queryTerms ){
        HashMap<String, Map<String, Integer>> WordsToRunON = new HashMap<>();
        for (String term:queryTerms){
            if (allTerms.get(term)!=null){
                Map<String, Integer> docTermInfo= allTerms.get(term);
                WordsToRunON.put(term, docTermInfo);
            }
        }
        return WordsToRunON;
    }

    /**
     * helps to calculate the BM-25 formula.
     */
    public double calculateBM25inputs(double tf , double df , int docSize , String docId){
        double b = 0.05 , k = 0.7 ;
        double IDF = Math.log(totalDocsNum/df);
        double mone = tf * ( k + 1 );
        double dDivAvgdl = docSize/ avgDocSize ;
        double mechane = tf + k * ( (1-b) + b * dDivAvgdl );
        double moneDivMehane = mone/mechane;
        double valueToAdd = IDF * moneDivMehane ;
        return valueToAdd;
    }

    /**
     * read the map that contains all the information data we wrote in partA and puts it on main memory map(docsData)
     */
    private void unserializeDocs() {
        try{
            String fileName;
            boolean toStem = useStem;
            if(toStem) fileName = "/s_DocsData.txt";
            else fileName = "/DocsData.txt";
            File toRead = new File(postingPath + fileName);
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
            docsData = (Map<String, Document>) ois.readObject();
            ois.close();
            fis.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * calculate the average document size in corpus.
     * @return
     */
    public double calculateAvgSize(){
        int size = docsData.size();
        int sumLength = 0;
        for (String docID :docsData.keySet()){
            Document tempDocument = docsData.get(docID);
            sumLength += tempDocument.getUniqueTermsNum();
        }
        return  sumLength/size;
    }

    /**
     * sorts a given set of docs based on their rank.
     * @param hm
     * @return
     */
    public static HashMap<String, Double> sortByValue(HashMap<String, Double> hm) {
        List<Map.Entry<String, Double> > list = new LinkedList<Map.Entry<String, Double> >(hm.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });
        HashMap<String, Double> temp = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    /**
     * convert a set of document to an 50 size array list of document ids.
     * @param hm
     * @return
     */
    public static ArrayList<String> convertToList(HashMap<String,Double> hm) {
        ArrayList<String> ans = new ArrayList<>();
        int counter = 0;
        for ( String docId : hm.keySet() ){
            if(counter < 50 ){
                ans.add(docId);
            }
            else break;
            counter ++;
        }
        return ans;
    }
}
