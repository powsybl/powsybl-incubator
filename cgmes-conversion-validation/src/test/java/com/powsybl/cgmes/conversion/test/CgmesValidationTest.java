/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.conversion.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.conversion.validation.TerminalFlow;
import com.powsybl.cgmes.conversion.validation.TerminalFlow.BranchEndType;
import com.powsybl.cgmes.conversion.validation.TerminalFlow.CgmesFlow;
import com.powsybl.cgmes.conversion.validation.ValidationResults;
import com.powsybl.cgmes.conversion.validation.ValidationResults.ValidationAlternativeResults;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversion;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversionFactory;
import com.powsybl.cgmes.conversion.validation.report.CatalogValidationReport;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class CgmesValidationTest {

    @Test
    public void microGridBaseCaseBE() throws IOException {
        ValidationResults conversionValidationResult = test(CgmesConformity1Catalog.microGridBaseCaseBE(), "microGridBaseCaseBE");
        assertFalse(getFailed(conversionValidationResult));
    }

    @Test
    public void microGridBaseCaseNL() throws IOException {
        ValidationResults conversionValidationResult = test(CgmesConformity1Catalog.microGridBaseCaseNL(), "microGridBaseCaseNL");
        assertFalse(getFailed(conversionValidationResult));
    }

    @Test
    public void microGridBaseCaseAssembled() throws IOException {
        ValidationResults conversionValidationResult = test(CgmesConformity1Catalog.microGridBaseCaseAssembled(), "microGridBaseCaseAssembled");
        assertFalse(getFailed(conversionValidationResult));
    }

    @Test
    public void microGridType4BE() throws IOException {
        ValidationResults conversionValidationResult = test(CgmesConformity1Catalog.microGridType4BE(), "microGridType4BE");
        assertFalse(getFailed(conversionValidationResult));
    }

    @Test
    public void miniBusBranch() throws IOException {
        ValidationResults conversionValidationResult = test(CgmesConformity1Catalog.miniBusBranch(), "miniBusBranch");
        assertFalse(getFailed(conversionValidationResult));
    }

    @Test
    public void miniNodeBreaker() throws IOException {
        ValidationResults conversionValidationResult = test(CgmesConformity1Catalog.miniNodeBreaker(), "miniNodeBreaker");
        assertFalse(getFailed(conversionValidationResult));
    }

    @Test
    public void smallBusBranch() throws IOException {
        ValidationResults conversionValidationResult = test(CgmesConformity1Catalog.smallBusBranch(), "smallBusBranch");
        assertFalse(getFailed(conversionValidationResult));
    }

    @Test
    public void smallNodeBreaker() throws IOException {
        ValidationResults conversionValidationResult = test(CgmesConformity1Catalog.smallNodeBreaker(), "smallNodeBreaker");
        assertFalse(getFailed(conversionValidationResult));
    }

    @Test
    public void report() throws IOException {
        Collection<ValidationResults> results = new ArrayList<>();
        ValidationResults r = new ValidationResults("test");
        InterpretationAlternative alternative = new InterpretationAlternative();
        ValidationAlternativeResults validation = new ValidationAlternativeResults(alternative);
        CgmesFlow cgmesFlow = new CgmesFlow(-10.0, -10.0, true);
        TerminalFlow terminalFlow = new TerminalFlow("test", BranchEndType.LINE_ONE, 10.0, 10.0, cgmesFlow);
        validation.addTerminalFlow(terminalFlow);
        r.validationAlternativeResults().put(alternative, validation);
        results.add(r);
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Files.createDirectory(fs.getPath("tmp"));
            new CatalogValidationReport(fs.getPath("tmp")).report(results);
            compareTxt(getClass().getResourceAsStream("/CGMESCatalogValidation"), Files.newInputStream(fs.getPath("tmp", "CGMESCatalogValidation")));
        }
    }

    private ValidationResults test(TestGridModel testGridModel, String modelName) throws IOException {
        ReadOnlyDataSource ds = testGridModel.dataSource();
        String impl = TripleStoreFactory.defaultImplementation();
        double threshold = 0.01;
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {

            ValidationConfig config = loadFlowParametersConfig(fs, threshold);
            CgmesModelConversion model = (CgmesModelConversion) CgmesModelConversionFactory.create(modelName, ds, impl);
            model.z0Nodes();
            return new CgmesValidationTester(model).test(config);
        }
    }

    private boolean getFailed(ValidationResults conversionValidationResult) {
        return conversionValidationResult.failedCount() > 0;
    }

    private ValidationConfig loadFlowParametersConfig(FileSystem fs, double threshold) {
        InMemoryPlatformConfig pconfig = new InMemoryPlatformConfig(fs);
        pconfig
            .createModuleConfig("componentDefaultConfig");
        ValidationConfig config = ValidationConfig.load(pconfig);
        config.setVerbose(true);
        config.setThreshold(threshold);
        config.setOkMissingValues(false);
        config.setLoadFlowParameters(new LoadFlowParameters());
        return config;
    }

    private static String normalizeLineSeparator(String str) {
        return str.replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    private static void compareTxt(InputStream expected, InputStream actual) {
        try {
            compareTxt(expected, new String(ByteStreams.toByteArray(actual), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void compareTxt(InputStream expected, String actual) {
        try {
            String expectedStr = normalizeLineSeparator(new String(ByteStreams.toByteArray(expected), StandardCharsets.UTF_8));
            String actualStr = normalizeLineSeparator(actual);
            assertEquals(expectedStr, actualStr);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
