package Model;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import Model.Document;
import View.Controller;
import javafx.util.Pair;
import org.tartarus.martin.Stemmer;

public class Parser {

    private ArrayList<String> Terms;
    protected static Map<String, Map<String, Double>> tempEntityMap;
    public static Map<String, Document> docsAfterParse;
    private static Set<String> stopWords;
    private int docCounter;
    private Boolean toStem;
    private Stemmer stemmer;
    private Document currentDoc;
    private String currentWord;
    private String secondWord;
    private String thirdWord;
    private String forthWord;
    private NumberFormat numFormatter;
    private static DecimalFormat formatter;


    //auxilary tables
    private static Map<String, String> Prices;
    private static Map<String, String> Percentage;
    private static Map<String, String> illions;
    private static Hashtable<String, String> Months;
    private static Set<Character> unecessaryChars;
    //regex expressions
    private static final String yearRegex = "^[12][0-9]{3}$";
    private static final String fractionRegex = "\\d+(/\\d+)";
    private static final String thousandRegex = "[T|t]housand";
    private static final String dollarsRegex = "[D|d]ollars";
    private static final String betweenRegex = "[B|b]etween";
    private static final String millionRegex = "[M|m]illion";
    private static final String billionRegex = "[B|b]illion";
    private static final String bnRegex = "\\d+(\\,\\d+)?(\\.\\d+)?bn";
    private static final String mRegex = "\\d+(\\,\\d+)?(\\.\\d+)?m";
    private static final String kgRegex = "[K|k]ilogram(s)?|[K|k]g(s)?";
    private static final String gramsRegex = "[G|g]ram(s)";

    /**
     * Constructor
     *
     * @param isStemm - in order to tell parser wheter stemm words or not.
     */
    public Parser(Boolean isStemm) {
        docCounter = 0;
        toStem = false;
        toStem = isStemm;
        docsAfterParse = new HashMap<>();
        tempEntityMap = new HashMap<>();
        formatter = new DecimalFormat("#,###,###");
        numFormatter = NumberFormat.getNumberInstance(Locale.US);
        loadStopWords();
        createUnnecessaryChars();
        createMonthsTable();
        createPriceTable();
        createIllionsTable();
        createPercantageTable();
        if (toStem)
            stemmer = new Stemmer();
    }

