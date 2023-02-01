/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tools.tois.storingDB;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import messif.data.DataObject;
import messif.distance.DistanceFunc;
import messif.distance.impl.ProteinDistance;
import proteinDB.CSVPivotPairsTables;
import proteinDB.ChainTable;
import proteinDB.DBGlobal;
import proteinDB.MetadataTable;
import proteinDB.PivotTables;
import sf.objects.sketchcreator.ObjectToSketchTransformator;
import sf.objects.sketchcreator.impl.GenHyperplane;

/**
 * First step for TOIS paper's secondary filtering with sketches.
 *
 * @author xmic
 */
public class AddMetadataToData {

    public static void main(String[] args) throws FileNotFoundException {
        String iniFilePath = args[0];

        Integer paralelisation = 8;
        if (args.length > 1) {
            paralelisation = Integer.parseInt(args[1]);
        }
        Integer pivotSetId = null;
        if (args.length > 2) {
            pivotSetId = Integer.parseInt(args[2]);
        }

        DistanceFunc distanceFunc = new ProteinDistance(DBGlobal.getLibraryPathFromIniFile(iniFilePath), 0f);
        evaluateAndAddDistancesAndSketches(distanceFunc, paralelisation, pivotSetId, iniFilePath);
    }

    private static void evaluateAndAddDistancesAndSketches(DistanceFunc distanceFunc, Integer paralelisation, Integer pivotSetId, String iniFilePath) {
        try {
            Connection db = DBGlobal.getConnectionFromIniFile();
            List<DataObject> data = Tools.getObjectsByIterator(ChainTable.getIteratorOfChainsWithoutMetadata(db, pivotSetId));
            Logger.getLogger(AddMetadataToData.class.getName()).log(Level.INFO, "Found {0} protein chains that shoud be updated", new Object[]{data.size()});
            if (data.isEmpty()) {
                return;
            }
//            DataObject[] pivots = ToolsProteins.wrapProteinObjects(ProteinDistance.ENCAPSULATED_PROTEIN_NAME, Tools.getObjectsArray(pivotPath, -1), true);
            int pivotSetIdActive = pivotSetId == null ? PivotTables.getActivePivotSet(db) : pivotSetId;
            DataObject[] pivots = Tools.collectionToArray(PivotTables.getPivotsFromDB(db, true, false, pivotSetIdActive));

            String csv512 = CSVPivotPairsTables.createTemporarCSVPivotPairsFile(db, true, pivotSetIdActive);
            String csv64 = CSVPivotPairsTables.createTemporarCSVPivotPairsFile(db, false, pivotSetIdActive);
            final ObjectToSketchTransformator tr512 = new GenHyperplane(distanceFunc, pivots, csv512);
            final ObjectToSketchTransformator tr64 = new GenHyperplane(distanceFunc, pivots, csv64);

            ExecutorService threadPool = Tools.initExecutor(paralelisation);
            CountDownLatch latch = new CountDownLatch(data.size());
            for (int i = 0; i < data.size(); i++) {
                final int index = i;
                DataObject obj = data.get(i);
                threadPool.execute(() -> {
                    DataObject objI = obj;
                    objI = Tools.addPrecomputedDistances(objI, objI, pivots, distanceFunc);
                    objI = tr512.addSketchToDataObject(objI, objI);
                    objI = tr64.addSketchToDataObject(objI, objI);
                    MetadataTable.insertOrUpdateMetadata(db, objI, pivotSetIdActive);
                    latch.countDown();
                    int remains = data.size() - index;
                    Logger.getLogger(AddMetadataToData.class.getName()).log(Level.INFO, "\n\n\n\nRemains {0} chains to process\n\n\n", new Object[]{remains});
                });
            }
            latch.await();
            threadPool.shutdown();
        } catch (InterruptedException ex) {
            Logger.getLogger(AddMetadataToData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
