/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinsDB;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.data.DataObject;
import messif.record.RecordImpl;

/**
 *
 * @author Vlada
 */
public class DBGlobal {

    private static final Logger LOG = Logger.getLogger(DBGlobal.class.getName());
    public static final String IP = "147.251.21.141";

    public static Connection getConnection() {
        return getConnection(null);
    }

    public static Connection getConnection(String ipAddress) {
        if (ipAddress == null) {
            ipAddress = "localhost";
        }
        String path = "jdbc:mysql://" + ipAddress + ":3306/protein_chain_db?user=chain&password=OneCha1n2RuleThem4ll&useUnicode=yes&characterEncoding=UTF-8";
        try {
            return makeConnection(path, null, Driver.class);
        } catch (IllegalArgumentException | SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static Connection makeConnection(String dbConnUrl, Properties dbConnInfo, Class<Driver> dbDriverClass) throws IllegalArgumentException, SQLException {
        if (dbConnUrl == null) {
            throw new IllegalArgumentException("Database connection URL cannot be null");
        }
        try {
            return DriverManager.getConnection(dbConnUrl, dbConnInfo);
        } catch (SQLException e) {
            // If the driver is not provided, we cannot establish a connection
            if (dbDriverClass == null) {
                throw e;
            }
            try {
                // Create an instance of the new driver (it should register itself automatically) and return the connection
                return dbDriverClass.newInstance().connect(dbConnUrl, dbConnInfo);
            } catch (InstantiationException | IllegalAccessException | SQLException ex) {
                throw new IllegalArgumentException("Cannot connect to database using driver " + dbDriverClass.getName() + ": " + ex, ex);
            }
        }
    }

    public static DataObject selectCachedPivotDists(Statement db, String queryGesamtId, int pivotCount) {
        Map<String, Object> ret = new HashMap<>();
        try {
            String pivotTableName = "pivot64ForSketches";
            if (pivotCount == 512) {
                pivotTableName = "pivot512";
            }
            if (pivotCount > 64 && pivotCount < 512) {
                pivotTableName = "pivot512ForSketches";
            }
            String sql = "SELECT intId AS pivotId, 1-qscore AS dist FROM (queriesNearestNeighboursStats s INNER JOIN proteinChain c ON s.nnGesamtId=c.gesamtId) WHERE BINARY queryGesamtId = '" + queryGesamtId + "' AND nnGesamtId IN (SELECT c.gesamtId FROM (" + pivotTableName + " p INNER JOIN proteinChain c ON p.chainIntId = c.intId) INNER JOIN pivotSet s ON p.pivotSetId=s.id WHERE s.currentlyUsed=1)";
//            LOG.log(Level.INFO, "selectCachedPivotDists:\n{0};", new Object[]{sql});
            ResultSet res = db.executeQuery(sql);
            while (res.next()) {
                String id = res.getString("pivotId");
                float dist = res.getFloat("dist");
                ret.put(id, dist);
            }
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
}
