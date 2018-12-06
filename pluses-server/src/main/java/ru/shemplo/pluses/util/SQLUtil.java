package ru.shemplo.pluses.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ru.shemplo.pluses.db.MySQLAdapter;
import ru.shemplo.pluses.log.Log;

public class SQLUtil {
 
    public static String makeInsertQuery (String table, Map <String, String> params) {
        MySQLAdapter adapter = MySQLAdapter.getInstance ();
        Set <String> paramKeys = params.keySet (),
                     columns = adapter.getTableColumns (table);
        if (!Objects.isNull (columns)) {
            paramKeys.retainAll (columns);  
        } else {
            Log.warning (SQLUtil.class.getSimpleName (), 
                "List of columns for `" + table + "` is empty");
        }
        
        List <String> paramLine = new ArrayList <> (paramKeys),
            valueLine = paramLine.stream ()
                .map (params::get).collect (Collectors.toList ());
        String paramArray = SQLUtil.toArray (paramLine, '`'),
            valueArray = SQLUtil.toArray (valueLine, '\'');
        
        String queryTemplate = "INSERT INTO `%s` %s VALUES %s";
        return String.format (queryTemplate, table, paramArray, valueArray);
    }
    
    public static String toArray (Iterable <?> iterable, char bound) {
        Iterator <?> iterator = iterable.iterator ();
        StringBuilder sb = new StringBuilder ("(");
        while (iterator.hasNext ()) {
            Object object = iterator.next ();
            sb.append (bound);
            sb.append ("" + object);
            sb.append (bound);
            if (iterator.hasNext ()) {
                sb.append (", ");
            }
        }
        sb.append (")");
        
        return sb.toString ();
    }
    
    private static DateFormat DATETIME_FORMAT = new SimpleDateFormat ("YYYY-LL-dd HH:mm:ss");
    
    public static String getDatetime (long time) {
        return DATETIME_FORMAT.format (new Date (time));
    }
    
    public static String getDatetime () {
        return getDatetime (System.currentTimeMillis ());
    }
    
}
