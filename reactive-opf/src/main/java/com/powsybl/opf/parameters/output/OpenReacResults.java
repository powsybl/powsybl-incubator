package com.powsybl.opf.parameters.output;

import com.powsybl.opf.parameters.output.ReactiveInvestmentOutput.ReactiveInvestment;

import java.util.List;

public class OpenReacResults {
    private final OpenReacStatus status;
    private final List<ReactiveInvestment> reactiveInvestments;

    public OpenReacResults(OpenReacStatus status, List<ReactiveInvestment> reactiveInvestments) {
        this.status = status;
        this.reactiveInvestments = reactiveInvestments;
    }

    public OpenReacStatus getStatus() {
        return status;
    }

    public List<ReactiveInvestment> getReactiveInvestments() {
        return reactiveInvestments;
    }
}
