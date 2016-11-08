package org.esa.s2tbx.dataio.s2.l2a;

import com.bc.ceres.core.Assert;
import org.apache.commons.io.IOUtils;
import org.esa.s2tbx.dataio.metadata.GenericXmlMetadata;
import org.esa.s2tbx.dataio.metadata.XmlMetadataParser;
import org.esa.s2tbx.dataio.s2.S2BandInformation;
import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;
import org.esa.s2tbx.dataio.s2.filepatterns.NamingConventionFactory;
import org.esa.s2tbx.dataio.s2.filepatterns.SAFECOMPACTNamingConvention;
import org.esa.snap.core.datamodel.MetadataElement;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by obarrile on 04/10/2016.
 */

public class L2aGranuleMetadataPSD13 extends GenericXmlMetadata implements IL2aGranuleMetadata {

    String format = null;

    private static class L2aGranuleMetadataPSD13Parser extends XmlMetadataParser<L2aGranuleMetadataPSD13> {

        public L2aGranuleMetadataPSD13Parser(Class metadataFileClass) {
            super(metadataFileClass);
            setSchemaLocations(L2aPSD13Constants.getGranuleSchemaLocations());
            setSchemaBasePath(L2aPSD13Constants.getGranuleSchemaBasePath());
        }

        @Override
        protected boolean shouldValidateSchema() {
            return false;
        }
    }

