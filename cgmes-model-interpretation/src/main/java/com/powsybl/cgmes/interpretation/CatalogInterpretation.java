/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.catalog.CatalogLocation;
import com.powsybl.cgmes.catalog.CatalogReview;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class CatalogInterpretation extends CatalogReview {

    public CatalogInterpretation(CatalogLocation location, List<InterpretationAlternative> alternatives) {
        super(location);
        this.alternatives = alternatives;
        setCatchExceptions(Configuration.CATCH_EXCEPTIONS);
    }

    public Collection<InterpretationResults> reviewAll(String pattern) throws IOException {
        Collection<InterpretationResults> results = new ArrayList<>();
        reviewAll(pattern, p -> {
            LOG.info("{}", modelName(p));
            if (Configuration.excluded(p)) {
                return;
            }
            boolean discreteStep = false;
            Interpretation i = new Interpretation(
                new CgmesModelForInterpretation(
                    modelName(p),
                    cgmes(p),
                    discreteStep));
            InterpretationResults r = i.interpret(alternatives);
            results.add(r);
        });
        return results;
    }

    private final List<InterpretationAlternative> alternatives;

    private static final Logger LOG = LoggerFactory.getLogger(CatalogInterpretation.class);
}
