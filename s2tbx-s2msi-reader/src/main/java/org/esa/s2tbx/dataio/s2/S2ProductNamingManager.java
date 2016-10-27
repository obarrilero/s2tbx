package org.esa.s2tbx.dataio.s2;

import org.esa.s2tbx.dataio.s2.ortho.S2CRSHelper;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.math.Array;
import org.esa.snap.utils.FileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by W7 on 26/10/2016.
 */
public class S2ProductNamingManager {

    public static String[] EXCLUDED_XML = {"INSPIRE"};
    public static String TILE_ID_REGEX = "(.*)(T[0-9]{2}[A-Z]{3})(.*)";

    public static boolean checkStructureFromProductXml(Path xmlPath) {
        if(!Files.exists(xmlPath.resolveSibling("GRANULE"))) {
            return false;
        }

        ArrayList<Path> tileDirs = getTilesFromProductXml(xmlPath);
        if(tileDirs.isEmpty()) {
            return false;
        }

        for(Path tileDir : tileDirs) {
            if(getXmlFromDir(tileDir) == null) {
                return false;
            }
        }

        //TODO add more checkins
        return true;
    }

    public static boolean checkStructureFromGranuleXml(Path xmlPath) {
        if(!Files.exists(xmlPath.resolveSibling("IMG_DATA"))) {
            return false;
        }

        if(!Files.exists(xmlPath.resolveSibling("QI_DATA"))) {
            return false;
        }

        //TODO add more checkins
        return true;
    }

    public static ArrayList<Path> getTilesFromProductXml(Path xmlPath) {
        ArrayList<Path> tilePaths = new ArrayList<>();
        Path granuleFolder = xmlPath.resolveSibling("GRANULE");
        try {
            File[] granuleFiles = granuleFolder.toFile().listFiles();
            for(File granule : granuleFiles) {
                //TODO q cumpla tb regex con tileID
                if (granule.isDirectory()){
                    tilePaths.add(granuleFolder.resolve(granule.getName()));
                }
            }
        } catch (Exception e) {
        }
        return tilePaths;
    }

    public static ArrayList<Path> getDatastripsFromProductXml(Path xmlPath) {
        ArrayList<Path> datastripPaths = new ArrayList<>();
        Path datastripFolder = xmlPath.resolveSibling("DATASTRIP");
        try {
            File[] datastripFiles = datastripFolder.toFile().listFiles();
            for(File granule : datastripFiles) {
                if (granule.isDirectory()){
                    datastripPaths.add(datastripFolder.resolve(granule.getName()));
                }
            }
        } catch (Exception e) {
        }
        return datastripPaths;
    }

    public static Path getXmlFromDir(Path dirPath) {
        if(!Files.isDirectory(dirPath)) {
            return null;
        }
        String[] listXmlFiles = dirPath.toFile().list((f, s) -> s.endsWith(".xml"));
        String xmlFile = "";
        int availableXmlCount = 0;

        for(String xml : listXmlFiles) {
            boolean bExcluded = false;
            for(String excluded : EXCLUDED_XML) {
                if (xml.substring(0, xml.lastIndexOf(".xml")).equals(excluded)) {
                    bExcluded = true;
                    break;
                }
            }
            if(!bExcluded) {
                xmlFile = xml;
                availableXmlCount++;
            }
        }
        if(availableXmlCount != 1) {
            return null;
        }

        return dirPath.resolve(xmlFile);
    }

    public static String getTileIdFromString(String string) {
        Pattern pattern = Pattern.compile(TILE_ID_REGEX);
        Matcher matcher = pattern.matcher(string);
        if(!matcher.matches()) {
            return null;
        }
        return matcher.group(2);

    }



    public static S2Config.Sentinel2InputType getInputType(Path xmlPath) {
        if(checkStructureFromProductXml(xmlPath)) {
            return S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA;
        }
        if(checkStructureFromGranuleXml(xmlPath)) {
            return S2Config.Sentinel2InputType.INPUT_TYPE_GRANULE_METADATA;
        }
        return null;
    }

