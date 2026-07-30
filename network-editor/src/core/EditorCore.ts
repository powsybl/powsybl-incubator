import { EditorModel } from './EditorModel';
import { CommandStack } from './commands/CommandStack';
import { CreateEquipmentCommand } from './commands/CreateEquipmentCommand';
import { DeleteElementCommand } from './commands/DeleteElementCommand';
import { UpdatePropertiesCommand } from './commands/UpdatePropertiesCommand';
import { SvgDomService } from '../dom/SvgDomService';
import {
    CONNECTION_POINT_CLASS,
    CREATABLE_TYPES,
    DELETABLE_TYPES,
    SWITCH_TYPES,
    toElementType,
    type ChangeSet,
    type ConnectionPoint,
    type ConnectionPointClickEvent,
    type CreateEquipmentSpec,
    type EquipmentProperties,
    type EditorEventListener,
    type EditorEvents,
    type EquipmentContextMenuEvent,
    type EquipmentInfo,
    type NodeMetadata, DELETABLE_BAY_TYPES, SELECTABLE_TYPES, SELECTED_CLASS,
} from './types';

const DRAG_THRESHOLD = 10;

export class EditorCore {
    private destroyed = false;

    private readonly history = new CommandStack((state) => {
        if (this.destroyed) return;
        this.refreshConnectionPoints();
        this.emit('history:changed', state);
        this.emit('model:changed', { changeSet: this.getPendingChanges() });
    });

    private connectionPoints = new Map<string, ConnectionPoint>();

    private createCounter = 0;

    private selectedEquipmentId: string | null = null;
    private highlighted: Element[] = [];
    private mouseDownX = 0;
    private mouseDownY = 0;

    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly onEvent?: EditorEventListener,
        private readonly onEquipmentContextMenu?: (event: EquipmentContextMenuEvent, )=> void,
        private readonly onConnectionPointClick?: (event: ConnectionPointClickEvent) => void,
    ) {
        const container: HTMLElement = this.dom.getContainer();
        container.addEventListener('mousedown', this.onMouseDown);
        container.addEventListener('mouseup', this.onMouseUp);
        this.refreshConnectionPoints();
    }

    destroy(): void {
        this.destroyed = true;
        const container: HTMLElement = this.dom.getContainer();
        container.removeEventListener('mousedown', this.onMouseDown);
        container.removeEventListener('mouseup', this.onMouseUp);
        this.clearHighlight();
        this.dom.setConnectionPoints([]);
        this.history.clear();
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

    private readonly onMouseUp = (event: MouseEvent) => {
        if (event.button !== 0) return;
        const moved = Math.hypot(
            event.clientX - this.mouseDownX,
            event.clientY - this.mouseDownY,
        );
        if (moved > DRAG_THRESHOLD) return;

        // Before the selection branch: a connection point is a hidden node, so
        // `resolveNodeAt` would resolve it and clear the selection instead.
        const point = this.resolveConnectionPointAt(event.target as Element | null);
        if (point) {
            this.onConnectionPointClick?.({
                point,
                position: { x: event.clientX, y: event.clientY },
            });
            return;
        }

        const node = this.resolveNodeAt(event.target as Element | null);
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

    clearPendingChanges(): void {
        this.history.clear();
    }

    getEquipmentInfo(equipmentId: string): EquipmentInfo | null {
        const node = this.resolveEquipmentNode(equipmentId);
        if (!node) return null;

        const type = toElementType(node.componentType);
        return {
            equipmentId: node.equipmentId ?? node.id,
            type,
            label: this.dom.findEquipmentLabelByNodeId(node.id) ?? node.equipmentId ?? node.id,
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

    private resolveConnectionPointAt(target: Element | null): ConnectionPoint | undefined {
        const marker = target?.closest<SVGGElement>(`g.${CONNECTION_POINT_CLASS}`);
        return marker ? this.connectionPoints.get(marker.id) : undefined;
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

    /** Debug overlay: shows the IIDM node each element stands on. */
    showIidmNodes(enabled: boolean): void {
        this.dom.setIidmOverlay(
            enabled ? this.model.collectIidmNodes() : new Map(),
            enabled ? this.model.collectFictitiousNodes() : new Map(),
        );
    }

    getConnectionPoints(): ConnectionPoint[] {
        return [...this.connectionPoints.values()];
    }

    createEquipment(pointId: string, spec: CreateEquipmentSpec): boolean {
        const point = this.connectionPoints.get(pointId);
        if (!point || !CREATABLE_TYPES.has(spec.type)) return false;

        const equipmentId =
            spec.provisionalId ?? `NEW_${spec.type}_${++this.createCounter}`;

        this.history.push(
            new CreateEquipmentCommand(
                equipmentId,
                spec.type,
                point,
                spec.properties,
                this.emit,
            ),
        );
        return true;
    }


    private refreshConnectionPoints(): void {
        const taken = this.pendingCreatePointIds();
        const points = this.model
            .collectConnectionPoints()
            .filter((point) => !taken.has(point.id));

        this.connectionPoints = new Map(points.map((point) => [point.id, point]));
        this.dom.setConnectionPoints(points.map((point) => point.id));
        this.emit('connection:changed', { points });
    }

    /** Read back from the change set */
    private pendingCreatePointIds(): Set<string> {
        const ids = new Set<string>();
        for (const entry of this.getPendingChanges()) {
            if (entry.op !== 'create') continue;
            const pointId = (entry.payload as { pointId?: string } | undefined)?.pointId;
            if (pointId) ids.add(pointId);
        }
        return ids;
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

    private readonly emit: EditorEventListener = <K extends keyof EditorEvents>(
        name: K,
        payload: EditorEvents[K],
    ): void => {
        if (this.destroyed) return;

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
