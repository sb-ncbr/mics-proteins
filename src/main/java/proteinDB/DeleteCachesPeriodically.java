/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proteinDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Vlada
 */
@Deprecated
public class DeleteCachesPeriodically {

    private static final Logger LOG = Logger.getLogger(DeleteCachesPeriodically.class.getName());

    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) throws SQLException {
        while (true) {
            try {
                long t = getWaitingUntilMidnight();
                Connection db = DBGlobal.getConnectionFromIniFile();
                Statement statement = db.createStatement();
                LOG.log(Level.INFO, "Next delete of DB caches in " + (t / 1000 / 60) + " minutes, on midnight.");
                Thread.sleep(t);
                deleteCachedDistances(statement);
            } catch (InterruptedException ex) {
                Logger.getLogger(DeleteCachesPeriodically.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static long getWaitingUntilMidnight() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() - System.currentTimeMillis();
    }

    private static void deleteCachedDistances(Statement statement) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
