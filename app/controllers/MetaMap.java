package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Concept;
import models.Term;
import models.Tuple;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.RequestBody;
import play.mvc.Result;

import gov.nih.nlm.nls.metamap.MetaMapApi;
import gov.nih.nlm.nls.metamap.MetaMapApiImpl;
import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.Mapping;
import gov.nih.nlm.nls.metamap.PCM;
import gov.nih.nlm.nls.metamap.Utterance;
//import gov.nih.nlm.nls.metamap.Result;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Hyunun on 2015-03-19.
 */
public class MetaMap extends Controller {

    public static final String URL = "dbpony.kaist.ac.kr";

    public static Result search() {
        RequestBody body = request().body();
        JsonNode json = body.asJson();
        String term = json.findPath("term").textValue(); //XXX: rename to keyword
        return searchWithKeyword(term);
    }

    public static Result searchWithKeyword(String keyword) {
        if (keyword == null || keyword.equals(""))
            return internalServerError("empty keyword");

        MetaMapApi api = new MetaMapApiImpl(URL);
        List<gov.nih.nlm.nls.metamap.Result> resultList = api.processCitationsFromString(keyword);
        gov.nih.nlm.nls.metamap.Result result = resultList.get(0);

        Tuple tuple = new Tuple("temp", "");
        int gid = 1;

        try {
            for (Utterance utterance : result.getUtteranceList()) {
                for (PCM pcm : utterance.getPCMList()) {
//                    System.out.println("Mappings:");
                    for (Mapping map : pcm.getMappingList()) {
//                        System.out.println(" Map Score: " + -map.getScore());
                        for (Ev mapEv : map.getEvList()) {
//                            System.out.println("   Score: " + -mapEv.getScore());
//                            System.out.println("   Concept Id: " + mapEv.getConceptId());
//                            System.out.println("   Concept Name: " + mapEv.getConceptName());
//                            System.out.println("   Preferred Name: " + mapEv.getPreferredName());
//                            System.out.println("   Semantic Types: " + mapEv.getSemanticTypes());

                            Concept c = new Concept();
                            c.gid = "TEMP" + gid;
                            c.conceptId = mapEv.getConceptId();
                            c.mapScore = -map.getScore();
                            c.conceptName = mapEv.getConceptName();
                            c.preferredName = mapEv.getPreferredName();
                            c.semanticType = mapEv.getSemanticTypes().toString();

                            tuple.addConcept(keyword, c);
                        }
                        gid++;
                    }
                }
            }
        } catch (Exception e) {
            return internalServerError(e.toString());
        }

        api.disconnect();

        List list = new LinkedList();
        Term term = tuple.terms.get(keyword);
        for (Map.Entry<String, List<Concept>> entry : term.conceptGroups.entrySet()) {
            ObjectNode concept = Json.newObject();
            concept.put("conceptid", term.getMergedConceptId(entry.getValue()));
            concept.put("text", term.getMergedText(entry.getValue()));
            concept.put("score", term.getMergedScore(entry.getValue()));
            list.add(concept);
        }

        return ok(Json.toJson(list));
    }
}
