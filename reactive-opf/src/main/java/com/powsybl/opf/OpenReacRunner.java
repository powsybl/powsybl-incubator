package com.powsybl.opf;

import com.powsybl.ampl.executor.AmplModelRunner;
import com.powsybl.ampl.executor.AmplResults;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.opf.parameters.OpenReacAmplIOFiles;
import com.powsybl.opf.parameters.OpenReacParameters;
import com.powsybl.opf.parameters.OpenReacResults;
import com.powsybl.opf.parameters.OpenReacStatus;
import org.apache.commons.lang3.tuple.Pair;

public final class OpenReacRunner {

    private OpenReacRunner() {
    }

    public static OpenReacResults runOpenReac(Network network, String variant, OpenReacParameters parameters) {
        AmplModel reactiveOpf = AmplModel.REACTIVE_OPF;
        ComputationManager manager = LocalComputationManager.getDefault();
        OpenReacAmplIOFiles amplIoInterface = new OpenReacAmplIOFiles(variant, parameters);
        preRunModifications(network, parameters);
        AmplResults run = AmplModelRunner.run(network, variant, reactiveOpf, manager, amplIoInterface);
        return new OpenReacResults(run.isSuccess() ? OpenReacStatus.OK : OpenReacStatus.NOT_OK,
                amplIoInterface.getReactiveInvestments());
    }

    /**
     * This function allows modifications before running the Ampl Run.
     * <p>
     * For now, we only modify the bounds of voltage levels.
     */
    private static void preRunModifications(Network network, OpenReacParameters params) {
        for (String voltageLevelId : params.getSpecificVoltageDelta().keySet()) {
            Pair<Double, Double> bounds = params.getSpecificVoltageDelta().get(voltageLevelId);
            double nominalV = network.getVoltageLevel(voltageLevelId).getNominalV();
            network.getVoltageLevel(voltageLevelId)
                   .setLowVoltageLimit(nominalV * bounds.getLeft())
                   .setHighVoltageLimit(nominalV * bounds.getRight());
        }
    }
}
