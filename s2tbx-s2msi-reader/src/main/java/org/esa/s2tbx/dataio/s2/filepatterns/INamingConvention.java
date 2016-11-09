package org.esa.s2tbx.dataio.s2.filepatterns;

import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by obarrile on 02/11/2016.
 */
public interface INamingConvention {
    public String[] getProductREGEXs();
    public String[] getProductXmlREGEXs();
    public String[] getGranuleREGEXs();
    public String[] getGranuleXmlREGEXs();
    public String[] getDatastripREGEXs();
    public String[] getDatastripXmlREGEXs();
    public boolean matches(String filename);
    public boolean hasValidStructure();
    public Path getXmlFromDir(Path path);
    public S2Config.Sentinel2InputType getInputType();
    public S2Config.Sentinel2ProductLevel getProductLevel();
    public Set<String> getEPSGList();
    public Path getInputXml();
    public Path getInputProductXml();
    public S2SpatialResolution getResolution();
    public String getProductName();
    public boolean matchesProductMetadata(String filename);
    public ArrayList<Path> getDatastripXmlPaths();
    public ArrayList<Path> getGranulesXmlPaths();
}
