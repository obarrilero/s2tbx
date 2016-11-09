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

package org.esa.s2tbx.dataio.s2.ortho;

import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2ProductReaderPlugIn;
import org.esa.s2tbx.dataio.s2.l2a.L2aUtils;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.util.Locale;

import static org.esa.s2tbx.dataio.s2.ortho.S2CRSHelper.epsgToDisplayName;
import static org.esa.s2tbx.dataio.s2.ortho.S2CRSHelper.epsgToShortDisplayName;

/**
 * @author Norman Fomferra
 */
public abstract class S2OrthoProductReaderPlugIn extends S2ProductReaderPlugIn {

    private static S2ProductCRSCache crsCache = new S2ProductCRSCache();
    private S2Config.Sentinel2ProductLevel level = S2Config.Sentinel2ProductLevel.UNKNOWN;

    public S2OrthoProductReaderPlugIn() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Natural Colors", new String[]{"B4", "B3", "B2"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI False-color Infrared", new String[]{"B8", "B4", "B3"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI False-color Urban", new String[]{"B12", "B11", "B4"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Agriculture", new String[]{"B11", "B8", "B2"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Atmospheric penetration", new String[]{"B12", "B11", "B8A"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Healthy Vegetation", new String[]{"B8", "B11", "B2"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Land/Water", new String[]{"B8", "B11", "B4"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Natural with Atmospherical Removal", new String[]{"B12", "B8", "B3"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Shortwave Infrared", new String[]{"B12", "B8", "B4"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Vegetation Analysis", new String[]{"B11", "B8", "B4"}));
    }

    protected S2Config.Sentinel2ProductLevel getLevel() {
        return level;
    }

    protected String getResolution() {
        return "Multi";
    }


    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        SystemUtils.LOG.fine("Getting decoders...");
        //File file = preprocessInput(input);
        if(!(input instanceof File)) {
            return DecodeQualification.UNABLE;
        }
        File file = (File) input;

        crsCache.ensureIsCached(file.toPath());

        level = crsCache.getProductLevel(file.getAbsolutePath());
        S2Config.Sentinel2InputType  inputType = crsCache.getInputType(file.getAbsolutePath());

        if(inputType == null) {
            return DecodeQualification.UNABLE;
        }

        if((level != S2Config.Sentinel2ProductLevel.L1C)  && (level != S2Config.Sentinel2ProductLevel.L2A) && (level != S2Config.Sentinel2ProductLevel.L3)) {
            return DecodeQualification.UNABLE;
        }

        if(!crsCache.hasEPSG(file.getAbsolutePath(), getEPSG())) {
            return DecodeQualification.UNABLE;
        }

        if (level != S2Config.Sentinel2ProductLevel.L2A && level != S2Config.Sentinel2ProductLevel.L3) {
            return DecodeQualification.INTENDED;
        }

        //if product is level2 or level3, check the specific folder
        if ((inputType == S2Config.Sentinel2InputType.INPUT_TYPE_PRODUCT_METADATA) && !L2aUtils.checkMetadataSpecificFolder(file, getResolution()))
            return DecodeQualification.UNABLE;
        if ((inputType == S2Config.Sentinel2InputType.INPUT_TYPE_GRANULE_METADATA) && !L2aUtils.checkGranuleSpecificFolder(file, getResolution()))
            return DecodeQualification.UNABLE;

        //level=S2Config.Sentinel2ProductLevel.L2A;
        return DecodeQualification.INTENDED;
    }

    public abstract String getEPSG();

    @Override
    public ProductReader createReaderInstance() {
        SystemUtils.LOG.info(String.format("Building product reader - %s", getEPSG()));
        return new Sentinel2OrthoProductReaderProxy(this, getEPSG());
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME + "-MultiRes-" + epsgToShortDisplayName(getEPSG())};
    }

    @Override
    public String getDescription(Locale locale) {
        return String.format("Sentinel-2 MSI %s - Native resolutions - %s", getLevel(), epsgToDisplayName(getEPSG()));
    }

}
