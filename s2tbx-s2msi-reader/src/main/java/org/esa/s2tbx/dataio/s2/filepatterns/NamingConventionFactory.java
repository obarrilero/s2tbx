package org.esa.s2tbx.dataio.s2.filepatterns;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by obarrile on 02/11/2016.
 */
public class NamingConventionFactory {

    //todo return array if more than one is possible??
    public static INamingConvention createNamingConvention(Path path)  {

        SAFENamingConvention safe = new SAFENamingConvention(path);
        if(safe.getInputType() != null){
            return safe;
        }

        SAFECOMPACTNamingConvention safeCompact = new SAFECOMPACTNamingConvention(path);
        if(safeCompact.getInputType() != null){
            return safeCompact;
        }

        return null;
    }
    
}
