package Model;

import com.medallia.word2vec.Word2VecModel;
import org.tartarus.martin.Stemmer;
import com.medallia.word2vec.Searcher;

import java.io.File;
import java.util.*;

public class SemanticFinder {

    private Stemmer stemmer;
    private boolean useStem;

    public SemanticFinder(boolean toStem) {
        useStem = toStem;
        if (toStem) {
            stemmer = new Stemmer();
        }
    }

    /**
     * this function get a map where the key is queryId and the value is a vector of the words in the query
     * it calls rankSimWords and return all similar words to the words in each query
     * @param parsedQueries map of all queries and a vector of all the words in the query
     * @return map <queryId,similarWordsVector>
     */
    public Map<String, Set<String>> findSim(Map<String, Set<String>> parsedQueries) {
        Map<String, Set<String>> allSynQuery = new LinkedHashMap<>();
        for (String queryNumber : parsedQueries.keySet()) {
            Set<String> theWordsForThisQuery = parsedQueries.get(queryNumber);
            for (String queryTerm : theWordsForThisQuery) {
                Set<String> querySimTerms = rankSimWords(queryTerm);
                querySimTerms = tokenizeSet(querySimTerms);
                if(useStem){
                    querySimTerms = stemmedSimSet(querySimTerms);
                }
                allSynQuery.put(queryNumber,querySimTerms);
            }
        }
        return allSynQuery;
    }
    

    /**
     * this function get a word from query
     * it use Word2VecModel and search for the similar words to this word from query
     * it choose the first 3 words with the highest rank and return those 3 words as set of strings
     * @param wordFromQuery String one word from the query
     * @return set<similarWords> best 3 words that matches to the given word
     */
    public Set<String> rankSimWords(String wordFromQuery) {
        Set<String> synonyms = new HashSet<>();
        try {
            Word2VecModel vecModel = Word2VecModel.fromTextFile(new File("word2vec.c.output.model.txt"));
            Searcher searcher = vecModel.forSearch();
            List<Searcher.Match> matches = searcher.getMatches(wordFromQuery.toLowerCase(), 10);

            int synonymCounter = 0;
            for (Searcher.Match match : matches) {
                if (match.distance() > 0.5 && synonymCounter < 3 ) {
                    synonyms.add(match.match());
                }
                else{
                    break;
                }
                synonymCounter++;
            }

        } catch (Exception e) {
            synonyms.add(wordFromQuery);
        }

        return synonyms;
    }


    /**
     *
     * @param synSet
     * @return
     */
    private Set<String> stemmedSimSet(Set<String> synSet) {
        Set<String> stemmedSet = new HashSet<>();
        for (String synTerm : synSet) {
            stemmer.add(synTerm.toCharArray(), synTerm.length());
            stemmer.stem();
            stemmedSet.add(stemmer.toString());
        }
        return stemmedSet;
    }


    public Set<String> tokenizeSet(Set<String> terms) {
        Set<String> splitedTerms = new HashSet<>();
        for (String term : terms) {
            term = term.replaceAll("-" ," ");
            splitedTerms.add(term);
        }
        return splitedTerms;
    }

}