    public static L2aGranuleMetadataPSD13 create(Path path) throws IOException, ParserConfigurationException, SAXException {
        Assert.notNull(path);
        L2aGranuleMetadataPSD13 result = null;
        InputStream stream = null;
        try {
            if (Files.exists(path)) {
                stream = Files.newInputStream(path, StandardOpenOption.READ);
                L2aGranuleMetadataPSD13Parser parser = new L2aGranuleMetadataPSD13Parser(L2aGranuleMetadataPSD13.class);
                result = parser.parse(stream);
                result.updateName();
                result.format = NamingConventionFactory.getGranuleFormat(path);
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return result;
    }

    public L2aGranuleMetadataPSD13(String name) {
        super(name);
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public String getMetadataProfile() {
        return null;
    }

    @Override
    public S2Metadata.ProductCharacteristics getTileProductOrganization(Path path,S2SpatialResolution resolution) {

        S2Metadata.ProductCharacteristics characteristics = new S2Metadata.ProductCharacteristics();

        //DatatakeSensingStart is not in the metadata, but it is needed for the image templates. We read it from the file system
        //TODO review
        Path folder = path.resolveSibling("IMG_DATA");
        Pattern pattern = Pattern.compile(SAFECOMPACTNamingConvention.SPECTRAL_BAND_REGEX);
        characteristics.setDatatakeSensingStartTime("Unknown");
        boolean bFound = false;
        if(Files.exists(folder) && Files.isDirectory(folder)) {
            File[] resolutions = folder.toFile().listFiles();
            for (File resolutionFolder :resolutions){
                if(resolutionFolder.isDirectory()) {
                    File[] images = resolutionFolder.listFiles();
                    if (images != null && images.length > 0) {
                        for (File image : images) {
                            String imageName = image.getName();
                            Matcher matcher = pattern.matcher(imageName);
                            if (matcher.matches()) {
                                characteristics.setDatatakeSensingStartTime(matcher.group(2));
                                bFound = true;
                                break;
                            }
                        }
                    }
                }
                if(bFound) {
                    break;
                }
            }
        }
        characteristics.setSpacecraft("Sentinel-2");
        characteristics.setProcessingLevel("Level-2A");
        characteristics.setMetaDataLevel("Standard");

        double boaQuantification = L2aPSD13Constants.DEFAULT_BOA_QUANTIFICATION;
        characteristics.setQuantificationValue(boaQuantification);
        double aotQuantification = L2aPSD13Constants.DEFAULT_AOT_QUANTIFICATION;
        double wvpQuantification = L2aPSD13Constants.DEFAULT_WVP_QUANTIFICATION;

        List<S2BandInformation> aInfo = L2aMetadataProc.getBandInformationList(getFormat(), resolution,boaQuantification,aotQuantification,wvpQuantification);
        int size = aInfo.size();
        characteristics.setBandInformations(aInfo.toArray(new S2BandInformation[size]));

        return characteristics;
    }

    @Override
    public Map<S2SpatialResolution, S2Metadata.TileGeometry> getTileGeometries() {
        Map<S2SpatialResolution, S2Metadata.TileGeometry> resolutions = new HashMap<>();
        for (String res : getAttributeValues(L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_RESOLUTION)) {
            S2SpatialResolution resolution = S2SpatialResolution.valueOfResolution(Integer.parseInt(res));
            S2Metadata.TileGeometry tgeox = new S2Metadata.TileGeometry();

            tgeox.setUpperLeftX(Double.parseDouble(getAttributeSiblingValue(L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_RESOLUTION, res,
                                                                            L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_ULX, "0")));
            tgeox.setUpperLeftY(Double.parseDouble(getAttributeSiblingValue(L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_RESOLUTION, res,
                                                                            L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_ULY, "0")));
            tgeox.setxDim(Double.parseDouble(getAttributeSiblingValue(L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_RESOLUTION, res,
                                                                      L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_XDIM, "0")));
            tgeox.setyDim(Double.parseDouble(getAttributeSiblingValue(L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_RESOLUTION, res,
                                                                      L2aPSD13Constants.PATH_GRANULE_METADATA_GEOPOSITION_YDIM, "0")));
            tgeox.setNumCols(Integer.parseInt(getAttributeSiblingValue(L2aPSD13Constants.PATH_GRANULE_METADATA_SIZE_RESOLUTION, res,
                                                                       L2aPSD13Constants.PATH_GRANULE_METADATA_SIZE_NCOLS, "0")));
            tgeox.setNumRows(Integer.parseInt(getAttributeSiblingValue(L2aPSD13Constants.PATH_GRANULE_METADATA_SIZE_RESOLUTION, res,
                                                                       L2aPSD13Constants.PATH_GRANULE_METADATA_SIZE_NROWS, "0")));
            resolutions.put(resolution, tgeox);
        }

        return resolutions;
    }

    @Override
    public String getTileID() {
        return getAttributeValue(L2aPSD13Constants.PATH_GRANULE_METADATA_TILE_ID, null);
    }

    @Override
    public String getHORIZONTAL_CS_CODE() {
        return getAttributeValue(L2aPSD13Constants.PATH_GRANULE_METADATA_HORIZONTAL_CS_CODE, null);
    }

    @Override
    public String getHORIZONTAL_CS_NAME() {
        return getAttributeValue(L2aPSD13Constants.PATH_GRANULE_METADATA_HORIZONTAL_CS_NAME, null);
    }

    @Override
    public int getAnglesResolution() {
        return Integer.parseInt(getAttributeValue(L2aPSD13Constants.PATH_GRANULE_METADATA_ANGLE_RESOLUTION, String.valueOf(L2aPSD13Constants.DEFAULT_ANGLES_RESOLUTION)));
    }

    @Override
    public S2Metadata.AnglesGrid getSunGrid() {
        return S2Metadata.wrapAngles(getAttributeValues(L2aPSD13Constants.PATH_GRANULE_METADATA_SUN_ZENITH_ANGLES),
                                     getAttributeValues(L2aPSD13Constants.PATH_GRANULE_METADATA_SUN_AZIMUTH_ANGLES));
    }

    @Override
    public S2Metadata.AnglesGrid[] getViewingAnglesGrid() {
        MetadataElement geometricElement = rootElement.getElement("Geometric_Info");
        if(geometricElement == null) {
            return null;
        }
        MetadataElement tileAnglesElement = geometricElement.getElement("Tile_Angles");
        if(tileAnglesElement == null) {
            return null;
        }
        return S2Metadata.wrapStandardViewingAngles(tileAnglesElement);
    }

    @Override
    public S2Metadata.MaskFilename[] getMasks(Path path) {
        S2Metadata.MaskFilename[] maskFileNamesArray;
        List<S2Metadata.MaskFilename> aMaskList = new ArrayList<>();
        String[] maskFilenames = getAttributeValues(L2aPSD13Constants.PATH_GRANULE_METADATA_MASK_FILENAME);
        if(maskFilenames == null) {
            return null;
        }
        for (String maskFilename : maskFilenames) {
            //To be sure that it is not a relative path and finish with .gml
            String filenameProcessed = Paths.get(maskFilename).getFileName().toString();
            if(!filenameProcessed.endsWith(".gml")) {
                filenameProcessed = filenameProcessed + ".gml";
            }

            Path QIData = path.resolveSibling("QI_DATA");
            File GmlData = new File(QIData.toFile(), filenameProcessed);

            aMaskList.add(new S2Metadata.MaskFilename(getAttributeSiblingValue(L2aPSD13Constants.PATH_GRANULE_METADATA_MASK_FILENAME, maskFilename,
                                                                                L2aPSD13Constants.PATH_GRANULE_METADATA_MASK_BAND, null),
                                                       getAttributeSiblingValue(L2aPSD13Constants.PATH_GRANULE_METADATA_MASK_FILENAME, maskFilename,
                                                                                L2aPSD13Constants.PATH_GRANULE_METADATA_MASK_TYPE, null),
                                                       GmlData));
        }
        maskFileNamesArray = aMaskList.toArray(new S2Metadata.MaskFilename[aMaskList.size()]);
        return maskFileNamesArray;
    }

    @Override
    public MetadataElement getMetadataElement() {
        return rootElement;
    }

    @Override
    public MetadataElement getSimplifiedMetadataElement() {
        //TODO ? new parse? or clone rootElement and remove some elements?
        return rootElement;
    }

    @Override
    public String getFormat() {
        return format;
    }

    private void updateName() {
        String tileId = getAttributeValue(L2aPSD13Constants.PATH_GRANULE_METADATA_TILE_ID, null);
        if(tileId == null || tileId.length()<56) {
            setName("Level-2A_Tile_ID");
            return;
        }
        setName("Level-2A_Tile_" + tileId.substring(50, 55));
    }
}
