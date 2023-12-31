/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import messif.data.DataObject;
import messif.distance.impl.ProteinDistance;
import sf.objects.sketchcreator.ObjectToSketchTransformator;

/**
 *
 * @author Vlada
 */
public class StatsCounter {

    private static final ConcurrentHashMap<String, AtomicInteger> TOTAL_PROGRESS_COUNTER;
    private static final ConcurrentHashMap<String, AtomicInteger> SEARCH_DC_EXPECTED_COUNTER;
    private static final ConcurrentHashMap<String, AtomicInteger> TOTAL_CACHED_DISTS_COUNTER;
    private static final ConcurrentHashMap<String, Integer> PIVOT_TOTAL_COUNT;
    private static final ConcurrentHashMap<String, Integer> PIVOT_TOTAL_CACHED;
    private static final ConcurrentHashMap<String, Integer> PIVOT_TOTAL_TIMES;
    private static final ConcurrentHashMap<String, Long> JOB_TIME_STAMP;

    static {
        TOTAL_PROGRESS_COUNTER = new ConcurrentHashMap<>();
        TOTAL_CACHED_DISTS_COUNTER = new ConcurrentHashMap<>();
        SEARCH_DC_EXPECTED_COUNTER = new ConcurrentHashMap<>();
        PIVOT_TOTAL_COUNT = new ConcurrentHashMap<>();
        PIVOT_TOTAL_TIMES = new ConcurrentHashMap<>();
        PIVOT_TOTAL_CACHED = new ConcurrentHashMap<>();
        JOB_TIME_STAMP = new ConcurrentHashMap<>();
    }

    public static int incrementDCToBeDone(String jobId) {
        return incrementCounter(jobId, SEARCH_DC_EXPECTED_COUNTER);
    }

    public static void incrementCached(String jobId) {
        incrementCounter(jobId, TOTAL_CACHED_DISTS_COUNTER);
    }

    public static void incrementCached(String jobId, int byValue) {
        if (TOTAL_CACHED_DISTS_COUNTER == null || jobId == null) {
            return;
        }
        AtomicInteger ai = TOTAL_CACHED_DISTS_COUNTER.get(jobId);
        if (ai == null) {
            TOTAL_CACHED_DISTS_COUNTER.put(jobId, new AtomicInteger(byValue));
        } else {
            ai.addAndGet(byValue);
        }
    }

    public static void incrementProgress(String jobId) {
        incrementCounter(jobId, TOTAL_PROGRESS_COUNTER);
    }

    private static int incrementCounter(String jobId, ConcurrentHashMap<String, AtomicInteger> counter) {
        if (counter == null || jobId == null) {
            return -1;
        }
        AtomicInteger ai = counter.get(jobId);
        if (ai == null) {
            counter.put(jobId, new AtomicInteger(1));
            return 1;
        } else {
            return ai.incrementAndGet();
        }
    }

    public static int[] getCacheAndProgressCounters(String jobId) {
        int[] ret = new int[2];
        AtomicInteger ai = TOTAL_CACHED_DISTS_COUNTER.get(jobId);
        ret[0] = ai == null ? 0 : ai.get();
        Integer pivotsCached = PIVOT_TOTAL_CACHED.get(jobId);
        if (pivotsCached == null) {
            pivotsCached = 0;
        }
        if (pivotsCached <= ret[0]) {
            ret[0] -= pivotsCached;
        }
        ai = TOTAL_PROGRESS_COUNTER.get(jobId);
        ret[1] = ai == null ? 0 : ai.get();
        Integer pivotsTotal = PIVOT_TOTAL_COUNT.get(jobId);
        if (pivotsTotal == null) {
            pivotsTotal = 0;
        }
        if (pivotsTotal <= ret[1]) {
            ret[1] -= pivotsTotal;
        }
        return ret;
    }

    public static int getPivotTotalTimes(String jobId) {
        Integer ret = PIVOT_TOTAL_TIMES.get(jobId);
        if (ret == null) {
            Logger.getLogger(StatsCounter.class.getName()).log(Level.WARNING, "Pivot times not set");
            return 0;
        }
        return ret;
    }

    public static void deleteCounters(String jobID) {
        TOTAL_PROGRESS_COUNTER.remove(jobID);
        TOTAL_CACHED_DISTS_COUNTER.remove(jobID);
        SEARCH_DC_EXPECTED_COUNTER.remove(jobID);
        PIVOT_TOTAL_COUNT.remove(jobID);
        PIVOT_TOTAL_TIMES.remove(jobID);
        PIVOT_TOTAL_CACHED.remove(jobID);
        JOB_TIME_STAMP.remove(jobID);
    }

    public static void deleteOldCounters() {
        long t = System.currentTimeMillis() - 1000 * 60 * 60 * 6;
        Set<String> delete = new HashSet<>();
        for (Map.Entry<String, Long> entry : JOB_TIME_STAMP.entrySet()) {
            if (entry.getValue() < t) {
                String jobID = entry.getKey();
                TOTAL_PROGRESS_COUNTER.remove(jobID);
                TOTAL_CACHED_DISTS_COUNTER.remove(jobID);
                SEARCH_DC_EXPECTED_COUNTER.remove(jobID);
                PIVOT_TOTAL_COUNT.remove(jobID);
                PIVOT_TOTAL_TIMES.remove(jobID);
                PIVOT_TOTAL_CACHED.remove(jobID);
                delete.add(jobID);
            }
        }
        for (String key : delete) {
            JOB_TIME_STAMP.remove(key);
        }
    }

