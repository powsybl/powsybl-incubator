package com.example.networkeditor;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.modification.topology.CreateCouplingDeviceBuilder;
import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder;
import com.powsybl.iidm.modification.topology.MoveFeederBayBuilder;
import com.powsybl.iidm.modification.topology.RemoveFeederBayBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.iidm.network.extensions.BusbarSectionPosition;
import com.powsybl.iidm.network.extensions.BusbarSectionPositionAdder;
import com.powsybl.iidm.network.extensions.ConnectablePositionAdder;
import com.powsybl.iidm.network.util.BusbarSectionFinderTraverser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

@org.springframework.stereotype.Service
public class Service {


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

    // APPLY: one change of the editor change set, with powsybl-iidm-modification or the IIDM API.

    public void apply(Network network, Changes.Change change) {
        switch (change) {
            case Changes.CreateFeederBay c -> createFeederBay(network, c);
            case Changes.CreateInjection c -> createInjection(network, c);
            case Changes.CreateSwitch c -> createSwitch(network, c);
            case Changes.CreateBusbarSection c -> createBusbarSection(network, c);
            case Changes.CreateCouplingDevice c -> new CreateCouplingDeviceBuilder()
                    .withBusOrBusbarSectionId1(c.busOrBusbarSectionId1())
                    .withBusOrBusbarSectionId2(c.busOrBusbarSectionId2())
                    .build()
                    .apply(network, true, ReportNode.NO_OP);
            case Changes.RemoveFeederBay c -> new RemoveFeederBayBuilder()
                    .withConnectableId(c.connectableId())
                    .build()
                    .apply(network, true, ReportNode.NO_OP);
            case Changes.RemoveElement c -> removeElement(network, c.id());
            case Changes.MoveFeederBay c -> moveFeederBay(network, c);
            case Changes.UpdateAttributes c -> updateAttributes(network, c);
            case Changes.Rename c -> rename(network, c);
            default -> throw new PowsyblException("Unsupported change " + change.getClass().getSimpleName());
        }
    }

    private void createFeederBay(Network network, Changes.CreateFeederBay change) {
        requireNewId(network, change.id());
        CreateFeederBayBuilder builder = new CreateFeederBayBuilder()
                .withInjectionAdder(injectionAdder(requireVoltageLevel(network, change.voltageLevelId()),
                        change.equipmentType(), change.id()))
                .withBusOrBusbarSectionId(change.busOrBusbarSectionId())
                .withLogOrThrowIfIncorrectPositionOrder(true);
        if (change.positionOrder() != null) {
            builder.withInjectionPositionOrder(change.positionOrder());
        }
        if (change.direction() != null) {
            builder.withInjectionDirection(change.direction());
        }
        builder.build().apply(network, true, ReportNode.NO_OP);
        applyAttributes(network.getIdentifiable(change.id()), change.attributes());
    }

    private void createInjection(Network network, Changes.CreateInjection change) {
        VoltageLevel voltageLevel = requireVoltageLevel(network, change.voltageLevelId());
        requireNewId(network, change.id());
        int node = change.node();
        if (change.behindSwitch() != null) {
            requireNewId(network, change.behindSwitch().id());
            int injectionNode = voltageLevel.getNodeBreakerView().getMaximumNodeIndex() + 1;
            addSwitch(voltageLevel, change.behindSwitch().id(), change.behindSwitch().kind(), node, injectionNode);
            node = injectionNode;
        }
        injectionAdder(voltageLevel, change.equipmentType(), change.id()).setNode(node).add();
        applyAttributes(network.getIdentifiable(change.id()), change.attributes());
    }

    private static void createSwitch(Network network, Changes.CreateSwitch change) {
        VoltageLevel voltageLevel = requireVoltageLevel(network, change.voltageLevelId());
        requireNewId(network, change.id());
        addSwitch(voltageLevel, change.id(), change.switchKind(), change.node1(), change.node2());
        network.getSwitch(change.id()).setOpen(change.open());
    }

    private static void createBusbarSection(Network network, Changes.CreateBusbarSection change) {
        VoltageLevel voltageLevel = requireVoltageLevel(network, change.voltageLevelId());
        requireNewId(network, change.id());
        BusbarSection busbarSection = voltageLevel.getNodeBreakerView().newBusbarSection()
                .setId(change.id())
                .setNode(voltageLevel.getNodeBreakerView().getMaximumNodeIndex() + 1)
                .add();
        busbarSection.newExtension(BusbarSectionPositionAdder.class)
                .withBusbarIndex(change.busbarIndex())
                .withSectionIndex(change.sectionIndex())
                .add();
    }

