/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.util.Collections;
import java.util.List;
import main.tools.Tools;
import main.tools.recallEvaluationFromJSON.KNNResults;
import messif.data.DataObject;

/**
 *
 * @author Vlada
 */
public class GtAnalysis {

    public static void main(String[] args) {
        String groundTruthPath = "c:\\Datasets\\proteins\\490000dataset\\ground_truth.json";
        KNNResults groundTruth = new KNNResults(groundTruthPath, 100000, -1);
        float[] toSearchFor = new float[6];
        for (int i = 0; i < toSearchFor.length; i++) {
            toSearchFor[i] = 0.1f * i;
        }
        List<DataObject> evaluatedQueries = groundTruth.getEvaluatedQueries();
        DataObject mostRepresentativeQuery = null;
        float minDiff = Float.MAX_VALUE;
        float[] bestDistsForPrint = new float[toSearchFor.length];
        String[] bestIDsForPrint = new String[toSearchFor.length];
        for (DataObject evaluatedQuery : evaluatedQueries) {
            float[] dists = evaluatedQuery.getField("answer_distances", float[].class);
            DataObject[] nn = evaluatedQuery.getField("answer_records", DataObject[].class);
            float[] diffsFromDesired = new float[toSearchFor.length];
            float[] bestDists = new float[toSearchFor.length];
            String[] bestNN = new String[toSearchFor.length];
            for (int i = 0; i < diffsFromDesired.length; i++) {
                diffsFromDesired[i] = Float.MAX_VALUE;
            }
            for (int i = 0; i < dists.length; i++) {
                float dist = dists[i];
                for (int j = 0; j < diffsFromDesired.length; j++) {
                    float dif = Math.abs(toSearchFor[j] - dist);
                    if (dif < diffsFromDesired[j]) {
                        diffsFromDesired[j] = dif;
                        bestDists[j] = dist;
                        bestNN[j] = nn[i].getID();
                    }
                }
            }
            float totalDif = 0;
            for (int i = 0; i < diffsFromDesired.length; i++) {
                totalDif += diffsFromDesired[i];
            }
            if (totalDif < minDiff) {
                minDiff = totalDif;
                mostRepresentativeQuery = evaluatedQuery;
                System.arraycopy(bestDists, 0, bestDistsForPrint, 0, bestDistsForPrint.length);
                System.arraycopy(bestNN, 0, bestIDsForPrint, 0, bestIDsForPrint.length);
            }
            System.err.print("Total diff: " + totalDif + ";");
            System.err.print("Query candidate: " + evaluatedQuery.getField("queryObj_id") + ";");
            System.err.print("Average: " + (minDiff / toSearchFor.length) + ";");
            System.err.print("Dists of NN:" + ";");
            Tools.printArray(bestDists, ";");
            System.err.print("IDs of NN:" + ";");
            Tools.printArray(bestNN, ";");
            System.err.println();
        }
        System.err.println("BEST query: " + mostRepresentativeQuery.getField("queryObj_id"));
        System.err.println("Total diff: " + minDiff);
        System.err.println("Average: " + (minDiff / toSearchFor.length));
        System.err.println("Dists of NN:");
        Tools.printArray(bestDistsForPrint, "\n");
        System.err.println("IDs of NN:");
        Tools.printArray(bestIDsForPrint, "\n");
    }

}
