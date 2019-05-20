package com.powsybl.cgmes.conversion.validation;

import java.util.HashMap;
import java.util.Map;

import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class ValidationResults {

    public ValidationResults(String cgmesName) {
        this.cgmesName = cgmesName;
    }

    public String cgmesName() {
        return cgmesName;
    }

    public static class ValidationAlternativeResults {

        public static final double FLOW_THRESHOLD = 0.000001;

        public ValidationAlternativeResults(InterpretationAlternative alternative) {
            this.alternative = alternative;
            terminalFlows = new HashMap<>();
        }

        public InterpretationAlternative alternative() {
            return alternative;
        }

        public Map<String, TerminalFlow> terminalFlows() {
            return terminalFlows;
        }

        public int nonCalculated() {
            return (int) terminalFlows.values().stream()
                .filter(fd -> !fd.calculated()).count();
        }

        public int failedCount() {
            return (int) terminalFlows.values().stream()
                .filter(fd -> {
                    return fd.flowError() > FLOW_THRESHOLD;
                }).count();
        }

        public void addTerminalFlow(TerminalFlow terminalFlow) {
            if (terminalFlow == null) {
                return;
            }
            this.terminalFlows.put(terminalFlow.code(), terminalFlow);
        }

        private final InterpretationAlternative alternative;
        Map<String, TerminalFlow> terminalFlows;
    }

    public Map<InterpretationAlternative, ValidationAlternativeResults> validationAlternativeResults() {
        return validationAlternativeResults;
    }

    public Exception exception() {
        return exception;
    }

    public int failedCount() {
        return validationAlternativeResults.values().stream().mapToInt(ValidationAlternativeResults::failedCount).sum();
    }

    private final String cgmesName;
    Map<InterpretationAlternative, ValidationAlternativeResults> validationAlternativeResults = new HashMap<>();
    Exception exception;
}
