/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.conversion.validation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conversion.validation.TerminalFlow.BranchEndType;
import com.powsybl.cgmes.conversion.validation.TerminalFlow.CgmesFlow;
import com.powsybl.cgmes.conversion.validation.ValidationResults.ValidationAlternativeResults;
import com.powsybl.cgmes.conversion.validation.conversion.CgmesConversion;
import com.powsybl.cgmes.conversion.validation.interpretation.InterpretationModel;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversion;
import com.powsybl.cgmes.interpretation.InterpretationAlternatives;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;
import com.powsybl.loadflow.resultscompletion.z0flows.Z0LineChecker;
import com.powsybl.loadflow.validation.ValidationConfig;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class Validation {

    public Validation(CgmesModelConversion cgmes) {
        this.cgmes = cgmes;
        interpretationModel = new InterpretationModel(cgmes);
    }

    public ValidationResults validate(ValidationConfig config) {
        ValidationResults results = new ValidationResults(cgmes.name());
        List<InterpretationAlternative> alternatives = InterpretationAlternatives.configured();
        for (InterpretationAlternative alternative : alternatives) {
            CgmesConversion conversion = new CgmesConversion(cgmes, alternative);
            Network network = conversion.convert();
            resetFlows(network);
            Z0LineChecker z0checker = computeIidmFlows(network, config.getLoadFlowParameters(),
                alternative.isLineRatio0());
            results.validationAlternativeResults.put(alternative, validateAlternative(alternative, network, z0checker));
        }
        return results;
    }

    private Z0LineChecker computeIidmFlows(Network network, LoadFlowParameters lfparams,
        boolean structuralRatioLineOn) {
        LoadFlowResultsCompletionParameters p = new LoadFlowResultsCompletionParameters(
            LoadFlowResultsCompletionParameters.EPSILON_X_DEFAULT,
            LoadFlowResultsCompletionParameters.APPLY_REACTANCE_CORRECTION_DEFAULT,
            LoadFlowResultsCompletionParameters.Z0_THRESHOLD_DIFF_VOLTAGE_ANGLE,
            structuralRatioLineOn);
        LoadFlowResultsCompletion lf = new LoadFlowResultsCompletion(p, lfparams);
        try {
            lf.run(network, null);
        } catch (Exception e) {
            LOG.error("computeFlows, error {}", e.getMessage());
        }
        return lf.z0checker();
    }

    private void resetFlows(Network network) {
        network.getBranchStream().forEach(b -> {
            b.getTerminal1().setP(Double.NaN);
            b.getTerminal2().setP(Double.NaN);
            b.getTerminal1().setQ(Double.NaN);
            b.getTerminal2().setQ(Double.NaN);
        });
        network.getDanglingLineStream().forEach(d -> {
            d.getTerminal().setP(Double.NaN);
            d.getTerminal().setQ(Double.NaN);
        });
        network.getThreeWindingsTransformerStream().forEach(tx -> {
            tx.getLeg1().getTerminal().setP(Double.NaN);
            tx.getLeg2().getTerminal().setP(Double.NaN);
            tx.getLeg3().getTerminal().setP(Double.NaN);
            tx.getLeg1().getTerminal().setQ(Double.NaN);
            tx.getLeg2().getTerminal().setQ(Double.NaN);
            tx.getLeg3().getTerminal().setQ(Double.NaN);
        });
    }

    private ValidationAlternativeResults validateAlternative(InterpretationAlternative alternative,
        Network network, Z0LineChecker z0checker) {

        ValidationAlternativeResults validationAlternativeResults = new ValidationAlternativeResults(alternative);
        validateLines(alternative, network, z0checker, validationAlternativeResults);
        validateDanglingLines(alternative, network, validationAlternativeResults);
        validateTwoWindingsTransformers(alternative, network, validationAlternativeResults);
        validategetThreeWindingsTransformers(alternative, network, validationAlternativeResults);

        return validationAlternativeResults;
    }

    private void validateLines(InterpretationAlternative alternative, Network network, Z0LineChecker z0checker,
        ValidationAlternativeResults validationAlternativeResults) {
        // Flows validation
        network.getLineStream().forEach(line -> {
            if (z0checker.isZ0(line)) {
                return;
            }

            Terminal terminal1 = line.getTerminal(Branch.Side.ONE);
            CgmesFlow cgmesFlow1 = interpretationModel.interpretLine(alternative, line, Branch.Side.ONE);
            validationAlternativeResults.addTerminalFlow(new TerminalFlow(line.getId(), BranchEndType.LINE_ONE, terminal1.getP(), terminal1.getQ(),
                cgmesFlow1));

            Terminal terminal2 = line.getTerminal(Branch.Side.TWO);
            CgmesFlow cgmesFlow2 = interpretationModel.interpretLine(alternative, line, Branch.Side.TWO);
            validationAlternativeResults.addTerminalFlow(new TerminalFlow(line.getId(), BranchEndType.LINE_TWO, terminal2.getP(), terminal2.getQ(),
                cgmesFlow2));
        });
    }

    private void validateDanglingLines(InterpretationAlternative alternative, Network network,
        ValidationAlternativeResults validationAlternativeResults) {
        network.getDanglingLines().forEach(line -> {
            Terminal terminal = line.getTerminal();
            CgmesFlow cgmesFlow = interpretationModel.interpretDanglingLine(alternative, line);
            validationAlternativeResults.addTerminalFlow(new TerminalFlow(line.getId(), BranchEndType.LINE_ONE, terminal.getP(), terminal.getQ(),
                cgmesFlow));
        });

    }

    private void validateTwoWindingsTransformers(InterpretationAlternative alternative, Network network,
        ValidationAlternativeResults validationAlternativeResults) {
        network.getTwoWindingsTransformerStream().forEach(transformer -> {
            Terminal terminal1 = transformer.getTerminal(Branch.Side.ONE);
            CgmesFlow cgmesFlow1 = interpretationModel.interpretXfmr2(alternative, transformer, Branch.Side.ONE);
            validationAlternativeResults.addTerminalFlow(new TerminalFlow(transformer.getId(), BranchEndType.XFMR2_ONE, terminal1.getP(), terminal1.getQ(),
                cgmesFlow1));

            Terminal terminal2 = transformer.getTerminal(Branch.Side.TWO);
            CgmesFlow cgmesFlow2 = interpretationModel.interpretXfmr2(alternative, transformer, Branch.Side.TWO);
            validationAlternativeResults.addTerminalFlow(new TerminalFlow(transformer.getId(), BranchEndType.XFMR2_TWO, terminal2.getP(), terminal2.getQ(),
                cgmesFlow2));
        });

    }

    private void validategetThreeWindingsTransformers(InterpretationAlternative alternative, Network network,
        ValidationAlternativeResults validationAlternativeResults) {
        network.getThreeWindingsTransformerStream().forEach(transformer -> {
            Terminal terminal1 = transformer.getTerminal(ThreeWindingsTransformer.Side.ONE);
            CgmesFlow cgmesFlow1 = interpretationModel.interpretXfmr3(alternative, transformer, ThreeWindingsTransformer.Side.ONE);
            validationAlternativeResults.addTerminalFlow(new TerminalFlow(transformer.getId(), BranchEndType.XFMR3_ONE, terminal1.getP(), terminal1.getQ(),
                cgmesFlow1));

            Terminal terminal2 = transformer.getTerminal(ThreeWindingsTransformer.Side.TWO);
            CgmesFlow cgmesFlow2 = interpretationModel.interpretXfmr3(alternative, transformer, ThreeWindingsTransformer.Side.TWO);
            validationAlternativeResults.addTerminalFlow(new TerminalFlow(transformer.getId(), BranchEndType.XFMR3_TWO, terminal2.getP(), terminal2.getQ(),
                cgmesFlow2));

            Terminal terminal3 = transformer.getTerminal(ThreeWindingsTransformer.Side.THREE);
            CgmesFlow cgmesFlow3 = interpretationModel.interpretXfmr3(alternative, transformer, ThreeWindingsTransformer.Side.THREE);
            validationAlternativeResults.addTerminalFlow(new TerminalFlow(transformer.getId(), BranchEndType.XFMR3_THREE, terminal3.getP(), terminal3.getQ(),
                cgmesFlow3));
        });
    }

    private CgmesModelConversion cgmes;
    private final InterpretationModel interpretationModel;
    private static final Logger LOG = LoggerFactory.getLogger(Validation.class);
}
