package View;

import javafx.beans.property.SimpleStringProperty;

public class EntityItem {
    private SimpleStringProperty entityName;
    private SimpleStringProperty entityRank;

    public EntityItem(String entity,String rank){
        entityName = new SimpleStringProperty(entity);
        entityRank = new SimpleStringProperty(rank);
    }


    // getters and setters
    public String getEntityName() {
        return entityName.get();
    }

    public SimpleStringProperty entityNameProperty() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName.set(entityName);
    }

    public String getEntityRank() {
        return entityRank.get();
    }

    public SimpleStringProperty entityRankProperty() {
        return entityRank;
    }

    public void setEntityRank(String entityRank) {
        this.entityRank.set(entityRank);
    }

}
