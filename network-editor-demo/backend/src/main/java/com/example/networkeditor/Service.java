package com.example.networkeditor;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder;
import com.powsybl.iidm.modification.topology.MoveFeederBayBuilder;
import com.powsybl.iidm.modification.topology.RemoveFeederBayBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.iidm.network.extensions.BusbarSectionPosition;
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

    public void apply(Network network, Dto.ChangeEntry change) {
        switch (change.op()) {
            case "create" -> create(network, change);
            case "create-bay" -> createBay(network, change);
            case "create-switch" -> createSwitch(network, change);
            case "create-switched-injection" -> createSwitchedInjection(network, change);
            case "update" -> update(network, change);
            case "update-position" -> updatePosition(network, change);
            case "move-bay" -> moveBay(network, change);
            case "rename" -> renameEquipment(network, change);
            case "replace" -> replace(network, change);
            default -> throw new PowsyblException("Unsupported change '" + change.op() + "'");
        }
    }

    public void renameEquipment(Network network, Dto.ChangeEntry change) {
        Connectable<?> connectable = network.getConnectable(change.equipmentId());
        if (connectable == null) {
            throw new PowsyblException("Connectable '" + change.equipmentId() + "' not found");
        }
        String newId = requireText(change.payload(), "newId");
        requireNewId(network, newId);

        connectable.setName(newId);
        connectable.setId(newId);
        renameFeeders(connectable, newId);
    }

    public void replace(Network network, Dto.ChangeEntry change) {
        if (!(network.getIdentifiable(change.equipmentId()) instanceof Injection<?> old)) {
            throw new PowsyblException("Injection '" + change.equipmentId() + "' not found");
        }
        Map<String, Object> payload = change.payload();
        String newId = requireText(payload, "newId");
        if (!newId.equals(old.getId())) {
            requireNewId(network, newId);
        }

        VoltageLevel voltageLevel = old.getTerminal().getVoltageLevel();
        int node = old.getTerminal().getNodeBreakerView().getNode();
        ConnectablePosition.Feeder feeder = feederOf(old, 0);
        Integer order = feeder == null ? null : feeder.getOrder().orElse(null);
        ConnectablePosition.Direction direction = feeder == null ? null : feeder.getDirection();

        old.remove();
        injectionAdder(voltageLevel, requireText(payload, "equipmentType"), newId)
                .setNode(node)
                .add();

        Connectable<?> created = network.getConnectable(newId);
        if (order != null) {
            setFeederPosition(created, 0, order, direction);
        }
        applyProperties(created, properties(payload));
    }

    public void create(Network network, Dto.ChangeEntry change) {
        Map<String, Object> payload = change.payload();
        VoltageLevel voltageLevel = requireVoltageLevel(network, requireText(payload, "vlId"));
        requireNewId(network, change.equipmentId());

        injectionAdder(voltageLevel, requireText(payload, "equipmentType"), change.equipmentId())
                .setNode(requireInt(payload, "node"))
                .add();

        if (payload.get("order") instanceof Number) {
            int order = requireInt(payload, "order");
            ConnectablePosition.Direction direction = payload.get("direction") == null
                    ? ConnectablePosition.Direction.BOTTOM
                    : direction(requireText(payload, "direction"));
            Connectable<?> connectable = network.getConnectable(change.equipmentId());
            try {
                setFeederPosition(connectable, 0, order, direction);
            } catch (IllegalArgumentException e) {
                throw new PowsyblException("Failed to set position of '" + change.equipmentId() + "': " + e.getMessage());
            }
        }

        applyProperties(network.getIdentifiable(change.equipmentId()), properties(payload));
    }

    public void createBay(Network network, Dto.ChangeEntry change) {
        Map<String, Object> payload = change.payload();
        String equipmentId = change.equipmentId();
        String bbsId = requireText(payload, "busbarSectionId");

        BusbarSection bbs = network.getBusbarSection(bbsId);
        if (bbs == null) {
            throw new PowsyblException("Unknown busbar section '" + bbsId + "'");
        }

        requireNewId(network, equipmentId);

        InjectionAdder<?, ?> adder = injectionAdder(bbs.getTerminal().getVoltageLevel(),
                requireText(payload, "equipmentType"), equipmentId);

        new CreateFeederBayBuilder()
                .withInjectionAdder(adder)
                .withBusOrBusbarSectionId(bbsId)
                .withInjectionPositionOrder(requireInt(payload, "order"))
                .withInjectionDirection(direction(requireText(payload, "direction")))
                .withLogOrThrowIfIncorrectPositionOrder(true)
                .build()
                .apply(network);

        applyProperties(network.getIdentifiable(equipmentId), properties(payload));
    }

    public void update(Network network, Dto.ChangeEntry change) {
        Identifiable<?> identifiable = network.getIdentifiable(change.equipmentId());
        if (identifiable == null) {
            throw new PowsyblException("Unknown element '" + change.equipmentId() + "'");
        }

        applyProperties(identifiable, change.payload());
        applyConnections(identifiable, change.payload());
    }

    public void updatePosition(Network network, Dto.ChangeEntry change) {

        Map<String, Object> payload = change.payload();
        int node = requireInt(payload, "node");
        int order = requireInt(payload, "order");
        ConnectablePosition.Direction direction = direction(requireText(payload, "direction"));

        Connectable<?> connectable = network.getConnectable(change.equipmentId());
        if (connectable == null) {
            throw new PowsyblException("Unknown element '" + change.equipmentId() + "'");
        }

        try {
            setFeederPosition(connectable, sideOf(connectable,node), order, direction);
        } catch (IllegalArgumentException e) {
            throw new PowsyblException("Failed to update position for element '" + change.equipmentId() + "': " + e.getMessage());
        }

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

    public void moveBay(Network network, Dto.ChangeEntry change) {
        Map<String, Object> payload = change.payload();
        Connectable<?> connectable = network.getConnectable(change.equipmentId());
        if (connectable == null) {
            throw new PowsyblException("Unknown element '" + change.equipmentId() + "'");
        }
        String bbsId = requireText(payload, "targetBusbarSectionId");
        BusbarSection bbs = network.getBusbarSection(bbsId);
        if (bbs == null) {
            throw new PowsyblException("Unknown busbar section '" + bbsId + "'");
        }

        int node = requireInt(payload, "node");
        int side = sideOf(connectable, node);
        ConnectablePosition.Feeder before = feederOf(connectable, side);
        Integer order = payload.get("order") instanceof Number number
                ? Integer.valueOf(number.intValue())
                : before == null ? null : before.getOrder().orElse(null);
        ConnectablePosition.Direction direction = payload.get("direction") != null
                ? direction(requireText(payload, "direction"))
                : before == null ? ConnectablePosition.Direction.BOTTOM : before.getDirection();

        new MoveFeederBayBuilder()
                .withConnectableId(connectable.getId())
                .withTargetVoltageLevelId(bbs.getTerminal().getVoltageLevel().getId())
                .withTerminal(terminalAt(connectable, node))
                .withTargetBusOrBusBarSectionId(bbsId)
                .build()
                .apply(network);

        if (order != null) {
            setFeederPosition(network.getConnectable(change.equipmentId()), side, order, direction);
        }
    }

    private static Terminal terminalAt(Connectable<?> connectable, int node) {
        for (Terminal terminal : connectable.getTerminals()) {
            if (terminal.getNodeBreakerView().getNode() == node) {
                return terminal;
            }
        }
        throw new PowsyblException("Node " + node + " not found on connectable " + connectable.getId());
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

    public void createBusbar(Network network, Dto.ChangeEntry change) {

        Map<String, Object> payload = change.payload();
        VoltageLevel voltageLevel = requireVoltageLevel(network, requireText(payload,"vlId"));
        requireNewId(network, change.equipmentId());

        voltageLevel.getNodeBreakerView().newBusbarSection()
                .setId(change.equipmentId())
                .setNode(voltageLevel.getNodeBreakerView().getMaximumNodeIndex() + 1)
                .add();

        applyProperties(network.getIdentifiable(change.equipmentId()), properties(change.payload()));

    }

    public void createSwitch(Network network, Dto.ChangeEntry change) {

        Map<String, Object> payload = change.payload();
        VoltageLevel voltageLevel = requireVoltageLevel(network, requireText(payload,"vlId"));
        requireNewId(network, change.equipmentId());

        addSwitch(voltageLevel, change.equipmentId(), requireText(payload, "equipmentType"),
                requireInt(payload, "node1"), requireInt(payload, "node2"));

        applyProperties(network.getIdentifiable(change.equipmentId()), properties(payload));

    }

    public void createSwitchedInjection(Network network, Dto.ChangeEntry change) {
        Map<String, Object> payload = change.payload();
        VoltageLevel voltageLevel = requireVoltageLevel(network, requireText(payload, "vlId"));
        String switchId = requireText(payload, "switchId");
        requireNewId(network, change.equipmentId());
        requireNewId(network, switchId);

        int injectionNode = voltageLevel.getNodeBreakerView().getMaximumNodeIndex() + 1;
        addSwitch(voltageLevel, switchId, requireText(payload, "switchType"), requireInt(payload, "node"), injectionNode);
        injectionAdder(voltageLevel, requireText(payload, "equipmentType"), change.equipmentId())
                .setNode(injectionNode)
                .add();

        applyProperties(network.getIdentifiable(change.equipmentId()), properties(payload));
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

    private static ConnectablePosition.Direction direction(String value) {
        try {
            return ConnectablePosition.Direction.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new PowsyblException("Unknown direction '" + value + "'");
        }
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

    private static String requireText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new PowsyblException("Missing property '" + key + "'");
        }
        return String.valueOf(value);
    }

    private static int requireInt(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof Number number)) {
            throw new PowsyblException("Property '" + key + "' must be a number");
        }
        return number.intValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(Map<String, Object> payload) {
        Object values = payload.get("properties");
        return values instanceof Map ? (Map<String, Object>) values : Map.of();
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
