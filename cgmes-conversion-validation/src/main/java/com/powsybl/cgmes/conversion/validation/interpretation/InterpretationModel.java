package com.powsybl.cgmes.conversion.validation.interpretation;

import com.powsybl.cgmes.conversion.validation.TerminalFlow.CgmesFlow;
import com.powsybl.cgmes.conversion.validation.model.CgmesModelConversion;
import com.powsybl.cgmes.interpretation.InterpretedComputedFlow;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesLine;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformer;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class InterpretationModel {

    public InterpretationModel(CgmesModelConversion cgmes) {
        interpretedModel = new CgmesModelForInterpretation(cgmes.name(), cgmes, true);
    }

    public CgmesFlow interpretLine(InterpretationAlternative alternative, Line line, Side side) {

        if (interpretedModel.getLine(line.getId()) == null) {
            return null;
        }

        if (line.getTerminal(side).isConnected()) {
            return lineFlow(alternative, line.getId(), side);
        }
        return null;
    }

    public CgmesFlow interpretDanglingLine(InterpretationAlternative alternative, DanglingLine line) {

        if (interpretedModel.getLine(line.getId()) == null) {
            return null;
        }

        if (line.getTerminal().isConnected()) {
            return danglingLineFlow(alternative, line.getId());
        }
        return null;
    }

    public CgmesFlow interpretXfmr2(InterpretationAlternative alternative, TwoWindingsTransformer transformer,
        Side side) {

        if (interpretedModel.getTransformer(transformer.getId()) == null) {
            return null;
        }

        if (transformer.getTerminal(side).isConnected()) {
            return xfmr2Flow(alternative, transformer.getId(), side);
        }
        return null;
    }

    public CgmesFlow interpretXfmr3(InterpretationAlternative alternative, ThreeWindingsTransformer transformer,
        ThreeWindingsTransformer.Side side) {

        if (interpretedModel.getTransformer(transformer.getId()) == null) {
            return null;
        }

        if (transformer.getTerminal(side).isConnected()) {
            return xfmr3Flow(alternative, transformer.getId(), side);
        }
        return null;
    }

    private CgmesFlow danglingLineFlow(InterpretationAlternative alternative,
        String id) {

        CgmesLine line = interpretedModel.getLine(id);
        CgmesModel model = interpretedModel.cgmes();
        for (PropertyBag node : model.boundaryNodes()) {
            if (node.getId("Node").equals(line.nodeId1())) {
                return lineFlow(alternative, id, Branch.Side.TWO);
            } else if (node.getId("Node").equals(line.nodeId2())) {
                return lineFlow(alternative, id, Branch.Side.ONE);
            }
        }
        return null;
    }

    private CgmesFlow lineFlow(InterpretationAlternative alternative, String id,
        Branch.Side side) {

        String n;
        CgmesLine line = interpretedModel.getLine(id);
        if (side == Branch.Side.ONE) {
            n = line.nodeId1();
        } else {
            n = line.nodeId2();
        }
        InterpretedComputedFlow calcFlow = InterpretedComputedFlow.forEquipment(id, n, alternative, interpretedModel);

        return new CgmesFlow(calcFlow.p(), calcFlow.q(), calcFlow.isCalculated());
    }

    private CgmesFlow xfmr2Flow(InterpretationAlternative alternative, String id,
        Branch.Side side) {

        String n;
        CgmesTransformer transformer = interpretedModel.getTransformer(id);
        if (side == Branch.Side.ONE) {
            n = transformer.end1().nodeId();
        } else {
            n = transformer.end2().nodeId();
        }
        InterpretedComputedFlow calcFlow = InterpretedComputedFlow.forEquipment(id, n, alternative, interpretedModel);

        return new CgmesFlow(calcFlow.p(), calcFlow.q(), calcFlow.isCalculated());
    }

    private CgmesFlow xfmr3Flow(InterpretationAlternative alternative, String id,
        ThreeWindingsTransformer.Side side) {

        String n;
        CgmesTransformer transformer = interpretedModel.getTransformer(id);
        if (side == ThreeWindingsTransformer.Side.ONE) {
            n = transformer.end1().nodeId();
        } else if (side == ThreeWindingsTransformer.Side.TWO) {
            n = transformer.end2().nodeId();
        } else {
            n = transformer.end3().nodeId();
        }
        InterpretedComputedFlow calcFlow = InterpretedComputedFlow.forEquipment(id, n, alternative, interpretedModel);

        return new CgmesFlow(calcFlow.p(), calcFlow.q(), calcFlow.isCalculated());
    }

    final CgmesModelForInterpretation interpretedModel;
}
