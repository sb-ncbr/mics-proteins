/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinsDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;

/**
 *
 * @author Vlada
 */
public class AddCSVPivotPairsIntoDB {

    private static final Logger LOG = Logger.getLogger(AddCSVPivotPairsIntoDB.class.getName());

    public static void main(String[] args) {
        try {
            int pivotCount = 64;
            List<String>[] csvPivots = Tools.parseCsv("c:\\Datasets\\proteins\\490000dataset\\csvPivotPairs\\64_512\\320_pairs.csv", 2, false);
            Connection connection = DBGlobal.getConnection(DBGlobal.IP);
            insertPivotsPairs(connection, pivotCount, csvPivots, 1);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private static void insertPivotsPairs(Connection db, int pivotCount, List<String>[] csvPivotPairs, int pivotSetId) throws SQLException {
        Statement st = db.createStatement();
        List<String> first = csvPivotPairs[0];
        List<String> second = csvPivotPairs[1];
        String table = "pivotPairsFor" + pivotCount + "pSketches";
        for (int i = 0; i < first.size(); i++) {
            String pivot1 = first.get(i);
            String pivot2 = second.get(i);
            String sql = "INSERT INTO " + table + "(pivotSetId,sketchBitOrder,pivot1, pivot2) VALUES (" + pivotSetId + ", " + i + ", " + pivot1 + ", " + pivot2 + ")";
            LOG.log(Level.INFO, "Executerd {0};", new Object[]{sql});
            st.execute(sql);
        }
        LOG.log(Level.INFO, "Inserted {0} pivot pairs.", new Object[]{first.size()});
    }

}