    /**
     * The main parser function. parse all terms in corpus by demand of specific laws.
     * try to eliminate non-semanthical words.
     *
     * @param docText
     */
        public void Parse(Pair<String,String> keyValue, String docText) {
        Document doc = new Document(keyValue.getKey(), keyValue.getValue());
        currentDoc = doc;
        docText = docText.replaceAll(",|\\*|\\(|\\)|'s|'|\"|:|;|`|\\{|}|\\?|\\[|]|\\\\|#|--+|\\+|&|\\.\\.\\.|\\.\\.|\\||=|>|<|//|!|_+|@|\\^|~|�|°", "");
        Terms = new ArrayList<>(Arrays.asList((docText.split("\\n|\\s+|\\t"))));
        String temp = "";
        currentDoc.setDocLength(Terms.size());
        for (int i = 0; i < Terms.size(); i++) {
            currentWord = Terms.get(i);

            if (stopWords.contains(currentWord.toLowerCase()) || currentWord.isEmpty() || currentWord.equals("TYPEBFN")) {
                continue;
            }
            if (currentWord.length() == 1 && unecessaryChars.contains(currentWord.charAt(0))) continue;
            if (mustToClean(currentWord)) currentWord = cleanWord(currentWord);
            updateCurrents(i);

            if (isNumber(currentWord)) {
                double currentNumber = convertStringToNumber(currentWord);
                //BELOW MILLION
                if (currentNumber < 1000000) {
                    if (!Prices.containsKey(secondWord) && !illions.containsKey(secondWord)) {
                        if (secondWord.matches(fractionRegex)) {
                            i += checkFractionDollars();
                            continue;
                        }
                        if (numericMonthsPattern()) {
                            i++;
                            continue;
                        }
                        if (Percentage.containsKey(secondWord)) {
                            temp = currentWord + "%";
                            updateTerms(temp);
                            i++;
                            continue;
                        }
                        if (currentWord.matches(yearRegex)) {
                            temp = currentWord;
                            updateTerms(temp);
                            continue;
                        }
                        if (checkKg(currentNumber)) {
                            i++;
                            continue;
                        }

                        //if larger than 1000 or next word is thousand -> number/1000 K ***************
                        if (currentNumber > 999) {
                            if (secondWord.matches(thousandRegex)) {
                                i++;
                            }
                            double tempNum = currentNumber / 1000;
                            temp = tempNum + "K";
                            updateTerms(temp);
                            continue;
                        }

                    } else {
                        int j = checkPriceDollarsPattern(currentNumber);
                        if (j != 0) {
                            i += j;
                            continue;
                        } else if (illions.containsKey(secondWord)) {
                            if (secondWord.matches(millionRegex)) {
                                temp = currentWord + "M";
                                updateTerms(temp);
                                i++;
                                continue;
                            }
                            if (secondWord.matches(billionRegex)) {
                                temp = currentNumber + "B";
                                updateTerms(temp);
                                i++;
                                continue;
                            }
                        }
                    }
                    updateTerms(currentWord);
                    continue;
                }

                // currentNumber is larger/equal from 1,000,000
                else {

                    //Price dollars/Dollars -> Price M Dollars
                    if (Prices.containsKey(secondWord)) {
                        double tempNum = (currentNumber / 1000000);
                        temp = formatter.format(tempNum) + " M Dollars";
                        updateTerms(temp);
                        i++;
                        continue;
                    }

                    //currentNumber is between million-billion
                    if (currentNumber < 1000000000) {
                        currentNumber = (currentNumber) / 1000000;
                        temp = currentNumber + "M";
                        updateTerms(temp);
                        continue;

                        //larger/equal from billion
                    } else {
                        currentNumber = currentNumber / 1000000000;
                        temp = currentNumber + "B";
                        updateTerms(temp);
                        continue;
                    }
                }


            } else { // if the currentWord isnt a number
                if (currentWord.length() > 0) {
                    if (checkDollarNumberPattern(currentWord)) {
                        i++;
                        continue;
                    }
                    //IF Pricebn Dollars or Pricem Dollars -> price M Dollars
                    else {
                        Double tempNum;
                        if (currentWord.matches(bnRegex) && secondWord.matches(dollarsRegex)) {
                            tempNum = seperateNumberFromString(currentWord) * 1000;
                            temp = formatter.format(tempNum);
                            temp += " M Dollars";
                            i++;
                            updateTerms(temp);
                            continue;
                        } else {
                            if (currentWord.matches(mRegex) && secondWord.matches(dollarsRegex)) {
                                temp = seperateNumberFromString(currentWord) + " M Dollars";
                                updateTerms(temp);
                                i++;
                                continue;
                            }
                        }
                    }

                    //For dates who starts with Month ( May 2020)
                    if (Months.containsKey(currentWord)) {
                        //if (secondWord.matches(numberRegex)) {
                        if (isNumber(secondWord)) {
                            if (secondWord.matches(yearRegex)) {
                                temp = secondWord + "-" + Months.get(currentWord);
                            } else {
                                if (secondWord.length() > 1)
                                    temp = Months.get(currentWord) + "-" + secondWord;
                                else {
                                    temp = Months.get(currentWord) + "-0" + secondWord;
                                }
                            }
                        }
                        updateTerms(temp);
                        i++;
                        continue;
                    }
                    if (currentWord.matches(betweenRegex) && isNumber(secondWord) && thirdWord.equals("and") && isNumber(forthWord)) {
                        temp = currentWord + " " + secondWord + " " + thirdWord + " " + forthWord;
                        updateTerms(temp);
                        i = i + 3;
                        continue;
                    }

                    if (currentWord.contains("/")) {
                        splitBySlash(currentWord);
                        continue;
                    }


                    if (Character.isUpperCase(currentWord.charAt(0))) {
                        int m = isInTempEntityMap();
                        if (m > 0) {
                            i += m;
                            continue;
                        }

                        int j = isEntity();
                        if (j > 0) {
                            i += j;
                            continue;
                        }
                        updateDocCapitals(currentWord);
                        continue;

                    }
                }
                if (currentWord.length() == 0 || currentWord.length() == 1)
                    continue;

            }
            if ( (!currentWord.equals (currentWord.toUpperCase()) && currentWord.contains("-"))){
                dealWithSeparator();
            }
            temp = currentWord;
            updateTerms(temp);
        }
        currentDoc.setUniqueTermsNum();
        currentDoc.setDocHeader(keyValue.getValue());
        currentDoc.updateMostFreq();
        docsAfterParse.put(keyValue.getKey(), doc);
        docCounter++;
    }

