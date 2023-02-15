package com.powsybl.opf;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.executor.IAmplOutputFile;
import com.powsybl.commons.PowsyblException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class ReactiveInvestmentOutput implements IAmplOutputFile {

    private final List<ReactiveInvestment> investments;

    public ReactiveInvestmentOutput() {
        this.investments = new LinkedList<>();
    }

    public List<ReactiveInvestment> getInvestments() {
        return investments;
    }

    @Override
    public String getFileName() {
        return "reactiveopf_results_reactive_slacks.csv";
    }

    @Override
    public void read(Path path) throws IOException {
        List<String> investmentsLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String headers = investmentsLines.get(0);
        int expectedCols = 6;
        String sep = ";";
        int readCols = headers.split(sep).length;
        if (readCols != expectedCols) {
            throw new PowsyblException(
                    "Error reading " + getFileName() + ", wrong number of columns. Expected: " + expectedCols + ", found:" + readCols);
        } else {
            for (String line : investmentsLines.subList(1, investmentsLines.size())) {
                readLine(line.split(sep));
            }
        }
    }

    private void readLine(String[] tokens) {
        int busNum = Integer.parseInt(tokens[1]);
        double slackCondensator = readDouble(tokens[2]);
        double slackSelf = readDouble(tokens[3]);
        String id = tokens[4];
        String substationId = tokens[5];
        investments.add(new ReactiveInvestment(id, busNum, substationId, slackCondensator, slackSelf));
    }

    private double readDouble(String d) {
        return Float.parseFloat(d) != AmplConstants.INVALID_FLOAT_VALUE ? Double.parseDouble(d) : Double.NaN;
    }

    private static class ReactiveInvestment {
        public final String id;
        public final int busId;
        public final String substationId;
        public final double slackCondensator;
        public final double slackSelf;

        public ReactiveInvestment(String id, int busId, String substationId, double slackCondensator,
                                  double slackSelf) {
            this.id = id;
            this.busId = busId;
            this.substationId = substationId;
            this.slackCondensator = slackCondensator;
            this.slackSelf = slackSelf;
        }
    }
}
