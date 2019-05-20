package com.powsybl.cgmes.model.interpretation.test.csi;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.powsybl.cgmes.catalog.CatalogLocation;
import com.powsybl.cgmes.interpretation.CatalogInterpretation;
import com.powsybl.cgmes.interpretation.InterpretationAlternatives;
import com.powsybl.cgmes.interpretation.report.BestInterpretationReport;
import com.powsybl.cgmes.interpretation.report.CatalogInterpretationReport;
import com.powsybl.cgmes.interpretation.report.DetectedModelsFromBestInterpretationsReport;

public class CsiTests {

    private static final String CATALOG_PATH = "\\cgmes-csi\\IOP";
    private static final String JAN_BOUNDARY_PATH = "\\cgmes-csi\\IOP\\CGMES_IOP_20190116\\boundary";
    private static final String MAR_BOUNDARY_PATH = "\\cgmes-csi\\IOP\\CGMES_IOP_20190313\\boundary";

    // @Test
    public void interpretSwissgridFeb1030() throws IOException {
        CatalogLocation location = new CatalogLocation() {
            public Path dataRoot() {
                return Paths.get(CATALOG_PATH);
            }

            public Path boundary() {
                return Paths.get(JAN_BOUNDARY_PATH);
            }
        };

        new CatalogInterpretationReport(Paths.get("\\tmp")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190213**BD*Swissgrid*/*1030*zip"));
    }

    @Test
    public void interpretAllEquipmentModelJan1030() throws IOException {
        CatalogLocation location = new CatalogLocation() {
            public Path dataRoot() {
                return Paths.get(CATALOG_PATH);
            }

            public Path boundary() {
                return Paths.get(JAN_BOUNDARY_PATH);
            }
        };

        new CatalogInterpretationReport(Paths.get("\\tmp\\20190116")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190116**BD*/*1030*zip"));
        new BestInterpretationReport(Paths.get("\\tmp")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190116**BD*/*1030*zip"));
        new DetectedModelsFromBestInterpretationsReport(Paths.get("\\tmp")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190116**BD*/*1030*zip"));
    }

    @Test
    public void interpretAllEquipmentModelFeb1030() throws IOException {
        CatalogLocation location = new CatalogLocation() {
            public Path dataRoot() {
                return Paths.get(CATALOG_PATH);
            }

            public Path boundary() {
                return Paths.get(JAN_BOUNDARY_PATH);
            }
        };

        new CatalogInterpretationReport(Paths.get("\\tmp\\20190213")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190213**BD*/*1030*zip"));
        new BestInterpretationReport(Paths.get("\\tmp")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190213**BD*/*1030*zip"));
        new DetectedModelsFromBestInterpretationsReport(Paths.get("\\tmp")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190213**BD*/*1030*zip"));
    }

    @Test
    public void interpretAllEquipmentModelMar1030() throws IOException {
        CatalogLocation location = new CatalogLocation() {
            public Path dataRoot() {
                return Paths.get(CATALOG_PATH);
            }

            public Path boundary() {
                return Paths.get(MAR_BOUNDARY_PATH);
            }
        };

        new CatalogInterpretationReport(Paths.get("\\tmp\\20190313")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190313**BD*/*1030*zip"));
        new BestInterpretationReport(Paths.get("\\tmp")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190313**BD*/*1030*zip"));
        new DetectedModelsFromBestInterpretationsReport(Paths.get("\\tmp")).report(
            new CatalogInterpretation(location, InterpretationAlternatives.configured())
                .reviewAll("glob:**20190313**BD*/*1030*zip"));
    }
}
