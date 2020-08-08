package Model;

import java.io.Serializable;

public class Term implements Serializable {

    private String value;
    private int df; //doc-frequency
    private int pointerToLine;
    private int totalCounter;
    private boolean isEntity;


    /**
     * Constructor - builds Term object after we know his df.
     * @param totalCounter
     */
    public Term(int totalCounter) {
        this.totalCounter = totalCounter;
        df =1;
    }

    public void incrementNumOfDocsTermIn(){ df++; }

    public void incrementTotal(int inc){ totalCounter = totalCounter + inc ; }

    /*
    getters and setters.
     */

    public void setTotalCounter(int totalCounter) { this.totalCounter = totalCounter; }

    public void setValue(String value) { this.value = value; }

    public void setDf(int df) { this.df = df; }

    public void setPointerToLine(int pointerToLine) { this.pointerToLine = pointerToLine; }

    public int getDf() { return df; }

    public int getPointerToLine() { return pointerToLine; }

    public String getValue() { return value; }

    public boolean isEntity() { return isEntity; }

    public void setEntity(boolean entity) { isEntity = entity; }

    public int getTotalCounter() { return totalCounter; }

}