    public static S2Config.Sentinel2ProductLevel getLevel(Path xmlPath, S2Config.Sentinel2InputType inputType) {
        if(inputType.equals(S2Config.Sentinel2InputType.INPUT_TYPE_GRANULE_METADATA)) {
            return getLevelFromGranuleXml(xmlPath);
        } else if (inputType.equals(S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA)) {
            return getLevelFromProductXml(xmlPath);
        }
        return S2Config.Sentinel2ProductLevel.UNKNOWN;
    }

    public static S2Config.Sentinel2ProductLevel getLevelFromGranuleXml(Path xmlPath) {
        String filename = xmlPath.getFileName().toString();
        S2Config.Sentinel2ProductLevel level = getLevelFromString(filename);
        if(level != S2Config.Sentinel2ProductLevel.UNKNOWN) {
            return level;
        }

        Path parentPath = xmlPath.getParent();
        if(parentPath == null) {
            return level;
        }

        filename = parentPath.getFileName().toString();
        level = getLevelFromString(filename);
        return level;
    }

    public static S2Config.Sentinel2ProductLevel getLevelFromProductXml(Path xmlPath) {
        String filename = xmlPath.getFileName().toString();
        S2Config.Sentinel2ProductLevel level = getLevelFromString(filename);
        if(level != S2Config.Sentinel2ProductLevel.UNKNOWN) {
            return level;
        }

        Path parentPath = xmlPath.getParent();
        if(parentPath == null) {
            return level;
        }

        filename = parentPath.getFileName().toString();
        level = getLevelFromString(filename);
        if(level != S2Config.Sentinel2ProductLevel.UNKNOWN) {
            return level;
        }

        for(Path tile : getTilesFromProductXml(xmlPath)) {
            Path xmlGranule = getXmlFromDir(tile);
            level = getLevelFromGranuleXml(xmlGranule);
            if(level != S2Config.Sentinel2ProductLevel.UNKNOWN) {
                return level;
            }
        }

        return level;
    }

    public static  S2Config.Sentinel2ProductLevel getLevelFromString(String string) {
        if(string.contains("L1C")) {
            return S2Config.Sentinel2ProductLevel.L1C;
        }
        if(string.contains("L2A")) {
            return S2Config.Sentinel2ProductLevel.L2A;
        }
        if(string.contains("L3")) {//comprobar
            return S2Config.Sentinel2ProductLevel.L3;
        }
        if(string.contains("L1B")) {
            return S2Config.Sentinel2ProductLevel.L1B;
        }
        return S2Config.Sentinel2ProductLevel.UNKNOWN;
    }

    public static Set<String> getEpsgCodeList(Path xmlPath, S2Config.Sentinel2InputType inputType) {
        Set<String> epsgCodeList = new HashSet<>();

        if(inputType == S2Config.Sentinel2InputType.INPUT_TYPE_GRANULE_METADATA) {
            String epsg = getEpsgCodeFromGranule(xmlPath);
            if(epsg != null) {
                epsgCodeList.add(epsg);
            }
        } else if(inputType == S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA) {
            for(Path tile : getTilesFromProductXml(xmlPath)) {
                Path xmlGranule = getXmlFromDir(tile);
                String epsg = getEpsgCodeFromGranule(xmlGranule);
                if(epsg != null) {
                    epsgCodeList.add(epsg);
                }
            }
        }


        return epsgCodeList;
    }

    public static String getEpsgCodeFromGranule(Path xmlPath) {
        String epsgCode = null;

        String tileId = getTileIdFromString(xmlPath.getFileName().toString());
        if(tileId == null && xmlPath.getParent() != null) {
            tileId = getTileIdFromString(xmlPath.getParent().getFileName().toString());
        }

        if(tileId != null) {
            epsgCode = S2CRSHelper.tileIdentifierToEPSG(tileId);
        }
        return epsgCode;
    }

    public static String getImageTemplate (String string) {
        //change the tileId by {{TILENUMBER}} and the band by {{BANDID}}
        string.replaceAll("(T[0-9]{2}[A-Z]{3})","{{TILENUMBER}}");
        string.replaceAll("(B[A-Z|0-9]{2})","{{BANDID}}");
        return string;
    }

}
