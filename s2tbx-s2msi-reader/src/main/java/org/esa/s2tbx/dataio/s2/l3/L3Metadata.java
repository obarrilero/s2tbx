package org.esa.s2tbx.dataio.s2.l3;

import org.esa.s2tbx.dataio.s2.S2BandInformation;
import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.S2ProductNamingUtils;
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.util.SystemUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;


/**
 * Created by obarrile on 15/06/2016.
 */
public class L3Metadata extends S2Metadata {

    public static final String MOSAIC_BAND_NAME = "quality_mosaic_info";
    private static final int DEFAULT_ANGLES_RESOLUTION = 5000;

    protected Logger logger = SystemUtils.LOG;

    public static L3Metadata parseHeader(File file, String granuleName, S2Config config, String epsg, S2SpatialResolution productResolution, boolean isAGranule) throws IOException, ParserConfigurationException, SAXException {

        return new L3Metadata(file.toPath(), granuleName, config, epsg, productResolution, isAGranule);

    }

    private L3Metadata(Path path, String granuleName, S2Config config, String epsg, S2SpatialResolution productResolution, boolean isAGranule) throws  IOException, ParserConfigurationException, SAXException {
        super(config);

        resetTileList();
        int maxIndex = 0;
        boolean isGranuleMetadata = isAGranule;

        if(!isGranuleMetadata) {
            maxIndex = initProduct(path, granuleName, epsg, productResolution);
        } else {
            maxIndex = initTile(path, epsg, productResolution);
        }

        //add band information (at the end because we need to read the metadata to know the maximum index of mosaic)
        List<S2BandInformation> bandInfoList = L3MetadataProc.getBandInformationList(getFormat(), productResolution, getProductCharacteristics().getQuantificationValue(), maxIndex);
        int size = bandInfoList.size();
        getProductCharacteristics().setBandInformations(bandInfoList.toArray(new S2BandInformation[size]));

    }

    private int initProduct(Path path, String granuleName, String epsg, S2SpatialResolution productResolution) throws IOException, ParserConfigurationException, SAXException {
        IL3ProductMetadata metadataProduct = L3MetadataFactory.createL3ProductMetadata(path);

        setFormat(metadataProduct.getFormat());
        setProductCharacteristics(metadataProduct.getProductOrganization(path, productResolution));

        Collection<String> tileNames = null;

        if (granuleName == null) {
            tileNames = metadataProduct.getTiles();
        } else {
            Collection<String> tileNamesAux = metadataProduct.getTiles();

            for(String tileNameAux : tileNamesAux) {
                String auxTileId = S2ProductNamingUtils.getTileIdFromString(tileNameAux);
                if(auxTileId.equals(S2ProductNamingUtils.getTileIdFromString(granuleName))) {
                    tileNames = Collections.singletonList(tileNameAux);
                    break;
                }
            }
        }

        //add product metadata
        getMetadataElements().add(metadataProduct.getMetadataElement());

        //add datastrip metadatas
        for(Path datastripFolder : S2ProductNamingUtils.getDatastripsFromProductXml(path)) {
            Path datastripPath = S2ProductNamingUtils.getXmlFromDir(datastripFolder);
            if(datastripPath != null) {
                IL3DatastripMetadata metadataDatastrip = L3MetadataFactory.createL3DatastripMetadata(datastripPath);
                getMetadataElements().add(metadataDatastrip.getMetadataElement());
            }
        }

        //Check if the tiles found in metadata exist and add them to fullTileNamesList
        ArrayList<Path> granulePaths = S2ProductNamingUtils.getTilesFromProductXml(path);
        ArrayList<Path> granuleMetadataPathList = new ArrayList<>();
        for (String tileName : tileNames) {
            S2ProductNamingUtils.getTileIdFromString(tileName);
            String tileId = S2ProductNamingUtils.getTileIdFromString(tileName);
            if(tileId == null) {
                continue;
            }

            for(Path granulePath : granulePaths) {
                String tileIdAux = S2ProductNamingUtils.getTileIdFromString(granulePath.getFileName().toString());
                if(tileId.equals(tileIdAux)) {
                    resourceResolver.put(tileName,granulePath);
                    Path nestedGranuleMetadata = S2ProductNamingUtils.getXmlFromDir(granulePath);
                    if(nestedGranuleMetadata != null) {
                        granuleMetadataPathList.add(nestedGranuleMetadata);
                        //TODO notificar algo si no lo encuentra
                    }
                }
            }
        }

        //Init Tiles
        int maxIndex=1;
        for (Path granuleMetadataPath : granuleMetadataPathList) {
            int maxIndexTile =initTile(granuleMetadataPath, epsg, productResolution);
            if (maxIndexTile > maxIndex) {
                maxIndex = maxIndexTile;
            }
        }
        return maxIndex;
    }

    private int initTile(Path path, String epsg, S2SpatialResolution resolution) throws IOException, ParserConfigurationException, SAXException {

        IL3GranuleMetadata granuleMetadata = L3MetadataFactory.createL3GranuleMetadata(path);

        if(getFormat() == null) {
            setFormat(granuleMetadata.getFormat());
        }

        if(getProductCharacteristics() == null) {
            setProductCharacteristics(granuleMetadata.getTileProductOrganization(path, resolution));
        }

        Map<S2SpatialResolution, TileGeometry> geoms = granuleMetadata.getTileGeometries();

        Tile tile = new Tile(granuleMetadata.getTileID());
        tile.setHorizontalCsCode(granuleMetadata.getHORIZONTAL_CS_CODE());
        tile.setHorizontalCsName(granuleMetadata.getHORIZONTAL_CS_NAME());

        if (epsg != null && !tile.getHorizontalCsCode().equals(epsg)) {
            // skip tiles that are not in the desired UTM zone
            logger.info(String.format("Skipping tile %s because it has crs %s instead of requested %s", path.getFileName().toString(), tile.getHorizontalCsCode(), epsg));
            return 0;
        }

        tile.setTileGeometries(geoms);

        try {
            tile.setAnglesResolution(granuleMetadata.getAnglesResolution());
        } catch (Exception e) {
            logger.warning("Angles resolution cannot be obtained");
            tile.setAnglesResolution(DEFAULT_ANGLES_RESOLUTION);
        }

        tile.setSunAnglesGrid(granuleMetadata.getSunGrid());
        if(getProductCharacteristics().getMetaDataLevel()== null || !getProductCharacteristics().getMetaDataLevel().equals("Brief")) {
            tile.setViewingIncidenceAnglesGrids(granuleMetadata.getViewingAnglesGrid());
        }

        addTileToList(tile);

        //Search "Granules" metadata element. If it does not exist, it is created
        MetadataElement granulesMetaData = null;
        for(MetadataElement metadataElement : getMetadataElements()) {
            if(metadataElement.getName().equals("Granules")) {
                granulesMetaData = metadataElement;
                break;
            }
        }
        if (granulesMetaData == null) {
            granulesMetaData = new MetadataElement("Granules");
            getMetadataElements().add(granulesMetaData);
        }

        granulesMetaData.addElement(granuleMetadata.getSimplifiedMetadataElement());

        return granuleMetadata.getMaximumMosaicIndex();
    }

}
