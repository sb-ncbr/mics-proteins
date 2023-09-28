/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import messif.data.DataObject;
import messif.data.util.DataObjectIterator;
import messif.distance.impl.ProteinDistance;
import messif.utility.json.JSONReader;

/**
 *
 * @author Vlada
 */
public class ChainTable {

    public static final Logger LOG = Logger.getLogger(ChainTable.class.getName());

    public static void main(String[] args) throws FileNotFoundException {
        // test natahování iterátorem data z DB jako RecordImpl
        Connection db = DBGlobal.getConnectionFromIniFile();
        DataObjectIterator it = getIteratorOverCompleteChainsToBeIndexedFromDB(db);
//        DataObjectList list = new DataObjectList(it);
        String path = "c:\\Datasets\\proteins\\490000dataset\\dataset\\sketches\\512\\results_512_sk1024b_fromDB_check.json";
        System.setOut(new PrintStream(path));
        Tools.writeDataObjectsIterator(it);
    }

    public static Map<String, Integer> selectChainSizes(Statement st) {
        return selectChainSizes(st, true);
    }

    public static Map<String, Integer> selectChainSizes(Statement st, boolean onlySearchedOnes) {
        Map<String, Integer> ret = new HashMap<>();
        try {
            String sql = "SELECT gesamtId, chainLength FROM proteinChain";
            if (onlySearchedOnes) {
                sql += " WHERE indexedAsDataObject=1";
            }
            LOG.log(Level.INFO, "Executed {0};", new Object[]{sql});
            ResultSet res = st.executeQuery(sql);
            for (int counter = 1; res.next(); counter++) {
                String key = res.getString("gesamtId");
                int size = res.getInt("chainLength");
                ret.put(key, size);
            }
            return ret;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static Map<String, String> idmap = null;

    public static String selectChainIntID(Statement st, String gesamtId) {
        if (idmap == null) {
            idmap = new HashMap<>();
            try {
                String sql = "SELECT gesamtId, intId FROM proteinChain";
                LOG.log(Level.INFO, "Executed {0};", new Object[]{sql});
                ResultSet res = st.executeQuery(sql);
                for (int counter = 1; res.next(); counter++) {
                    String key = res.getString("gesamtId");
                    String intId = res.getString("intId");
                    idmap.put(key, intId);
                    if (counter % 100000 == 0) {
                        LOG.log(Level.INFO, "Loaded {0} IDs", idmap.size());
                    }
                }
            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        if (!idmap.containsKey(gesamtId)) {
            LOG.log(Level.SEVERE, "Map does not contain ID {0}", gesamtId);
            return "-1";
        }
        return idmap.get(gesamtId);
    }

    public static DataObjectIterator getIteratorOverCompleteChainsToBeIndexedFromDB(Connection db) {
        return getIteratorOverChainsToBeIndexedFromDB(db, true, 0, false, null);
    }

//    public static DataObjectIterator getIteratorOverChainsToBeIndexedFromDB(Connection db, boolean withPrecomputedDistsToPivots, int withSketches, Integer pivotSetId) {
//        return getIteratorOverChainsToBeIndexedFromDB(db, withPrecomputedDistsToPivots, withSketches, true, pivotSetId);
//    }
    public static DataObjectIterator getIteratorOfChainsWithoutMetadata(Connection db, Integer pivotSetId) {
        return getIteratorOverChainsToBeIndexedFromDB(db, false, 0, true, pivotSetId);
    }

    /**
     *
     * @param db
     * @param withPrecomputedDistsToPivots flag whether include
     * @param withSketches -1 to select riws without long sketches, 1 to select
     * rows with long sketches, 0 to ignore parameter
     * @param plusAllNew true to include all rows updated afted metadata
     * eveluated (metadata = pivot dists and sketches)
     * @param pivotSetId null to take active (pivotSet.currentlyUsed = 1, opr
     * specify.
     * @return
     */
    public static DataObjectIterator getIteratorOverChainsToBeIndexedFromDB(Connection db, boolean withPrecomputedDistsToPivots, int withSketches, boolean plusAllNew, Integer pivotSetId) {
        try {
            Statement st = db.createStatement();
            String pivotSetCondition = "p.currentlyUsed=1";
            if (pivotSetId != null) {
                pivotSetCondition = "p.id = " + pivotSetId;
            }
            String pivotDistsCond = withPrecomputedDistsToPivots ? "pivotDistances IS NOT NULL AND " + pivotSetCondition : "pivotDistances IS NULL";
            String longSketchCond = "1";
            if (withSketches == 1) {
                longSketchCond = "AND sketch512p IS NOT NULL";
            }
            if (withSketches == -1) {
                longSketchCond = "AND sketch512p IS NULL";
            }
            String plusAllNewCond = plusAllNew ? " c.added > m.lastUpdate AND " + pivotSetCondition : "0";
            String sql = "SELECT gesamtId, intId, pivotDistances, sketch512p, sketch64p FROM (proteinChain c LEFT OUTER JOIN proteinChainMetadata m ON c.intId=m.chainIntId) LEFT OUTER JOIN pivotSet p ON m.pivotSetId = p.id"
                    + " WHERE (( " + pivotDistsCond + ") OR (" + plusAllNewCond + ")) AND " + longSketchCond + " AND c.indexedAsDataObject=1";
            LOG.log(Level.INFO, "Executed\n{0};", new Object[]{sql});
            ResultSet res = st.executeQuery(sql);
            LOG.log(Level.INFO, "Finished");
            return new DBChainIterator(res);
        } catch (SQLException | IllegalArgumentException | IOException ex) {
            Logger.getLogger(ChainTable.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static DataObjectIterator getIteratorOverSketchesToBeIndexedFromDB(Connection db, Integer pivotSetId) {
        try {
            Statement st = db.createStatement();
            String pivotSetCondition = "p.currentlyUsed=1";
            if (pivotSetId != null) {
                pivotSetCondition = "p.id = " + pivotSetId;
            }
            String sql = "SELECT NULL as gesamtId, chainIntId AS intId, NULL as pivotDistances, sketch512p, sketch64p FROM (proteinChainMetadata m NATURAL JOIN pivotSet p) INNER JOIN proteinChain c ON c.intId=m.chainIntId WHERE " + pivotSetCondition + " AND m.sketch512p IS NOT NULL AND c.indexedAsDataObject=1";
            LOG.log(Level.INFO, "Executed\n{0};", new Object[]{sql});
            ResultSet res = st.executeQuery(sql);
            LOG.log(Level.INFO, "Finished");
            return new DBChainIterator(res);
        } catch (SQLException | IllegalArgumentException | IOException ex) {
            Logger.getLogger(ChainTable.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static class DBChainIterator extends DataObjectIterator {

        /**
         * Instance of a next object. This is needed for implementing reading
         * objects from a stream
         */
        protected DataObject nextObject;
        /**
         * Instance of the current object
         */
        protected DataObject currentObject;
        protected ResultSet set;

        /**
         * Number of objects read from the stream
         */
        protected int objectsRead;

        public DBChainIterator(ResultSet set) throws IllegalArgumentException, IOException {
            this.set = set;
            this.nextObject = nextStreamObject();
        }

        private DataObject nextStreamObject() {
            try {
                set.next();
                String gesamtId = set.getString("gesamtId");
                String id = set.getString("intId");
                String pivotDistances = set.getString("pivotDistances");
                String sketch512p = set.getString("sketch512p");
                String sketch64p = set.getString("sketch64p");

                DataObject obj = Tools.getNewDataObjectWithId(id);
                if (pivotDistances != null) {
                    DataObject pivotDistsRecord = (DataObject) JSONReader.readObjectFrom(pivotDistances, true);
                    obj = DataObject.addField(obj, "dists", pivotDistsRecord.getField("dists"));
                }
                if (sketch512p != null) {
                    DataObject sketch512pRecord = (DataObject) JSONReader.readObjectFrom(sketch512p, true);
                    obj = DataObject.addField(obj, "sk1024_long", sketch512pRecord.getField("sk1024_long"));
                }
                if (sketch64p != null) {
                    DataObject sketch64pRecord = (DataObject) JSONReader.readObjectFrom(sketch64p, true);
                    obj = DataObject.addField(obj, "sk192_long", sketch64pRecord.getField("sk192_long"));
                }
                if (gesamtId != null) {
                    DataObject protein = Tools.getNewDataObjectWithId(gesamtId);
                    obj = DataObject.addField(obj, ProteinDistance.ENCAPSULATED_PROTEIN_NAME, protein);
                }
                return obj;
            } catch (SQLException ex) {
                try {
                    set.close();
                } catch (SQLException ex1) {
                    Logger.getLogger(ChainTable.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            return null;
        }

        @Override
        public DataObject getCurrentObject() throws NoSuchElementException {
            if (currentObject == null) {
                throw new NoSuchElementException("Can't execute getCurrentObject() before first execute to next()");
            }
            return currentObject;
        }

        @Override
        public boolean hasNext() {
            return nextObject != null;
        }

        @Override
        public DataObject next() throws NoSuchElementException, IllegalArgumentException, IllegalStateException {
            // No next object available
            if (nextObject == null) {
                throw new NoSuchElementException("No more objects in the stream");
            }

            // Reading object on the fly from a stream
            currentObject = nextObject;
            nextObject = nextStreamObject();
            return currentObject;
        }

    }

}
