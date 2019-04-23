package com.powsybl.cgmes.model.interpretation.test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.junit.Test;

import com.powsybl.cgmes.catalog.CatalogLocation;
import com.powsybl.cgmes.interpretation.CatalogInterpretation;
import com.powsybl.cgmes.interpretation.InterpretationAlternatives;
import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.report.BestInterpretationReport;
import com.powsybl.cgmes.interpretation.report.CatalogInterpretationReport;
import com.powsybl.cgmes.interpretation.report.DetectedModelsFromBestInterpretationsReport;

public class CsiModels {

    @Test
    public void hertz50FebruaryIncubator() throws IOException {
        report(IOP_FEBRUARY,
            "glob:**20190213**1D*50hertz*/*1030*.zip",
            Paths.get("/Users/zamarrenolm/Downloads/cgmes-model-interpretation/actual-50hertz/"));
    }

    @Test
    public void NG1DFebruaryIncubator() throws IOException {
        report(IOP_FEBRUARY,
            "glob:**20190213**1D*NGET*/*1030*.zip",
            Paths.get("/Users/zamarrenolm/Downloads/cgmes-model-interpretation/actual-ng/"));
    }

    @Test
    public void IPTOFebruaryIncubator() throws IOException {
        report(IOP_FEBRUARY,
            "glob:**20190213**1D*IPTO*/*1030*.zip",
            Paths.get("/Users/zamarrenolm/Downloads/cgmes-model-interpretation/actual-ipto/"));
    }

    @Test
    public void AmprionFebruaryIncubator() throws IOException {
        report(IOP_FEBRUARY,
            "glob:**20190213**Amprion*/*1030*.zip",
            Paths.get("/Users/zamarrenolm/Downloads/cgmes-model-interpretation/actual-amprion/"));
    }

    @Test
    public void SEPSFebruaryIncubator() throws IOException {
        report(IOP_FEBRUARY,
            "glob:**20190213**1D*SEPS*/*1030*.zip",
            Paths.get("/Users/zamarrenolm/Downloads/cgmes-model-interpretation/actual-seps/"));
    }

    @Test
    public void ESOFebruaryIncubator() throws IOException {
        report(IOP_FEBRUARY,
            "glob:**20190213**ESO*/*1030*.zip",
            Paths.get("/Users/zamarrenolm/Downloads/cgmes-model-interpretation/actual-eso/"));
    }

    @Test
    public void NOSBiHFebruaryIncubator() throws IOException {
        report(IOP_FEBRUARY,
            "glob:**20190213**NOSBiH*/*1030*BA2.zip",
            Paths.get("/Users/zamarrenolm/Downloads/cgmes-model-interpretation/actual-nosbih/"));
    }

    @Test
    public void allFebruary() throws IOException {
        report(IOP_FEBRUARY, "glob:**20190213**BD*/*1030*zip");
    }

    @Test
    public void allJanuay() throws IOException {
        report(IOP_JANUARY, "glob:**20190116**BD*/*1030*zip");
    }

    @Test
    public void elering201902() throws IOException {
        report(SELECTED_SAMPLES, "glob:**elering*201902**/*zip");
    }

    @Test
    public void mavir() throws IOException {
        report(IOP_JANUARY, "glob:**201901**MAVIR*/*1230*zip");
    }

    @Test
    public void rte() throws IOException {
        report(IOP_JANUARY, "glob:**201901**RTE*BusBranch/*1030*zip");
    }

    private static void report(CatalogLocation catalog, String pattern) throws IOException {
        report(catalog, pattern, OUTPUT_PATH);
    }

    private static void report(CatalogLocation catalog, String pattern, Path output) throws IOException {
        CatalogInterpretation ci = new CatalogInterpretation(
            catalog,
            InterpretationAlternatives.configured());
        Collection<InterpretationResults> results = ci.reviewAll(pattern);
        new CatalogInterpretationReport(output).report(results);
        new DetectedModelsFromBestInterpretationsReport(output).report(results);
        new BestInterpretationReport(output).report(results);
        ci.reportWrong();
    }

    private static final Path OUTPUT_PATH = Paths
        .get("/Users/zamarrenolm/Downloads/cgmes-model-interpretation/actual/");

    private static final CatalogLocation SELECTED_SAMPLES = new CatalogLocation() {
        @Override
        public Path dataRoot() {
            return Paths.get("/Users/zamarrenolm/works/RTE/data");
        }

        @Override
        public Path boundary() {
            return null;
        }
    };

    private static final CatalogLocation IOP_JANUARY = new CatalogLocation() {
        @Override
        public Path dataRoot() {
            return Paths.get("/Volumes/LuScratch/work/works/RTE/CGMES/data/csi/IOP/CGMES_IOP_20190116");
        }

        @Override
        public Path boundary() {
            return Paths.get("/Volumes/LuScratch/work/works/RTE/CGMES/data/csi/IOP/CGMES_IOP_20190116/boundary");
        }
    };

    private static final CatalogLocation IOP_FEBRUARY = new CatalogLocation() {
        @Override
        public Path dataRoot() {
            return Paths.get("/Volumes/LuScratch/work/works/RTE/CGMES/data/csi/IOP/CGMES_IOP_20190213");
        }

        @Override
        public Path boundary() {
            // Boundary data from January IOP
            return Paths.get("/Volumes/LuScratch/work/works/RTE/CGMES/data/csi/IOP/CGMES_IOP_20190116/boundary");
        }
    };
}