    private static void removeElement(Network network, String id) {
        Identifiable<?> identifiable = network.getIdentifiable(id);
        if (identifiable instanceof Connectable<?> connectable) {
            connectable.remove();
        } else if (identifiable instanceof Switch aSwitch) {
            aSwitch.getVoltageLevel().getNodeBreakerView().removeSwitch(id);
        } else {
            throw new PowsyblException("Cannot remove '" + id + "': neither a connectable nor a switch");
        }
    }

    private static void moveFeederBay(Network network, Changes.MoveFeederBay change) {
        Connectable<?> connectable = network.getConnectable(change.connectableId());
        if (connectable == null) {
            throw new PowsyblException("Unknown element '" + change.connectableId() + "'");
        }
        int side = sideOf(connectable, change.side());
        Terminal terminal = connectable.getTerminals().get(Math.max(side, 1) - 1);
        String target = change.targetBusOrBusbarSectionId();
        if (target != null && !target.equals(BusbarSectionFinderTraverser.findBusbarSectionId(terminal))) {
            new MoveFeederBayBuilder()
                    .withConnectableId(connectable.getId())
                    .withTargetVoltageLevelId(change.targetVoltageLevelId())
                    .withTargetBusOrBusBarSectionId(target)
                    .withTerminal(terminal)
                    .build()
                    .apply(network, true, ReportNode.NO_OP);
            connectable = network.getConnectable(change.connectableId());
        }

        ConnectablePosition.Feeder before = feederOf(connectable, side);
        Integer order = change.positionOrder() != null ? change.positionOrder()
                : before == null ? null : before.getOrder().orElse(null);
        ConnectablePosition.Direction direction = change.direction() != null ? change.direction()
                : before == null ? ConnectablePosition.Direction.BOTTOM : before.getDirection();
        if (order != null) {
            setFeederPosition(connectable, side, order, direction);
        }
    }

    private void updateAttributes(Network network, Changes.UpdateAttributes change) {
        Identifiable<?> identifiable = network.getIdentifiable(change.id());
        if (identifiable == null) {
            throw new PowsyblException("Unknown element '" + change.id() + "'");
        }
        applyAttributes(identifiable, change.attributes());
    }

    /** IIDM has no id change: the demo renames in place, feeder names included. */
    private static void rename(Network network, Changes.Rename change) {
        Connectable<?> connectable = network.getConnectable(change.id());
        if (connectable == null) {
            throw new PowsyblException("Connectable '" + change.id() + "' not found");
        }
        requireNewId(network, change.newId());
        connectable.setName(change.newId());
        connectable.setId(change.newId());
        renameFeeders(connectable, change.newId());
    }

    private static <C extends Connectable<C>> ConnectablePosition.Feeder feederOf(Connectable<C> connectable, int side) {
        ConnectablePosition<C> position = connectable.getExtension(ConnectablePosition.class);
        return position == null ? null : feederAt(position, side);
    }

    private static ConnectablePosition.Feeder feederAt(ConnectablePosition<?> position, int side) {
        return switch (side) {
            case 0 -> position.getFeeder();
            case 1 -> position.getFeeder1();
            case 2 -> position.getFeeder2();
            default -> position.getFeeder3();
        };
    }

    @SuppressWarnings("unchecked")
    private static <C extends Connectable<C>> void setFeederPosition(Connectable<C> connectable, int side,int order, ConnectablePosition.Direction direction) {
        ConnectablePosition<C> position = connectable.getExtension(ConnectablePosition.class);
        ConnectablePosition.Feeder feeder = position == null ? null : feederAt(position, side);
        if (feeder != null) {
            feeder.setOrder(order).setDirection(direction);
            return;
        }

        ConnectablePositionAdder<C> adder = connectable.newExtension(ConnectablePositionAdder.class);
        for (int s = 0; s <= 3; s++) {
            ConnectablePosition.Feeder kept = position == null ? null : feederAt(position, s);
            if (s == side) {
                newFeeder(adder, s).withName(connectable.getId()).withOrder(order).withDirection(direction).add();
            } else if (kept != null) {
                ConnectablePositionAdder.FeederAdder<C> copy = newFeeder(adder, s)
                        .withName(kept.getName().orElse(connectable.getId()))
                        .withDirection(kept.getDirection());
                kept.getOrder().ifPresent(copy::withOrder);
                copy.add();
            }
        }
        adder.add();
    }