    public static void setPivotsTimes(String jobId, Long time) {
        if (jobId != null) {
            PIVOT_TOTAL_TIMES.put(jobId, time.intValue());
        }
    }

    public static String getJobProgressMessage(String jobId) {
        AtomicInteger ai = TOTAL_PROGRESS_COUNTER.get(jobId);
        if (ai == null) {
            String ret = "{"
                    + "\"Job_id\":\"" + jobId + "\","
                    + "\"Running\":0"
                    + "}";
            System.out.println(ret);
            return ret;
        } else {
            Integer pivotTotalCount = PIVOT_TOTAL_COUNT.get(jobId);
            if (pivotTotalCount == null) {
                pivotTotalCount = 0;
            }
            Integer pivotsCached = PIVOT_TOTAL_CACHED.get(jobId);
            if (pivotsCached == null) {
                pivotsCached = 0;
            }
            Integer pivotsTimes = getPivotTotalTimes(jobId);
            int totalDCCount = TOTAL_PROGRESS_COUNTER.get(jobId).get();
            int pivotDoneCount = Math.min(totalDCCount, pivotTotalCount);
            pivotDoneCount = Math.max(pivotDoneCount, 0);
            String ret = "{"
                    + "\"Job_id\":\"" + jobId + "\","
                    + "\"Running\":1,"
                    + "\"pivotDistCountComputed\":" + pivotDoneCount + ","
                    + "\"pivotDistCountExpected\":" + pivotTotalCount + ","
                    + "\"pivotDistCountCached\":" + pivotsCached + ","
                    + "\"pivotTime\":" + pivotsTimes;
//            if (pivotTotalCount > dcCount) {
//                ret += "}";
//                System.out.println(ret);
//                return ret;
//            }
            ai = SEARCH_DC_EXPECTED_COUNTER.get(jobId);
            int searchExpected = ai == null ? 0 : ai.get();
            ai = TOTAL_CACHED_DISTS_COUNTER.get(jobId);
            int totalCached = ai == null ? pivotsCached : ai.get();
            ret += ",";
            int searchDistCountComputed = Math.max(0, totalDCCount - pivotTotalCount);
            ret += "\"searchDistCountComputed\":" + searchDistCountComputed + ","
                    + "\"searchDistCountExpected\":" + searchExpected + ","
                    + "\"searchDistCountCached\":" + (totalCached - pivotsCached) + "}";
            System.out.println(ret);
            return ret;
        }
    }

    public static void setPivotTotalCount(String jobId, int value) {
        PIVOT_TOTAL_COUNT.put(jobId, value);
    }

    public static void setCurrProgress(String jobId, int value) {
        TOTAL_PROGRESS_COUNTER.put(jobId, new AtomicInteger(value));
    }

    public static void setPivotCached(String jobId, int value) {
        PIVOT_TOTAL_CACHED.put(jobId, value);
    }

    public static void setTimeStamp(String jobId) {
        AtomicLong l = new AtomicLong();
        synchronized (l) {
            l.addAndGet(System.currentTimeMillis());
            JOB_TIME_STAMP.put(jobId, l.get());
        }
    }

    public static DataObject addCachedDists(DataObject query, int objectsForDistsCount, boolean filterJustPivots) {
        synchronized (query) {
            String gesamtId = query.getField(ProteinDistance.ENCAPSULATED_PROTEIN_NAME, DataObject.class).getID();
            Statement st = query.getField("statement", Statement.class);
            if (st == null) {
                Logger.getLogger(StatsCounter.class.getName()).log(Level.SEVERE, "No Statement to access DB is provided!");
            }
            try {
                if (st == null) {
                    Logger.getLogger(StatsCounter.class.getName()).log(Level.SEVERE, "No Statement to access DB is provided!");
                } else if (st.isClosed()) {
                    Logger.getLogger(StatsCounter.class.getName()).log(Level.SEVERE, "Statement is closed!");
                }
            } catch (SQLException ex) {
                Logger.getLogger(StatsCounter.class.getName()).log(Level.SEVERE, null, ex);
            }
            long time = -System.currentTimeMillis();
            DataObject cachedDists = DBGlobal.selectCachedDists(st, gesamtId, objectsForDistsCount, filterJustPivots);
            time += System.currentTimeMillis();
            query = DataObject.addField(query, ObjectToSketchTransformator.DISTS_MAP_FIELD, cachedDists);
            query = DataObject.addField(query, Tools.PIVOT_COUNT, objectsForDistsCount);
            Integer cachedPivots = filterJustPivots ? cachedDists.getField("pivotDistCountCached", Integer.class) : 0;
            Long pivotTimes = filterJustPivots ? cachedDists.getField("pivotTimes", Long.class) : 0L;
            query = DataObject.addField(query, "pivotDistCountCached", cachedPivots);
            query = DataObject.addField(query, "pivotDistTimes", time);
            String jobId = query.getField(ProteinDistance.JOB_ID, String.class);
            StatsCounter.setTimeStamp(jobId);
            setPivotTotalCount(jobId, objectsForDistsCount);
            if (cachedPivots != null) {
                setPivotCached(jobId, cachedPivots);
            } else {
                setPivotCached(jobId, 0);
            }
            if (pivotTimes != null) {
                setPivotsTimes(jobId, pivotTimes);
            } else {
                setPivotsTimes(jobId, 0L);
            }
            StatsCounter.setCurrProgress(jobId, 0);
            StatsCounter.deleteOldCounters();
            return query;
        }
    }

}
