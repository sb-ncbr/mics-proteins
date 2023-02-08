/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import messif.data.DataObject;
import messif.distance.impl.ProteinDistance;

/**
 *
 * @author Vlada
 */
public class PivotTables {

    private static final Logger LOG = Logger.getLogger(PivotTables.class.getName());

    public static void main(String[] args) {
        try {
            List<DataObject> pivots = Tools.getObjects("c:\\Datasets\\proteins\\490000dataset\\pivots\\Pivots_512.json.intIDs");
            Connection connection = DBGlobal.getConnectionFromIniFile();
            insertPivots(connection, pivots, 1);
        } catch (SQLException ex) {
            Logger.getLogger(PivotTables.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void insertPivots(Connection db, List<DataObject> pivots, int pivotSetId) throws SQLException {
        Statement st = db.createStatement();
        for (DataObject pivot : pivots) {
            String id = pivot.getID();
            String sql = "INSERT INTO pivot512(chainIntId, pivotSetId) VALUES (" + id + ", " + pivotSetId + ")";
            LOG.log(Level.INFO, "Executerd {0};", new Object[]{sql});
            st.execute(sql);
        }
        LOG.log(Level.INFO, "Inserted {0} pivots.", new Object[]{pivots.size()});
    }

    public static List<DataObject> getPivotsFromDB(Connection db, boolean full512, boolean forSketches, Integer pivotSetId) {
        return getPivotsFromDB(db, full512, forSketches, false, pivotSetId);
    }

    /**
     *
     * @param db
     * @param full512 switcher between numberOfPivots: 512 or 64
     * @param forSketches switcher whether to return just pivots for sketches
     * @param randomOrder
     * @return
     */
    public static List<DataObject> getPivotsFromDB(Connection db, boolean full512, boolean forSketches, boolean randomOrder, Integer pivotSetId) {
        List<DataObject> ret = new ArrayList<>();
        if (pivotSetId == null) {
            pivotSetId = PivotTables.getActivePivotSet(db);
        }
        try {
            String table;
            if (full512) {
                table = "pivot512";
            } else {
                table = "pivot64";
            }
            if (forSketches) {
                table += "ForSketches";
            }
            String sql = "SELECT DISTINCT chainIntId, gesamtId FROM (" + table + " p INNER JOIN proteinChain c ON p.chainIntId=c.intId) INNER JOIN pivotSet ps ON pivotSetId = ps.id WHERE ps.id=" + pivotSetId;
            if (randomOrder) {
                sql += " ORDER BY RAND()";
            }
            Statement st = db.createStatement();
            Logger.getLogger(DbTmp.class.getName()).log(Level.INFO, "getPivotsFromDB\n{0};", new Object[]{sql});
            ResultSet res = st.executeQuery(sql);
            while (res.next()) {
                String id = res.getString("chainIntId");
                String gesamtId = res.getString("gesamtId");
                DataObject pivot = Tools.getNewDataObjectWithId(id);
                DataObject protein = Tools.getNewDataObjectWithId(gesamtId);
                pivot = DataObject.addField(pivot, ProteinDistance.ENCAPSULATED_PROTEIN_NAME, protein);
                ret.add(pivot);
            }
            res.close();
            st.close();
        } catch (SQLException ex) {
            Logger.getLogger(PivotTables.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(PivotTables.class.getName()).log(Level.INFO, "Returning {0} pivots from DB", ret.size());
        return ret;
    }

    public static int getActivePivotSet(Connection db) {
        int ret = -1;
        try {
            String sql = "SELECT id FROM pivotSet WHERE currentlyUsed=1";
            Statement st = db.createStatement();
            ResultSet res = st.executeQuery(sql);
            if (res.next()) {
                String id = res.getString("id");
                ret = Integer.parseInt(id);
            }
            res.close();
            st.close();
            return ret;
        } catch (SQLException ex) {
            Logger.getLogger(PivotTables.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

}
