/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import messif.data.DataObject;

/**
 *
 * @author Vlada
 */
public class MetadataTable {

    private static final Logger LOG = Logger.getLogger(MetadataTable.class.getName());

    public static void main(String[] args) {
        try {
            Iterator<DataObject> dataObjects512Pivots = Tools.getIterator("c:\\Datasets\\proteins\\490000dataset\\dataset\\sketches\\512\\results_512_sk1024b.json");
            Map<String, DataObject> dataObjects64Pivots = Tools.getObjectsAsLocatorMap("c:\\Datasets\\proteins\\490000dataset\\dataset\\sketches\\64_512\\results_512_sk192b.json", -1);
            Connection connection = DBGlobal.getConnectionFromIniFile();
            insertMetadata(connection, dataObjects512Pivots, dataObjects64Pivots, 1);
        } catch (SQLException ex) {
            Logger.getLogger(PivotTables.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void insertMetadata(Connection db, Iterator<DataObject> dataObjects512Pivots, Map<String, DataObject> dataObjects64Pivots, int pivotSetId) throws SQLException {
        Statement st = db.createStatement();
        for (int i = 0; dataObjects512Pivots.hasNext(); i++) {
            DataObject obj512Pivots = dataObjects512Pivots.next();
            String chainIntId = obj512Pivots.getID();
            DataObject obj64Pivots = dataObjects64Pivots.get(chainIntId);
            String pivotDistances = Tools.getSubObject(obj512Pivots, "dists", false).toJSONString();
            String sketch512p = Tools.getSubObject(obj512Pivots, "sk1024_long", false).toJSONString();
            String sketch64p = Tools.getSubObject(obj64Pivots, "sk192_long", false).toJSONString();
            String sql = "INSERT INTO proteinChainMetadata(pivotSetId, chainIntId, pivotDistances, sketch512p, sketch64p) VALUES (" + pivotSetId + ", " + chainIntId + ", '" + pivotDistances + "', '" + sketch512p + "', '" + sketch64p + "')";
            LOG.log(Level.INFO, "Added object {0} with id {1}.", new Object[]{i, chainIntId});
            st.execute(sql);
        }
        LOG.log(Level.INFO, "Inserted {0} pivots.", new Object[]{dataObjects64Pivots.size()});
    }

    public static void insertOrUpdateMetadata(Connection db, DataObject obj, int pivotSetId) {
        try {
            Statement st = db.createStatement();
            String chainIntId = obj.getID();
            String pivotDistances = Tools.getSubObject(obj, "dists", false).toJSONString();
            String sketch512p = Tools.getSubObject(obj, "sk1024_long", false).toJSONString();
            String sketch64p = Tools.getSubObject(obj, "sk192_long", false).toJSONString();
            String sql = "REPLACE INTO proteinChainMetadata(pivotSetId, chainIntId, pivotDistances, sketch512p, sketch64p) VALUES (" + pivotSetId + ", " + chainIntId + ", '" + pivotDistances + "', '" + sketch512p + "', '" + sketch64p + "')";
            LOG.log(Level.INFO, "SQL command to add metadata to protein {0}:", new Object[]{chainIntId});
            LOG.log(Level.INFO, "{0}.", new Object[]{sql});
            st.execute(sql);
        } catch (SQLException ex) {
            Logger.getLogger(MetadataTable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
