/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.interpretation.model.interpreted;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
// TODO Consider renaming this class to EquipmentInterpretation
public abstract class AbstractInterpretation {

    public AbstractInterpretation(InterpretationAlternative alternative) {
        this.alternative = alternative;
    }

    // TODO An EquipmentInterpretation should return an InterpretedEquipment
    // For lines and transformers2 a single InterpretedBranch,
    // For transformers3 it will contain 3 InterpretedBranch objects
    public abstract InterpretedBranch interpret();

    protected InterpretationAlternative alternative;
}
