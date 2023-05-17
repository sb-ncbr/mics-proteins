/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinsDB;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import messif.utility.json.JSONException;
import org.json.JSONObject;
import proteinDB.PivotTables;

/**
 *
 * @author Vlada
 */
public class TMP {

    public static void main(String[] args) {
        Connection connection = proteinDB.DBGlobal.getConnectionFromIniFile();
        int activePivotSet = PivotTables.getActivePivotSet(connection);
        PivotTables.getPivotsFromDB(connection, false, true, activePivotSet);
//        String tableNameAndSuffix = "proteinChainMetadata NATURAL JOIN pivotSet WHERE pivotSet.currentlyUsed = 1 LIMIT 30000";
//        initSketchesFromDB(connection, tableNameAndSuffix, "chainIntId", "sketch512p", "sk1024_long");
    }

    public static final void initSketchesFromDB(Connection connection, String tableNameAndSuffix, String idColumnName, String sketchColumnName, String sketchFieldName) {
        Map<String, long[]> sketches = new HashMap<>();
        try {
            Statement st = connection.createStatement();
            String sql = "SELECT " + idColumnName + ", " + sketchColumnName + " FROM " + tableNameAndSuffix;
            System.out.println(sql);
            Logger.getLogger(TMP.class.getName()).log(Level.INFO, "Executerd {0};", new Object[]{sql});
            ResultSet res = st.executeQuery(sql);
            for (int counter = 1; res.next(); counter++) {
                String string = res.getString(sketchColumnName);
                if (string == null) {
                    continue;
                }
                String key = res.getString(idColumnName);
                string = new JSONObject(string).getString(sketchFieldName);
                string = string.substring(1, string.length() - 1);
                long[] sketch = Tools.parseArrayOfLongs(string, ",");
                sketches.put(key, sketch);
                if (counter % 10000 == 0) {
                    Logger.getLogger(TMP.class.getName()).log(Level.INFO, "Loaded {0} sketches.", counter);
                }
            }
        } catch (SQLException | JSONException | org.json.JSONException ex) {
            Logger.getLogger(TMP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
