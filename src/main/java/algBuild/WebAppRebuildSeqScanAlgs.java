/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algBuild;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import messif.algorithm.Algorithm;
import messif.algorithm.impl.SequentialScan;
import messif.data.DataObject;
import messif.data.util.DataObjectIterator;
import messif.distance.DataObjectDistanceFunc;
import messif.distance.DistanceFunc;
import messif.distance.impl.HammingDistanceLongs;
import messif.operation.OperationBuilder;
import messif.operation.crud.CRUDOperation;
import messif.operation.crud.InsertOperation;
import proteinDB.ChainTable;
import proteinDB.DBGlobal;

/**
 *
 * @author Vlada
 */
public class WebAppRebuildSeqScanAlgs {

    public static void main(String[] args) {
//        String pathToShortSketchesAgs = "";
//        String pathToLongSketchesAgs = "";
        String pathToShortSketchesAgs = args[0];
        String pathToLongSketchesAgs = args[1];
        Connection db = DBGlobal.getConnectionFromIniFile(args[2]);
        DataObjectIterator it = ChainTable.getIteratorOverSketchesToBeIndexedFromDB(db, null);
        List<DataObject> shortSketches = new ArrayList<>();
        List<DataObject> longSketches = new ArrayList<>();
        for (int i = 0; it.hasNext(); i++) {
            DataObject proteinChain = it.next();
            DataObject shortSketch = Tools.getSubObject(proteinChain, "sk192_long");
            DataObject longSketch = Tools.getSubObject(proteinChain, "sk1024_long");
            shortSketches.add(shortSketch);
            longSketches.add(longSketch);
            if (i % 10000 == 0) {
                Logger.getLogger(WebAppRebuildSeqScanAlgs.class.getName()).log(Level.INFO, "Loaded {0} sketches from DB", i);
            }
        }
        buildAndStoreAlg(shortSketches, "sk192_long", pathToShortSketchesAgs);
        buildAndStoreAlg(longSketches, "sk1024_long", pathToLongSketchesAgs);
    }

    @SuppressWarnings("FinalizeCalledExplicitly")
    private static void buildAndStoreAlg(List<DataObject> sketchesList, String fieldName, String pathToStore) {
        try {
            File file = new File(pathToStore);
            file.delete();
            DataObject[] sketches = Tools.collectionToArray(sketchesList);
            DistanceFunc distFunc = new DataObjectDistanceFunc(fieldName, new HammingDistanceLongs());

            Algorithm alg = new SequentialScan(distFunc);
            alg.init();

            final InsertOperation insertOp = OperationBuilder.create(InsertOperation.class)
                    .addParam(CRUDOperation.OBJECTS_FIELD, sketches)
                    .checkFields(true).build();
            alg.evaluate(insertOp);

            alg.storeToFile(pathToStore);
            alg.finalize();
        } catch (Throwable ex) {
            Logger.getLogger(WebAppRebuildSeqScanAlgs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