    public void dealWithSeparator(){
        String [] tokens = currentWord.split("-");
        for (String token : tokens){
            if( (!token.equals(token.toUpperCase())) && !stopWords.contains(token.toLowerCase()))
               updateTerms(token);
        }
    }

    public int isInTempEntityMap() {
        String dou = "";
        String trio = "";
        String quad = "";
        if (secondWord != "" && Character.isUpperCase(secondWord.charAt(0))) {
            dou = currentWord + " " + secondWord;
        } else if (thirdWord != "" && Character.isUpperCase(thirdWord.charAt(0))) {
            trio = currentWord + " " + secondWord + " " + thirdWord;
        } else if (forthWord != "" && Character.isUpperCase(forthWord.charAt(0))) {
            quad = currentWord + " " + secondWord + " " + thirdWord + " " + forthWord;
        }
        if (tempEntityMap.containsKey(quad)) {
            updateTempEntityMap(quad);
            return 3;
        } else if (tempEntityMap.containsKey(trio)) {
            updateTempEntityMap(trio);
            return 2;
        } else if (tempEntityMap.containsKey(dou)) {
            updateTempEntityMap(dou);
            return 1;
        }
        return -1;
    }

    /**
     * send to a document term map a given term in order to update it.
     *
     * @param temp - current word to
     */
    private void updateTerms(String temp) {
        if (!temp.isEmpty() && !stopWords.contains(temp)) {
            if (!toStem) {
                currentDoc.addWordToDic(temp);
            } else {
                if (temp.equals(temp.toUpperCase())) {
                    temp = temp.toLowerCase();
                    for (char c : temp.toCharArray()) {
                        stemmer.add(c);
                    }
                    stemmer.stem();
                    temp = stemmer.toString().toUpperCase();
                    currentDoc.addWordToDic(temp);
                } else {
                    for (char c : temp.toCharArray()) {
                        stemmer.add(c);
                    }
                    stemmer.stem();
                    temp = stemmer.toString();
                    currentDoc.addWordToDic(temp);
                }
            }
        }
    }

    /**
     * Chosen rull - seperates a which contains a / in it into two words.
     *
     * @param currentWord
     */
    private void splitBySlash(String currentWord) {
        String[] wordsAfterSep = currentWord.split("/");
        for (String str : wordsAfterSep) {
            if (!stopWords.contains(str.toLowerCase())) {
                currentDoc.addWordToDic(str);
            }
        }
    }

    private boolean numericMonthsPattern() {
        String temp;
        if (Months.containsKey(secondWord.intern())) {
            if (currentWord.length() > 1)
                temp = Months.get(secondWord) + "-" + currentWord;
            else
                temp = Months.get(secondWord) + "-0" + currentWord;
            updateTerms(temp);
            return true;
        }
        return false;
    }

    private double convertStringToNumber(String currentWord) {
        double currentNumber;
        if (currentWord.contains(","))
            currentNumber = Double.parseDouble(currentWord.replaceAll(",", "")); // for larger than 1000 numbers - deletes comma
        else
            currentNumber = Double.parseDouble(currentWord); // for larger than 1000 numbers - deletes dot
        return currentNumber;
    }

    private int checkFractionDollars() {
        int increment;
        String temp;
        if (thirdWord.matches(dollarsRegex)) {
            temp = currentWord + " " + secondWord + " Dollars";
            increment = 2;
        } else {
            temp = currentWord + " " + secondWord;
            increment = 1;
        }
        updateTerms(temp);
        return increment;
    }

    private boolean checkKg(Double currentNumber) {
        String temp = "";
        if (secondWord.matches(kgRegex)) {
            temp = formatter.format(currentNumber) + " Kg";
            updateTerms(temp);
            return true;
        } else {
            if (secondWord.matches(gramsRegex)) {
                temp = currentNumber / 1000 + " Kg";
                updateTerms(temp);
                return true;
            }
        }
        return false;
    }

