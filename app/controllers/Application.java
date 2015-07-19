package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;

import play.db.*;
import play.libs.Json;
import play.mvc.*;

import java.sql.*;
import java.util.*;

public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render());
    }

    public static Result db() {
        return ok(views.html.db.render());
    }

    public static Result admin() {
        return ok(views.html.admin.render());
    }

    public static Result list(int offset) { // default offset = 0
        Map<String, String> map = new TreeMap<String, String>();
        Map<String, String> hnameMap = new TreeMap<String, String>();
        Map<String, String> originMap = new TreeMap<String, String>();

        try (Connection con = DB.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT T.hid, p_ehf_1.desc_eng, p_herb_1.hname_latin, p_herb_1.originhid\n" +
                    "FROM (SELECT hid from p_herb_ehf_tess_2 GROUP BY hid ORDER BY hid LIMIT 11 OFFSET "+offset+") AS T\n" +
                    "INNER JOIN p_herb_1 ON T.hid = p_herb_1.hid\n" +
                    "INNER JOIN p_herb_ehf_1 ON p_herb_1.originhid = p_herb_ehf_1.originhid\n" +
                    "INNER JOIN p_ehf_1 ON p_herb_ehf_1.originfid = p_ehf_1.originfid\n");
            while (rs.next()) {
                String hid = rs.getString("hid");
                String context = map.getOrDefault(hid, "") + rs.getString("desc_eng") + "; ";
                map.put(hid, context);
                hnameMap.put(hid, rs.getString("hname_latin"));
                originMap.put(hid, rs.getString("originhid").substring(0, 4));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            return internalServerError(e.toString());
        }

        List<Tuple> result = new LinkedList<Tuple>();
        for (Map.Entry<String, String> pair : map.entrySet()) {
            Tuple t = new Tuple(pair.getKey(), pair.getValue());
            String hid = pair.getKey();
            t.hname = hnameMap.get(hid);
            t.originDB = originMap.get(hid);
            result.add(t);
        }

        // directly render list view
        if (offset == 0)
            return ok(views.html.list.render(result));

        // return json object
        return ok(Json.toJson(result));
    }

    public static Result show(String hid) {
        // Find context (in case of no post body arrived)
        String context = "";
        String hname = "";
        String originDB = "";
        try (Connection con = DB.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT p_herb_1.hid, p_ehf_1.desc_eng, p_herb_1.hname_latin, p_herb_1.originhid\n" +
                    "FROM p_herb_1\n" +
                    "INNER JOIN p_herb_ehf_1 ON p_herb_1.originhid = p_herb_ehf_1.originhid\n" +
                    "INNER JOIN p_ehf_1 ON p_herb_ehf_1.originfid = p_ehf_1.originfid\n" +
                    "WHERE p_herb_1.hid = '" + hid + "'\n");
            while (rs.next()) {
                context = context + rs.getString("desc_eng") + "; ";
                hname = rs.getString("hname_latin");
                originDB = rs.getString("originhid");
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            return internalServerError(e.toString());
        }

        // Construct TupleSet
        Tuple tuple = new Tuple(hid, context);
        tuple.hname = hname;
        tuple.originDB = originDB;

        // Find terms
        try (Connection con = DB.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from p_herb_ehf_tess_2 WHERE hid='"+hid+"'");
            while (rs.next()) {
                String originPhrase = rs.getString("originphrase");

                Concept c = new Concept();
                c.gid = rs.getString("gid");
                c.conceptId = rs.getString("conceptid");
                c.mapScore = rs.getInt("mapscore");
                c.conceptName = rs.getString("conceptname");
                c.preferredName = rs.getString("preferredname");
                c.semanticType = "[" + rs.getString("semantictype") + "]";

                tuple.addConcept(originPhrase, c);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            return internalServerError(e.toString());
        }

        return ok(views.html.app.render(hid, tuple));
    }

    /*
    Request body format for save(:hid)
    Sequence of resolved results
    [{
        "term": "toxicity",
        "action": "C000001;" | "expert",
        "desc": "1000 Stasis (Stasis) [patf]",
        "expert": {
            "action": "C00001;" | "newTerm" | "notApplicable",
            "desc": "1000 Stasis (Stasis) [patf]",
        },
        "rt": { // possible keys are "rtOrigin" | "rtSearch" | "rtNew"
            "targetTerm": "1000 Stasis (SAP) [patf]",
            "targetCid": "C00001",
            "term": "SAP",
            "preferred": "Yes" | "No",
            "expert": "hjkim",
            "semantic": "Physical Object",
            "language": "English"
        }, ...
    }]
    */
    public static Result save(String hid) {
        Http.RequestBody body = request().body();
        JsonNode json = body.asJson();
        if (json == null)
            return badRequest("Expecting Json data");
        return saveResult(hid, json);
    }

    public static Result showResult(String hid) {
        return TODO;
    }

    public static Result vote(String hid) {
        return TODO;
    }

    private static Result saveResult(String hid, JsonNode data) {
        ArrayNode results = (ArrayNode)data;
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            for (JsonNode result : results) {
                String originPhrase = result.get("term").asText();
                String action = result.get("action").asText();

                // handle expert defined case
                if (action.equals("expert")) {
                    JsonNode expert = result.get("expert");
                    String expertAction = expert.get("action").asText();

                    // expert - not applicable
                    if (expertAction.equals("notApplicable")) {
                        continue;
                    }

                    // expert - new term
                    else if (expertAction.equals("newTerm")) {
                        String cid = saveRTSingle(con, hid, null, result.get("rtNew"));
                        saveFim(con, hid, originPhrase, cid);
                        saveRTSingle(con, hid, null, result.get("rtOrigin"));
                        //saveRTSingle(con, hid, cid, result.get("rtOrigin"));
                    }

                    // expert - select among metamap search results
                    else {
                        saveFim(con, hid, originPhrase, expertAction);
                        saveRT(con, hid, result.get("rtSearch"));
                    }
                }

                // normal case
                else {
                    saveFim(con, hid, originPhrase, action);
                    saveRT(con, hid, result.get("rtOrigin"));
                }

                //TODO: delete tuple from fim_2
            }
            con.commit();
        } catch (SQLException e) {
            return internalServerError(e.toString());
        }
        return ok(data);
    }

    private static void saveFim(Connection con, String hid, String originPhrase, String cidSeq) throws SQLException {
        String[] cidArray = cidSeq.split(";");
        for (String cid : cidArray) {
            Statement stmt = con.createStatement();
            stmt.executeUpdate("INSERT INTO p_herb_ehf_fim_3(hid, conceptid, originphrase) VALUES ('" + hid + "','" + cid + "','" + originPhrase + "')");
            stmt.close();
        }
    }

    /*
    "rt": { // possible keys are "rtOrigin" | "rtSearch" | "rtNew"
        "targetTerm": "1000 Stasis (SAP) [patf]",
        "targetCid": "C00001",
        "term": "SAP",
        "preferred": "Yes" | "No",
        "expert": "hjkim",
        "semantic": "Physical Object",
        "language": "English"
    }
     */

    private static void saveRT(Connection con, String hid, JsonNode data) throws SQLException {
        if (data == null)
            return;

        String[] cidArray = data.get("targetCid").asText().split(";");
        for (String cid : cidArray) {
            saveRTSingle(con, hid, cid, data);
        }
    }

    private static String saveRTSingle(Connection con, String hid, String cid, JsonNode data) throws SQLException {
        boolean isNewTerm = cid == null;

        if (data == null)
            return null;

        // generate IDs
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM rt_id_record");
        rs.next();
        String cui = (isNewTerm) ? String.format("P%07d", rs.getInt(1)) : cid;
        String lui = String.format("L%07d", rs.getInt(2));
        String sui = String.format("S%07d", rs.getInt(3));
        String aui = String.format("A%07d", rs.getInt(4));
        rs.close();
        stmt.close();

        // parse user input
        String ispref = data.get("preferred").asText().substring(0, 1);
        String sab = data.get("expert").asText(); //expert name
        String str = data.get("term").asText();
        String sty = data.get("semantic").asText();

        // insert into mrconse
        PreparedStatement psmt = con.prepareStatement("INSERT INTO mrconso VALUES (?,'ENG','P',?,'PF',?,?,?,'','','',?,'','',?,0,'N','')");
        psmt.setString(1, cui); //cui (cid)
        psmt.setString(2, lui); //lui
        psmt.setString(3, sui); //sui
        psmt.setString(4, ispref); //ispref
        psmt.setString(5, aui); //aui
        psmt.setString(6, sab); //sab
        psmt.setString(7, str); //str
        psmt.executeUpdate();
        psmt.close();

        // get semantic data
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT tui, stn FROM semanticdef WHERE sty='"+ sty +"'");
        rs.next();
        String tui = rs.getString(1);
        String stn = rs.getString(2);
        rs.close();
        stmt.close();

        // insert into mrsty
        psmt = con.prepareStatement("INSERT INTO mrsty VALUES (?,?,?,?,'','')");
        psmt.setString(1, cui);
        psmt.setString(2, tui);
        psmt.setString(3, stn);
        psmt.setString(4, sty);
        psmt.executeUpdate();
        psmt.close();

        // insert into mrrank, check duplication before insert
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT * FROM mrrank WHERE sab='"+ sab +"'");
        if (rs.next() == false) {
            psmt = con.prepareStatement("INSERT INTO mrrank VALUES (1000, ?, 'N', '')");
            psmt.setString(1, sab);
            psmt.executeUpdate();
            psmt.close();
        }
        rs.close();
        stmt.close();

        // update IDs
        stmt = con.createStatement();
        if (isNewTerm)
            stmt.executeUpdate("UPDATE rt_id_record SET cui=cui+1, lui=lui+1, sui=sui+1, aui=aui+1");
        else
            stmt.executeUpdate("UPDATE rt_id_record SET lui=lui+1, sui=sui+1, aui=aui+1");
        stmt.close();

        return cui;
    }

    public static Result searchFromRT( ) throws SQLException {
        String str = null;
        String sty = null;
        Http.RequestBody body = request().body();
        JsonNode json = body.asJson();
        String term = json.findPath("term").textValue();

        try (Connection con = DB.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT mrconso.str, mrsty.sty\n" +
                    "FROM mrconso, mrsty \n" +
                    "WHERE str = '" + term + "' and mrconso.cui = mrsty.cui\n");

            while (rs.next()) {
                str = rs.getString(1);
                sty = rs.getString(2);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            return internalServerError(e.toString());
        }

        List list = new LinkedList();
        ObjectNode concept = Json.newObject();
        concept.put("str", str);
        concept.put("sty", sty);
        list.add(concept);

        return ok(Json.toJson(list));
    }
}
