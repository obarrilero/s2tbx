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
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;
import org.esa.s2tbx.dataio.s2.filepatterns.NamingConventionFactory;
import org.esa.s2tbx.dataio.s2.masks.MaskInfo;
import org.esa.s2tbx.dataio.s2.ortho.S2OrthoProductReaderPlugIn;
import org.esa.s2tbx.dataio.s2.ortho.S2ProductCRSCache;
import org.esa.s2tbx.dataio.s2.ortho.Sentinel2OrthoProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>
 * This product reader can currently read single L2A tiles (also called L2A granules) and entire L2A scenes composed of
 * multiple L2A tiles.
 * </p>
 * <p>
 * To read single tiles, select any tile image file (IMG_*.jp2) within a product package. The reader will then
 * collect other band images for the selected tile and wiull also try to read the metadata file (MTD_*.xml).
 * </p>
 * <p>To read an entire scene, select the metadata file (MTD_*.xml) within a product package. The reader will then
 * collect other tile/band images and create a mosaic on the fly.
 * </p>
 *
 * @author Norman Fomferra
 * @author Oscar Picas-Puig
 */
public class Sentinel2L2AProductReader extends Sentinel2OrthoProductReader {

    static final String L2A_CACHE_DIR = "l2a-reader";

    public Sentinel2L2AProductReader(ProductReaderPlugIn readerPlugIn, String epsgCode) {
        super(readerPlugIn, epsgCode);
    }

    @Override
    public S2SpatialResolution getProductResolution() {
        if(namingConvention == null && (getInput() instanceof File)) {
            namingConvention = NamingConventionFactory.createNamingConvention(((File) getInput()).toPath());
        }

        if(namingConvention == null) {
            return S2SpatialResolution.R10M;
        }

        return namingConvention.getResolution();
    }

    @Override
    protected String getReaderCacheDir() {
        return L2A_CACHE_DIR;
    }

    @Override
    protected S2Metadata parseHeader(
            File file, String granuleName, S2Config config, String epsg, boolean isAGranule) throws IOException {

        try {
            return L2aMetadata.parseHeader(file, granuleName, config, epsg, getProductResolution(), isAGranule, namingConvention);
        } catch (ParserConfigurationException | SAXException e) {
           throw new IOException("Failed to parse metadata in " + file.getName());
        }
    }

    @Override
    protected String[] getBandNames(S2SpatialResolution resolution) {
        return null;
    }

    @Override
    protected DirectoryStream<Path> getImageDirectories(Path pathToImages, S2SpatialResolution spatialResolution) throws IOException {
        String resolutionFolder = "R" + Integer.toString(spatialResolution.resolution) + "m";
        Path pathToImagesOfResolution = pathToImages.resolve(resolutionFolder);

        return Files.newDirectoryStream(pathToImagesOfResolution, entry -> {
            return entry.toString().endsWith("_" + spatialResolution.resolution + "m.jp2");
        });
    }

    @Override
    protected String getImagePathString(String imageFileName, S2SpatialResolution resolution) {
        String resolutionFolder = String.format("R%dm", resolution.resolution);
        String imageWithoutExtension = imageFileName.substring(0, imageFileName.length() - 4);
        return String.format("%s%s%s_%dm.jp2",
                resolutionFolder,
                File.separator,
                imageWithoutExtension,
                resolution.resolution);
    }

    @Override
    protected int getMaskLevel() {
        return MaskInfo.L2A;
    }
}
