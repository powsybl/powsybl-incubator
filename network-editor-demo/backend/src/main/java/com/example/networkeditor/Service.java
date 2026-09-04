package com.example.networkeditor;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.modification.topology.RemoveFeederBayBuilder;
import com.powsybl.iidm.network.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

@org.springframework.stereotype.Service
public class Service {


    public void removeElement(Network n, String elementId) throws PowsyblException {

        Identifiable<?> identifiable = n.getIdentifiable(elementId);
        if(identifiable == null) {
            throw new PowsyblException("Element with id " + elementId + " not found");
        }

        try {
            if (identifiable instanceof Connectable<?> connectable) {
                connectable.remove();
            } else if (identifiable instanceof Switch aSwitch) {
                aSwitch.getVoltageLevel().getNodeBreakerView().removeSwitch(elementId);
            } else {
                throw new PowsyblException("Failed to delete '" + elementId + "' is neither a connectable nor a switch");
            }
        } catch (PowsyblException e) {
            throw new PowsyblException("Failed to delete '" + elementId + "': " + e.getMessage());
        }
    }

    public void removeBay(Network n, String elementId) throws PowsyblException {
        try {
            new RemoveFeederBayBuilder().withConnectableId(elementId).build().apply(n);
        } catch (PowsyblException e) {
            throw new PowsyblException("Failed to delete bay '" + elementId + "': " + e.getMessage());
        }
    }

    public Map<String, Map<String, Object>> readProperties(Network network, String vlId) {
        VoltageLevel voltageLevel = network.getVoltageLevel(vlId);
        if (voltageLevel == null) {
            throw new PowsyblException("Unknown voltage level '" + vlId + "'");
        }

        Map<String, Map<String, Object>> properties = new LinkedHashMap<>();
        voltageLevel.getSwitches().forEach(aSwitch -> properties.put(aSwitch.getId(), readProperties(aSwitch)));
        voltageLevel.getConnectables().forEach(connectable -> properties.put(connectable.getId(), readProperties(connectable)));
        return properties;
    }

    private Map<String, Object> readProperties(Identifiable<?> identifiable) {
        Map<String, Object> properties = new LinkedHashMap<>();

        if (identifiable instanceof Switch aSwitch) {
            properties.put("open", aSwitch.isOpen());

        } else if (identifiable instanceof Load load) {
            putDouble(properties, "p0", load.getP0());
            putDouble(properties, "q0", load.getQ0());

        } else if (identifiable instanceof Generator generator) {
            putDouble(properties, "minP", generator.getMinP());
            putDouble(properties, "maxP", generator.getMaxP());
            putDouble(properties, "targetP", generator.getTargetP());
            putDouble(properties, "targetQ", generator.getTargetQ());
            putDouble(properties, "targetV", generator.getTargetV());
            properties.put("voltageRegulatorOn", generator.isVoltageRegulatorOn());

        } else if (identifiable instanceof Battery battery) {
            putDouble(properties, "minP", battery.getMinP());
            putDouble(properties, "maxP", battery.getMaxP());
            putDouble(properties, "targetP", battery.getTargetP());
            putDouble(properties, "targetQ", battery.getTargetQ());

        } else if (identifiable instanceof ShuntCompensator shunt) {
            properties.put("sectionCount", shunt.getSectionCount());
            properties.put("maximumSectionCount", shunt.getMaximumSectionCount());
            if (shunt.getModelType() == ShuntCompensatorModelType.LINEAR) {
                ShuntCompensatorLinearModel model = shunt.getModel(ShuntCompensatorLinearModel.class);
                putDouble(properties, "bPerSection", model.getBPerSection());
                putDouble(properties, "gPerSection", model.getGPerSection());
            }

        } else if (identifiable instanceof StaticVarCompensator svc) {
            putDouble(properties, "bMin", svc.getBmin());
            putDouble(properties, "bMax", svc.getBmax());
            putDouble(properties, "voltageSetpoint", svc.getVoltageSetpoint());
            putDouble(properties, "reactivePowerSetpoint", svc.getReactivePowerSetpoint());
            properties.put("regulationMode", svc.getRegulationMode().name());
            properties.put("regulating", svc.isRegulating());

        } else if (identifiable instanceof VscConverterStation vsc) {
            putDouble(properties, "lossFactor", vsc.getLossFactor());
            putDouble(properties, "voltageSetpoint", vsc.getVoltageSetpoint());
            putDouble(properties, "reactivePowerSetpoint", vsc.getReactivePowerSetpoint());
            properties.put("voltageRegulatorOn", vsc.isVoltageRegulatorOn());

        } else if (identifiable instanceof LccConverterStation lcc) {
            putDouble(properties, "lossFactor", lcc.getLossFactor());
            putDouble(properties, "powerFactor", lcc.getPowerFactor());

        } else if (identifiable instanceof BoundaryLine boundaryLine) {
            putDouble(properties, "p0", boundaryLine.getP0());
            putDouble(properties, "q0", boundaryLine.getQ0());
            putDouble(properties, "r", boundaryLine.getR());
            putDouble(properties, "x", boundaryLine.getX());
            putDouble(properties, "g", boundaryLine.getG());
            putDouble(properties, "b", boundaryLine.getB());

        } else if (identifiable instanceof Line line) {
            putDouble(properties, "r", line.getR());
            putDouble(properties, "x", line.getX());

        } else if (identifiable instanceof TwoWindingsTransformer transformer) {
            putDouble(properties, "r", transformer.getR());
            putDouble(properties, "x", transformer.getX());

        } else if (identifiable instanceof ThreeWindingsTransformer transformer) {
            for (ThreeWindingsTransformer.Leg leg : transformer.getLegs()) {
                int side = leg.getSide().getNum();
                putDouble(properties, "r" + side, leg.getR());
                putDouble(properties, "x" + side, leg.getX());
            }
        }

        readConnections(identifiable, properties);
        return properties;
    }

