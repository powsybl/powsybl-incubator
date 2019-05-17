/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc;

import com.google.common.collect.ImmutableList;
import com.powsybl.commons.io.table.AsciiTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.api.Network;
import com.powsybl.iidm.api.test.EurostagTutorialExample1Factory;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.security.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleDcSecurityAnalysisTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDcSecurityAnalysisTest.class);

    private final MatrixFactory matrixFactory = new DenseMatrixFactory();

    @Test
    public void securityAnalysis() throws IOException {
        Network network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();

        LimitViolationFilter currentFilter = new LimitViolationFilter(EnumSet.of(LimitViolationType.CURRENT));
        SecurityAnalysis securityAnalysis = new SimpleDcSecurityAnalysisFactory(matrixFactory).create(network, currentFilter, null, 0);

        ContingenciesProvider provider = n -> ImmutableList.of("NHV1_NHV2_1", "NHV1_NHV2_2").stream()
                .map(id -> new Contingency(id, new BranchContingency(id)))
                .collect(Collectors.toList());
        SecurityAnalysisResult res = securityAnalysis.run(network.getVariantManager().getWorkingVariantId(), new SecurityAnalysisParameters(), provider)
                .join();

        try (Writer writer = new OutputStreamWriter(LoggerFactory.getInfoPrintStream(LOGGER))) {
            Security.print(res, network, writer, new AsciiTableFormatterFactory(),
                    new Security.PostContingencyLimitViolationWriteConfig(null, new TableFormatterConfig(), true, false));
        }

        assertNotNull(res);
        assertNotNull(res.getPreContingencyResult());
        assertTrue(res.getPreContingencyResult().isComputationOk());

        //2 violations, 1 on each line
        assertTrue(res.getPreContingencyResult().getLimitViolations().isEmpty());

        List<PostContingencyResult> contingenciesResult = res.getPostContingencyResults();
        assertEquals(2, contingenciesResult.size());

        LimitViolationsResult contingency1 = contingenciesResult.get(0).getLimitViolationsResult();
        assertTrue(contingency1.isComputationOk());

        assertTrue(contingency1.getLimitViolations().isEmpty());

        LimitViolationsResult contingency2 = contingenciesResult.get(1).getLimitViolationsResult();
        assertTrue(contingency2.isComputationOk());

        //1 violation on the line which is still connected
        assertEquals(1, contingency2.getLimitViolations().stream()
                .filter(l -> l.getSubjectId().equals("NHV1_NHV2_1"))
                .count());
    }

}