    private static <C extends Connectable<C>> ConnectablePositionAdder.FeederAdder<C> newFeeder(ConnectablePositionAdder<C> adder, int side) {
        return switch (side) {
            case 0 -> adder.newFeeder();
            case 1 -> adder.newFeeder1();
            case 2 -> adder.newFeeder2();
            default -> adder.newFeeder3();
        };
    }

    private record FeederSlot(Connectable<?> connectable, int side, int section, Integer order,
                              ConnectablePosition.Direction direction) {
    }

    public void ensurePositions(VoltageLevel voltageLevel) {
        if (voltageLevel.getTopologyKind() != TopologyKind.NODE_BREAKER) {
            return;
        }

        List<FeederSlot> feeders = new ArrayList<>();
        for (Connectable<?> connectable : voltageLevel.getConnectables()) {
            if (connectable instanceof BusbarSection) {
                continue;
            }
            for (Terminal terminal : connectable.getTerminals()) {
                if (terminal.getVoltageLevel() != voltageLevel) {
                    continue;
                }
                int side = sideOf(connectable, terminal.getNodeBreakerView().getNode());
                ConnectablePosition.Feeder feeder = feederOf(connectable, side);
                ConnectablePosition.Direction direction = feeder == null
                        || feeder.getDirection() == ConnectablePosition.Direction.UNDEFINED
                        ? ConnectablePosition.Direction.BOTTOM : feeder.getDirection();
                feeders.add(new FeederSlot(connectable, side, sectionOf(voltageLevel, terminal),
                        feeder == null ? null : feeder.getOrder().orElse(null), direction));
            }
        }
        feeders.sort(Comparator.comparingInt(FeederSlot::section)
                .thenComparing(FeederSlot::order, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(feeder -> feeder.connectable().getId()));
        if (isConsistent(feeders)) {
            return;
        }

        int order = 0;
        for (FeederSlot feeder : feeders) {
            order += 10;
            setFeederPosition(feeder.connectable(), feeder.side(), order, feeder.direction());
        }
    }

    /** Sorted by section then order: every order is set, and they strictly grow along the busbars. */
    private static boolean isConsistent(List<FeederSlot> sortedFeeders) {
        int previous = Integer.MIN_VALUE;
        for (FeederSlot feeder : sortedFeeders) {
            if (feeder.order() == null || feeder.order() <= previous) {
                return false;
            }
            previous = feeder.order();
        }
        return true;
    }

    private static int sectionOf(VoltageLevel voltageLevel, Terminal terminal) {
        String bbsId = BusbarSectionFinderTraverser.findBusbarSectionId(terminal);
        BusbarSection bbs = bbsId == null ? null : voltageLevel.getNodeBreakerView().getBusbarSection(bbsId);
        BusbarSectionPosition position = bbs == null ? null : bbs.getExtension(BusbarSectionPosition.class);
        return position == null ? 0 : position.getSectionIndex();
    }

    private static int sideOf(Connectable<?> connectable, String side) {
        if (connectable instanceof Injection<?> || side == null) {
            return connectable instanceof Injection<?> ? 0 : 1;
        }
        return switch (side) {
            case "ONE" -> 1;
            case "TWO" -> 2;
            case "THREE" -> 3;
            default -> throw new PowsyblException("Unknown side '" + side + "'");
        };
    }

    private static int sideOf( Connectable<?> connectable, int node) {
        if ( connectable instanceof Injection<?>) {
            return 0;
        }

        List<? extends Terminal> terminals = connectable.getTerminals();
        for (int i=0; i< terminals.size(); i++) {
            if (terminals.get(i).getNodeBreakerView().getNode() == node) {
                return i+1;
            }
        }
        throw new PowsyblException("Node " + node + " not found on connectable " + connectable.getId());
    }

    //HELPERS
    private InjectionAdder<?, ?> injectionAdder(VoltageLevel voltageLevel, String equipmentType, String equipmentId) {
        return switch (equipmentType) {
            case "LOAD" -> voltageLevel.newLoad()
                    .setId(equipmentId)
                    .setP0(0).setQ0(0);

            case "GENERATOR" -> voltageLevel.newGenerator()
                    .setId(equipmentId)
                    .setMinP(0).setMaxP(0).setTargetP(0)
                    .setVoltageRegulatorOn(false).setTargetQ(0);

            case "BATTERY" -> voltageLevel.newBattery()
                    .setId(equipmentId)
                    .setMinP(0).setMaxP(0).setTargetP(0).setTargetQ(0);

            case "SHUNT" -> voltageLevel.newShuntCompensator()
                    .setId(equipmentId)
                    .setSectionCount(0)
                    .newLinearModel().setBPerSection(1).setMaximumSectionCount(1).add();

            case "STATIC_VAR_COMPENSATOR" -> voltageLevel.newStaticVarCompensator()
                    .setId(equipmentId)
                    .setBmin(0).setBmax(0)
                    .setRegulationMode(StaticVarCompensator.RegulationMode.REACTIVE_POWER)
                    .setRegulating(false).setReactivePowerSetpoint(0);

            case "VSC_CONVERTER_STATION" -> voltageLevel.newVscConverterStation()
                    .setId(equipmentId)
                    .setLossFactor(0)
                    .setVoltageRegulatorOn(false).setReactivePowerSetpoint(0);

            case "LCC_CONVERTER_STATION" -> voltageLevel.newLccConverterStation()
                    .setId(equipmentId)
                    .setLossFactor(0).setPowerFactor(1);

            case "BOUNDARY_LINE" -> voltageLevel.newBoundaryLine()
                    .setId(equipmentId)
                    .setP0(0).setQ0(0)
                    .setR(0).setX(0).setG(0).setB(0);

            default -> throw new PowsyblException(
                    "Cannot create '" + equipmentType + "'");
        };
    }

    private static void addSwitch(VoltageLevel voltageLevel, String switchId, String switchType, int node1, int node2) {
        VoltageLevel.NodeBreakerView view = voltageLevel.getNodeBreakerView();
        switch (switchType) {
            case "BREAKER" -> view.newBreaker()
                    .setId(switchId).setNode1(node1).setNode2(node2).add();
            case "DISCONNECTOR" -> view.newDisconnector()
                    .setId(switchId).setNode1(node1).setNode2(node2).add();
            case "LOAD_BREAK_SWITCH" -> view.newSwitch()
                    .setId(switchId).setKind(SwitchKind.LOAD_BREAK_SWITCH).setNode1(node1).setNode2(node2).add();
            default -> throw new PowsyblException("Cannot create switch of type '" + switchType + "'");
        }
    }

    private static <C extends Connectable<C>> void renameFeeders(Connectable<C> connectable, String name) {
        ConnectablePosition<C> position = connectable.getExtension(ConnectablePosition.class);
        if (position == null) {
            return;
        }
        for (ConnectablePosition.Feeder feeder : Arrays.asList(
                position.getFeeder(), position.getFeeder1(), position.getFeeder2(), position.getFeeder3())) {
            if (feeder != null) {
                feeder.setName(name);
            }
        }
    }

    private static VoltageLevel requireVoltageLevel(Network network, String vlId) {
        VoltageLevel voltageLevel = network.getVoltageLevel(vlId);
        if (voltageLevel == null) {
            throw new PowsyblException("Unknown voltage level '" + vlId + "'");
        }
        return voltageLevel;
    }

    private static void requireNewId(Network network, String equipmentId) {
        if (network.getIdentifiable(equipmentId) != null) {
            throw new PowsyblException("Element '" + equipmentId + "' already exists");
        }
    }

    private void applyAttributes(Identifiable<?> identifiable, Map<String, Object> attributes) {
        if (attributes == null) {
            return;
        }
        applyValues(identifiable, attributes);
        applyConnections(identifiable, attributes);
    }

    private void applyValues(Identifiable<?> identifiable, Map<String, Object> payload) {
        if (identifiable instanceof Switch aSwitch) {
            setBoolean(payload, "open", aSwitch::setOpen);

        } else if (identifiable instanceof Load load) {
            setDouble(payload, "p0", load::setP0);
            setDouble(payload, "q0", load::setQ0);
            setText(payload, "loadType", type -> load.setLoadType(LoadType.valueOf(type)));

        } else if (identifiable instanceof Generator generator) {
            setDouble(payload, "minP", generator::setMinP);
            setDouble(payload, "maxP", generator::setMaxP);
            setDouble(payload, "targetP", generator::setTargetP);
            setDouble(payload, "targetQ", generator::setTargetQ);
            setDouble(payload, "targetV", generator::setTargetV);
            setBoolean(payload, "voltageRegulatorOn", generator::setVoltageRegulatorOn);
            setText(payload, "energySource", source -> generator.setEnergySource(EnergySource.valueOf(source)));

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
