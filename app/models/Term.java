package models;

import java.util.*;

/**
 * Created by Hyunun on 2015-03-17.
 */
public class Term {
    public String originPhrase;

    // Concept map for grouping with gid
    // K: gid
    // V: List of Concept
    public Map<String, List<Concept>> conceptGroups;

    public Term(String originPhrase) {
        this.originPhrase = originPhrase;
        this.conceptGroups = new HashMap<String, List<Concept>>();
    }

    public void addConcept(Concept c) {
        List<Concept> concepts = conceptGroups.getOrDefault(c.gid, new LinkedList<Concept>());
        concepts.add(c);
        conceptGroups.put(c.gid, concepts);
    }

    public String getMergedConceptId(List<Concept> concepts) {
        String cid = "";
        for (Concept c : concepts) {
            cid = cid + c.conceptId + ';';
        }
        return cid;
    }

    public String getMergedText(List<Concept> concepts) {
        String text = "";
        for (Concept c : concepts) {
            text = text + c.toString() + " ";
        }
        //return concepts.get(0).mapScore + " " + text;
        return text;
    }

    public String getMergedScore(List<Concept> concepts) {
        return String.valueOf(concepts.get(0).mapScore);
    }

    public String convertTags(List<Concept> concepts) {
        String text = getMergedText(concepts);
        text.replace("[idcn]", "<span class='label label-info'>idcn</span>");
        return "<span class='label label-info'>idcn</span>";
    }
}
