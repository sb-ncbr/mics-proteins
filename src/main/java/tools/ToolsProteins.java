package tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.tools.Tools;
import messif.data.DataObject;
import messif.distance.impl.ProteinDistance;
import static messif.distance.impl.ProteinDistance.ENCAPSULATED_PROTEIN_NAME;
import messif.record.RecordImpl;
import proteinDB.ChainTable;
import proteinDB.DBGlobal;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author xmic
 */
public class ToolsProteins {

    public static void main(String args[]) throws FileNotFoundException {
//        String path = "c:\\Datasets\\proteins\\490000dataset\\dataset\\data.json";
//        DataObject[] objs = Tools.getObjectsArray(path);
//        System.setOut(new PrintStream(path + ".intIDs"));
//        DataObject[] wrapProteinObjects = ToolsProteins.wrapProteinObjects(ProteinDistance.ENCAPSULATED_PROTEIN_NAME, objs, true);
//        Tools.writeDataObjects(wrapProteinObjects);
        //folder
        String folder = "c:\\Datasets\\proteins\\490000dataset\\queries\\";
//        String folder = args[0];
        File[] listFiles = new File(folder).listFiles((File dir, String name) -> name.endsWith("json"));
        for (File file : listFiles) {
            String path = file.getAbsolutePath();
            System.setOut(new PrintStream(path + ".intIDs"));
            DataObject[] objs = Tools.getObjectsArray(path);
            DataObject[] wrapProteinObjects = ToolsProteins.wrapProteinObjects(ProteinDistance.ENCAPSULATED_PROTEIN_NAME, objs, true);
            Tools.writeDataObjects(wrapProteinObjects);
        }
    }

    public static DataObject wrapProteinObject(String objName, DataObject obj, boolean transformKeyToInt, Statement st) {
        Map<String, Object> map = new HashMap<>();
        String id = obj.getID();
        if (transformKeyToInt) {
            id = ChainTable.selectChainIntID(st, id);
        }
        map.put(DataObject.ID_FIELD, id);
        map.put(objName, obj.getMap());
        return new RecordImpl(map);
    }

    public static DataObject[] wrapProteinObjects(String objName, DataObject[] objs, boolean hashKey) {
        try {
            DataObject[] ret = new DataObject[objs.length];
            Connection db = DBGlobal.getConnectionFromIniFile(null);
            Statement st = db.createStatement();
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ToolsProteins.wrapProteinObject(objName, objs[i], hashKey, st);
            }
            st.close();
            db.close();
            return ret;
        } catch (SQLException ex) {
            Logger.getLogger(ToolsProteins.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static DataObject wrapProteinObject(String id, String iniPath, boolean transformKeyToInt) {
        try {
            DataObject obj = Tools.getNewDataObjectWithId(id);
            Connection db = DBGlobal.getConnectionFromIniFile(iniPath);
            Statement st = db.createStatement();
            DataObject ret = wrapProteinObject(ENCAPSULATED_PROTEIN_NAME, obj, transformKeyToInt, st);
            st.close();
            db.close();
            return ret;
        } catch (SQLException ex) {
            Logger.getLogger(ToolsProteins.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

//    public static void recreateIDs(Iterator<DataObject> objects) {
//        while (objects.hasNext()) {
//            DataObject obj = objects.next();
//            RecordImpl record = obj.getField(ENCAPSULATED_PROTEIN_NAME, RecordImpl.class);
//            if (record == null) {
//                continue;
//            }
//            String proteinId = record.getID();
//            String newId = transformIdToInt(proteinId, false);
//            Map<String, Object> map = new HashMap<>(obj.getMap());
//            map.put("_id", newId);
//            RecordImpl newObj = new RecordImpl(map);
//            String toJSONString = newObj.toJSONString();
//            System.out.println(toJSONString);
//        }
//        saveGlobalIdMap();
//    }
}
