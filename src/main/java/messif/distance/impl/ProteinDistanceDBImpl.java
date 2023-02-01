/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.distance.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.data.DataObject;
import messif.record.Record;
import proteinDB.ChainTable;
import proteinDB.StatsCounter;
import proteinDB.StatsTable;

/**
 *
 * @author xmic
 */
public class ProteinDistanceDBImpl extends ProteinDistance {

    /**
     * Class id for Java serialization.
     */
    private static final long serialVersionUID = 124687651465L;
    public static final Logger LOG = Logger.getLogger(ProteinDistanceDBImpl.class.getName());

    private static StatsTable statsTable = null;
    private static Map<String, Integer> chainSizes;
    public static final Integer TIME_THRESHOLD_FOR_DB_STORING = 30;
    public static final Queue<Object[]> QUEUE_FOR_DB_STORE;

    /**
     * Distance function with no caching of any objects and no time limit
     */
    public ProteinDistanceDBImpl() {
        this(null);
    }

    /**
     *
     * @param gesamtLibraryPath
     */
    public ProteinDistanceDBImpl(String gesamtLibraryPath) {
        super(gesamtLibraryPath, ProteinNativeQScoreDistance.IMPLICIT_INNER_PARAMETER_ON_SIZE_CHECK);
    }

    static {
        QUEUE_FOR_DB_STORE = new ConcurrentLinkedQueue<>();
    }

    /**
     *
     * @param gesamtLibraryPath
     * @param innerParameterOnSizeDiff 0 for no check, 0.6 according to us, 0.7
     */
    public ProteinDistanceDBImpl(String gesamtLibraryPath, float innerParameterOnSizeDiff) {
        super(gesamtLibraryPath, innerParameterOnSizeDiff);
    }

    public static DataObject setDB(DataObject obj, Connection db) {
        try {
            Statement st = db.createStatement();
            DataObject ret = DataObject.addField(obj, "statement", st);
            ret = DataObject.addField(ret, "db", db);
            chainSizes = ChainTable.selectChainSizes(st);
            statsTable = new StatsTable();
            statsTable.init(db);
            return ret;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return obj;
    }

    @Override
    public float getDistance(DataObject o1, DataObject o2, float threshold) {
        String jobId = o1.getField(JOB_ID, String.class);
        float ret = checkPrecomputedDists(o1, o2);
        if (ret >= 0) {
            StatsCounter.incrementCached(jobId);
            StatsCounter.incrementProgress(jobId);
            return ret;
        }
        ret = checkPrecomputedDists(o2, o1);
        if (ret >= 0) {
            StatsCounter.incrementCached(jobId);
            StatsCounter.incrementProgress(jobId);
            return ret;
        }
        String id1 = (String) o1.getField(ENCAPSULATED_PROTEIN_NAME, Record.class).getField("_id");
        String id2 = (String) o2.getField(ENCAPSULATED_PROTEIN_NAME, Record.class).getField("_id");
        if (chainSizes != null) {
            Integer size1 = chainSizes.get(id1);
            Integer size2 = chainSizes.get(id2);
            float minScore = threshold > 1 ? 0 : 1 - threshold;
            if (size1 != null && size2 != null && (size2 < minScore * size1 || size1 < minScore * size2)) {
                return 2f;
            }
        }
        long time = -System.currentTimeMillis();
        float[] stats = nativeDist.getStatsFloats(id1, id2, innerParameterOnSizeDiff);
        time += System.currentTimeMillis();
//        LOG.log(Level.FINE, "Returned values by getStats: {0}; {1}; {2}; {3}. Length: {4}", new Object[]{stats[0], stats[1], stats[2], stats[3], stats.length});

        if (time >= TIME_THRESHOLD_FOR_DB_STORING && statsTable != null) {
            QUEUE_FOR_DB_STORE.add(new Object[]{time, id1, id2, stats});
        }
        StatsCounter.incrementProgress(jobId);
        return 1 - stats[0];
    }

    private float checkPrecomputedDists(DataObject o1, DataObject o2) {
        if (o1.containsField(DISTS_MAP_FIELD)) {
            String id = o2.getID();
            Map<String, Object> dists = o1.getField(DISTS_MAP_FIELD, Record.class).getMap();
//            LOG.log(Level.INFO, "Precomputed dists size in object {1} is {0}", new Object[]{dists.size(), o1.getID()});
            if (dists.containsKey(id)) {
                return (float) dists.get(id);
            }
        }
        return -1;
    }

    public static void storeLongDistsToDB() {
        statsTable.storeLongDistsToDB(QUEUE_FOR_DB_STORE);
    }

}
