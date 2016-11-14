package org.esa.s2tbx.dataio.s2.l3;

import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;
import org.esa.s2tbx.dataio.s2.filepatterns.NamingConventionFactory;
import org.esa.s2tbx.dataio.s2.l2a.L2aUtils;
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
 * Created by obarrile on 15/06/2016.
 */
public class Sentinel2L3ProductReader  extends Sentinel2OrthoProductReader {

    static final String L3_CACHE_DIR = "l3-reader";

    public Sentinel2L3ProductReader(ProductReaderPlugIn readerPlugIn, String epsgCode) {
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
        return L3_CACHE_DIR;
    }

    @Override
    protected S2Metadata parseHeader(
            File file, String granuleName, S2Config config, String epsg, boolean isAGranule) throws IOException {

        try {
            return L3Metadata.parseHeader(file, granuleName, config, epsg, getProductResolution(), isAGranule, namingConvention);
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
        return MaskInfo.L3;
    }
}
