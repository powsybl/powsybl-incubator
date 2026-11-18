package com.example.networkeditor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.extensions.ConnectablePosition;

import java.util.List;
import java.util.Map;

/**
 * The change set of @powsybl/network-editor (docs/format-change-set.md), read without GridSuite.
 * Plain data: Service applies each change with powsybl-iidm-modification and the IIDM API.
 * The "extra" field of a change holds host data a PowSyBl backend ignores: it is not read.
 */
public final class Changes {

    private Changes() {
    }

    public record ChangeSet(int formatVersion, List<Change> changes) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = CreateFeederBay.class, name = "CREATE_FEEDER_BAY"),
        @JsonSubTypes.Type(value = CreateInjection.class, name = "CREATE_INJECTION"),
        @JsonSubTypes.Type(value = CreateSwitch.class, name = "CREATE_SWITCH"),
        @JsonSubTypes.Type(value = CreateBusbarSection.class, name = "CREATE_BUSBAR_SECTION"),
        @JsonSubTypes.Type(value = CreateCouplingDevice.class, name = "CREATE_COUPLING_DEVICE"),
        @JsonSubTypes.Type(value = RemoveFeederBay.class, name = "REMOVE_FEEDER_BAY"),
        @JsonSubTypes.Type(value = RemoveElement.class, name = "REMOVE_ELEMENT"),
        @JsonSubTypes.Type(value = MoveFeederBay.class, name = "MOVE_FEEDER_BAY"),
        @JsonSubTypes.Type(value = UpdateAttributes.class, name = "UPDATE_ATTRIBUTES"),
        @JsonSubTypes.Type(value = Rename.class, name = "RENAME"),
    })
    public interface Change {
    }

    public record CreateFeederBay(String equipmentType, String id, String voltageLevelId, String busOrBusbarSectionId,
                                  Integer positionOrder, ConnectablePosition.Direction direction,
                                  Map<String, Object> attributes) implements Change {
    }

    public record SwitchRef(String id, String kind) {
    }

    public record CreateInjection(String equipmentType, String id, String voltageLevelId, int node,
                                  @JsonProperty("switch") SwitchRef behindSwitch,
                                  Map<String, Object> attributes) implements Change {
    }

    public record CreateSwitch(String id, String voltageLevelId, String switchKind, int node1, int node2,
                               boolean open) implements Change {
    }

    public record CreateBusbarSection(String id, String voltageLevelId, int busbarIndex, int sectionIndex) implements Change {
    }

    public record CreateCouplingDevice(String voltageLevelId, String busOrBusbarSectionId1,
                                       String busOrBusbarSectionId2) implements Change {
    }

    public record RemoveFeederBay(String connectableId, String equipmentType) implements Change {
    }

    public record RemoveElement(String id, String equipmentType) implements Change {
    }

    /** "side" is ONE, TWO or THREE for a branch, absent for an injection. */
    public record MoveFeederBay(String connectableId, String targetVoltageLevelId, String targetBusOrBusbarSectionId,
                                Integer positionOrder, ConnectablePosition.Direction direction,
                                String side) implements Change {
    }

    public record UpdateAttributes(String equipmentType, String id, Map<String, Object> attributes) implements Change {
    }

    public record Rename(String id, String newId) implements Change {
    }
}
