package org.esa.s2tbx.dataio.s2.filepatterns;

import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2ProductNamingUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Created by obarrile on 07/11/2016.
 */
public class S2NamingConventionUtils {

    public static boolean matches(String filename, INamingConvention namingConvention) {

        for(String REGEX : namingConvention.getProductREGEXs()) {
            if (matches(filename,REGEX)) {
                return true;
            }
        }
        for(String REGEX : namingConvention.getProductXmlREGEXs()) {
            if (matches(filename,REGEX)) {
                return true;
            }
        }

        for(String REGEX : namingConvention.getGranuleREGEXs()) {
            if (matches(filename,REGEX)) {
                return true;
            }
        }

        for(String REGEX : namingConvention.getGranuleXmlREGEXs()) {
            if (matches(filename,REGEX)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(String filename, String REGEX) {
        Pattern pattern = Pattern.compile(REGEX);
        if (pattern.matcher(filename).matches()) {
            return true;
        }
        return false;
    }

    public static boolean matches(String filename, String[] REGEXs) {
        for(String REGEX : REGEXs) {
            Pattern pattern = Pattern.compile(REGEX);
            if (pattern.matcher(filename).matches()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasValidStructure(S2Config.Sentinel2InputType inputType, Path xmlPath) {
        if(inputType == S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA) {
            return S2ProductNamingUtils.checkStructureFromProductXml(xmlPath);
        }
        if(inputType == S2Config.Sentinel2InputType.INPUT_TYPE_GRANULE_METADATA) {
            return S2ProductNamingUtils.checkStructureFromGranuleXml(xmlPath);
        }
        return false;
    }

    public static Path getXmlFromDir(Path path, String PRODUCT_XML_REGEX, String GRANULE_XML_REGEX) {
        if(!Files.isDirectory(path)) {
            return null;
        }
        Pattern productPattern = Pattern.compile(PRODUCT_XML_REGEX);
        Pattern granulePattern = Pattern.compile(GRANULE_XML_REGEX);


        String[] listXmlFiles = path.toFile().list((f, s) -> s.endsWith(".xml"));
        String xmlFile = "";
        int availableXmlCount = 0;

        for(String xml : listXmlFiles) {
            if (productPattern.matcher(xml).matches() || granulePattern.matcher(xml).matches()) {
                xmlFile = xml;
                availableXmlCount++;
            }
        }

        if(availableXmlCount != 1) {
            return null;
        }

        return path.resolve(xmlFile);
    }

    /**
     * Searches the file from path that matches some REGEX.
     * If more than one file is found, returns null
     * @param path
     * @param REGEXs
     * @return
     */
    public static Path getFileFromDir(Path path, String[] REGEXs) {
        if(!Files.isDirectory(path)) {
            return null;
        }
        Pattern[] patterns = new Pattern[REGEXs.length];
        for(int i = 0 ; i < REGEXs.length ; i++) {
            patterns[i] = Pattern.compile(REGEXs[i]);
        }

        String[] listXmlFiles = path.toFile().list((f, s) -> s.endsWith(".xml"));
        String xmlFile = "";
        int availableXmlCount = 0;

        for(String xml : listXmlFiles) {
            for(Pattern pattern : patterns) {
                if (pattern.matcher(xml).matches() ) {
                    xmlFile = xml;
                    availableXmlCount++;
                    break;
                }
            }
        }

        if(availableXmlCount != 1) {
            return null;
        }

        return path.resolve(xmlFile);
    }

    public static ArrayList<Path> getDatastripXmlPaths(S2Config.Sentinel2InputType inputType, Path inputXml, String[] datastripREGEXs, String[] datastripXmlREGEXs) {
        ArrayList<Path> paths = new ArrayList<>();
        if(inputType.equals(S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA)){
            Path datastripPath = inputXml.resolveSibling("DATASTRIP");
            if(datastripPath == null) {
                return paths;
            }
            if(!Files.isDirectory(datastripPath)) {
                return paths;
            }
            for(File datastrip : datastripPath.toFile().listFiles()) {
                if(datastrip.isDirectory() && S2NamingConventionUtils.matches(datastrip.getName(),datastripREGEXs)){
                    Path xml = S2NamingConventionUtils.getFileFromDir(datastrip.toPath(), datastripXmlREGEXs);
                    if(xml != null) {
                        paths.add(xml);
                    }
                }
            }
        }
        return paths;
    }

    public static ArrayList<Path> getGranulesXmlPaths(S2Config.Sentinel2InputType inputType, Path inputXml, String[] granuleREGEXs, String[] granuleXmlREGEXs) {
        ArrayList<Path> paths = new ArrayList<>();
        if(inputType.equals(S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA)){
            Path granulePath = inputXml.resolveSibling("GRANULE");
            if(granulePath == null) {
                return paths;
            }
            if(!Files.isDirectory(granulePath)) {
                return paths;
            }
            for(File granule : granulePath.toFile().listFiles()) {
                if(granule.isDirectory() && S2NamingConventionUtils.matches(granule.getName(),granuleREGEXs)){
                    Path xml = S2NamingConventionUtils.getFileFromDir(granule.toPath(), granuleXmlREGEXs);
                    if(xml != null) {
                        paths.add(xml);
                    }
                }
            }
        }
        return paths;
    }

    public static Path getProductXmlFromGranuleXml(Path granuleXmlPath, String[] REGEXs) {
        Path productXml = null;
        try {
            Objects.requireNonNull(granuleXmlPath.getParent());
            Objects.requireNonNull(granuleXmlPath.getParent().getParent());
            Objects.requireNonNull(granuleXmlPath.getParent().getParent().getParent());

            Path up2levels = granuleXmlPath.getParent().getParent().getParent();

            if (up2levels == null) {
                return productXml;
            }
            return getFileFromDir(up2levels, REGEXs);
        } catch (NullPointerException npe) {
            return null;
        }
    }

}