    private void readConnections(Identifiable<?> identifiable, Map<String, Object> properties) {
        if (identifiable instanceof Injection<?> injection) {
            properties.put("connected", injection.getTerminal().isConnected());

        } else if (identifiable instanceof Branch<?> branch) {
            properties.put("connected1", branch.getTerminal1().isConnected());
            properties.put("connected2", branch.getTerminal2().isConnected());

        } else if (identifiable instanceof ThreeWindingsTransformer transformer) {
            for (ThreeWindingsTransformer.Leg leg : transformer.getLegs()) {
                properties.put("connected" + leg.getSide().getNum(), leg.getTerminal().isConnected());
            }
        }
    }

    private static void putDouble(Map<String, Object> properties, String key, double value) {
        if (!Double.isNaN(value)) {
            properties.put(key, value);
        }
    }

    public void update(Network network, Dto.ChangeEntry change) {
        Identifiable<?> identifiable = network.getIdentifiable(change.equipmentId());
        if (identifiable == null) {
            throw new PowsyblException("Unknown element '" + change.equipmentId() + "'");
        }

        applyProperties(identifiable, change.payload());
        applyConnections(identifiable, change.payload());
    }

    private void applyProperties(Identifiable<?> identifiable, Map<String, Object> payload) {
        if (identifiable instanceof Switch aSwitch) {
            setBoolean(payload, "open", aSwitch::setOpen);

        } else if (identifiable instanceof Load load) {
            setDouble(payload, "p0", load::setP0);
            setDouble(payload, "q0", load::setQ0);

        } else if (identifiable instanceof Generator generator) {
            setDouble(payload, "minP", generator::setMinP);
            setDouble(payload, "maxP", generator::setMaxP);
            setDouble(payload, "targetP", generator::setTargetP);
            setDouble(payload, "targetQ", generator::setTargetQ);
            setDouble(payload, "targetV", generator::setTargetV);
            setBoolean(payload, "voltageRegulatorOn", generator::setVoltageRegulatorOn);

        } else if (identifiable instanceof Battery battery) {
            setDouble(payload, "minP", battery::setMinP);
            setDouble(payload, "maxP", battery::setMaxP);
            setDouble(payload, "targetP", battery::setTargetP);
            setDouble(payload, "targetQ", battery::setTargetQ);

        } else if (identifiable instanceof ShuntCompensator shunt) {
            ShuntCompensatorLinearModel model = shunt.getModel(ShuntCompensatorLinearModel.class);
            setInt(payload, "maximumSectionCount", model::setMaximumSectionCount);
            setDouble(payload, "bPerSection", model::setBPerSection);
            setDouble(payload, "gPerSection", model::setGPerSection);
            setInt(payload, "sectionCount", shunt::setSectionCount);

        } else if (identifiable instanceof StaticVarCompensator svc) {
            setDouble(payload, "bMin", svc::setBmin);
            setDouble(payload, "bMax", svc::setBmax);
            setDouble(payload, "voltageSetpoint", svc::setVoltageSetpoint);
            setDouble(payload, "reactivePowerSetpoint", svc::setReactivePowerSetpoint);
            setText(payload, "regulationMode", mode -> svc.setRegulationMode(regulationMode(mode)));
            setBoolean(payload, "regulating", svc::setRegulating);

        } else if (identifiable instanceof VscConverterStation vsc) {
            setDouble(payload, "lossFactor", value -> vsc.setLossFactor((float) value));
            setDouble(payload, "voltageSetpoint", vsc::setVoltageSetpoint);
            setDouble(payload, "reactivePowerSetpoint", vsc::setReactivePowerSetpoint);
            setBoolean(payload, "voltageRegulatorOn", vsc::setVoltageRegulatorOn);

        } else if (identifiable instanceof LccConverterStation lcc) {
            setDouble(payload, "lossFactor", value -> lcc.setLossFactor((float) value));
            setDouble(payload, "powerFactor", value -> lcc.setPowerFactor((float) value));

        } else if (identifiable instanceof BoundaryLine boundaryLine) {
            setDouble(payload, "p0", boundaryLine::setP0);
            setDouble(payload, "q0", boundaryLine::setQ0);
            setDouble(payload, "r", boundaryLine::setR);
            setDouble(payload, "x", boundaryLine::setX);
            setDouble(payload, "g", boundaryLine::setG);
            setDouble(payload, "b", boundaryLine::setB);

        } else if (identifiable instanceof Line line) {
            setDouble(payload, "r", line::setR);
            setDouble(payload, "x", line::setX);

        } else if (identifiable instanceof TwoWindingsTransformer transformer) {
            setDouble(payload, "r", transformer::setR);
            setDouble(payload, "x", transformer::setX);

        } else if (identifiable instanceof ThreeWindingsTransformer transformer) {
            for (ThreeWindingsTransformer.Leg leg : transformer.getLegs()) {
                int side = leg.getSide().getNum();
                setDouble(payload, "r" + side, leg::setR);
                setDouble(payload, "x" + side, leg::setX);
            }

        } else if (!(identifiable instanceof Ground)) {
            throw new PowsyblException("Update not supported for '" + identifiable.getId() + "'");
        }
    }

