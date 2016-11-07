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

package org.esa.s2tbx.dataio.s2;

import com.bc.ceres.glevel.MultiLevelImage;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.esa.s2tbx.dataio.jp2.TileLayout;
import org.esa.s2tbx.dataio.openjpeg.OpenJpegUtils;
import org.esa.s2tbx.dataio.readers.PathUtils;
import org.esa.s2tbx.dataio.s2.filepatterns.INamingConvention;
import org.esa.s2tbx.dataio.s2.filepatterns.NamingConventionFactory;
import org.esa.s2tbx.dataio.s2.filepatterns.S2ProductFilename;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.esa.s2tbx.dataio.Utils.GetLongPathNameW;
import static org.esa.s2tbx.dataio.Utils.getMD5sum;

/**
 * Base class for all Sentinel-2 product readers
 *
 * @author Nicolas Ducoin
 */
public abstract class Sentinel2ProductReader extends AbstractProductReader {



    private S2Config config;
    private File cacheDir;
    private Product product;

    protected INamingConvention namingConvention;



    public Sentinel2ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);

        this.config = new S2Config();
    }

    /**
     * @return The configuration file specific to a product reader
     */
    public S2Config getConfig() {
        return config;
    }



    public S2SpatialResolution getProductResolution() {
        return S2SpatialResolution.R10M;
    }

    public boolean isMultiResolution() {
        return true;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    protected abstract Product getMosaicProduct(File granuleMetadataFile) throws IOException;

    protected abstract String getReaderCacheDir();

    protected void initCacheDir(File productDir) throws IOException {
        Path versionFile = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("version/version.properties");
        Properties versionProp = new Properties();

        try (InputStream inputStream = Files.newInputStream(versionFile)) {
            versionProp.load(inputStream);
        } catch (IOException e) {
            SystemUtils.LOG.severe("S2MSI-reader configuration error: Failed to read " + versionFile.toString());
            throw new IOException("Failed to read " + versionFile);
        }

        String version = versionProp.getProperty("project.version");
        if (version == null) {
            throw new IOException("Unable to get project.version property from " + versionFile);
        }

        String md5sum = getMD5sum(productDir.toString());
        if (md5sum == null) {
            throw new IOException("Unable to get md5sum of path " + productDir.toString());
        }

        cacheDir = new File(new File(SystemUtils.getCacheDir(), "s2tbx" + File.separator + getReaderCacheDir() + File.separator + version + File.separator + md5sum),
                            productDir.getName());

        //noinspection ResultOfMethodCallIgnored
        cacheDir.mkdirs();
        if (!cacheDir.exists() || !cacheDir.isDirectory() || !cacheDir.canWrite()) {
            throw new IOException("Can't access package cache directory");
        }
        SystemUtils.LOG.fine("Successfully set up cache dir for product " + productDir.getName() + " to " + cacheDir.toString());
    }


    @Override
    protected Product readProductNodesImpl() throws IOException {
        SystemUtils.LOG.fine("readProductNodeImpl, " + getInput().toString());

        final File inputFile;
        File file;

        String longInput = GetLongPathNameW(getInput().toString());
        if (longInput.length() != 0) {
            file = new File(longInput);
        } else {
            file = new File(getInput().toString());
        }

        namingConvention = NamingConventionFactory.createNamingConvention(file.toPath());
        if(namingConvention == null) {
            throw new IOException("Unhandled file type.");
        }

        inputFile = namingConvention.getInputXml().toFile();

        if (!inputFile.exists()) {
            throw new FileNotFoundException(inputFile.getPath());
        }

        if (namingConvention.hasValidStructure()) {
            product = getMosaicProduct(inputFile);

            addQuicklook(product, getQuicklookFile(inputFile));

            if (product != null) {
                product.setModified(false);
            }
        } else {
            throw new IOException("Unhandled file type.");
        }

        return product;
    }

    private void addQuicklook(final Product product, final File qlFile) {
        if (qlFile != null) {
            product.getQuicklookGroup().add(new Quicklook(product, Quicklook.DEFAULT_QUICKLOOK_NAME, qlFile));
        }
    }

    private File getQuicklookFile(final File metadataFile) {
        File[] files = metadataFile.getParentFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".png") && (name.startsWith("S2") || name.startsWith("BWI_"));
            }
        });
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    /**
     * update the tile layout in S2Config
     *
     * @param metadataFilePath the path to the product metadata file
     * @param isGranule        true if it is the metadata file of a granule
     * @return false when every tileLayout is null
     */
    protected boolean updateTileLayout(Path metadataFilePath, boolean isGranule) {
        boolean valid = false;
        for (S2SpatialResolution layoutResolution : S2SpatialResolution.values()) {
            TileLayout tileLayout;
            if (isGranule) {
                tileLayout = retrieveTileLayoutFromGranuleMetadataFile(
                        metadataFilePath, layoutResolution);
            } else {
                tileLayout = retrieveTileLayoutFromProduct(metadataFilePath, layoutResolution);
            }
            config.updateTileLayout(layoutResolution, tileLayout);
            if (tileLayout != null) {
                valid = true;
            }
        }
        return valid;
    }


    /**
     * From a granule path, search a jpeg file for the given resolution, extract tile layout
     * information and update
     *
     * @param granuleMetadataFilePath the complete path to the granule metadata file
     * @param resolution              the resolution for which we wan to find the tile layout
     * @return the tile layout for the resolution, or {@code null} if none was found
     */
    public TileLayout retrieveTileLayoutFromGranuleMetadataFile(Path granuleMetadataFilePath, S2SpatialResolution resolution) {
        TileLayout tileLayoutForResolution = null;

        if (Files.exists(granuleMetadataFilePath) && granuleMetadataFilePath.toString().endsWith(".xml")) {
            Path granuleDirPath = granuleMetadataFilePath.getParent();
            tileLayoutForResolution = retrieveTileLayoutFromGranuleDirectory(granuleDirPath, resolution);
        }

        return tileLayoutForResolution;
    }


    /**
     * From a product path, search a jpeg file for the given resolution, extract tile layout
     * information and update
     *
     * @param productMetadataFilePath the complete path to the product metadata file
     * @param resolution              the resolution for which we wan to find the tile layout
     * @return the tile layout for the resolution, or {@code null} if none was found
     */
    public TileLayout retrieveTileLayoutFromProduct(Path productMetadataFilePath, S2SpatialResolution resolution) {
        TileLayout tileLayoutForResolution = null;

        if (Files.exists(productMetadataFilePath) && productMetadataFilePath.toString().endsWith(".xml")) {
            Path productFolder = productMetadataFilePath.getParent();
            Path granulesFolder = productFolder.resolve("GRANULE");
            try {
                DirectoryStream<Path> granulesFolderStream = Files.newDirectoryStream(granulesFolder);

                for (Path granulePath : granulesFolderStream) {
                    tileLayoutForResolution = retrieveTileLayoutFromGranuleDirectory(granulePath, resolution);
                    if (tileLayoutForResolution != null) {
                        break;
                    }
                }
            } catch (IOException e) {
                SystemUtils.LOG.warning("Could not retrieve tile layout for product " +
                                                productMetadataFilePath.toAbsolutePath().toString() +
                                                " error returned: " +
                                                e.getMessage());
            }
        }

        return tileLayoutForResolution;
    }

    /**
     * From a granule path, search a jpeg file for the given resolution, extract tile layout
     * information and update
     *
     * @param granuleMetadataPath the complete path to the granule directory
     * @param resolution          the resolution for which we wan to find the tile layout
     * @return the tile layout for the resolution, or {@code null} if none was found
     */
    private TileLayout retrieveTileLayoutFromGranuleDirectory(Path granuleMetadataPath, S2SpatialResolution resolution) {
        TileLayout tileLayoutForResolution = null;

        Path pathToImages = granuleMetadataPath.resolve("IMG_DATA");

        try {

            for (Path imageFilePath : getImageDirectories(pathToImages, resolution)) {
                try {
                    tileLayoutForResolution =
                            OpenJpegUtils.getTileLayoutWithOpenJPEG(S2Config.OPJ_INFO_EXE, imageFilePath);
                    if (tileLayoutForResolution != null) {
                        break;
                    }
                } catch (IOException | InterruptedException e) {
                    // if we have an exception, we try with the next file (if any)
                    // and log a warning
                    SystemUtils.LOG.warning(
                            "Could not retrieve tile layout for file " +
                                    imageFilePath.toAbsolutePath().toString() +
                                    " error returned: " +
                                    e.getMessage());
                }
            }
        } catch (IOException e) {
            SystemUtils.LOG.warning(
                    "Could not retrieve tile layout for granule " +
                            granuleMetadataPath.toAbsolutePath().toString() +
                            " error returned: " +
                            e.getMessage());
        }

        return tileLayoutForResolution;
    }


    /**
     * For a given resolution, gets the list of band names.
     * For example, for 10m L1C, {"B02", "B03", "B04", "B08"} should be returned
     *
     * @param resolution the resolution for which the band names should be returned
     * @return then band names or {@code null} if not applicable.
     */
    protected abstract String[] getBandNames(S2SpatialResolution resolution);

    /**
     * get an iterator to image files in pathToImages containing files for the given resolution
     * <p>
     * This method is based on band names, if resolution can't be based on band names or if image files are not in
     * pathToImages (like for L2A products), this method has to be overriden
     *
     * @param pathToImages the path to the directory containing the images
     * @param resolution   the resolution for which we want to get images
     * @return a {@link DirectoryStream<Path>}, iterator on the list of image path
     * @throws IOException if an I/O error occurs
     */
    protected DirectoryStream<Path> getImageDirectories(Path pathToImages, S2SpatialResolution resolution) throws IOException {
        return Files.newDirectoryStream(pathToImages, entry -> {
            String[] bandNames = getBandNames(resolution);
            if (bandNames != null) {
                for (String bandName : bandNames) {
                    if (entry.toString().endsWith(bandName + ".jp2")) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    protected Band addBand(Product product, BandInfo bandInfo, Dimension nativeResolutionDimensions) {
        Dimension dimension = new Dimension();
        if (isMultiResolution()) {
            dimension.width = nativeResolutionDimensions.width;
            dimension.height = nativeResolutionDimensions.height;
        } else {
            dimension.width = product.getSceneRasterWidth();
            dimension.height = product.getSceneRasterHeight();
        }

        Band band = new Band(
                bandInfo.getBandName(),
                S2Config.SAMPLE_PRODUCT_DATA_TYPE,
                dimension.width,
                dimension.height
        );

        S2BandInformation bandInformation = bandInfo.getBandInformation();
        band.setScalingFactor(bandInformation.getScalingFactor());

        if (bandInformation instanceof S2SpectralInformation) {
            S2SpectralInformation spectralInfo = (S2SpectralInformation) bandInformation;
            band.setSpectralWavelength((float) spectralInfo.getWavelengthCentral());
            band.setSpectralBandwidth((float) spectralInfo.getSpectralBandwith());
            band.setSpectralBandIndex(spectralInfo.getBandId());

            band.setNoDataValueUsed(false);
            band.setNoDataValue(0);
            band.setValidPixelExpression(String.format("%s.raw > %s",
                                                       bandInfo.getBandName(), S2Config.RAW_NO_DATA_THRESHOLD));

        } else if (bandInformation instanceof S2IndexBandInformation) {
            S2IndexBandInformation indexBandInfo = (S2IndexBandInformation) bandInformation;
            band.setSpectralWavelength(0);
            band.setSpectralBandwidth(0);
            band.setSpectralBandIndex(-1);
            band.setSampleCoding(indexBandInfo.getIndexCoding());
            band.setImageInfo(indexBandInfo.getImageInfo());
        } else {
            band.setSpectralWavelength(0);
            band.setSpectralBandwidth(0);
            band.setSpectralBandIndex(-1);
        }

        product.addBand(band);
        return band;
    }

    @Override
    public void close() throws IOException {
        if (product != null) {
            for (Band band : product.getBands()) {
                MultiLevelImage sourceImage = band.getSourceImage();
                if (sourceImage != null) {
                    sourceImage.reset();
                    sourceImage.dispose();
                    sourceImage = null;
                }
            }
        }
        super.close();
    }

    public static class BandInfo {
        private final Map<String, File> tileIdToFileMap;
        private final S2BandInformation bandInformation;
        private final TileLayout imageLayout;

        public BandInfo(Map<String, File> tileIdToFileMap, S2BandInformation spectralInformation, TileLayout imageLayout) {
            this.tileIdToFileMap = Collections.unmodifiableMap(tileIdToFileMap);
            this.bandInformation = spectralInformation;
            this.imageLayout = imageLayout;
        }

        public S2BandInformation getBandInformation() {
            return bandInformation;
        }

        public Map<String, File> getTileIdToFileMap() {
            return tileIdToFileMap;
        }

        public TileLayout getImageLayout() {
            return imageLayout;
        }

        public String getBandName() {
            return getBandInformation().getPhysicalBand();
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }

    }

}
