package com.powsybl.cgmes.conversion.validation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.catalog.CatalogLocation;
import com.powsybl.cgmes.catalog.CatalogReview;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversion;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversionFactory;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class CatalogValidation extends CatalogReview {

    public CatalogValidation(CatalogLocation location) {
        super(location);
    }

    public Collection<ValidationResults> reviewAll(String pattern, ValidationConfig config)
        throws IOException {
        Collection<ValidationResults> results = new ArrayList<>();
        reviewAll(pattern, p -> {
            LOG.info("case {}", modelName(p));
            CgmesModelConversion m = (CgmesModelConversion) cgmes(p);
            m.z0Nodes();

            Validation validation = new Validation(m);
            ValidationResults result = validation.validate(config);
            results.add(result);
        });
        return results;
    }

    @Override
    public CgmesModel cgmes(Path rpath) {
        String impl = TripleStoreFactory.defaultImplementation();
        if (location.boundary() == null) {
            return CgmesModelConversionFactory.create(modelName(rpath), dataSource(location.dataRoot().resolve(rpath)),
                impl);
        } else {
            return CgmesModelConversionFactory.create(modelName(rpath), dataSource(location.dataRoot().resolve(rpath)),
                dataSource(location.boundary()),
                impl);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CatalogValidation.class);
}
