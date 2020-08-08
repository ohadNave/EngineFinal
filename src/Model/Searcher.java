package Model;

import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Searcher {

    private Ranker ranker;
    private Parser parser;
    private SemanticFinder semanticFinder;
    public static boolean semanticPref;
    private boolean isEntity;
    private boolean stem;
    private boolean singleQuery;
    private static String postingPath;
    private String queryPath;
    private Map<String, Term> dictionary; //dictionary from posting path
    private ArrayList<Query> queries; // holds all queries
    private Map<String, Set<String>> parsedTitles; //holds each query and its own tokenized words set.
    private Map<String, Set<String>> parsedDescs; //holds each query and its own tokenized words set.
    public static Map<String, Set<String>> simQueryTerms;
    public static Map<String, Map<String, Integer>> termsData;  //  for each term (from all queries), holds the relevant data from our corpus - <docID,tf>
    public static Map<String, Map<String, Integer>> simTermsData;//  for each similar term (from all queries), holds the relevant data from our corpus - <docID,tf>
    private Map<String, Integer> termsToRead; // holds the terms to read and relevant line pointer
    private static Map<String, Map<String, Double>> docEntityMap;
    private Map<String, Set<String>> potentialDocs;        // holds the relevant docs for each query number.
    private Map<Integer, ArrayList<String>> relevantDocs; //holds final relevant docs for each query


    public Searcher(boolean useStem, boolean semanticalBool, boolean entityBool, Map<String, Term> loadedDictionary, boolean freeQuery, String queryFilePath, String pathPosting) {
        stem = useStem;
        ranker = new Ranker(pathPosting, semanticalBool, stem);
        parser = new Parser(stem);
        parsedTitles = new HashMap<>();
        parsedDescs = new HashMap<>();
        termsData = new HashMap<>();
        simTermsData = new HashMap<>();
        potentialDocs = new HashMap<>();
        relevantDocs = new TreeMap<>(new queryNumComparator());
        semanticPref = semanticalBool;
        termsToRead = new TreeMap<>(new termComparator());
        semanticFinder = new SemanticFinder(stem);
        isEntity = entityBool;
        dictionary = loadedDictionary;
        singleQuery = freeQuery;
        queryPath = queryFilePath;
        postingPath = pathPosting;
        queries = new ArrayList<>();
        docEntityMap = new HashMap<>();
    }

    /**
     * main function of the class - calls all the class functions in chronological order.
     * @param queryText - in case of free query
     */
    public void start(String queryText) {
        long start = System.currentTimeMillis();
        if (!singleQuery) {
            ReadQuery readQuery = new ReadQuery(queryPath);
            readQuery.start();
            queries = readQuery.getQueries();
        } else {
            String title = queryText;
            Query query = new Query(title);
            queries.add(query);
        }
        parseQueries();
        if (semanticPref) {
            System.out.println(" looking for similar query terms . . . ");
            simQueryTerms = semanticFinder.findSim(parsedTitles);
        }

        getRelevantData();
        findPotentialDocs();
        rankPotentialDocs();
        writeResults();
        if (isEntity) {
            unserializeDocsEntityMap();
        }
        long finish = System.currentTimeMillis();
        System.out.println("Total MILLIS for searching: " + TimeUnit.MILLISECONDS.toMillis(finish - start));
        System.out.println("Total SECONDS for searching: " + TimeUnit.MILLISECONDS.toSeconds(finish - start));
    }

    /**
     * gets all title & desc parsed terms and insert to terms to read(in case they are in dictionary)
     */
    public void parseQueries() {
        for (Query query : queries) {
            String titleToParse = query.getTitle();
            String descToParse = query.getDesc();
            descToParse=descToParse.replaceAll("Identify|identify|documents|discuss|find|Find|information|role|issues", "");
            Pair<String, String> numNtitle = new Pair<>(query.getNumber(), titleToParse);
            Pair<String, String> numNdesc = new Pair<>(query.getNumber(), descToParse);

            // PARSE TITLE TERMS
            parser.Parse(numNtitle, numNtitle.getValue());
            Map<String, Document> parsedTitleMap = parser.getDocsAfterParse();
            for (String queryNumber : parsedTitleMap.keySet()) {
                query.setTitleSet(parsedTitleMap.get(queryNumber).getDocTerms().keySet());
                Set<String> titleSet = query.getTitleSet();
                titleSet = tokenizeSet(titleSet); // for each term after parse that his length more than 1 word - split it and return new set
                titleSet.addAll(getEntitesList(titleToParse));
                parsedTitles.put(queryNumber, titleSet);
                createTermsToRead(titleSet);
            }
            parser.getDocsAfterParse().clear();

            // PARSE DESCRIPTION TERMS
            parser.Parse(numNtitle, numNdesc.getValue());
            Map<String, Document> parsedDescMap = parser.getDocsAfterParse();
            for (String queryNumber : parsedDescMap.keySet()) {
                query.setDescSet(parsedDescMap.get(queryNumber).getDocTerms().keySet());
                Set<String> descSet = query.getDescSet();
                descSet = tokenizeSet(descSet); // for each term after parse that his length more than 1 word - split it and return new set
                parsedDescs.put(queryNumber, descSet);
                createTermsToRead(descSet);
            }
            parser.getDocsAfterParse().clear();

        }
    }


    public Set<String> getEntitesList(String queryTitle) {
        Set<String> entities = new HashSet<>();
        String[] wordArray = queryTitle.split(" ");
        Set<String> result = new HashSet<>();
        int n = wordArray.length;
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                result.add(
                        IntStream.rangeClosed(i, j)
                                .mapToObj(v -> wordArray[v])
                                .collect(Collectors.joining(" ")));
            }
        }
        for (String potentialEntity : result) {
            if (dictionary.containsKey(potentialEntity.toUpperCase())) {
                if (dictionary.get(potentialEntity.toUpperCase()).isEntity())
                    entities.add(potentialEntity.toUpperCase());
            }
        }
        return entities;
    }

    /**
     * returns a splited set in case that term in the original set contains more than two words.
     * @param terms - original parsed query set
     * @return
     */
    public Set<String> tokenizeSet(Set<String> terms) {
        Set<String> splitedTerms = new HashSet<>();
        for (String term : terms) {
            StringTokenizer stringTokenizer = new StringTokenizer(term);
            if (stringTokenizer.countTokens() > 1) {
                splitedTerms.add(term);
                while (stringTokenizer.hasMoreTokens()) {
                    splitedTerms.add(stringTokenizer.nextToken());
                }
            } else {
                splitedTerms.add(term);
            }
        }
        return splitedTerms;
    }


    /**
     * gets a single word set of random query and adds his terms to a the full words map to read from disk - in order to read the word from disk only once.
     *
     * @param queryTerms
     */
    private void createTermsToRead(Set<String> queryTerms) {
        String allUpper;
        String allLower;

        for (String term : queryTerms) {
            allUpper = term.toUpperCase();
            allLower = term.toLowerCase();

            //FOR LOWERS
            if (dictionary.containsKey(allLower)) {
                int ptr = dictionary.get(allLower).getPointerToLine();
                termsToRead.put(allLower, ptr);
                termsData.put(allLower, null);
                if (semanticPref) {
                    simTermsData.put(allLower, null);
                }
            }
            //FOR UPPERS
            else if (dictionary.containsKey(allUpper)) {
                int ptr = dictionary.get(allUpper).getPointerToLine();
                termsToRead.put(allUpper, ptr);
                termsData.put(allUpper, null);
                if (semanticPref)
                    simTermsData.put(allUpper, null);
            }
        }
    }

    /**
     * iterates each query term inside a query in order to get his potential set of docs, updates his potential set of docs and updates the total potential docs map to run on.
     */
    private void findPotentialDocs() {
        for (Query query : queries) {
            Set<String> documents = new HashSet<>();

            Set<String> titleTerms = parsedTitles.get(query.getNumber());
            /// --- for all title terms --- ///
            for (String titleTerm : titleTerms) {
                if (termsData.get(titleTerm) != null) { // only if exist in corpus
                    documents.addAll(termsData.get(titleTerm).keySet());
                }
            }

            /// --- for all description terms --- ///
            Set<String> descTerms = parsedDescs.get(query.getNumber());
            for (String descTerm : descTerms) {
                if (termsData.get(descTerm) != null) { // only if exist in corpus
                    documents.addAll(termsData.get(descTerm).keySet());
                }
            }

            if (semanticPref) {
                Set<String> querySimTerms = simQueryTerms.get(query.getNumber());
                for (String querySimTerm : querySimTerms) {
                    if (simTermsData.containsKey(querySimTerm)) {
                        documents.addAll(simTermsData.get(querySimTerm).keySet());
                    }
                }
            }
            potentialDocs.put(query.getNumber(), documents);
        }
    }

    /***
     * sends each query, his parsed words and his set of potential docs to the ranker, gets the most 50 relevant set of docs and puts it in a map in order to write the results.
     */
    private void rankPotentialDocs() {
        for (Query query : queries) {
            String queryNumber = query.getNumber();
            ArrayList<String> docsSet = ranker.rankDocuments(queryNumber, parsedTitles.get(queryNumber), parsedDescs.get(queryNumber), potentialDocs.get(queryNumber));
            relevantDocs.put(Integer.parseInt(queryNumber), docsSet);
        }
    }

    /**
     * write results file to disk.
     */
    private void writeResults() {
        try {
            File resultsFile = new File(postingPath + "/results.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile));
            for (Integer queryNum : relevantDocs.keySet()) {
                ArrayList<String> docs = relevantDocs.get(queryNum);
                for (String docID : docs) {
                    writer.write(queryNum + " 0 " + docID + " 1 2.0 mt");
                    writer.newLine();
                    writer.flush();
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * in case that entity search is on - read entity file from disk.
     */
    private void unserializeDocsEntityMap() {
        try {
            String fileName;
            boolean toStem = stem;
            if (toStem) fileName = "/s_docEntityFile.txt";
            else fileName = "/docEntityFile.txt";
            File toRead = new File(postingPath + fileName);
            FileInputStream fis = new FileInputStream(toRead);
            ObjectInputStream ois = new ObjectInputStream(fis);
            docEntityMap = (Map<String, Map<String, Double>>) ois.readObject();
            ois.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * iterates the full words map and collects all relevant data from the posting files.
     */
    private void getRelevantData() {
        try {
            String fileName = "";
            if (stem) {
                fileName = postingPath + "/s_";
            } else {
                fileName = postingPath + "/";
            }
            char firstChar = ' ';
            char c;
            int ptr;
            BufferedReader reader = null;
            boolean firstTerm = true;
            int prevPtr = 0;
            for (String term : termsToRead.keySet()) {
                c = Character.toLowerCase(term.charAt(0));
                ptr = termsToRead.get(term);

                if (firstChar != c) {
                    firstTerm = true;
                    firstChar = c;
                    prevPtr = 0;
                }
                if (Character.isDigit(c)) {
                    if (firstTerm) {
                        File postingFile = new File(fileName + "Numbers.txt");
                        reader = new BufferedReader(new FileReader(postingFile));
                        firstTerm = false;
                    }
                    for (int i = 0; i < ptr; i++)
                        reader.readLine();
                    String line = reader.readLine();
                    splitTermData(line);

                } else if (Character.toLowerCase(term.charAt(0)) == c) {
                    if (firstTerm) {
                        File postingFile = new File(fileName + Character.toUpperCase(c) + ".txt");
                        reader = new BufferedReader(new FileReader(postingFile));
                        firstTerm = false;
                    }
                    for (int i = 0; i < ptr - prevPtr; i++)
                        reader.readLine();

                    String line = reader.readLine();
                    splitTermData(line);
                    prevPtr = ptr + 1;
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * split relevant data from each line in a posting file
     *
     * @param line
     */
    private void splitTermData(String line) {
        Map<String, Integer> tempDocsForTerm = new HashMap<>();
        String[] tempData = line.split("<> ");
        String[] tempDocData = tempData[1].split(",");
        for (int i = 0; i < tempDocData.length; i++) {
            tempDocsForTerm.put(tempDocData[i].split("=")[0], Integer.parseInt(tempDocData[i].split("=")[1]));
        }
        String term = tempData[0];
        if (termsData.containsKey(term)) {
            termsData.put(term, tempDocsForTerm);
        }
        if (semanticPref) {
            if (simTermsData.containsKey(term))
                simTermsData.put(term, tempDocsForTerm);
        }
    }

    /**
     * in order to get sorted results.
     */
    static class queryNumComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer t1, Integer t2) {
            return t1.compareTo(t2);
        }
    }

    /**
     * in order to read from disk in alphabetical order.
     */
    static class termComparator implements Comparator<String> {
        @Override
        public int compare(String t1, String t2) {
            return t1.compareTo(t2);
        }
    }


    public Map<Integer, ArrayList<String>> getRelevantDocs() {
        return relevantDocs;
    }

    public static Map<String, Map<String, Double>> getDocEntityMap() {
        return docEntityMap;
    }
}
