/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
