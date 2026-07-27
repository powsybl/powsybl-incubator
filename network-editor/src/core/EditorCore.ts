import { EditorModel } from './EditorModel';
import { CommandStack } from './commands/CommandStack';
import { DeleteElementCommand } from './commands/DeleteElementCommand';
import { UpdatePropertiesCommand } from './commands/UpdatePropertiesCommand';
import { SvgDomService, type BusbarSegment } from '../dom/SvgDomService';
import {
    BUSBAR_SECTION_TYPE,
    DELETABLE_TYPES,
    SWITCH_TYPES,
    toElementType,
    type ChangeSet,
    type ConnectionTarget,
    type EquipmentProperties,
    type EditorEventListener,
    type EditorEvents,
    type EquipmentContextMenuEvent,
    type EquipmentInfo,
    type NodeMetadata, DELETABLE_BAY_TYPES, SELECTABLE_TYPES, SELECTED_CLASS,
} from './types';

const DRAG_THRESHOLD = 10;
const DEFAULT_CONNECTION_POINT_RADIUS = 40;
const BUSBAR_SWITCH_TOLERANCE = 12;

export class EditorCore {
    private destroyed = false;

    private readonly history = new CommandStack((state) => {
        if (this.destroyed) return;
        this.emit('history:changed', state);
        this.emit('model:changed', { changeSet: this.getPendingChanges() });
    });

    private selectedEquipmentId: string | null = null;
    private highlighted: Element[] = [];
    private mouseDownX = 0;
    private mouseDownY = 0;

