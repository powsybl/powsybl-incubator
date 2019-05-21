package com.powsybl.cgmes.model.interpretation.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.interpretation.Configuration;
import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.report.BestInterpretationReport;
import com.powsybl.cgmes.interpretation.report.CatalogInterpretationReport;
import com.powsybl.cgmes.interpretation.report.DetectedModelsFromBestInterpretationsReport;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.triplestore.api.TripleStoreFactory;

public class CgmesInterpretationTest {

    @BeforeClass
    public static void setUp() {
        catalog = new CgmesConformity1Catalog();
    }

    @Test
    public void microGridBaseCaseBE() throws IOException {
        InterpretationResults results = test(catalog.microGridBaseCaseBE(), "microGridBaseCaseBE");
        assertFalse(results.error() > Configuration.ERROR_TOLERANCE);
    }

    @Test
    public void microGridBaseCaseNL() throws IOException {
        InterpretationResults results = test(catalog.microGridBaseCaseNL(), "microGridBaseCaseNL");
        assertFalse(results.error() > Configuration.ERROR_TOLERANCE);
    }

    @Test
    public void microGridBaseCaseAssembled() throws IOException {
        InterpretationResults results = test(catalog.microGridBaseCaseAssembled(), "microGridBaseCaseAssembled");
        assertFalse(results.error() > Configuration.ERROR_TOLERANCE);
    }

    @Test
    public void microGridType4BE() throws IOException {
        InterpretationResults results = test(catalog.microGridType4BE(), "microGridType4BE");
        assertTrue(results.error() > Configuration.ERROR_TOLERANCE);
    }

    @Test
    public void miniBusBranch() throws IOException {
        InterpretationResults results = test(catalog.miniBusBranch(), "miniBusBranch");
        assertFalse(results.error() > Configuration.ERROR_TOLERANCE);
    }

    @Test
    public void miniNodeBreaker() throws IOException {
        InterpretationResults results = test(catalog.miniNodeBreaker(), "miniNodeBreaker");
        assertFalse(results.error() > Configuration.ERROR_TOLERANCE);
    }

    @Test
    public void smallBusBranch() throws IOException {
        InterpretationResults results = test(catalog.smallBusBranch(), "smallBusBranch");
        assertTrue(results.error() > Configuration.ERROR_TOLERANCE);
    }

    @Test
    public void smallNodeBreaker() throws IOException {
        InterpretationResults results = test(catalog.smallNodeBreaker(), "smallNodeBreaker");
        assertTrue(results.error() > Configuration.ERROR_TOLERANCE);
    }

    @Test
    public void report() throws IOException {
        Collection<InterpretationResults> results = new ArrayList<>();
        InterpretationResults r = test(catalog.miniNodeBreaker(), "miniNodeBreaker");
        results.add(r);
        try (FileSystem fs = Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix())) {
            Files.createDirectory(fs.getPath("tmp"));
            new CatalogInterpretationReport(fs.getPath("tmp")).report(results);
            compareTxt(getClass().getResourceAsStream("/CGMESCatalogInterpretation"), Files.newInputStream(fs.getPath("tmp", "CGMESCatalogInterpretation")));
            new BestInterpretationReport(fs.getPath("tmp")).report(results);
            compareTxt(getClass().getResourceAsStream("/CGMESBestInterpretation.csv"), Files.newInputStream(fs.getPath("tmp", "CGMESBestInterpretation.csv")));
            new DetectedModelsFromBestInterpretationsReport(fs.getPath("tmp")).report(results);
            compareTxt(getClass().getResourceAsStream("/CGMESDetectedModelsFromBestInterpretations.csv"), Files.newInputStream(fs.getPath("tmp", "CGMESDetectedModelsFromBestInterpretations.csv")));
        }
    }

    private InterpretationResults test(TestGridModel testGridModel, String modelName) {
        ReadOnlyDataSource ds = testGridModel.dataSource();
        String impl = TripleStoreFactory.defaultImplementation();
        CgmesModel model = CgmesModelFactory.create(ds, impl);
        boolean discreteStep = false;
        return new CgmesInterpretationTester(
            new CgmesModelForInterpretation(modelName, model, discreteStep)).test();
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

    private static CgmesConformity1Catalog catalog;
}
