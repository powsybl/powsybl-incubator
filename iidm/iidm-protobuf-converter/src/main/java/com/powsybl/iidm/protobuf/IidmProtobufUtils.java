/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.protobuf;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.protobuf.proto.Iidm;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public final class IidmProtobufUtils {
    private IidmProtobufUtils() {
    }

    public static Iidm.TopologyKind iidmToProtoTopologyKind(TopologyKind tKind) {
        return tKind.equals(TopologyKind.NODE_BREAKER) ? Iidm.TopologyKind.TopologyKind_NODE_BREAKER : Iidm.TopologyKind.TopologyKind_BUS_BREAKER;
    }

    public static TopologyKind protoToIidmTopologyKind(Iidm.TopologyKind tKind) {
        return tKind.equals(Iidm.TopologyKind.TopologyKind_NODE_BREAKER) ? TopologyKind.NODE_BREAKER : TopologyKind.BUS_BREAKER;
    }

    public static LoadType protoToIidmLoadType(Iidm.LoadType ploadType) {
        switch (ploadType) {
            case LoadType_FICTITIOUS:
                return LoadType.FICTITIOUS;
            case LoadType_AUXILIARY:
                return LoadType.AUXILIARY;
            default:
                return LoadType.UNDEFINED;
        }
    }

    public static Iidm.LoadType iidmtoProtoLoadType(LoadType loadType) {
        switch (loadType) {
            case FICTITIOUS:
                return Iidm.LoadType.LoadType_FICTITIOUS;
            case AUXILIARY:
                return Iidm.LoadType.LoadType_AUXILIARY;
            default:
                return Iidm.LoadType.LoadType_UNDEFINED;
        }
    }

    public static EnergySource protoToIidmEnergySource(Iidm.EnergySource pEnergySource) {
        switch (pEnergySource) {
            case EnergySource_WIND:
                return EnergySource.WIND;
            case EnergySource_HYDRO:
                return EnergySource.HYDRO;
            case EnergySource_SOLAR:
                return EnergySource.SOLAR;
            case EnergySource_NUCLEAR:
                return EnergySource.NUCLEAR;
            case EnergySource_THERMAL:
                return EnergySource.THERMAL;
            default:
                return EnergySource.OTHER;
        }

    }

    public static Iidm.EnergySource iidmToProtoEnergySource(EnergySource energySource) {
        switch (energySource) {
            case WIND:
                return Iidm.EnergySource.EnergySource_WIND;
            case THERMAL:
                return Iidm.EnergySource.EnergySource_THERMAL;
            case HYDRO:
                return Iidm.EnergySource.EnergySource_HYDRO;
            case NUCLEAR:
                return Iidm.EnergySource.EnergySource_NUCLEAR;
            case SOLAR:
                return Iidm.EnergySource.EnergySource_SOLAR;
            default:
                return Iidm.EnergySource.EnergySource_OTHER;
        }
    }

    public static Iidm.Side iidmToProtoBranchSide(Branch.Side side) {
        switch (side) {
            case ONE:
                return Iidm.Side.Side_ONE;
            case TWO:
                return Iidm.Side.Side_TWO;
            default:
                throw new AssertionError("Unexpected value " + side);
        }
    }

    public static Branch.Side protoToIidmBranchSide(Iidm.Side side) {
        switch (side) {
            case Side_ONE:
                return Branch.Side.ONE;
            case Side_TWO:
                return Branch.Side.TWO;
            default:
                throw new AssertionError("Unexpected value " + side);
        }
    }

    public static Iidm.Side iidmToProtoThreeWindingTransformerSide(ThreeWindingsTransformer.Side side) {
        switch (side) {
            case ONE:
                return Iidm.Side.Side_ONE;
            case TWO:
                return Iidm.Side.Side_TWO;
            case THREE:
                return Iidm.Side.Side_THREE;
            default:
                throw new AssertionError("Unexpected value " + side);
        }
    }

    public static ThreeWindingsTransformer.Side protoToIidmThreeWindingTransformerSide(Iidm.Side side) {
        switch (side) {
            case Side_ONE:
                return ThreeWindingsTransformer.Side.ONE;
            case Side_TWO:
                return ThreeWindingsTransformer.Side.TWO;
            case Side_THREE:
                return ThreeWindingsTransformer.Side.THREE;
            default:
                throw new AssertionError("Unexpected value " + side);
        }
    }

    public static Iidm.PhaseRegulationMode iidmToProtoPhaseRegulationMode(PhaseTapChanger.RegulationMode mode) {
        switch (mode) {
            case FIXED_TAP:
                return Iidm.PhaseRegulationMode.PhaseRegulationMode_FIXED_TAP;
            case CURRENT_LIMITER:
                return Iidm.PhaseRegulationMode.PhaseRegulationMode_CURRENT_LIMITER;
            case ACTIVE_POWER_CONTROL:
                return Iidm.PhaseRegulationMode.PhaseRegulationMode_ACTIVE_POWER_CONTROL;
            default:
                throw new AssertionError("Unexpected value " + mode);
        }
    }

    public static PhaseTapChanger.RegulationMode protoToIidmPhaseRegulationMode(Iidm.PhaseRegulationMode pMode) {
        switch (pMode) {
            case PhaseRegulationMode_FIXED_TAP:
                return PhaseTapChanger.RegulationMode.FIXED_TAP;
            case PhaseRegulationMode_CURRENT_LIMITER:
                return PhaseTapChanger.RegulationMode.CURRENT_LIMITER;
            case PhaseRegulationMode_ACTIVE_POWER_CONTROL:
                return PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL;
            default:
                throw new AssertionError("Unexpected value " + pMode);
        }
    }

    public static Iidm.SwitchKind iidmToProtoSwitchKind(SwitchKind switchKind) {
        switch (switchKind) {
            case BREAKER:
                return Iidm.SwitchKind.SwitchKind_BREAKER;
            case DISCONNECTOR:
                return Iidm.SwitchKind.SwitchKind_DISCONNECTOR;
            case LOAD_BREAK_SWITCH:
                return Iidm.SwitchKind.SwitchKind_LOAD_BREAK_SWITCH;
            default:
                throw new AssertionError("Unexpected value " + switchKind);
        }
    }

    public static SwitchKind protoToIidmSwitchKind(Iidm.SwitchKind switchKind) {
        switch (switchKind) {
            case SwitchKind_BREAKER:
                return SwitchKind.BREAKER;
            case SwitchKind_DISCONNECTOR:
                return SwitchKind.DISCONNECTOR;
            case SwitchKind_LOAD_BREAK_SWITCH:
                return SwitchKind.LOAD_BREAK_SWITCH;
            default:
                throw new AssertionError("Unexpected value " + switchKind);
        }
    }

    public static Iidm.StaticVarCompensatorRegulationMode iidmToProtoStaticVarRegulationMode(StaticVarCompensator.RegulationMode regulationMode) {
        switch (regulationMode) {
            case OFF:
                return Iidm.StaticVarCompensatorRegulationMode.StaticVarCompensatorRegulationMode_OFF;
            case VOLTAGE:
                return Iidm.StaticVarCompensatorRegulationMode.StaticVarCompensatorRegulationMode_VOLTAGE;
            case REACTIVE_POWER:
                return Iidm.StaticVarCompensatorRegulationMode.StaticVarCompensatorRegulationMode_REACTIVE_POWER;
            default:
                throw new AssertionError("Unexpected value " + regulationMode);
        }
    }

    public static StaticVarCompensator.RegulationMode protoToIidmStaticVarCompensatorRegulationMode(Iidm.StaticVarCompensatorRegulationMode regulationMode) {
        switch (regulationMode) {
            case StaticVarCompensatorRegulationMode_OFF:
                return StaticVarCompensator.RegulationMode.OFF;
            case StaticVarCompensatorRegulationMode_VOLTAGE:
                return StaticVarCompensator.RegulationMode.VOLTAGE;
            case StaticVarCompensatorRegulationMode_REACTIVE_POWER:
                return StaticVarCompensator.RegulationMode.REACTIVE_POWER;
            default:
                throw new AssertionError("Unexpected value " + regulationMode);
        }
    }

    public static Iidm.ConvertersMode iidmToProtoConvertersMode(HvdcLine.ConvertersMode mode) {
        switch (mode) {
            case SIDE_1_INVERTER_SIDE_2_RECTIFIER:
                return Iidm.ConvertersMode.ConvertersMode_SIDE_1_INVERTER_SIDE_2_RECTIFIER;
            case SIDE_1_RECTIFIER_SIDE_2_INVERTER:
                return Iidm.ConvertersMode.ConvertersMode_SIDE_1_RECTIFIER_SIDE_2_INVERTER;
            default:
                throw new AssertionError("Unexpected value " + mode);
        }
    }

    public static HvdcLine.ConvertersMode protoToIidmConvertersMode(Iidm.ConvertersMode mode) {
        switch (mode) {
            case ConvertersMode_SIDE_1_INVERTER_SIDE_2_RECTIFIER:
                return HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER;
            case ConvertersMode_SIDE_1_RECTIFIER_SIDE_2_INVERTER:
                return HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER;
            default:
                throw new AssertionError("Unexpected value " + mode);
        }
    }

}

