package org.esa.s2tbx.dataio.s2.l1b;

import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by obarrile on 10/10/2016.
 */
public class L1bMetadataFactory {

    public static IL1bProductMetadata createL1bProductMetadata(Path metadataPath) throws IOException, ParserConfigurationException, SAXException {
        int psd = S2Metadata.getPSD(metadataPath);
        if(psd == 14 || psd == 13 || psd == 12 || psd == 0 )  {
            return L1bProductMetadataPSD13.create(metadataPath);
        } else {
            //TODO
            return null;
        }
    }

    public static IL1bGranuleMetadata createL1bGranuleMetadata(Path metadataPath) throws IOException, ParserConfigurationException, SAXException {
        int psd = S2Metadata.getPSD(metadataPath);
        if(psd == 14 || psd == 13 || psd == 12 || psd == 0 )  {
            return L1bGranuleMetadataPSD13.create(metadataPath);
        } else {
            //TODO
            return null;
        }
    }

    public static IL1bDatastripMetadata createL1bDatastripMetadata(Path metadataPath) throws IOException, ParserConfigurationException, SAXException {
        int psd = S2Metadata.getPSD(metadataPath);
        if(psd == 14 || psd == 13 || psd == 12 || psd == 0 )  {
            return L1bDatastripMetadataPSD13.create(metadataPath);
        } else {
            //TODO
            return null;
        }
    }
}