    private int checkPriceDollarsPattern(double currentNumber) {
        int increment = 0;
        String temp = "";
        if (secondWord.matches(dollarsRegex)) {
            temp = currentWord + " Dollars";
            increment = 1;
            updateTerms(temp);
            return increment;
        }
        if (thirdWord.matches(dollarsRegex)) {
            if (secondWord.intern() == "m") {
                temp = currentWord + " M Dollars";
                increment = 2;
            } else if (secondWord.intern() == "bn") {
                currentNumber = currentNumber * 1000;
                temp = currentNumber + " M Dollars";
                increment = 2;
            }
            updateTerms(temp);
            return increment;
        }
        if (thirdWord.intern() == "U.S" && forthWord.matches(dollarsRegex)) {
            if (secondWord.matches(millionRegex)) {
                temp = currentWord + " M Dollars";

            } else {
                currentNumber = currentNumber * 1000;
                temp = formatter.format(currentNumber) + " M Dollars";
            }
            increment = 3;
            updateTerms(temp);
            return increment;
        }
        return 0;
    }

    private boolean checkDollarNumberPattern(String currentWord) {
        if (currentWord.charAt(0) == '$') {
            currentWord = currentWord.substring(1);
            if (isNumber(currentWord)) {
                currentWord = currentWord.replace(",", "");
                double theNumNextTo = Double.parseDouble(currentWord);
                //$Price million -> Price M Dollars
                if (secondWord.matches(millionRegex)) {
                    //String yourFormattedString = formatter.format(theNumNextTo);
                    String yourFormattedString = numFormatter.format(theNumNextTo);
                    currentWord = yourFormattedString + " M Dollars";
                    updateTerms(currentWord);
                    return true;
                    //$Price billion -> Price*1000 M Dollars
                } else if (secondWord.matches(billionRegex)) {
                    theNumNextTo = theNumNextTo * 1000;
                    String yourFormattedString = formatter.format(theNumNextTo);
                    currentWord = yourFormattedString + " M Dollars";
                    updateTerms(currentWord);
                    return true;
                } else {
                    //$Price below million -> Price Dollars
                    if (theNumNextTo < 1000000) {
                        String yourFormattedString = formatter.format(theNumNextTo);
                        currentWord = yourFormattedString + " Dollars";
                        updateTerms(currentWord);
                        return true;

                        //$Price -> Price/million M Dollars
                    } else {
                        theNumNextTo = theNumNextTo / 1000000;
                        String yourFormattedString = formatter.format(theNumNextTo);
                        currentWord = yourFormattedString + " M Dollars";
                        updateTerms(currentWord);
                        return true;

                    }
                }
            }
            return false;
        }
        return false;
    }

    /**
     * in case we saw a single word with first/all of it is capitalized.
     * Take care of updating final dictionary wheter it has more than two appearnces in entire corpus.
     *
     * @param firstCapital
     */
    private void updateDocCapitals(String firstCapital) {
        if (toStem) {
            firstCapital = firstCapital.toLowerCase();
            for (char c : firstCapital.toCharArray()) {
                stemmer.add(c);
            }
            stemmer.stem();
            firstCapital = stemmer.toString().toUpperCase();
        }
        currentDoc.addWordToDic(firstCapital.toUpperCase());
    }

    /**
     * cleans word from unnecessary chars(if it contains any).
     *
     * @param word
     * @return returns the cleaned word.
     */
    private String cleanWord(String word) {
        if (!word.isEmpty()) {
            while (unecessaryChars.contains(word.charAt(0)) || unecessaryChars.contains(word.charAt(word.length() - 1))) {
                if (unecessaryChars.contains(word.charAt(0)))
                    word = word.substring(1);
                if (!word.isEmpty() && unecessaryChars.contains(word.charAt(word.length() - 1)))
                    word = word.substring(0, word.length() - 1);
                if (word.isEmpty())
                    return "";
            }
        }
        return word;
    }

    /**
     * in each iteration on Terms map,makes sure the next words are also "cleaned".
     *
     * @param index
     */
    private void updateCurrents(int index) {
        if (index + 1 < Terms.size()) {
            secondWord = Terms.get(index + 1);
            if (mustToClean(secondWord))
                secondWord = cleanWord(secondWord);
        } else {
            secondWord = "";
        }
        if (index + 2 < Terms.size()) {
            thirdWord = Terms.get(index + 2);
            if (mustToClean(thirdWord))
                thirdWord = cleanWord(thirdWord);
        } else {
            thirdWord = "";
        }
        if (index + 3 < Terms.size()) {
            forthWord = Terms.get(index + 3);
            if (mustToClean(forthWord))
                forthWord = cleanWord(forthWord);
        } else {
            forthWord = "";
        }
    }

