package org.esa.s2tbx.dataio.s2.l1c;

import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.esa.snap.core.datamodel.MetadataElement;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Created by obarrile on 29/09/2016.
 */
public interface IL1cProductMetadata {
    S2Metadata.ProductCharacteristics getProductOrganization(Path xmlPath);
    Collection<String> getTiles();
    Collection<String> getImagesFromTile(String tileId);
    Collection<String> getDatastripIds();
    S2DatastripDirFilename getDatastripDir();
    MetadataElement getMetadataElement();
}
