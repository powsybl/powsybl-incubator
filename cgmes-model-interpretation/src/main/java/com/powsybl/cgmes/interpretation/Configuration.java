package com.powsybl.cgmes.interpretation;

import java.nio.file.Path;

public final class Configuration {

    private Configuration() {
    }

    // To keep results identical to previous implementation
    public static final boolean CONSIDER_GCH_FOR_LINES = false;

    // Process only default interpretation alternative
    public static final boolean ONLY_DEFAULT_CONFIGURATION = false;

    // When debugging, stop at first exception
    public static final boolean CATCH_EXCEPTIONS = false;

    // Global value for allowed tolerance
    public static final double ERROR_TOLERANCE = 1.0;

    // How many nodes with error will appear in the summary reports
    public static final int REPORT_SUMMARY_NUM_BAD_NODES = 5;

    // The name of the field interpretation alternative in the reports
    public static final Object REPORT_INTERPRETATION_ALTERNATIVE_TITLE = "config";

    // Timestamp as suffix to report filenames
    public static final boolean REPORT_FILENAME_WITH_TIMESTAMP = false;

    // FIXME This is to filter out models not processed by previous implementation
    public static boolean excluded(Path p) {
        String sp = p.toString();
        return sp.contains("_IPTO_NodeBreaker") || sp.contains("_2D_NGET_NodeBreaker");
    }
}
