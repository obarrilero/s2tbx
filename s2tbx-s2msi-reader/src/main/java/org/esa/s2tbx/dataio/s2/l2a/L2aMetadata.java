/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s2tbx.dataio.s2.l2a;

import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.S2ProductNamingManager;
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoGranuleDirFilename;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoGranuleMetadataFilename;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.util.SystemUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the Sentinel-2 MSI L1C XML metadata header file.
 * <p>
 * Note: No data interpretation is done in this class, it is intended to serve the pure metadata content only.
 *
 * @author Norman Fomferra
 */
public class L2aMetadata extends S2Metadata {

    private static final int DEFAULT_ANGLES_RESOLUTION = 5000;

    protected Logger logger = SystemUtils.LOG;

    public static L2aMetadata parseHeader(File file, String granuleName, S2Config config, String epsg, S2SpatialResolution productResolution, boolean isAGranule) throws IOException, ParserConfigurationException, SAXException {
        return new L2aMetadata(file.toPath(), granuleName, config, epsg, productResolution, isAGranule);
    }


    private L2aMetadata(Path path, String granuleName, S2Config s2config, String epsg, S2SpatialResolution productResolution, boolean isAGranule) throws IOException, ParserConfigurationException, SAXException {
        super(s2config);
        resetTileList();
        boolean isGranuleMetadata = isAGranule;

        if(!isGranuleMetadata) {
            initProduct(path, granuleName, epsg, productResolution);
        } else {
            initTile(path, epsg, productResolution);
        }
        //TODO
    }


    private void initProduct(Path path, String granuleName, String epsg, S2SpatialResolution productResolution) throws IOException, ParserConfigurationException, SAXException {
        IL2aProductMetadata metadataProduct = L2aMetadataFactory.createL2aProductMetadata(path);

        setFormat(metadataProduct.getFormat());
        setProductCharacteristics(metadataProduct.getProductOrganization(path, productResolution));

        Collection<String> tileNames = null;

        if (granuleName == null) {
            tileNames = metadataProduct.getTiles();
        } else {
            Collection<String> tileNamesAux = metadataProduct.getTiles();

            for(String tileNameAux : tileNamesAux) {
                String auxTileId = S2ProductNamingManager.getTileIdFromString(tileNameAux);
                if(auxTileId.equals(S2ProductNamingManager.getTileIdFromString(granuleName))) {
                    tileNames = Collections.singletonList(tileNameAux);
                    break;
                }
            }
        }

        //add product metadata
        getMetadataElements().add(metadataProduct.getMetadataElement());

        //add datastrip metadatas
        for(Path datastripFolder : S2ProductNamingManager.getDatastripsFromProductXml(path)) {
            Path datastripPath = S2ProductNamingManager.getXmlFromDir(datastripFolder);
            if(datastripPath != null) {
                IL2aDatastripMetadata metadataDatastrip = L2aMetadataFactory.createL2aDatastripMetadata(datastripPath);
                getMetadataElements().add(metadataDatastrip.getMetadataElement());
            }
        }


        //Check if the tiles found in metadata exist and add them to fullTileNamesList
        ArrayList<Path> granulePaths = S2ProductNamingManager.getTilesFromProductXml(path);
        ArrayList<Path> granuleMetadataPathList = new ArrayList<>();
        for (String tileName : tileNames) {
            S2ProductNamingManager.getTileIdFromString(tileName);
            String tileId = S2ProductNamingManager.getTileIdFromString(tileName);
            if(tileId == null) {
                continue;
            }

            for(Path granulePath : granulePaths) {
                String tileIdAux = S2ProductNamingManager.getTileIdFromString(granulePath.getFileName().toString());
                if(tileId.equals(tileIdAux)) {
                    resourceResolver.put(tileName,granulePath);
                    Path nestedGranuleMetadata = S2ProductNamingManager.getXmlFromDir(granulePath);
                    if(nestedGranuleMetadata != null) {
                        granuleMetadataPathList.add(nestedGranuleMetadata);
                        //TODO notificar algo si no lo encuentra
                    }
                }
            }
        }

        //Init Tiles
        for (Path granuleMetadataPath : granuleMetadataPathList) {
            initTile(granuleMetadataPath, epsg, productResolution);
        }
    }

    private void initTile(Path path, String epsg, S2SpatialResolution resolution) throws IOException, ParserConfigurationException, SAXException {

        IL2aGranuleMetadata granuleMetadata = L2aMetadataFactory.createL2aGranuleMetadata(path);

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
            return;
        }

        tile.setTileGeometries(geoms);

        try {
            tile.setAnglesResolution(granuleMetadata.getAnglesResolution());
        } catch (Exception e) {
            logger.warning("Angles resolution cannot be obtained");
            tile.setAnglesResolution(DEFAULT_ANGLES_RESOLUTION);
        }

        tile.setSunAnglesGrid(granuleMetadata.getSunGrid());
        if(!getProductCharacteristics().getMetaDataLevel().equals("Brief")) {
            tile.setViewingIncidenceAnglesGrids(granuleMetadata.getViewingAnglesGrid());
        }

        //granuleMetadata.getMasks(path);
        tile.setMaskFilenames(granuleMetadata.getMasks(path));

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
    }
}
