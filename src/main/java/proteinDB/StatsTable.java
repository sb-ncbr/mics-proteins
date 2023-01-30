/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Vlada
 */
public class StatsTable {

    public static final Logger LOG = Logger.getLogger(StatsTable.class.getName());
    private static final TreeMap<Integer, PreparedStatement> STORE_DISTS_BATCH = new TreeMap<>();
    private static final DecimalFormat DF = new DecimalFormat("#.###");

    public static void init(Connection db) throws SQLException {
        if (!STORE_DISTS_BATCH.isEmpty()) {
            LOG.log(Level.INFO, "Prepared statements already prepared");
            return;
        }
        String basis = "INSERT IGNORE INTO `queriesNearestNeighboursStats`("
                + "`evaluationTime`, `queryGesamtId`, `nnGesamtId`, `qscore`, `rmsd`, `alignedResidues`, `seqIdentity`, rotationStats"
                + ") VALUES ";
        String block1 = "(?, ?, ?, ?, ?, ?, ?, ?)";
        String currBlock = block1;
        STORE_DISTS_BATCH.put(1, db.prepareStatement(basis + currBlock));
        for (int i = 1; i < 10; i++) {
            int batchSize = (int) Math.pow(2, i);
            currBlock += ", " + currBlock;
            STORE_DISTS_BATCH.put(batchSize, db.prepareStatement(basis + currBlock));
        }
    }

    public void storeLongDistsToDB(Queue<Object[]> queue) {
        long t = -System.currentTimeMillis();
        LOG.log(Level.INFO, "Storing {0} evaluated distances into DB", new Object[]{queue.size()});
        while (!queue.isEmpty()) {
            int size = queue.size();
            Map.Entry<Integer, PreparedStatement> floorEntry = STORE_DISTS_BATCH.floorEntry(size);
            store(queue, floorEntry.getValue(), floorEntry.getKey());
        }
        t += System.currentTimeMillis();
        LOG.log(Level.INFO, "All evaluated distances stored into DB in {0} ms", new Object[]{t});
    }

    private void store(Queue<Object[]> queue, PreparedStatement st, int batchSize) {
        if (st == null) {
            return;
        }
        try {
            synchronized (st) {
                try {
                    st.clearParameters();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                for (int i = 0; i < batchSize; i++) {
                    Object[] data = queue.poll();
                    if (data == null) {
                        return; // it will bestored next time - must be done in this way to make paralelism safe
                    }
                    float[] stats = (float[]) data[3];
                    String rotationStats = DF.format(stats[4]);
                    for (int j = 5; j < stats.length; j++) {
                        rotationStats += ";" + DF.format(stats[j]);
                    }
                    st.setLong(1 + i * 8, (long) data[0]);
                    st.setString(2 + i * 8, (String) data[1]);
                    st.setString(3 + i * 8, (String) data[2]);
                    st.setFloat(4 + i * 8, stats[0]);
                    st.setFloat(5 + i * 8, stats[1]);
                    st.setFloat(6 + i * 8, stats[3]);
                    st.setFloat(7 + i * 8, stats[2]);
                    st.setString(8 + i * 8, rotationStats);
                }
                long t1 = -System.currentTimeMillis();
                st.execute();
                t1 += System.currentTimeMillis();
                LOG.log(Level.INFO, "Stored {0} distances in {1} ms", new Object[]{batchSize, t1});
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
