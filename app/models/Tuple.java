package models;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Hyunun on 2015-03-17.
 */
public class Tuple {

    public String hid;
    public String context;
    public Map<String, Term> terms; // K: originPhrase, V: Term

    // Additional attributes
    public String hname;
    public String originDB;

    public Tuple(String hid, String context) {
        this.hid = hid;
        this.context = context;
        this.terms = new HashMap<String, Term>();
    }

    public void addConcept(String originPhrase, Concept c) {
        Term term = terms.getOrDefault(originPhrase, new Term(originPhrase));
        term.addConcept(c);
        terms.put(originPhrase, term);
    }
}
