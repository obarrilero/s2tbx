package org.esa.s2tbx.dataio.s2.filepatterns;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String getSpectralBandImageTemplate (String format, String bandFileId) {
        if(format.equals("SAFE")) {
            return SAFENamingConvention.SPECTRAL_BAND_TEMPLATE.replace("{{BANDFILEID}}",bandFileId);
        } else if (format.equals("SAFE_COMPACT")) {
            return SAFECOMPACTNamingConvention.SPECTRAL_BAND_TEMPLATE.replace("{{BANDFILEID}}",bandFileId);
        }
        return null;
    }

    public static String getSpectralBandImageTemplate_L2a (String format, String bandFileId) {
        if(format.equals("SAFE")) {
            return SAFENamingConvention.SPECTRAL_BAND_TEMPLATE_L2A.replace("{{BANDFILEID}}",bandFileId);
        } else if (format.equals("SAFE_COMPACT")) {
            return SAFECOMPACTNamingConvention.SPECTRAL_BAND_TEMPLATE_L2A.replace("{{BANDFILEID}}",bandFileId);
        }
        return null;
    }

    public static String getAOTTemplate_L2a (String format) {
        if(format.equals("SAFE")) {
            return SAFENamingConvention.AOT_FILE_TEMPLATE_L2A;
        } else if (format.equals("SAFE_COMPACT")) {
            return SAFECOMPACTNamingConvention.AOT_FILE_TEMPLATE_L2A;
        }
        return null;
    }

    public static String getWVPTemplate_L2a (String format) {
        if(format.equals("SAFE")) {
            return SAFENamingConvention.WVP_FILE_TEMPLATE_L2A;
        } else if (format.equals("SAFE_COMPACT")) {
            return SAFECOMPACTNamingConvention.WVP_FILE_TEMPLATE_L2A;
        }
        return null;
    }

    public static String getSCLTemplate_L2a (String format) {
        if(format.equals("SAFE")) {
            return SAFENamingConvention.SCL_FILE_TEMPLATE_L2A;
        } else if (format.equals("SAFE_COMPACT")) {
            return SAFECOMPACTNamingConvention.SCL_FILE_TEMPLATE_L2A;
        }
        return null;
    }

    public static String getCLDTemplate_L2a (String format) {
        if(format.equals("SAFE")) {
            return SAFENamingConvention.CLD_FILE_TEMPLATE_L2A;
        } else if (format.equals("SAFE_COMPACT")) {
            return SAFECOMPACTNamingConvention.CLD_FILE_TEMPLATE_L2A;
        }
        return null;
    }

    public static String getSNWTemplate_L2a (String format) {
        if(format.equals("SAFE")) {
            return SAFENamingConvention.SNW_FILE_TEMPLATE_L2A;
        } else if (format.equals("SAFE_COMPACT")) {
            return SAFECOMPACTNamingConvention.SNW_FILE_TEMPLATE_L2A;
        }
        return null;
    }

    public static String getDDVTemplate_L2a (String format) {
        if(format.equals("SAFE")) {
            return SAFENamingConvention.DDV_FILE_TEMPLATE_L2A;
        } else if (format.equals("SAFE_COMPACT")) {
            return SAFECOMPACTNamingConvention.DDV_FILE_TEMPLATE_L2A;
        }
        return null;
    }


    public static String getGranuleFormat (Path path) {

        String filename = path.getFileName().toString();
        Pattern pattern = Pattern.compile(SAFECOMPACTNamingConvention.GRANULE_XML_REGEX);
        Matcher matcher = pattern.matcher(filename);
        if(matcher.matches()) {
            return "SAFE_COMPACT";
        }

        pattern = Pattern.compile(SAFENamingConvention.GRANULE_XML_REGEX);
        matcher = pattern.matcher(filename);
        if(matcher.matches()) {
            return "SAFE";
        }

        return null;
    }
    
}
