/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
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
public class CSVPivotPairsTables {

    private static final Logger LOG = Logger.getLogger(CSVPivotPairsTables.class.getName());

    public static void main(String[] args) {
        try {
            int pivotCount = 64;
            List<String>[] csvPivots = Tools.parseCsv("c:\\Datasets\\proteins\\490000dataset\\csvPivotPairs\\64_512\\320_pairs.csv", 2, false);
            Connection connection = DBGlobal.getConnectionFromIniFile();
            insertPivotsPairs(connection, pivotCount, csvPivots, 1);
        } catch (SQLException ex) {
            Logger.getLogger(PivotTables.class.getName()).log(Level.SEVERE, null, ex);
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

    public static String createTemporarCSVPivotPairsFile(Connection db, boolean pivotsFor512Sketches, int pivotSetId) {
        try {
            String tableName = pivotsFor512Sketches ? "pivotPairsFor512pSketches" : "pivotPairsFor64pSketches";
            File file = File.createTempFile("temp", null);
            int counter = 0;
            try (PrintStream ps = new PrintStream(file)) {
                String sql = "SELECT * FROM " + tableName + " c INNER JOIN pivotSet p ON c.pivotSetId = p.id AND p.id = " + pivotSetId + " ORDER BY sketchBitOrder";
                LOG.log(Level.INFO, "Executed {0};", new Object[]{sql});
                Statement st = db.createStatement();
                ResultSet res = st.executeQuery(sql);
                for (counter = 1; res.next(); counter++) {
                    String p1 = res.getString("pivot1");
                    String p2 = res.getString("pivot2");
                    ps.println(p1 + ";" + p2);
                }
                ps.flush();
            }
            String ret = file.getAbsolutePath();
            LOG.log(Level.INFO, "Printed {0} pivot pairs into temporary file {1}.", new Object[]{counter, ret});
            file.deleteOnExit();
            return ret;
        } catch (IOException | SQLException ex) {
            Logger.getLogger(CSVPivotPairsTables.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
