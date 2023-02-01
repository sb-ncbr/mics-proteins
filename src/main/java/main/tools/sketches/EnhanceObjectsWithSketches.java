/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tools.sketches;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import main.tools.Tools;
import messif.data.DataObject;
import messif.distance.DistanceFunc;
import messif.utility.json.JSONException;
import sf.distance.impl.CsvFilesDistanceFunction;
import sf.objects.sketchcreator.ObjectToSketchTransformator;
import sf.objects.sketchcreator.impl.GenHyperplane;

/**
 *
 * @author xmic
 */
@Deprecated //Use LearnAndAddSketchesToObjects or AddSketchesToObjectsWithLearnedPivots instead
public class EnhanceObjectsWithSketches {

    public static final Integer PIVOTS_COUNT = 512;
    private static DataObject[] pivots;
    private static DistanceFunc distanceFunction;
    private static ObjectToSketchTransformator[] transformators;

    /**
     * Class for adding sketches of more lengths to objects. Requires all csv
     * files for sketchingtechnique created in advance.
     *
     * @param args
     * @throws FileNotFoundException
     * @throws JSONException
     * @throws IOException
     */
    public static void main(String[] args) throws FileNotFoundException, JSONException, IOException {
        int count = 1000;
        boolean addDists = true;
        String dataPath = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\queries_1000.json";
//        String dataPath = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\ids.json";
        String objNewName = "proteinObj";

//        String output = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\queries_1000_withSketches.json";
        String output = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\queries_1000_withSketches_withDists.json";

//        String output = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\ids_withSketches.json";
//        String output = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\ids_withSketches_withDists.json";
//        String output = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\pivots.json";
        int[] skLengths = new int[]{64, 128, 192, 256, 320, 384, 448, 512};
        init(skLengths);

        EnhanceDataObjectsWithSketches engine = new EnhanceDataObjectsWithSketches(objNewName, dataPath, pivots, transformators);
        engine.run(output, addDists, count);
    }

    private static void init(int[] skLengths) {
        transformators = new ObjectToSketchTransformator[skLengths.length];
        String idsFile = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\ids.json";
        String matrixCsvFile = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\dist_matrix.txt";
        List<DataObject> sampleObjects = Tools.getObjects(idsFile);
        pivots = new DataObject[PIVOTS_COUNT];
        pivots = sampleObjects.subList(0, 512).toArray(pivots);
        distanceFunction = new CsvFilesDistanceFunction(idsFile, matrixCsvFile);
        for (int i = 0; i < skLengths.length; i++) {
            String csvFilePath = "c:\\Data\\2019\\2019_Proteiny\\Transformace_sketches_22K_dat\\Pivots\\Pivots_" + skLengths[i] + "b.csv";
            transformators[i] = new GenHyperplane(distanceFunction, pivots, csvFilePath);
        }
    }

}
