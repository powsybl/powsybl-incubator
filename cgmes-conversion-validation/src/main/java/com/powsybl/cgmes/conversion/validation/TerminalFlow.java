package com.powsybl.cgmes.conversion.validation;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class TerminalFlow {

    public enum BranchEndType {
        LINE_ONE, LINE_TWO, XFMR2_ONE, XFMR2_TWO, XFMR3_ONE, XFMR3_TWO, XFMR3_THREE
    }

    public TerminalFlow(String id, BranchEndType endType, double p, double q, CgmesFlow cgmesFlow) {
        this.id = id;
        this.endType = endType;
        this.pIidm = p;
        this.qIidm = q;
        this.cgmesFlow = cgmesFlow;
    }

    public String id() {
        return id;
    }

    public static class CgmesFlow {
        double p;
        double q;
        boolean calculated;

        public CgmesFlow(double p, double q, boolean calculated) {
            this.p = p;
            this.q = q;
            this.calculated = calculated;
        }

        public boolean calculated() {
            return calculated;
        }
    }

    public BranchEndType endType() {
        return endType;
    }

    public double pIidm() {
        return pIidm;
    }

    public double qIidm() {
        return qIidm;
    }

    public double pCgmes() {
        return cgmesFlow.p;
    }

    public double qCgmes() {
        return cgmesFlow.q;
    }

    public double flowError() {
        if (calculated() && !Double.isNaN(pIidm)) {
            return Math.abs(pCgmes() - pIidm) + Math.abs(qCgmes() - qIidm);
        }
        return 0.0;
    }

    public boolean calculated() {
        return cgmesFlow != null && cgmesFlow.calculated();
    }

    public String code() {
        StringBuilder code = new StringBuilder();
        switch (endType) {
            case LINE_ONE:
                code.append("LINE-1.");
                break;
            case LINE_TWO:
                code.append("LINE-2.");
                break;
            case XFMR2_ONE:
                code.append("XFMR2-1.");
                break;
            case XFMR2_TWO:
                code.append("XFMR2-2.");
                break;
            case XFMR3_ONE:
                code.append("XFMR3-1.");
                break;
            case XFMR3_TWO:
                code.append("XFMR3-2.");
                break;
            case XFMR3_THREE:
                code.append("XFMR3-3.");
                break;
        }

        code.append(id);

        return code.toString();
    }

    final String id;
    final BranchEndType endType;
    final double pIidm;
    final double qIidm;
    final CgmesFlow cgmesFlow;
}
