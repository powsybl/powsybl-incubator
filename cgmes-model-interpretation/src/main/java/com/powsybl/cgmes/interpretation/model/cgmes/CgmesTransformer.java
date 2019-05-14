/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.cgmes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.commons.PowsyblException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesTransformer {

    public CgmesTransformer(
        String id,
        CgmesTransformerEnd end1,
        CgmesTransformerEnd end2,
        CgmesTransformerEnd end3) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(end1);
        Objects.requireNonNull(end2);
        Objects.requireNonNull(end3);
        this.id = id;
        this.end1 = end1;
        this.end2 = end2;
        this.end3 = end3;
    }

    public CgmesTransformer(
        String id,
        CgmesTransformerEnd end1,
        CgmesTransformerEnd end2) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(end1);
        Objects.requireNonNull(end2);
        this.id = id;
        this.end1 = end1;
        this.end2 = end2;
        this.end3 = null;
    }

    CgmesTransformer(CgmesModel cgmes, String id, PropertyBags endsp, Map<String, PropertyBag> allTapChangers, boolean discreteStep) {
        List<CgmesTransformerEnd> ends = endsp.stream()
            .map(end -> new CgmesTransformerEnd(cgmes, end, allTapChangers, discreteStep))
            .sorted(Comparator.comparingInt(CgmesTransformerEnd::endNumber))
            .collect(Collectors.toList());
        Objects.requireNonNull(id);
        this.id = id;
        this.end1 = ends.get(0);
        this.end2 = ends.get(1);
        this.end3 = ends.size() > 2 ? ends.get(2) : null;
    }

    public String id() {
        return id;
    }

    public int numEnds() {
        return end3 == null ? 2 : 3;
    }

    public CgmesTransformerEnd[] ends() {
        if (end3 != null) {
            return new CgmesTransformerEnd[] {end1, end2, end3};
        } else {
            return new CgmesTransformerEnd[] {end1, end2};
        }
    }

    public CgmesTransformerEnd end1() {
        return end1;
    }

    public CgmesTransformerEnd end2() {
        return end2;
    }

    public CgmesTransformerEnd end3() {
        return end3;
    }

    public CgmesTransformerEnd end(String endNumber) {
        if (endNumber.equals("1")) {
            return end1;
        } else if (endNumber.equals("2")) {
            return end2;
        } else if (endNumber.equals("3")) {
            return end3;
        } else {
            throw new PowsyblException("invalid endNumber " + endNumber);
        }
    }

    private final String id;
    private final CgmesTransformerEnd end1;
    private final CgmesTransformerEnd end2;
    private final CgmesTransformerEnd end3;
}
