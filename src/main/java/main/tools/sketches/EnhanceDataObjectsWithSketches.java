/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tools.sketches;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import tools.ToolsProteins;
import messif.data.DataObject;
import messif.utility.json.JSONException;
import messif.utility.json.JSONWriter;
import proteinDB.DBGlobal;
import sf.objects.sketchcreator.ObjectToSketchTransformator;

/**
 *
 * @author xmic
 */
public class EnhanceDataObjectsWithSketches {

    private final String objNewName;
    private final List<DataObject> data;
    private final DataObject[] allPivots;
    private final ObjectToSketchTransformator[] transformators;

    public EnhanceDataObjectsWithSketches(String objNewName, List<DataObject> data, DataObject[] allPivots, ObjectToSketchTransformator[] transformators) {
        this.objNewName = objNewName;
        this.data = data;
        this.allPivots = allPivots;
        this.transformators = transformators;
    }

    public EnhanceDataObjectsWithSketches(String objNewName, String dataPath, DataObject[] allPivots, ObjectToSketchTransformator[] transformators) {
        this.objNewName = objNewName;
        this.data = Tools.getObjects(dataPath);
        this.transformators = transformators;
        this.allPivots = allPivots;
    }

    public void run(String output, boolean addDistances, int count) {
        if (output != null) {
            try {
                System.setOut(new PrintStream(output));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(EnhanceDataObjectsWithSketches.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (count < 0) {
            count = Integer.MAX_VALUE;
        }
        try (JSONWriter jsonWriter = new JSONWriter(System.out)) {
            Connection db = DBGlobal.getConnectionFromIniFile(null);
            Statement st = db.createStatement();
            for (int i = 0; i < Math.min(count, data.size()); i++) {
                DataObject obj = data.get(i);
                DataObject objWithSketch = ToolsProteins.wrapProteinObject(objNewName, obj, true, st);
                if (addDistances) {
                    objWithSketch = Tools.addPrecomputedDistancesParallel(objWithSketch, obj, allPivots, transformators[0].getDistanceFunc(), null);
                }
                for (int j = 0; j < transformators.length; j++) {
                    objWithSketch = transformators[j].addSketchDataObject(objWithSketch, obj, !addDistances);
                }
                objWithSketch.writeJSON(jsonWriter);
                jsonWriter.write('\n');
            }
            jsonWriter.flush();
            st.close();
            db.close();
        } catch (JSONException | IOException ex) {
            Logger.getLogger(EnhanceDataObjectsWithSketches.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(EnhanceDataObjectsWithSketches.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
