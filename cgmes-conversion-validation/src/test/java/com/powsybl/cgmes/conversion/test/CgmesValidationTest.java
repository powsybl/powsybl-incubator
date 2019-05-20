package com.powsybl.cgmes.conversion.test;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.FileSystem;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.conversion.validation.ValidationResults;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversion;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversionFactory;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.mock.LoadFlowFactoryMock;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class CgmesValidationTest {

    @BeforeClass
    public static void setUp() {
        catalog = new CgmesConformity1Catalog();
    }

    @Test
    public void microGridBaseCaseBE() throws IOException {
        test(catalog.microGridBaseCaseBE(), "microGridBaseCaseBE");
    }

    @Test
    public void microGridBaseCaseNL() throws IOException {
        test(catalog.microGridBaseCaseNL(), "microGridBaseCaseNL");
    }

    @Test
    public void microGridBaseCaseAssembled() throws IOException {
        test(catalog.microGridBaseCaseAssembled(), "microGridBaseCaseAssembled");
    }

    @Test
    public void microGridType4BE() throws IOException {
        test(catalog.microGridType4BE(), "microGridType4BE");
    }

    @Test
    public void miniBusBranch() throws IOException {
        test(catalog.miniBusBranch(), "miniBusBranch");
    }

    @Test
    public void miniNodeBreaker() throws IOException {
        test(catalog.miniNodeBreaker(), "miniNodeBreaker");
    }

    @Test
    public void smallBusBranch() throws IOException {
        test(catalog.smallBusBranch(), "smallBusBranch");
    }

    @Test
    public void smallNodeBreaker() throws IOException {
        test(catalog.smallNodeBreaker(), "smallNodeBreaker");
    }

    private void test(TestGridModel testGridModel, String modelName) throws IOException {
        ReadOnlyDataSource ds = testGridModel.dataSource();
        String impl = TripleStoreFactory.defaultImplementation();
        double threshold = 0.01;
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {

            ValidationConfig config = loadFlowParametersConfig(fs, threshold);
            CgmesModelConversion model = (CgmesModelConversion) CgmesModelConversionFactory.create(modelName, ds, impl);
            model.z0Nodes();
            ValidationResults conversionValidationResult = new CgmesValidationTester(model).test(config);
            assertFalse(getFailed(conversionValidationResult));
        }
    }

    private boolean getFailed(ValidationResults conversionValidationResult) {
        return conversionValidationResult.failedCount() > 0;
    }

    private ValidationConfig loadFlowParametersConfig(FileSystem fs, double threshold) {
        InMemoryPlatformConfig pconfig = new InMemoryPlatformConfig(fs);
        pconfig
            .createModuleConfig("componentDefaultConfig")
            .setStringProperty("LoadFlowFactory", LoadFlowFactoryMock.class.getCanonicalName());
        ValidationConfig config = ValidationConfig.load(pconfig);
        config.setVerbose(true);
        config.setThreshold(threshold);
        config.setOkMissingValues(false);
        config.setLoadFlowParameters(new LoadFlowParameters());
        return config;
    }

    private static CgmesConformity1Catalog catalog;
}
