package org.esa.s2tbx.dataio.s2.l1c;

import com.bc.ceres.core.Assert;
import org.apache.commons.io.IOUtils;
import org.esa.s2tbx.dataio.metadata.GenericXmlMetadata;
import org.esa.s2tbx.dataio.metadata.XmlMetadataParser;
import org.esa.s2tbx.dataio.s2.S2BandInformation;
import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.filepatterns.NamingConventionFactory;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoDatastripFilename;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoGranuleDirFilename;
import org.esa.snap.core.datamodel.MetadataElement;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by obarrile on 29/09/2016.
 */

public class L1cProductMetadataPSD13 extends GenericXmlMetadata implements IL1cProductMetadata {

    HashMap<String,String> namingItems;
    private static class L1cProductMetadataPSD13Parser extends XmlMetadataParser<L1cProductMetadataPSD13> {

        public L1cProductMetadataPSD13Parser(Class metadataFileClass) {
            super(metadataFileClass);
            setSchemaLocations(L1cPSD13Constants.getProductSchemaLocations());
            setSchemaBasePath(L1cPSD13Constants.getProductSchemaBasePath());
        }

        @Override
        protected boolean shouldValidateSchema() {
            return false;
        }
    }

    public static L1cProductMetadataPSD13 create(Path path) throws IOException, ParserConfigurationException, SAXException {
        Assert.notNull(path);
        L1cProductMetadataPSD13 result = null;
        InputStream stream = null;
        try {
            if (Files.exists(path)) {
                stream = Files.newInputStream(path, StandardOpenOption.READ);
                L1cProductMetadataPSD13Parser parser = new L1cProductMetadataPSD13Parser(L1cProductMetadataPSD13.class);
                result = parser.parse(stream);
                result.setName("Level-1C_User_Product");
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return result;
    }


    public L1cProductMetadataPSD13(String name) {
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
    public S2Metadata.ProductCharacteristics getProductOrganization(Path xmlPath) {

        S2Metadata.ProductCharacteristics characteristics = new S2Metadata.ProductCharacteristics();

        String datatakeSensingStart = getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_SENSING_START, null);
        if(datatakeSensingStart!=null && datatakeSensingStart.length()>19) {
            String formattedDatatakeSensingStart = datatakeSensingStart.substring(0,4) +
                    datatakeSensingStart.substring(5,7) +
                    datatakeSensingStart.substring(8,13) +
                    datatakeSensingStart.substring(14,16)+
                    datatakeSensingStart.substring(17,19);
            characteristics.setDatatakeSensingStartTime(formattedDatatakeSensingStart);
        } else {
            characteristics.setDatatakeSensingStartTime("Unknown");
        }

        characteristics.setSpacecraft(getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_SPACECRAFT, "Sentinel-2"));
        characteristics.setDatasetProductionDate(getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_SENSING_START, "Unknown"));

        characteristics.setProductStartTime(getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_PRODUCT_START_TIME, "Unknown"));
        characteristics.setProductStopTime(getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_PRODUCT_STOP_TIME, "Unknown"));

        characteristics.setProcessingLevel(getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_PROCESSING_LEVEL, "Level-1C"));
        characteristics.setMetaDataLevel(getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_METADATA_LEVEL, "Standard"));

        double toaQuantification = Double.valueOf(getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_QUANTIFICATION_VALUE, String.valueOf(L1cPSD13Constants.DEFAULT_TOA_QUANTIFICATION)));
        characteristics.setQuantificationValue(toaQuantification);

        List<S2BandInformation> aInfo = L1cMetadataProc.getBandInformationList (/*xmlPath*/getFormat(),toaQuantification);
        int size = aInfo.size();
        characteristics.setBandInformations(aInfo.toArray(new S2BandInformation[size]));

        return characteristics;
    }

    @Override
    public Collection<String> getTiles() {
        String[] granuleList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_GRANULE_LIST);
        if(granuleList == null) {
            granuleList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_GRANULE_LIST_ALT);
            if(granuleList == null) {
                return null;
            }
        }
        return new ArrayList<>(Arrays.asList(granuleList));
    }

    @Override
    public Collection<String> getImagesFromTile(String tileId){

        String[] imageList = getAttributeSiblingValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_GRANULE_LIST,tileId,L1cPSD13Constants.PATH_PRODUCT_METADATA_IMAGE_ID);
        if(imageList != null) return new ArrayList<>(Arrays.asList(imageList));
        imageList = getAttributeSiblingValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_GRANULE_LIST_ALT,tileId,L1cPSD13Constants.PATH_PRODUCT_METADATA_IMAGE_FILE_ALT);
        if(imageList != null) return new ArrayList<>(Arrays.asList(imageList));
        imageList = getAttributeSiblingValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_GRANULE_LIST_ALT,tileId,L1cPSD13Constants.PATH_PRODUCT_METADATA_IMAGE_ID);
        if(imageList != null) return new ArrayList<>(Arrays.asList(imageList));
        imageList = getAttributeSiblingValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_GRANULE_LIST,tileId,L1cPSD13Constants.PATH_PRODUCT_METADATA_IMAGE_FILE);
        if(imageList != null) return new ArrayList<>(Arrays.asList(imageList));

        return null;
    }


    @Override
    public Collection<String> getDatastripIds() {
        String[] datastripList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_DATASTRIP_LIST);
        if (datastripList == null) {
            datastripList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_DATASTRIP_LIST_ALT);
            if (datastripList == null) {
                return null;
            }
        }
        return new ArrayList<>(Arrays.asList(datastripList));
    }


    public S2DatastripFilename getDatastrip() {
        String[] datastripList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_DATASTRIP_LIST);
        if(datastripList == null) {
            datastripList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_DATASTRIP_LIST_ALT);
            if(datastripList == null) {
                return null;
            }
        }

        S2DatastripDirFilename dirDatastrip = S2DatastripDirFilename.create(datastripList[0], null);

        S2DatastripFilename datastripFilename = null;
        if (dirDatastrip != null) {
            String fileName = dirDatastrip.getFileName(null);

            if (fileName != null) {
                datastripFilename = S2OrthoDatastripFilename.create(fileName);
            }
        }

        return datastripFilename;
    }

    @Override
    public S2DatastripDirFilename getDatastripDir() {
        String[] granuleList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_GRANULE_LIST);
        String[] datastripList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_DATASTRIP_LIST);
        if(datastripList == null) {
            datastripList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_DATASTRIP_LIST_ALT);
            if(datastripList == null) {
                return null;
            }
        }
        if(granuleList == null) {
            granuleList = getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_GRANULE_LIST_ALT);
            if(granuleList == null) {
                return null;
            }
        }

        S2OrthoGranuleDirFilename grafile = S2OrthoGranuleDirFilename.create(granuleList[0]);

        S2DatastripDirFilename datastripDirFilename = null;
        if (grafile != null) {
            String fileCategory = grafile.fileCategory;
            String dataStripMetadataFilenameCandidate = datastripList[0];
            datastripDirFilename = S2DatastripDirFilename.create(dataStripMetadataFilenameCandidate, fileCategory);

        }
        return datastripDirFilename;
    }

    @Override
    public MetadataElement getMetadataElement() {
        return rootElement;
    }


    @Override
    public String getFormat() {
        return getAttributeValue(L1cPSD13Constants.PATH_PRODUCT_METADATA_PRODUCT_FORMAT, null);
    }

    private String[] getBandList() {
        return getAttributeValues(L1cPSD13Constants.PATH_PRODUCT_METADATA_BAND_LIST);
    }



}
