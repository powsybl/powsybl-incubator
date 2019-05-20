package com.powsybl.cgmes.conversion.validation.model;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesOnDataSource;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreException;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public final class CgmesModelConversionFactory {

    private CgmesModelConversionFactory() {
    }

    public static CgmesModelTripleStore create(String name, ReadOnlyDataSource ds, String tripleStoreImpl) {
        return create(name, ds, null, tripleStoreImpl);
    }

    public static CgmesModelTripleStore create(String name, ReadOnlyDataSource ds, ReadOnlyDataSource dsBoundary, String tripleStoreImpl) {
        CgmesOnDataSource cds = new CgmesOnDataSource(ds);
        TripleStore tripleStore = TripleStoreFactory.create(tripleStoreImpl);
        CgmesModelConversion cgmes = new CgmesModelConversion(name, cds.cimNamespace(), tripleStore);
        readCgmesModel(cgmes, cds, cds.baseName());
        // Only try to read boundary data from additional sources if the main data
        // source does not contain boundary info
        if (!cgmes.hasBoundary() && dsBoundary != null) {
            // Read boundary using same baseName of the main data
            readCgmesModel(cgmes, new CgmesOnDataSource(dsBoundary), cds.baseName());
        }
        return cgmes;
    }

    private static void readCgmesModel(CgmesModelConversion cgmes, CgmesOnDataSource cds, String base) {
        for (String name : cds.names()) {
            LOG.info("Reading [{}]", name);
            try (InputStream is = cds.dataSource().newInputStream(name)) {
                cgmes.read(base, name, is);
            } catch (IOException e) {
                String msg = String.format("Reading [%s]", name);
                LOG.warn(msg);
                throw new TripleStoreException(msg, e);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CgmesModelConversionFactory.class);
}
