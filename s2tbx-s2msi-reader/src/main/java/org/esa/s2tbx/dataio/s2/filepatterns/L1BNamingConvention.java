package org.esa.s2tbx.dataio.s2.filepatterns;

import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2ProductNamingManager;
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;
import org.esa.snap.core.util.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by obarrile on 08/11/2016.
 */
public class L1BNamingConvention implements INamingConvention {
    public static String PRODUCT_REGEX = "(S2A|S2B|S2_)_([A-Z|0-9]{4})_([A-Z|0-9|_]{4})([A-Z|0-9|_]{3})L1B_([A-Z|0-9|_]{4})_([0-9]{8}T[0-9]{6})_R([0-9]{3}).*";
    public static String PRODUCT_COMPACT_REGEX = "(S2A|S2B|S2_)_([A-Z|0-9|_]{3})L1B_([0-9]{8}T[0-9]{6})_N([0-9]{4})_R([0-9]{3})_.*";
    public static String PRODUCT_XML_REGEX = "(S2A|S2B|S2_)_([A-Z|0-9]{4})_([A-Z|0-9|_]{4})([A-Z|0-9|_]{3})L1B_([A-Z|0-9|_]{4})_([0-9]{8}T[0-9]{6})_R([0-9]{3}).*\\.xml";
    public static String GRANULE_REGEX = "(S2A|S2B|S2_)_([A-Z|0-9]{4})_([A-Z|0-9|_]{4})([A-Z|0-9|_]{3})L1B_([A-Z|0-9|_]{4})_([0-9]{8}T[0-9]{6})_.*";
    public static String GRANULE_XML_REGEX = "(S2A|S2B|S2_)_([A-Z|0-9]{4})_([A-Z|0-9|_]{4})([A-Z|0-9|_]{3})L1B_([A-Z|0-9|_]{4})_([0-9]{8}T[0-9]{6})_.*\\.xml";

    private S2Config.Sentinel2InputType inputType = null;
    private S2Config.Sentinel2ProductLevel level = S2Config.Sentinel2ProductLevel.UNKNOWN;
    Path inputDirPath = null;
    Path inputXmlPath = null;
    private S2SpatialResolution resolution = S2SpatialResolution.R10M;


    public L1BNamingConvention(Path input){
        String inputName = input.getFileName().toString();

        if(Files.isDirectory(input)) {
            inputDirPath = input;
            Pattern pattern = Pattern.compile(PRODUCT_REGEX);
            Pattern patternCompact = Pattern.compile(PRODUCT_COMPACT_REGEX);
            if (pattern.matcher(inputName).matches()||patternCompact.matcher(inputName).matches()) {
                inputXmlPath = getXmlProductFromDir(input);
                inputType = S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA;
            }
            if(inputXmlPath == null) {
                pattern = Pattern.compile(GRANULE_REGEX);
                if (pattern.matcher(inputName).matches()) {
                    inputXmlPath = getXmlGranuleFromDir(input);
                    inputType = S2Config.Sentinel2InputType.INPUT_TYPE_GRANULE_METADATA;
                }
            }
            if(inputXmlPath == null) {
                inputType = null;
                return;
            }
        } else {
            Pattern pattern = Pattern.compile(PRODUCT_XML_REGEX);
            if (pattern.matcher(inputName).matches()) {
                inputXmlPath = input;
                inputType = S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA;
            }
            if(inputXmlPath == null) {
                pattern = Pattern.compile(GRANULE_XML_REGEX);
                if (pattern.matcher(inputName).matches()) {
                    inputXmlPath = input;
                    inputType = S2Config.Sentinel2InputType.INPUT_TYPE_GRANULE_METADATA;
                }
            }
            if(inputXmlPath == null) {
                inputType = null;
                return;
            }
        }


        level = S2Config.Sentinel2ProductLevel.L1B;
    }


    @Override
    public boolean matches(String filename) {
        Pattern pattern = Pattern.compile(PRODUCT_REGEX);
        if (pattern.matcher(filename).matches()) {
            return true;
        }

        pattern = Pattern.compile(PRODUCT_COMPACT_REGEX);
        if (pattern.matcher(filename).matches()) {
            return true;
        }

        pattern = Pattern.compile(PRODUCT_XML_REGEX);
        if (pattern.matcher(filename).matches()) {
            return true;
        }

        pattern = Pattern.compile(GRANULE_REGEX);
        if (pattern.matcher(filename).matches()) {
            return true;
        }

        pattern = Pattern.compile(GRANULE_XML_REGEX);
        if (pattern.matcher(filename).matches()) {
            return true;
        }


        return false;
    }

    @Override
    public boolean hasValidStructure() {
        if(inputType == S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA) {
            return S2ProductNamingManager.checkStructureFromProductXml(getInputXml());
        }
        if(inputType == S2Config.Sentinel2InputType.INPUT_TYPE_GRANULE_METADATA) {
            return S2ProductNamingManager.checkStructureFromGranuleXml(getInputXml());
        }

        return false;
    }

    @Override
    public Path getXmlFromDir(Path path) {
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

    @Override
    public S2Config.Sentinel2InputType getInputType() {
        return inputType;
    }

    @Override
    public S2Config.Sentinel2ProductLevel getProductLevel() {
        return level;
    }

    @Override
    public Set<String> getEPSGList() {
        return null;
    }

    @Override
    public Path getInputXml() {
        return inputXmlPath;
    }

    @Override
    public S2SpatialResolution getResolution() {
        return resolution;
    }

    @Override
    public String getProductName() {
        if(getInputXml().getParent() != null) {
            return FileUtils.getFilenameWithoutExtension(getInputXml().getParent().getFileName().toString());
        }
        return FileUtils.getFilenameWithoutExtension(getInputXml().getFileName().toString());
    }

    private Path getXmlProductFromDir(Path path) {

        if(!Files.isDirectory(path)) {
            return null;
        }
        Pattern productPattern = Pattern.compile(PRODUCT_XML_REGEX);

        String[] listXmlFiles = path.toFile().list((f, s) -> s.endsWith(".xml"));
        String xmlFile = "";
        int availableXmlCount = 0;

        for(String xml : listXmlFiles) {
            if (productPattern.matcher(xml).matches()) {
                xmlFile = xml;
                availableXmlCount++;
            }
        }

        if(availableXmlCount != 1) {
            return null;
        }

        return path.resolve(xmlFile);
    }

    private Path getXmlGranuleFromDir(Path path) {

        if(!Files.isDirectory(path)) {
            return null;
        }
        Pattern granulePattern = Pattern.compile(GRANULE_XML_REGEX);

        String[] listXmlFiles = path.toFile().list((f, s) -> s.endsWith(".xml"));
        String xmlFile = "";
        int availableXmlCount = 0;

        for(String xml : listXmlFiles) {
            if (granulePattern.matcher(xml).matches()) {
                xmlFile = xml;
                availableXmlCount++;
            }
        }

        if(availableXmlCount != 1) {
            return null;
        }

        return path.resolve(xmlFile);
    }
}
