package Model;

import javafx.util.Pair;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class Indexer {
    //indexer 1:00 WORKING!

    private ReadFile reader;
    private Parser parser;
    private String postingPath;
    private Map<String, Document> docsAfterIndex;
    private static Map<String, Term> Dictionary;
    private Map<String, String> postInfo;
    private int chunksCounter;
    private int docCounter;
    private File[] postings;
    private boolean toStem;
    private int DicSize;
    private Map<String, Map<String, Double>> docEntityMap;

    public Indexer(String postingPath, String corpusPath, boolean stem)  {
        reader = new ReadFile(corpusPath);
        parser = new Parser(stem);
        this.postingPath = postingPath;
        toStem = parser.getToStem();
        Dictionary = new HashMap<>();
        docsAfterIndex = new HashMap<>();
        chunksCounter = 0;
        docEntityMap = new HashMap<>();

    }

    /**
     * Main indexer function - while the reader has still more files to read,the Indexer keep geting 10 files in each chunk that has been read,parsed,and labeld to documents.
     * Iterating each document terms map and updates the dictionary and the temporary posting info structure.
     * after each document map iteration - clears map from memory.
     * after each chunk of 10 files - clears postings info structure from memory.
     * after each chunk has ended - builds a posting file.
     * when there are no files left,the indexer start merging all the postings file into one large file,and then seperates it into 27 different postings files(A-Z) + numbers and signs.
     *
     * @throws Exception
     */
    public void createInvertedIndex() throws Exception {
        while (reader.hasMoreToRead()) {
            postInfo = new TreeMap<>(new termComparator());
            reader.seperateDocsInFile();
            Map<Pair<String,String>, String> docsBeforeParse = reader.getDocsBeforeParse();

            for (Pair<String,String> keyValue : docsBeforeParse.keySet()) {

                String textBeforeParse = docsBeforeParse.get(keyValue);
                parser.Parse(keyValue, textBeforeParse);
            }
            docsBeforeParse.clear();
            Map<String, Document> docsAfterParse = parser.getDocsAfterParse();
            for (String docNO : docsAfterParse.keySet()) {
                Document docBeforeIndex = docsAfterParse.get(docNO);
                Map<String, Integer> currDocTermsMap = docBeforeIndex.getDocTerms();
                for (String term : currDocTermsMap.keySet()) {
                    int termCurrCounter = currDocTermsMap.get(term);
                    updateDictionary(term, termCurrCounter);
                    updatePostInfo(term, docNO, termCurrCounter);
                }
                docBeforeIndex.clearDocTermsMap();
                docCounter++;
            }
            chunksCounter++;
            docsAfterIndex.putAll(parser.getDocsAfterParse());
            parser.clearDocsAfterParse();
            buildPostingFile();
            postInfo.clear();
        }
        validateEntities();
        mergePostingFiles();

        System.gc();
        System.out.println("ranking entities ");
        rankEntities();
        System.out.println("sorting entities ");
        sortByValue();
        System.out.println("writing entity file to disc");
        writeDocEntity();

        docEntityMap.clear();

        splitMergedAlphabetical();
        saveToDisplayDictionary();
        saveDocAfterIndexToFile();
        seralizeDocs();
        saveDictionary();
        DicSize = Dictionary.size();
    }


    private void updateDictionary(String term, int currTermCounter) {
        Term currTerm;
        String allUpper = term.toUpperCase();
        String allLower = term.toLowerCase();
        if (Dictionary.containsKey(allLower)) {
            currTerm = Dictionary.get(allLower);
            currTerm.incrementTotal(currTermCounter);
            currTerm.incrementNumOfDocsTermIn();
            Dictionary.replace(allLower, currTerm);
        } else {
            if (Dictionary.containsKey(allUpper)) {
                if (term.equals(allLower)) {
                    currTerm = Dictionary.get(allUpper);
                    currTerm.setValue(allLower);
                    currTerm.incrementTotal(currTermCounter);
                    currTerm.incrementNumOfDocsTermIn();
                    Dictionary.put(allLower, currTerm);
                    Dictionary.remove(allUpper);
                } else {
                    currTerm = Dictionary.get(allUpper);
                    currTerm.incrementTotal(currTermCounter);
                    currTerm.incrementNumOfDocsTermIn();
                    Dictionary.replace(allUpper, currTerm);
                }
            } else {
                currTerm = new Term(currTermCounter);
                currTerm.setValue(term);
                Dictionary.put(term, currTerm);
            }
        }
    }

    /**
     * updates the posting info for this specific term ( term~docNO1=17,docNO2=9, ...)
     * @param term
     * @param docNO
     * @param currTermCounter
     */
    public void updatePostInfo(String term, String docNO, int currTermCounter){
        String termPostInfo;
        if (!postInfo.isEmpty() && postInfo.containsKey(term)) {
            termPostInfo = postInfo.get(term) + docNO.trim() + "=" + currTermCounter + ",";
            postInfo.replace(term, termPostInfo);
        } else {
            termPostInfo = term + "<> " + docNO.trim() + "=" + currTermCounter + ",";
            postInfo.put(term, termPostInfo);
        }
    }

    /**
     * after indexing process has been ended - all entities must have to be authenticated.
     * if the the inner map entity size is larger than one,the term must be a valid entity.
     */
    private void validateEntities() {
        Map<String, Map<String, Double>> tempEntityMap = parser.getTempEntityMap();
        for (String entity : tempEntityMap.keySet()) {
            Map<String, Double> innerDocsTFsMap = tempEntityMap.get(entity);
            if (innerDocsTFsMap.size() > 1) {
                Term entityTerm = Dictionary.get(entity);
                entityTerm.setEntity(true);
            } else {

                Dictionary.remove(entity);
            }
        }
        tempEntityMap.clear();
    }

    //********************** Entities Treatment For Part 2 of the Engine ***********************
    // Missing - we will need to show the rank for each entity - we can either write it in the file itself but its tricky with the sort,
    // or we can find it in the searcher (I don't think this is the right way).

    private void rankEntities() {
        Map<String, Double> innerMap;
        for (String DocId : docEntityMap.keySet()) {
            innerMap = docEntityMap.get(DocId);
            for (String entity : innerMap.keySet()) {
                double value = rankDominantEntity(entity, innerMap.get(entity), DocId);
                innerMap.replace(entity, value);
            }
            docEntityMap.replace(DocId, innerMap);
        }
    }

    private double rankDominantEntity(String entity, double tf, String docId) {
        double value ;
        double df = Dictionary.get(entity).getDf();
        int maxTerm = docsAfterIndex.get(docId).getMostFrequentTermNum();
        value = (tf/maxTerm) * Math.log(docsAfterIndex.size()/df) ;
        //value = value / docsAfterIndex.get(docId).getDocLength();
        DecimalFormat decimalFormat = new DecimalFormat("#.###");
        value = Double.parseDouble(decimalFormat.format(value));
        value = Math.min( 50 , value );
        value = Math.max( value , 0.001);
        return value;
    }

    private void writeDocEntity() {
        try {
            String docEntityFileName;
            if (toStem) docEntityFileName = "/s_docEntityFile.txt";
            else docEntityFileName = "/docEntityFile.txt";
            File docEntityFile = new File(postingPath + docEntityFileName);
            FileOutputStream fos = new FileOutputStream(docEntityFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(docEntityMap);
            oos.flush();
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sortByValue() {
        for (String docID : docsAfterIndex.keySet()){
            if(docEntityMap.containsKey(docID)){
                Map<String,Double> innerMap = docEntityMap.get(docID);

                List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(innerMap.entrySet());
                Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                    public int compare(Map.Entry<String, Double> o1,
                                       Map.Entry<String, Double> o2) {
                        return (o2.getValue()).compareTo(o1.getValue());
                    }
                });
                HashMap<String, Double> temp = new LinkedHashMap<String, Double>();
                for (int i = 0; i < 5; i++) {
                    if(i<list.size()){
                        Map.Entry<String, Double> curr = list.get(i);
                        temp.put(curr.getKey(), curr.getValue());
                    }
                }
                docEntityMap.put(docID,temp);
            }
        }
    }

    /**
     * after each chunk iteration, this method responsible to build a temporary posting file.
     */
    private void buildPostingFile() {
        String postFileName = "";
        if (parser.getToStem()) {
            postFileName = postingPath + "/" + "SPF" + chunksCounter;
            postFileName += ".txt";
        } else {
            postFileName = postingPath + "/" + "PF" + chunksCounter;
            postFileName += ".txt";
        }
        try (FileWriter writer = new FileWriter(postFileName);
             BufferedWriter BW = new BufferedWriter(writer)) {
            String termInfo;
            for (String term : postInfo.keySet()) {
                termInfo = postInfo.get(term);
                BW.write(termInfo);
                BW.newLine();
            }
            BW.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(chunksCounter + " Files has been posted");
    }

    /**
     * after all posting files has been written to disk,this function responsible to merge all of them into one file.
     *
     * @throws Exception
     */
    public void mergePostingFiles() throws Exception {
        File postingsFile = new File(postingPath);
        String startOfString;
        if (parser.getToStem()) startOfString = "SPF";
        else startOfString = "PF";
        String finalStartOfString = startOfString;
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(finalStartOfString);
            }
        };
        postings = postingsFile.listFiles(filenameFilter);
        int theNumForPostingFile = postings.length;
        while (postings.length > 1) {
            FileReader fr1 = new FileReader(postings[0].getAbsoluteFile());
            FileReader fr2 = new FileReader(postings[1].getAbsoluteFile());
            String postingFileName = postingPath + "/" + startOfString + (theNumForPostingFile + 1) + ".txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(postingFileName));
            mergeTwoFiles(fr1, fr2, writer);
            theNumForPostingFile++;
            postings = postingsFile.listFiles(filenameFilter);
            Arrays.sort(postings, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                }
            });
        }

    }

    public void mergeTwoFiles(FileReader fr1, FileReader fr2, BufferedWriter writer) throws IOException { // מניחים שיש יותר מתיקייה אחת
        BufferedReader br1 = new BufferedReader(fr1);
        BufferedReader br2 = new BufferedReader(fr2);
        String firstTextLine = br1.readLine();
        String secondTextLine = br2.readLine();
        String firstTerm = firstTextLine.split("<>")[0];
        String secondTerm = secondTextLine.split("<>")[0];
        String mixedData;
        int lineCounter = 0;
        while (firstTextLine != null && secondTextLine != null && !firstTextLine.isEmpty() && !secondTextLine.isEmpty()) {
            int result = firstTerm.compareToIgnoreCase(secondTerm);
            if (result == 0) { // terms are equal - not case sensitive
                if (firstTerm.equals(firstTerm.toUpperCase()) && !(secondTerm.equals(secondTerm.toUpperCase()))) { //  first in upper & sec in lower
                    mixedData = secondTextLine + firstTextLine.split("<> ")[1];
                } else{
                    mixedData = firstTextLine + secondTextLine.split("<> ")[1];
                }
                if (postings.length == 2) {
                    String finalTerm = mixedData.split("<> ")[0];
                    if (validateTerm(finalTerm)) {
                        writer.write(mixedData);
                        writer.newLine();
                        if(finalTerm.equals(finalTerm.toUpperCase())){
                            updateDocEntityMap(finalTerm,mixedData);
                        }
                        lineCounter++;
                    }
                } else {
                    writer.write(mixedData);
                    writer.newLine();
                }
                firstTextLine = br1.readLine();
                secondTextLine = br2.readLine();
                if (firstTextLine != null)
                    firstTerm = firstTextLine.split("<> ")[0];
                if (secondTextLine != null)
                    secondTerm = secondTextLine.split("<> ")[0];
            } else if (result < 0) { // first is smaller
                if (postings.length == 2) {
                    if (validateTerm(firstTerm)) {
                        writer.write(firstTextLine);
                        writer.newLine();
                        if(firstTerm.equals(firstTerm.toUpperCase())){
                            updateDocEntityMap(firstTerm,firstTextLine);
                        }
                        lineCounter++;
                    }
                } else {
                    writer.write(firstTextLine);
                    writer.newLine();
                }
                firstTextLine = br1.readLine();
                if (firstTextLine != null)
                    firstTerm = firstTextLine.split("<> ")[0];
            } else { // second is smaller
                if (postings.length == 2) {
                    if (validateTerm(secondTerm)) {
                        writer.write(secondTextLine);
                        updateDocEntityMap(secondTerm,secondTextLine);
                        writer.newLine();
                        if(secondTerm.equals(secondTerm.toUpperCase())){
                            updateDocEntityMap(secondTerm,secondTextLine);
                        }
                        lineCounter++;
                    }
                } else {
                    writer.write(secondTextLine);
                    writer.newLine();
                }
                secondTextLine = br2.readLine();
                if (secondTextLine != null)
                    secondTerm = secondTextLine.split("<> ")[0];
            }
        }
        //if left only first file terms - run repeatedly
        while (firstTextLine != null && !firstTextLine.isEmpty()) {

            if (postings.length == 2) {
                if (validateTerm(firstTextLine.split("<> ")[0])) {
                    writer.write(firstTextLine);

                    writer.newLine();
                    if(firstTextLine.split("<> ")[0].equals(firstTextLine.split("<> ")[0].toUpperCase())){
                        updateDocEntityMap(firstTextLine.split("<> ")[0],firstTextLine);
                    }
                    lineCounter++;
                }
            } else {
                writer.write(firstTextLine);
                writer.newLine();
            }
            firstTextLine = br1.readLine();
        }

        while (secondTextLine != null && !secondTextLine.isEmpty()) {
            if (postings.length == 2) {
                if (validateTerm(secondTextLine.split("<> ")[0])) {
                    writer.write(secondTextLine);
                    writer.newLine();
                    if(secondTerm.equals(secondTextLine.split("<> ")[0].toUpperCase())){
                        updateDocEntityMap(secondTextLine.split("<> ")[0],secondTextLine);
                    }
                    lineCounter++;
                }
            } else {
                writer.write(secondTextLine);
                writer.newLine();
            }
            secondTextLine = br2.readLine();
        }
        writer.flush();
        br1.close();
        br2.close();
        writer.close();
        postings[0].getAbsoluteFile().delete();
        postings[1].getAbsoluteFile().delete();
    }

    public void updateDocEntityMap(String potentialEntity , String line){
        if(Dictionary.containsKey(potentialEntity)){
            if (Dictionary.get(potentialEntity).isEntity()){
                splitEntityData(line);
            }
        }
    }

    private boolean hasToBeLower(String term){
        if(Dictionary.containsKey(term.toLowerCase()) && Character.isUpperCase(term.charAt(0))){
            return true;
        }
        return false;
    }

    private void splitEntityData(String line) {
        String docId ;
        String[] tempData = line.split(",");
        Map<String, Double> innerEntityMap = new HashMap<>();
        String entity = tempData[0].split("<>")[0];
        for (int i = 1; i < tempData.length; i++) { //for each one of docs in a specific entity entry.

            docId = tempData[i].split("=")[0];

            double tf = Double.parseDouble(tempData[i].split("=")[1]);

            innerEntityMap.put(entity,tf);

            if(docEntityMap.containsKey(docId)){
                docEntityMap.get(docId).put(entity,tf);
            }
            else{
                docEntityMap.put(docId, innerEntityMap);
            }
        }
    }

    /**
     * This function responsible to update the relevant pointer line to each term in his relevant posting file.
     */
    private boolean setPointer(String term, int line) {
        if (Dictionary.containsKey(term)) {
            Term termObj = Dictionary.get(term);
            termObj.setPointerToLine(line);
            return true;
        }
        return false;
    }

    /**
     * After merging all the temporary posting files into one large file,this function splits it into 29 different files for future use(A-Z/numbers/signs).
     *
     * @throws IOException
     */
    private void splitMergedAlphabetical() throws IOException {
        boolean toStem = parser.getToStem();
        String prefixName;
        if (toStem) prefixName = "SPF";
        else prefixName = "PF";
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefixName);
            }
        };

        char[] chars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '~'};
        int charsCounter = 0;
        int lineNumber = 0;
        File mergedFile = new File(postingPath).listFiles(filenameFilter)[0]; // the merged file
        BufferedReader mergedReader = new BufferedReader(new FileReader(mergedFile));
        BufferedWriter numWriter;
        BufferedWriter signsWriter;
        BufferedWriter letterWriter;
        String fileName = "";
        for (int i = 0; i < 29; i++) {
            String line = "";
            if (i == 0) {
                if (toStem) fileName = "//stem_Signs.txt";
                else fileName = "//Signs.txt";
                File signsFile = new File(postingPath + fileName);
                signsWriter = new BufferedWriter(new FileWriter(signsFile));
                line = mergedReader.readLine();
                while (line != null && !Character.isDigit(line.charAt(0))) {
                    String term = line.split("<>")[0];
                    if (setPointer(term, lineNumber)) {
                        signsWriter.write(line);
                        signsWriter.newLine();
                        lineNumber++;
                    }
                    line = mergedReader.readLine();
                }
                signsWriter.flush();
                signsWriter.close();
            } else {
                if (i == 1) {
                    lineNumber = 0;
                    if (toStem) fileName = "//stem_Numbers.txt";
                    else fileName = "//Numbers.txt";
                    File numbersFile = new File(postingPath + fileName);
                    numWriter = new BufferedWriter(new FileWriter(numbersFile));
                    line = mergedReader.readLine();
                    while (line != null && Character.isDigit(line.charAt(0))) {
                        String term = line.split("<>")[0];
                        if (setPointer(term, lineNumber)) {
                            numWriter.write(line);
                            numWriter.newLine();
                            lineNumber++;
                        }
                        line = mergedReader.readLine();
                    }
                    numWriter.flush();
                    numWriter.close();
                } else {
                    char name = chars[charsCounter];
                    if (toStem) fileName = "//" + "s_" + Character.toUpperCase(name) + ".txt";
                    else fileName = "//" + Character.toUpperCase(name) + ".txt";
                    lineNumber = 0;
                    if (name == '~') break;
                    char nextChar = chars[charsCounter + 1];
                    char uppenextChar = Character.toUpperCase(nextChar);
                    File letterFile = new File(postingPath + fileName);
                    letterWriter = new BufferedWriter(new FileWriter(letterFile));
                    line = mergedReader.readLine();
                    while (line != null && line.charAt(0) != nextChar && line.charAt(0) != uppenextChar) {
                        String term = line.split("<>")[0];


                        if(hasToBeLower(term) ){
                            term = term.toLowerCase();
                            line = correctLine(line);
                        }

                        if (setPointer(term, lineNumber)) {
                            letterWriter.write(line);
                            letterWriter.newLine();
                            lineNumber++;
                        }
                        line = mergedReader.readLine();
                    }
                    charsCounter++;
                    letterWriter.flush();
                    letterWriter.close();
                }
            }
        }
        mergedReader.close();
        mergedFile.delete();
    }

    private String correctLine(String line){
        String newLine = "";
        String termToBeAdded = line.split("<> ")[0].toLowerCase();
        String descToBeAdded = line.split("<> ")[1];
        newLine = termToBeAdded + "<> " + descToBeAdded;
        return newLine;
    }

    /**
     * Helps to keep the posting data up-to-date - validate with Dictionary if term are still relevant or redundant,if redundant - do not write their data to the disk.
     *
     * @param checkTerm
     * @return
     */
    private boolean validateTerm(String checkTerm) {
        if (Dictionary.containsKey(checkTerm.toLowerCase()) || Dictionary.containsKey(checkTerm.toUpperCase())) {
            return true;
        }
        return false;
    }

    /**
     * saves the dictionary into the disk.
     *
     * @throws IOException
     */
    private void saveDictionary() throws IOException {
        String dictName;
        if (toStem) dictName = "/s_Dictionary.txt";
        else dictName = "/Dictionary.txt";
        File dictionaryFile = new File(postingPath + dictName);
        FileOutputStream fos = new FileOutputStream(dictionaryFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(Dictionary);
        oos.flush();
        oos.close();
        fos.close();
    }


    private void saveToDisplayDictionary() throws IOException {
        String dispDictName;
        if (toStem) dispDictName = "/s_DisplayDictionary.txt";
        else dispDictName = "/DisplayDictionary.txt";
        String dictionaryFileName = postingPath + dispDictName;
        PrintWriter pr = new PrintWriter(new FileWriter(dictionaryFileName));
        String finalTerm;
        Object[] keys = Dictionary.keySet().toArray();
        Arrays.sort(keys);
        for (Object term : keys) {
            Term currTerm = Dictionary.get(term);
            int termCounter = currTerm.getTotalCounter();
            finalTerm = term + " <>  tf  : " + termCounter;
            pr.println(finalTerm);
        }
        pr.flush();
        pr.close();
    }

    /**
     * deletes all data in the posting path.
     */
    public static void deletePostingFilesAndDictionary(String path) {
        File allFilesInDir = new File(path);
        File[] filesToDelete = allFilesInDir.listFiles();
        if (filesToDelete != null) {
            for (File file : filesToDelete)
                file.delete();
        }
        if (Dictionary != null) {
            Dictionary.clear();
        }
    }


    /**
     * Responsible for writting all the Documents data into the disk.
     *
     * @throws IOException
     */
    public void saveDocAfterIndexToFile() throws IOException {
        String docsDataName;
        if (toStem) docsDataName = "/s_DocInfo.txt";
        else docsDataName = "/DocInfo.txt";
        String dictionaryFileName = postingPath + docsDataName;
        PrintWriter pr = new PrintWriter(new FileWriter(dictionaryFileName));
        for (String docNum : docsAfterIndex.keySet()) {
            StringJoiner sj = new StringJoiner(",");
            sj.add(docNum);
            Document doc = docsAfterIndex.get(docNum);
            sj.add("doc Header is: " + doc.getDocHeader());
            sj.add(" MFT-value: " + doc.getMostFrequentTerm());
            sj.add(" MFT-num: " + doc.getMostFrequentTermNum());//most frequent term apearences
            sj.add(" uniqeTermNum: " + doc.getUniqueTermsNum());// uniqe terms num
            sj.add(" docLen: " + doc.getDocLength()); //docLength
            pr.println(sj);
        }
        pr.flush();
        pr.close();
    }

    public void seralizeDocs() {
        try {
            String docsDataName;
            if (toStem) docsDataName = "/s_DocsData.txt";
            else docsDataName = "/DocsData.txt";
            File sourceFile = new File(postingPath + docsDataName);
            FileOutputStream fos = new FileOutputStream(sourceFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(docsAfterIndex);
            oos.flush();
            oos.close();
            fos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the dictionary from the disk.
     *
     * @return the Dictionary (map).
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Map<String, Term> loadDictionary(String path, boolean stem) throws Exception {
        Map<String, Term> dict;
        String dictName;
        boolean toStem = stem;
        if (toStem) {
            System.out.println("Loading stemmed dictionary");
            dictName = "/s_Dictionary.txt";
        }
        else {
            System.out.println("Loading regular dictionary");
            dictName = "/Dictionary.txt";
        }
        File toRead = new File(path + dictName);
        FileInputStream fis = new FileInputStream(toRead);
        ObjectInputStream ois = new ObjectInputStream(fis);
        dict = (Map<String, Term>) ois.readObject();
        if (dict == null) {
            throw new Exception("Dictionary didn't load");
        }
        ois.close();
        fis.close();
        return dict;
    }

    public Map<String, Term> getDictionary() {
        return Dictionary;
    }

    public int getDocCounter() {
        return docCounter;
    }

    public int getDicSize() {
        return DicSize;
    }

    public Map<String, String> getPostInfo() {
        return postInfo;
    }

    static class termComparator implements Comparator<String> {
        @Override
        public int compare(String t1, String t2) {
            return t1.toLowerCase().compareTo(t2.toLowerCase());
        }
    }
}