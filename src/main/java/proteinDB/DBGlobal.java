/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.data.DataObject;
import messif.record.RecordImpl;
import messif.utility.json.JSONReader;

/**
 *
 * @author Vlada
 */
public class DBGlobal {

    public static final String IP = "147.251.21.141";
    private static final Logger LOG = Logger.getLogger(DBGlobal.class.getName());
    private static final String INI_PATH = "c:\\Users\\Vlada\\Documents\\NetBeansProjects\\Skola\\proteins\\protein_search.ini";

    public static Connection getConnectionFromIniFile() {
        return getConnectionFromIniFile(INI_PATH);
    }

    public static Connection getConnectionFromIniFile(String iniFilePath) {
        IniFile iniFile = new IniFile(iniFilePath);
        String ipAddress = iniFile.getString("db", "ip", "localhost");
        String database = iniFile.getString("db", "database", "protein_chain_db");
        String user = iniFile.getString("db", "user", "chain");
        String password = iniFile.getString("db", "password", null);

        String path = "jdbc:mysql://" + ipAddress + "/" + database;
//        String path = "jdbc:mysql://" + ipAddress + ":3306/" + database + "?autoReconnect=true&user=" + user + "&password=" + password + "&useUnicode=yes&characterEncoding=UTF-8";
//        System.err.println(path);
        try {
            return DriverManager.getConnection(path, user, password);
//            return makeConnection(path, null, Driver.class);
        } catch (IllegalArgumentException | SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;

    }

    public static String getLibraryPathFromIniFile(String iniFilePath) {
        IniFile iniFile = new IniFile(iniFilePath);
        return iniFile.getString("dirs", "archive", null);
    }

    public static DataObject selectCachedDists(Statement db, String queryGesamtId, int pivotCount, boolean filterPivots) {
        Map<String, Object> ret = new HashMap<>();
        if (db == null) {
            if (filterPivots) {
                ret.put("pivotDistCountCached", 0);
            }
            return new RecordImpl(ret);
        }
        try {
            String pivotTableName = "pivot64ForSketches";
            if (pivotCount == 512) {
                pivotTableName = "pivot512";
            }
            if (pivotCount > 64 && pivotCount < 512) {
                pivotTableName = "pivot512ForSketches";
            }
            String sql;

            //try to take it from DB as the dists to pivots
            sql = "SELECT * FROM "
                    + "((SELECT chainIntId, pivotDistances, pivotSetId FROM proteinChainMetadata m INNER JOIN proteinChain p ON m.chainIntId = p.intId "
                    + "WHERE p.gesamtId = '" + queryGesamtId + "') x INNER JOIN (SELECT id FROM pivotSet WHERE currentlyUsed=1) y ON x.pivotSetId = y.id)";
            LOG.log(Level.INFO, "selectPrecomputedDistsToPivots:\n{0};", new Object[]{sql});
            ResultSet res = db.executeQuery(sql);
            Integer counterPrecomputedPivots = 0;
            if (res.next()) {
                String jsonString = res.getString("pivotDistances");
                DataObject json = (DataObject) JSONReader.readObjectFrom(jsonString, true);
                json = json.getField("dists", DataObject.class);
                Map<String, Object> map = json.getMap();
                ret.putAll(map);
                LOG.log(Level.INFO, "selectPrecomputedDistsToPivots: found {0} distances", new Object[]{map.size()});
                counterPrecomputedPivots = map.size();
            } else {
                LOG.log(Level.INFO, "selectPrecomputedDistsToPivots: nothing was found");
            }
            res.close();
            if (filterPivots && counterPrecomputedPivots >= pivotCount) {
                LOG.log(Level.INFO, "selectPrecomputedDistsToPivots: All dists to pivots already computed in advance. Found {0} dists, required {1} of them", new Object[]{counterPrecomputedPivots, pivotCount});
                ret.put("pivotDistCountCached", pivotCount);
                return new RecordImpl(ret);
            }

            //add cached dists evaluated during the runtime of the demo
            if (!filterPivots) {
                sql = "SELECT intId AS objId, 1-qscore AS dist"
                        + "    FROM (queriesNearestNeighboursStats s INNER JOIN proteinChain c ON s.nnGesamtId=c.gesamtId)"
                        + "    WHERE BINARY queryGesamtId = '" + queryGesamtId + "'";
            } else {
                sql = "SELECT intId AS objId, 1-qscore AS dist"
                        + " FROM (queriesNearestNeighboursStats s INNER JOIN proteinChain c ON s.nnGesamtId=c.gesamtId)"
                        + " WHERE BINARY queryGesamtId = '" + queryGesamtId + "' AND nnGesamtId IN"
                        + "      (SELECT c.gesamtId\n"
                        + "       FROM (" + pivotTableName + " p INNER JOIN proteinChain c ON p.chainIntId = c.intId) INNER JOIN pivotSet s ON p.pivotSetId=s.id"
                        + "       WHERE s.currentlyUsed=1"
                        + "       )";
            }
            LOG.log(Level.INFO, "selectCachedDists:\n{0};", new Object[]{sql});
            res = db.executeQuery(sql);
            int cachedCounter = 0;
            for (cachedCounter = 0; res.next(); cachedCounter++) {
                String id = res.getString("objId");
                float dist = res.getFloat("dist");
                ret.put(id, dist);
            }
            if (filterPivots) {
                ret.put("pivotDistCountCached", counterPrecomputedPivots + cachedCounter);
            }
            res.close();
            return new RecordImpl(ret);
        } catch (SQLException ex) {
            Logger.getLogger(DBGlobal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new RecordImpl(ret);
    }

    public static int selectNumberOfCachedDists(Statement db, String queryGesamtId) {
        try {
            String sql = "SELECT count(*) AS ret FROM `queriesNearestNeighboursStats` WHERE BINARY queryGesamtId = '" + queryGesamtId + "'";
            LOG.log(Level.INFO, "selectNumberOfCachedDists\n{0};", new Object[]{sql});
            ResultSet res = db.executeQuery(sql);
            if (res.next()) {
                return Math.max(0, res.getInt("ret"));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBGlobal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    public static void closeStatementAndDB(DataObject obj) {
        Statement st = obj.getField("statement", Statement.class);
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ex) {
                Logger.getLogger(DBGlobal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Connection conn = obj.getField("db", Connection.class);
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ex) {
                Logger.getLogger(DBGlobal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
