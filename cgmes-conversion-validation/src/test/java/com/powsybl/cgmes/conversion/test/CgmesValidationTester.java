package com.powsybl.cgmes.conversion.test;

import com.powsybl.cgmes.conversion.validation.Validation;
import com.powsybl.cgmes.conversion.validation.ValidationResults;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversion;
import com.powsybl.loadflow.validation.ValidationConfig;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class CgmesValidationTester {

    public CgmesValidationTester(CgmesModelConversion gm) {
        this.model = gm;
    }

    public ValidationResults test(ValidationConfig config) {

        Validation validation = new Validation(model);
        return validation.validate(config);
    }

    final CgmesModelConversion model;
}
