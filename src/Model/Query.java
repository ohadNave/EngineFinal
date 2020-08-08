package Model;

import java.util.Map;
import java.util.Set;

public class Query {

    private String number;
    private String title;
    private String desc;
    private String narr;
    private Set<String> titleSet;
    private Set<String> descSet;
    private static int randomQueryNum = 1 ;

    public Query(String number, String title, String desc, String narr) {
        this.number = number;
        this.title = title;
        this.desc = desc;
        this.narr = narr;
    }

    public Query(String title)
    {
        this.title = title;
        this.number = Integer.toString(randomQueryNum);
        this.desc = "";
        randomQueryNum ++;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getNarr() {
        return narr;
    }

    public void setNarr(String narr) {
        this.narr = narr;
    }

  //  public void setParsedQuerySet(Set<String> parsedQuerySet) { this.parsedQuerySet = parsedQuerySet; }

  //  public Set<String> getParsedQuerySet() { return parsedQuerySet; }

    public Set<String> getTitleSet() {
        return titleSet;
    }

    public void setTitleSet(Set<String> titleSet) {
        this.titleSet = titleSet;
    }

    public Set<String> getDescSet() {
        return descSet;
    }

    public void setDescSet(Set<String> descSet) {
        this.descSet = descSet;
    }
}
