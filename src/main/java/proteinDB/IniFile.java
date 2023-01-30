/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Vlada
 */
public class IniFile {

    private final Pattern section = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
    private final Pattern keyValue = Pattern.compile("\\s*([^=]*)=(.*)");
    private final Map< String, Map< String, String>> entries;

    public IniFile(String path) {
        this.entries = new HashMap<>();
        load(path);
    }

    public final void load(String path) {
        if (path == null) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            String sectionString = null;
            while ((line = br.readLine()) != null) {
                Matcher m = section.matcher(line);
                if (m.matches()) {
                    sectionString = m.group(1).trim();
                } else if (sectionString != null) {
                    m = keyValue.matcher(line);
                    if (m.matches()) {
                        String key = m.group(1).trim();
                        String value = m.group(2).trim();
                        Map< String, String> kv = entries.get(sectionString);
                        if (kv == null) {
                            entries.put(sectionString, kv = new HashMap<>());
                        }
                        kv.put(key, value);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(IniFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getString(String section, String key, String defaultvalue) {
        Map< String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        String ret = kv.get(key);
        return ret == null ? defaultvalue : ret;
    }

    public int getInt(String section, String key, int defaultvalue) {
        Map< String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Integer.parseInt(kv.get(key));
    }

    public float getFloat(String section, String key, float defaultvalue) {
        Map< String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Float.parseFloat(kv.get(key));
    }

    public double getDouble(String section, String key, double defaultvalue) {
        Map< String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Double.parseDouble(kv.get(key));
    }
}