    private connectionPointsInteractive: boolean;
    private readonly connectionPointRadius: number;
    private hoverFrame: number | null = null;

    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly onEvent?: EditorEventListener,
        private readonly onEquipmentContextMenu?: (event: EquipmentContextMenuEvent, )=> void,
        connectionPoints: { interactive?: boolean; radius?: number } = {},
    ) {
        const container: HTMLElement = this.dom.getContainer();
        container.addEventListener('mousedown', this.onMouseDown);
        container.addEventListener('mouseup', this.onMouseUp);
        container.addEventListener('mousemove', this.onMouseMove);
        container.addEventListener('mouseleave', this.onMouseLeave);
        this.connectionPointsInteractive = connectionPoints.interactive ?? true;
        this.connectionPointRadius = connectionPoints.radius ?? DEFAULT_CONNECTION_POINT_RADIUS;
        this.dom.setConnectionPointsInteractive(this.connectionPointsInteractive);
    }

    destroy(): void {
        this.destroyed = true;
        const container: HTMLElement = this.dom.getContainer();
        container.removeEventListener('mousedown', this.onMouseDown);
        container.removeEventListener('mouseup', this.onMouseUp);
        container.removeEventListener('mousemove', this.onMouseMove);
        container.removeEventListener('mouseleave', this.onMouseLeave);
        this.cancelHoverFrame();
        this.dom.setConnectionPointsInteractive(false);
        this.clearHighlight();
        this.history.clear();
    }

    setConnectionPointsInteractive(enabled: boolean): void {
        this.connectionPointsInteractive = enabled;
        this.dom.setConnectionPointsInteractive(enabled);
    }

    /**
     * Marks the busbar spot the cursor is closest to and describes it in IIDM
     * terms. Meant to be called from a `dragover` handler too, so a drop knows
     * what it would attach to.
     */
    highlightConnectionPointsNear(clientX: number, clientY: number): ConnectionTarget | null {
        if (!this.connectionPointsInteractive) return null;

        const hovered = this.resolveNodeAt(
            document.elementFromPoint?.(clientX, clientY) ?? null,
        );
        if (hovered && hovered.componentType !== BUSBAR_SECTION_TYPE) {
            this.dom.clearConnectionPointHighlight();
            return null;
        }

        const cursor = this.dom.toDiagramPoint(clientX, clientY);
        if (!cursor) {
            this.dom.clearConnectionPointHighlight();
            return null;
        }

        const slot = this.findFreeSlot(cursor, this.connectionPointRadius / cursor.scale);
        if (!slot) {
            this.dom.clearConnectionPointHighlight();
            return null;
        }
        this.dom.showBusbarMarker(slot.point);
        return this.toBusbarTarget(slot, cursor.y);
    }

    private findFreeSlot(
        cursor: { x: number; y: number },
        radius: number,
    ): { svgId: string; point: { x: number; y: number }; busbarY: number } | null {
        let best: { svgId: string; point: { x: number; y: number }; busbarY: number } | null = null;
        let bestDistance = Infinity;

        for (const busbar of this.dom.getBusbarSegments()) {
            const projected = projectOnSegment(cursor, busbar);
            if (!projected || projected.distance > radius || projected.distance >= bestDistance) {
                continue;
            }

            // Gap containing the projected point, between the switches on the bar.
            const bounds = [busbar.x1, ...this.findSwitchesOnBusbar(busbar), busbar.x2];
            let start = bounds[0];
            let end = bounds[bounds.length - 1];
            for (let index = 0; index < bounds.length - 1; index++) {
                if (projected.point.x <= bounds[index + 1]) {
                    start = bounds[index];
                    end = bounds[index + 1];
                    break;
                }
                start = bounds[index];
                end = bounds[index + 1];
            }

            bestDistance = projected.distance;
            best = {
                svgId: busbar.id,
                point: { x: (start + end) / 2, y: projected.point.y },
                busbarY: projected.point.y,
            };
        }
        return best;
    }


    private findSwitchesOnBusbar(busbar: BusbarSegment): number[] {
        const voltageLevelId = this.model.getNodeById(busbar.id)?.vid;
        if (!voltageLevelId) return [];

        const positions: number[] = [];
        for (const node of this.model.getNodesForVoltageLevel(voltageLevelId)) {
            if (!SWITCH_TYPES.has(node.componentType)) continue;
            const at = this.dom.getNodePosition(node.id);
            if (!at || Math.abs(at.y - busbar.y1) > BUSBAR_SWITCH_TOLERANCE) continue;
            positions.push(at.x);
        }
        return positions.sort((a, b) => a - b);
    }


    private toBusbarTarget(
        slot: { svgId: string; point: { x: number; y: number }; busbarY: number },
        cursorY: number,
    ): ConnectionTarget | null {
        const busbar = this.model.getNodeById(slot.svgId);
        if (!busbar?.equipmentId || !busbar.vid) return null;

        return {
            kind: 'busbar',
            busbarSectionId: busbar.equipmentId,
            voltageLevelId: busbar.vid,
            svgId: slot.svgId,
            direction: cursorY < slot.busbarY ? 'TOP' : 'BOTTOM',
            position: slot.point,
            ...this.findFeederNeighbours(busbar.vid, slot.point.x),
        };
    }

    private findFeederNeighbours(
        voltageLevelId: string,
        x: number,
    ): { previousEquipmentId?: string; nextEquipmentId?: string } {
        const placed = this.model
            .getFeedersForVoltageLevel(voltageLevelId)
            .flatMap((node) => {
                const equipmentId = node.equipmentId;
                const at = equipmentId && this.dom.getNodePosition(node.id);
                return at ? [{ equipmentId, x: at.x }] : [];
            })
            .sort((a, b) => a.x - b.x);

        const previous = placed.filter((feeder) => feeder.x <= x).at(-1);
        const next = placed.find((feeder) => feeder.x > x);
        return {
            previousEquipmentId: previous?.equipmentId,
            nextEquipmentId: next?.equipmentId,
        };
    }

    clearConnectionPointHighlight(): void {
        this.dom.clearConnectionPointHighlight();
    }

    undo(): void {
        this.history.undo();
    }

    redo(): void {
        this.history.redo();
    }

    private readonly onMouseDown = (event: MouseEvent) => {
        this.mouseDownX = event.clientX;
        this.mouseDownY = event.clientY;
    };

    private readonly onMouseMove = (event: MouseEvent) => {
        if (!this.connectionPointsInteractive || this.hoverFrame !== null) return;
        const { clientX, clientY } = event;
        this.hoverFrame = requestAnimationFrame(() => {
            this.hoverFrame = null;
            if (this.destroyed) return;
            this.highlightConnectionPointsNear(clientX, clientY);
        });
    };

    private readonly onMouseLeave = () => {
        this.cancelHoverFrame();
        this.dom.clearConnectionPointHighlight();
    };

    private cancelHoverFrame(): void {
        if (this.hoverFrame === null) return;
        cancelAnimationFrame(this.hoverFrame);
        this.hoverFrame = null;
    }

    private readonly onMouseUp = (event: MouseEvent) => {
        if (event.button !== 0) return;
        const moved = Math.hypot(
            event.clientX - this.mouseDownX,
            event.clientY - this.mouseDownY,
        );
        if (moved > DRAG_THRESHOLD) return;

        const node = this.resolveNodeAt(event.target as Element | null);

        if (node && node.componentType !== BUSBAR_SECTION_TYPE) {
            this.selectEquipement(node);
            return;
        }

        const target = this.highlightConnectionPointsNear(event.clientX, event.clientY);
        if (target) {
            this.emit('connection-point:picked', target);
            return;
        }

        if (node) {
            this.selectEquipement(node);
        } else {
            this.clearSelection();
        }
    };

    private selectEquipement(node: NodeMetadata) {
        const type = toElementType(node.componentType);
        if (!SELECTABLE_TYPES.has(type)) {
            this.clearSelection();
            return;
        }

        const equipmentId = node.equipmentId ?? node.id;
        if (equipmentId === this.selectedEquipmentId) return;

        this.clearHighlight();
        this.selectedEquipmentId = equipmentId;
        const nodes = node.equipmentId
            ? this.model.getNodesForEquipment(equipmentId)
            : [node];
        for (const n of nodes) {
            const element = this.dom.findElementById(n.id);
            if (!element) continue;
            element.classList.add(SELECTED_CLASS);
            this.highlighted.push(element);
        }
        this.emit('element:selected', { id: equipmentId, type });
    }

    private clearSelection(): void {
        if (this.selectedEquipmentId === null) return;
        this.clearHighlight();
        this.selectedEquipmentId = null;
        this.emit('element:selected', { id: null, type: null });
    }

    private clearHighlight(): void {
        for (const element of this.highlighted) element.classList.remove(SELECTED_CLASS);
        this.highlighted = [];
    }

    get canUndo(): boolean {
        return this.history.canUndo;
    }

    get canRedo(): boolean {
        return this.history.canRedo;
    }

    getPendingChanges(): ChangeSet {
        return [...this.history.pending.map((command) => command.toChangeSetEntry())];
    }

    getEquipmentInfo(equipmentId: string): EquipmentInfo | null {
        const node = this.resolveEquipmentNode(equipmentId);
        if (!node) return null;

        const type = toElementType(node.componentType);
        return {
            equipmentId: node.equipmentId ?? node.id,
            type,
            label: this.getEquipmentLabel(node),
            deletable: DELETABLE_TYPES.has(type),
            bayDeletable: DELETABLE_BAY_TYPES.has(type)
        };
    }

    handleContextMenu(event: MouseEvent): void {
        if (!this.onEquipmentContextMenu) return;
        const node = this.resolveNodeAt(event.target as Element | null);
        if (!node) return;

        const info = this.getEquipmentInfo(node.equipmentId ?? node.id);
        if (!info || info.type === 'UNKNOWN') return;

        event.preventDefault();
        this.onEquipmentContextMenu({
            info,
            position: { x: event.clientX, y: event.clientY },
        });
    }

    private resolveNodeAt(target: Element | null): NodeMetadata | undefined {
        for (
            let g = target?.closest<SVGGElement>('g[id]') ?? null;
            g;
            g = g.parentElement?.closest<SVGGElement>('g[id]') ?? null
        ) {
            const node = this.model.resolveNodeForSvgId(g.id);
            if (node) return node;
        }
        return undefined;
    }

    deleteElement(equipmentId: string): boolean {
        return this.deleteCommand(equipmentId, 'element');
    }

    deleteFeederBay(equipmentId: string): boolean {
        return this.deleteCommand(equipmentId, 'bay');
    }

    private deleteCommand(equipmentId: string, kind: 'element' | 'bay'): boolean {
        const node = this.resolveEquipmentNode(equipmentId);
        const deletable = kind === 'bay' ? DELETABLE_BAY_TYPES : DELETABLE_TYPES;
        if (!node || !deletable.has(toElementType(node.componentType))) {
            return false;
        }

        const id = node.equipmentId ?? node.id;
        const scope =
            kind === 'bay'
                ? this.model.collectBay(id)
                : this.model.collectElementScope(id);
        if (scope.nodes.length === 0) return false;

        this.history.push(
            new DeleteElementCommand(
                node,
                scope,
                kind,
                this.model,
                this.dom,
                this.emit,
            ),
        );
        return true;
    }

    getSelectedEquipmentId(): string | null {
        return this.selectedEquipmentId;
    }

    getProperties(equipmentId: string): EquipmentProperties {
        return this.model.getProperties(equipmentId);
    }

    seedProperties(equipmentId: string, values: EquipmentProperties): void {
        this.model.seedProperties(equipmentId, values);
    }

    applyProperties(equipmentId: string, changes: EquipmentProperties): boolean {
        if (Object.keys(changes).length === 0) return false;
        const node = this.resolveEquipmentNode(equipmentId);
        if (!node) return false;

        this.history.push(
            new UpdatePropertiesCommand(
                node.equipmentId ?? node.id,
                toElementType(node.componentType),
                changes,
                this.model,
                this.emit,
            ),
        );
        return true;
    }

    /** First node of an equipment (or a raw node id) — undefined if unknown. */
    private resolveEquipmentNode(equipmentId: string): NodeMetadata | undefined {
        return (
            this.model.getNodesForEquipment(equipmentId)[0] ??
            this.model.getNodeById(equipmentId)
        );
    }

    private getEquipmentLabel(node: NodeMetadata): string {
        return this.dom.findEquipmentLabelByNodeId(node.id);
    }

    private readonly emit: EditorEventListener = <K extends keyof EditorEvents>(
        name: K,
        payload: EditorEvents[K],
    ): void => {
        if (this.destroyed) return;

        if (name === 'element:removed' || name === 'element:added') {
            this.dom.refreshConnectionPoints();
        }

        if (name === 'element:removed') {
            const { id } = payload as EditorEvents['element:removed'];
            if (id === this.selectedEquipmentId) this.clearSelection();
        }

        if (name === 'properties:changed') {
            const { id, changes } = payload as EditorEvents['properties:changed'];
            if (typeof  changes.open === 'boolean') this.updateSwitchState(id, Boolean(changes.open));
        }

        this.onEvent?.(name, payload);
    };

    private updateSwitchState(equipmentId: string, open: boolean): void {
        for (const node of this.model.getNodesForEquipment(equipmentId)) {
            if (SWITCH_TYPES.has(node.componentType)) {
                this.dom.setSwitchState(node.id, open);
            }
        }
    }
}

function projectOnSegment(
    point: { x: number; y: number },
    segment: BusbarSegment,
): { point: { x: number; y: number }; distance: number } | null {
    const dx = segment.x2 - segment.x1;
    const dy = segment.y2 - segment.y1;
    const lengthSquared = dx * dx + dy * dy;
    if (lengthSquared === 0) return null;

    const ratio = Math.min(
        1,
        Math.max(0, ((point.x - segment.x1) * dx + (point.y - segment.y1) * dy) / lengthSquared),
    );
    const projected = { x: segment.x1 + ratio * dx, y: segment.y1 + ratio * dy };
    return {
        point: projected,
        distance: Math.hypot(projected.x - point.x, projected.y - point.y),
    };
}