    private boolean mustToClean(String word) {
        if (!word.isEmpty() && (unecessaryChars.contains(word.charAt(0)) || unecessaryChars.contains(word.charAt(word.length() - 1))))
            return true;
        return false;
    }

    private Double seperateNumberFromString(String word) {
        String temp = "";

        for (int i = 0; i < word.length(); i++) {
            if (Character.isDigit(word.charAt(i))) {
                temp += word.charAt(i) + "";
            } else {
                if (word.charAt(i) == '.')
                    temp += word.charAt(i) + "";
            }
        }
        return Double.parseDouble(temp);
    }

    /**
     * checks wheter a given string is a number - if it does returns true,else false.
     *
     * @param currentWord
     * @return
     */
    private boolean isNumber(String currentWord) {
        if (currentWord.length() > 0 && Character.isDigit(currentWord.charAt(0))) {
            int dotCounter = 0;
            for (int i = 0; i < currentWord.length(); i++) {
                char c = currentWord.charAt(i);
                if (Character.isDigit(c) || c == ',')
                    continue;
                if (c == '.')
                    dotCounter++;
                if (c != '.' || c != '.')
                    return false;
                if (dotCounter > 1)
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Takes care of entity issues(Entity defined only for more than 2 appearences in entire corpus.
     *
     * @return
     */
    private int isEntity() {
        String theEntity = "";
        StringTokenizer tokenizer;
        int numberOfWordsToSkip = 0;
        if (!currentWord.isEmpty() &&  currentWord.length() != 1  &&  Character.isUpperCase(currentWord.charAt(0))) {
            theEntity += currentWord;
            if (secondWord != null && !secondWord.isEmpty() && Character.isUpperCase(secondWord.charAt(0)) && !stopWords.contains(secondWord.toLowerCase())) {
                theEntity += " " + secondWord;
                if (thirdWord != null && !thirdWord.isEmpty() && Character.isUpperCase(thirdWord.charAt(0))  && !stopWords.contains(thirdWord.toLowerCase() )) {
                    theEntity += " " + thirdWord;
                    if (forthWord != null && !forthWord.isEmpty() && forthWord.length() != 1 && Character.isUpperCase(forthWord.charAt(0)) && !stopWords.contains(forthWord.toLowerCase()) )
                        theEntity += " " + forthWord;
                }
            }
        }
        tokenizer = new StringTokenizer(theEntity);
        if (tokenizer.countTokens() > 1) {
            numberOfWordsToSkip = tokenizer.countTokens();
            insertPartialEntity(tokenizer);
            updateTempEntityMap(theEntity);
        } else {
            numberOfWordsToSkip = -1;
        }
        return numberOfWordsToSkip - 1;
    }


    /**
     * This function responsible to entities laws(Entity defined only for more than 2 appearences in the entire corpus.
     *
     * @param potentialEntity
     */
    private void updateTempEntityMap(String potentialEntity) {
        double counter = 1;
        String upperEntity = potentialEntity.toUpperCase();
        Map<String, Double> innerEntityMap;
        if (!tempEntityMap.containsKey(upperEntity)) { //first time potential entity - add Entity & the non-entity terms
            innerEntityMap = new HashMap<>();
            innerEntityMap.put(currentDoc.getDocNO(), counter);
            tempEntityMap.put(upperEntity, innerEntityMap);
            currentDoc.addWordToDic(upperEntity);
        } else {
            innerEntityMap = tempEntityMap.get(upperEntity);
            if (!innerEntityMap.containsKey(currentDoc.getDocNO())) // facing entity for the 2nd time in different doc - add entity to currDoc
            {
                innerEntityMap.put(currentDoc.getDocNO(), counter);
                currentDoc.addWordToDic(upperEntity);
            } else { //   second or more appearences of this entity for the same doc
                counter = tempEntityMap.get(upperEntity).get(currentDoc.getDocNO()); //gets the old counter for this term for the same doc and updates it.
                tempEntityMap.get(upperEntity).replace(currentDoc.getDocNO(), counter + 1);
            }
        }
    }

    private void insertPartialEntity(StringTokenizer tokenizer) {
        while (tokenizer.hasMoreElements()) {
            StringBuilder stringBuilder = new StringBuilder(tokenizer.nextToken());
            String paritalEntity = stringBuilder.toString();
            String lower = paritalEntity.toLowerCase();
            String upper = paritalEntity.toUpperCase();
            if (!stopWords.contains(lower)) {
                currentDoc.addWordToDic(upper);
            }
        }
    }

    /*
    one run-time functions.
     */
    private void loadStopWords() {
        stopWords = new HashSet<>();
        try {
           // List<String> lines = Files.readAllLines(Paths.get(Controller.stringCorpusPath + "//stop_words.txt"));
            List<String> lines = Files.readAllLines(Paths.get("stop_words.txt"));
            stopWords.addAll(lines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createUnnecessaryChars() {
        unecessaryChars = new HashSet<>();
        Character[] whiteSpaces = {',', '.', ':', '"', '(', ')', '[', ']', '{', '}', '-', '\'', ';', '*', '+', '&', '?', '=', ' ', '#'}; // NO EMPTY STRING
        for (Character c : whiteSpaces)
            unecessaryChars.add(c);
    }

    public static Hashtable<String, String> createMonthsTable() {
        Months = new Hashtable<>();
        Months.put("January", "01");
        Months.put("JANUARY", "01");
        Months.put("Jan", "01");
        Months.put("JAN", "01");
        Months.put("February", "02");
        Months.put("FEBRUARY", "02");
        Months.put("Feb", "02");
        Months.put("FEB", "02");
        Months.put("March", "03");
        Months.put("MARCH", "03");
        Months.put("Mar", "03");
        Months.put("MAR", "03");
        Months.put("April", "04");
        Months.put("APRIL", "04");
        Months.put("Apr", "04");
        Months.put("APR", "04");
        Months.put("May", "05");
        Months.put("MAY", "05");
        Months.put("June", "06");
        Months.put("JUNE", "06");
        Months.put("Jun", "06");
        Months.put("JUN", "06");
        Months.put("July", "07");
        Months.put("JULY", "07");
        Months.put("Jul", "07");
        Months.put("JUL", "07");
        Months.put("August", "08");
        Months.put("AUGUST", "08");
        Months.put("Aug", "08");
        Months.put("AUG", "08");
        Months.put("September", "09");
        Months.put("SEPTEMBER", "09");
        Months.put("Sep", "09");
        Months.put("SEP", "09");
        Months.put("October", "10");
        Months.put("OCTOBER", "10");
        Months.put("Oct", "10");
        Months.put("OCT", "10");
        Months.put("November", "11");
        Months.put("NOVEMBER", "11");
        Months.put("Nov", "11");
        Months.put("NOV", "11");
        Months.put("December", "12");
        Months.put("DECEMBER", "12");
        Months.put("Dec", "12");
        Months.put("DEC", "12");
        return Months;
    }

    public static void createPriceTable() {
        Prices = new HashMap<>();
        Prices.put("dollars", "Dollars");
        Prices.put("DOLLARS", "Dollars");
        Prices.put("Dollars", "Dollars");
        Prices.put("m", "M");
        Prices.put("bn", "M");
        Prices.put("U.S.", "U.S.");
        Prices.put("U.S", "U.S.");
    }

    public static void createPercantageTable() {
        Percentage = new HashMap<>();
        Percentage.put("percent", "%");
        Percentage.put("percentages", "%");
        Percentage.put("%", "%");
    }

    public static void createIllionsTable() {
        illions = new HashMap<>();
        illions.put("million", "Million");
        illions.put("Million", "Million");
        illions.put("billion", "Billion");
        illions.put("Billion", "Billion");
        illions.put("trillion", "Trillion");
        illions.put("Trillion", "Trillion");
    }

    public void clearDocsAfterParse() {
        docsAfterParse.clear();
    }

    /*
    getters
     */
    public Boolean getToStem() {
        return toStem;
    }

    public Map<String, Document> getDocsAfterParse() {
        return docsAfterParse;
    }

    public Map<String, Map<String, Double>> getTempEntityMap() {
        return tempEntityMap;
    }

    public Document getCurrentDoc() {
        return currentDoc;
    }

    public int getDocCounter() {
        return docCounter;
    }
}