    private void applyConnections(Identifiable<?> identifiable, Map<String, Object> payload) {
        if (identifiable instanceof Injection<?> injection) {
            setConnected(payload, "connected", injection.getTerminal());

        } else if (identifiable instanceof Branch<?> branch) {
            setConnected(payload, "connected1", branch.getTerminal1());
            setConnected(payload, "connected2", branch.getTerminal2());

        } else if (identifiable instanceof ThreeWindingsTransformer transformer) {
            for (ThreeWindingsTransformer.Leg leg : transformer.getLegs()) {
                setConnected(payload, "connected" + leg.getSide().getNum(), leg.getTerminal());
            }
        }
    }

    private static StaticVarCompensator.RegulationMode regulationMode(String value) {
        try {
            return StaticVarCompensator.RegulationMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new PowsyblException("Unknown regulation mode '" + value + "'");
        }
    }


    private static void setDouble(Map<String, Object> payload, String key, DoubleConsumer setter) {
        Object value = payload.get(key);
        if (value != null) {
            if (!(value instanceof Number number)) {
                throw new PowsyblException("Property '" + key + "' must be a number");
            }
            setter.accept(number.doubleValue());
        }
    }

    private static void setInt(Map<String, Object> payload, String key, IntConsumer setter) {
        setDouble(payload, key, value -> setter.accept((int) value));
    }

    private static void setBoolean(Map<String, Object> payload, String key, Consumer<Boolean> setter) {
        Object value = payload.get(key);
        if (value != null) {
            if (!(value instanceof Boolean bool)) {
                throw new PowsyblException("Property '" + key + "' must be a boolean");
            }
            setter.accept(bool);
        }
    }

    private static void setText(Map<String, Object> payload, String key, Consumer<String> setter) {
        Object value = payload.get(key);
        if (value != null) {
            setter.accept(String.valueOf(value));
        }
    }

    private static void setConnected(Map<String, Object> payload, String key, Terminal terminal) {
        setBoolean(payload, key, connected -> {
            if (connected) {
                terminal.connect();
            } else {
                terminal.disconnect();
            }
        });
    }
}
