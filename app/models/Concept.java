package models;

import java.util.List;

/**
 * Created by Hyunun on 2015-03-17.
 */
public class Concept {
    public String gid;
    public String conceptId;

    public int mapScore;
    public String conceptName;
    public String preferredName;
    public String semanticType;

    @Override
    public String toString() {
        return "" + conceptName + " (" + preferredName + ") " + semanticType;
    }

    public String[] getSemanticTypeList() {
        String str = semanticType.substring(1, semanticType.length() - 1);
        String[] types = str.split(",");
        return types;
    }
}
