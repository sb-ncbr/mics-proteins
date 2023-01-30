///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package messif.distance.impl;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import messif.distance.DistanceFunc;
//import messif.distance.Metric;
//
///**
// *
// * @author xmic
// */
//public class ProteinNativeQScoreDistanceMemoryCached implements DistanceFunc<String>, Metric {
//
//    private static final FIFOCache<String, Float> CACHE = new FIFOCache<>();
//    private static final int TIME_THRESHOLD_FOR_CACHING_DIST = 1000;
//
//    /**
//     * Class id for Java serialization.
//     */
//    private static final long serialVersionUID = 124687651460L;
//
//    public static final Float IMPLICIT_INNER_PARAMETER_ON_SIZE_CHECK = 0.6f;
//    private static final Logger LOG = Logger.getLogger(ProteinNativeQScoreDistanceMemoryCached.class.getName());
//
//    /**
//     * Threshold in seconds for each distance evaluation. If evaluation
//     * shouldtake more time, it is interrupted and maximum disatcne is returned.
//     */
//    private final float timeThresholdForEval;
//
//    private static native void init(String archiveDirectory, boolean binaryArchive, double inherentApprox);
//
//    private native float getNativeDistance(String id1, String id2, float timeThresholdInSeconds, boolean storeLongToCache);
//
//    /**
//     *
//     * @param timeThresholdForEval time threshold for the distanec computation.
//     * If is exceesed, returns distance 3. Set -1 for unlimited.
//     */
//    public ProteinNativeQScoreDistanceMemoryCached(float timeThresholdForEval) {
//        if (timeThresholdForEval > 3600) {
//            throw new IllegalArgumentException("Time threshold " + timeThresholdForEval + " is higher than allowed (3600 s)");
//        }
//        this.timeThresholdForEval = timeThresholdForEval;
//    }
//
//    /**
//     * Must be called before first evaluation. Objects from specified file are
//     * read into the main memorys for more efficient distance evaluations
//     *
//     * @param gesamtLibraryPath
//     * @param innerParameterOnSizeDiff 0 for no check, 0.6 according tu us, 0.7
//     * according to gesamt
//     */
//    public static void initDistance(String gesamtLibraryPath, float innerParameterOnSizeDiff) {
//        try {
//            System.loadLibrary("ProteinDistance");
//            // parameter 0.6 is inherent parametr in C library that was examined to speed-up distance evaluation
//            // while well approximating the geometric similarity of protein structures
//            if (gesamtLibraryPath == null) {
//                init("/mnt/data/PDBe_clone_binary", true, innerParameterOnSizeDiff);
//            } else {
//                init(gesamtLibraryPath, true, innerParameterOnSizeDiff);
//            }
//        } catch (Exception ex) {
//            LOG.log(Level.WARNING, "Initialization of the distance function not successfull.", ex);
//        }
//
//    }
//
//    /**
//     * Must be called before first evaluation. Objects from specified file are
//     * read into the main memorys for more efficient distance evaluations
//     *
//     * @param gesamtLibraryPath
//     * @param cachedObjectsInPlainText
//     */
//    public static void initDistance(String gesamtLibraryPath) {
//        ProteinNativeQScoreDistanceMemoryCached.initDistance(gesamtLibraryPath, IMPLICIT_INNER_PARAMETER_ON_SIZE_CHECK);
//    }
//
//    @Override
//    public float getDistance(String o1, String o2, float threshold) {
//        if (o1 == null || o2 == null) {
//            LOG.log(Level.SEVERE, "Attempt to evaluate distance between null proteins {0}, {1}.", new Object[]{o1, o2});
//        }
//        if (o1.equals(o2)) {
//            return 0;
//        }
//        int comp = o1.compareTo(o2);
//        String key = comp < 0 ? o1 + "+" + o2 : o2 + "+" + o1;
//        if (CACHE.contains(key)) {
//            return CACHE.get(key);
//        }
//        try {
//            long t = -System.currentTimeMillis();
//            float ret = getNativeDistance(o1, o2, timeThresholdForEval, true);
//            t += System.currentTimeMillis();
//            if (t > TIME_THRESHOLD_FOR_CACHING_DIST) {
//                CACHE.put(key, ret);
//            }
//            return ret;
//        } catch (Exception e) {
//            LOG.log(Level.SEVERE, "Unsuccessful attempt to evaluate distance between protein structures with IDs {0}, {1}. Original exception: {2}", new Object[]{o1, o2, e.getMessage()});
//            throw new IllegalArgumentException("Unsuccessful attempt to evaluate distance between protein structures with IDs " + o1 + ", " + o2);
//        }
//    }
//
//    @Override
//    public Class<String> getObjectClass() {
//        return String.class;
//    }
//
//    @Override
//    public String toString() {
//        return getClass().getSimpleName();
//    }
//
//    @Override
//    public float getMaxDistance() {
//        return 1f;
//
//    }
//
//    protected static class FIFOCache<K, V> {
//
//        private final int MAX_CACHE_SIZE = 5000;
//
//        LinkedHashMap<K, V> map;
//
//        public FIFOCache() {
//            map = new LinkedHashMap<K, V>(MAX_CACHE_SIZE) {
//                @Override
//                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
//                    return size() > MAX_CACHE_SIZE;
//                }
//            };
//        }
//
//        public synchronized void put(K key, V value) {
//            map.put(key, value);
//        }
//
//        public synchronized boolean contains(K key) {
//            return map.containsKey(key);
//        }
//
//        public synchronized V get(K key) {
//            return map.get(key);
//        }
//
//        public synchronized void remove(K key) {
//            map.remove(key);
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder stringBuilder = new StringBuilder();
//            for (Map.Entry<K, V> entry : map.entrySet()) {
//                stringBuilder.append(String.format("%s: %s  ", entry.getKey(), entry.getValue()));
//            }
//            return stringBuilder.toString();
//        }
//    }
//
//}
