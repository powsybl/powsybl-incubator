import { EditorModel } from './EditorModel';
import { buildActions, type EditorAction } from './actions';
import {
    availableOperations,
    creatableTypesFor,
    isSwitchNode,
    operationsForEquipment,
} from './operations';
import { pushTo } from './utils.ts';
import { CommandStack } from './commands/CommandStack';
import { CreateCommand } from './commands/CreateCommand';
import { CreateLinkCommand } from './commands/CreateLinkCommand';
import { DeleteElementCommand } from './commands/DeleteElementCommand';
import { MoveBayCommand } from './commands/MoveBayCommand';
import { RenameCommand } from './commands/RenameCommand';
import { UpdateBayPositionCommand } from './commands/UpdateBayPositionCommand';
import { UpdatePropertiesCommand } from './commands/UpdatePropertiesCommand';
import { SvgDomService } from '../dom/SvgDomService';
import {
    DELETABLE_TYPES,
    toElementType,
    type BayInsertion,
    type BayPosition,
    type BusbarTarget,
    type ChangeSet,
    type CreateSpec,
    type EditOperation,
    type EditTarget,
    type EquipmentProperties,
    type EditorEmit,
    type EditorEvent,
    type EditorEventListener,
    type FeederDirection,
    type NodeMetadata,
    type NodeTarget,
    type PendingOrders,
    type SelectionState,
    type TargetEvent,
} from './types';

const DRAG_THRESHOLD = 10;

type BuildableTarget = Exclude<EditTarget, { kind: 'EQUIPMENT' }>;

const CREATE_OPERATIONS: Record<BuildableTarget['kind'], EditOperation> = {
    NODE: 'CREATE_INJECTION',
    BUSBAR: 'CREATE_FEEDER_BAY',
};

const EXCLUSIVE_TARGETS: ReadonlySet<EditTarget['kind']> = new Set<EditTarget['kind']>(['NODE']);

export class EditorCore {
    private destroyed = false;

    private selection: SelectionState | null = null;

    private pendingSpec?: CreateSpec;

    private readonly history = new CommandStack((state) => {
        if (this.destroyed) return;
        this.refreshTargets();
        this.emit('history:changed', state);
        this.emit('model:changed', { changeSet: this.getPendingChanges() });
    });

    private targets = new Map<string, EditTarget>();
    private targetsByNodeId = new Map<string, EditTarget[]>();

    private selectedEquipmentId: string | null = null;
    private mouseDownX = 0;
    private mouseDownY = 0;

    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly onEvent?: EditorEventListener,
        private readonly onTargets?: (event: TargetEvent) => void,
    ) {
        const container: HTMLElement = this.dom.getContainer();
        container.addEventListener('mousedown', this.onMouseDown);
        container.addEventListener('mouseup', this.onMouseUp);
        this.refreshTargets();
    }

    destroy(): void {
        this.destroyed = true;
        const container: HTMLElement = this.dom.getContainer();
        container.removeEventListener('mousedown', this.onMouseDown);
        container.removeEventListener('mouseup', this.onMouseUp);
        this.cancelSelection();
        this.dom.setSelection([]);
        this.dom.setNodeTargets([]);
        this.dom.setPendingCreations(new Map());
        this.history.clear();
    }

    undo(): void {
        this.history.undo();
    }

    redo(): void {
        this.history.redo();
    }

    deleteElement(equipmentId: string): boolean {
        return this.deleteCommand(equipmentId, 'element');
    }

    deleteFeederBay(equipmentId: string): boolean {
        return this.deleteCommand(equipmentId, 'bay');
    }

    create(targetId: string, spec: CreateSpec): boolean {
        const target = this.targets.get(targetId);
        if (!target || target.kind === 'EQUIPMENT') return false;

        const operation = CREATE_OPERATIONS[target.kind];
        if (!creatableTypesFor(operation).has(spec.type)) return false;
        if (!availableOperations(target).includes(operation)) return false;

        let markerNodeId = target.id;
        let bay: { order: number; direction: FeederDirection } | undefined;

        if (target.kind === 'BUSBAR') {
            const pending = this.pendingOrders(target.vlId);
            const order =
                spec.order === undefined
                    ? this.model.nextOrderForBusbar(target, pending)
                    : this.model.isOrderAvailable(target, spec.order, pending)
                      ? spec.order
                      : undefined;
            if (order === undefined) return false;
            bay = { order, direction: spec.direction ?? 'BOTTOM' };
        }

        if (this.isExistingEquipmentId(spec.provisionalId)) return false;

        this.history.push(
            new CreateCommand(
                spec.provisionalId,
                spec.type,
                target,
                spec.properties,
                markerNodeId,
                bay,
            ),
        );
        return true;
    }

    renameEquipment(equipmentId: string, newId: string): boolean {
        if (!newId || newId === equipmentId) return false;

        const target = this.targets.get(equipmentId);
        if (target?.kind !== 'EQUIPMENT') return false;

        if (!availableOperations(target).includes('RENAME')) return false;

        const host = this.model.getNodesForEquipment(equipmentId)[0];
        if (!host) return false;

        if (this.isExistingEquipmentId(newId)) return false;

        this.history.push(
            new RenameCommand(equipmentId, newId, this.model, this.onRenamed, host.id),
        );
        return true;
    }

    private isExistingEquipmentId(equipmentId: string): boolean {
        return (
            this.model.getNodesForEquipment(equipmentId).length > 0 ||
            this.history.pending.some((command) => command.equipmentId === equipmentId)
        );
    }

    moveDestinations(equipmentId: string): BusbarTarget[] {
        const feeder = this.targets.get(equipmentId);
        if (feeder?.kind !== 'EQUIPMENT') return [];
        if (!availableOperations(feeder).includes('MOVE_BAY')) return [];

        return [...this.targets.values()].filter(
            (target): target is BusbarTarget =>
                target.kind === 'BUSBAR' && target.vlId === feeder.vlId,
        );
    }

    moveFeederBay(equipmentId: string, busbarTargetId: string): boolean {
        const feeder = this.targets.get(equipmentId);
        if (feeder?.kind !== 'EQUIPMENT' || feeder.node === undefined) return false;

        const destination = this.moveDestinations(equipmentId).find(
            (target) => target.id === busbarTargetId,
        );
        if (!destination) return false;

        const host = this.model.getNodesForEquipment(feeder.equipmentId)[0];
        if (!host) return false;

        this.history.push(new MoveBayCommand(feeder, destination, host.id));
        return true;
    }

    actionsFor(target: EditTarget, insertion?: BayInsertion): EditorAction[] {
        return buildActions(this, target, insertion);
    }

    /** The order a new bay would take here — undefined when the component abstains. */
    proposedOrder(target: BusbarTarget, insertion?: BayInsertion): number | undefined {
        return (
            insertion?.order ??
            this.model.nextOrderForBusbar(target, this.pendingOrders(target.vlId))
        );
    }

    getBayPosition(equipmentId: string): BayPosition | undefined {
        const target = this.targets.get(equipmentId);
        if (target?.kind !== 'EQUIPMENT' || target.order === undefined) return undefined;
        return { order: target.order, direction: target.direction ?? 'BOTTOM' };
    }

    setBayPosition(equipmentId: string, position: BayPosition): boolean {
        const target = this.targets.get(equipmentId);
        if (target?.kind !== 'EQUIPMENT' || target.node === undefined) return false;
        if (!availableOperations(target).includes('UPDATE_BAY_POSITION')) return false;

        const node = this.model
            .getNodesForEquipment(equipmentId)
            .find(
                (candidate) =>
                    candidate.order === target.order && (candidate.vid ?? '') === target.vlId,
            );
        const slot = node && this.model.slotOfFeeder(node);
        if (!node || !slot) return false;

        const pending = this.pendingOrders(slot.vlId, node.id);
        if (!this.model.isOrderAvailable(slot, position.order, pending)) return false;

        this.history.push(new UpdateBayPositionCommand(target, node, slot, position));
        return true;
    }

    applyProperties(equipmentId: string, changes: EquipmentProperties): boolean {
        if (Object.keys(changes).length === 0) return false;

        this.history.push(
            new UpdatePropertiesCommand(equipmentId, changes, this.model, this.syncSwitch),
        );
        return true;
    }

    private deleteCommand(equipmentId: string, kind: 'element' | 'bay'): boolean {
        const operation: EditOperation = kind === 'bay' ? 'DELETE_BAY' : 'DELETE';
        const target = this.targets.get(equipmentId);
        if (target && !availableOperations(target).includes(operation)) return false;

        const node = this.model.getNodesForEquipment(equipmentId)[0];
        if (!node || !this.isOperationAllowed(node, operation)) {
            return false;
        }

        const scope =
            kind === 'bay'
                ? this.model.collectBay(equipmentId)
                : this.model.collectElementScope(equipmentId);
        if (scope.nodes.length === 0) return false;

        this.history.push(
            new DeleteElementCommand(equipmentId, scope, kind, this.model, this.dom, this.dropSelection),
        );
        return true;
    }

    private isOperationAllowed(node: NodeMetadata, operation: EditOperation): boolean {
        return operationsForEquipment(toElementType(node.componentType)).includes(operation);
    }

    getPendingChanges(): ChangeSet {
        return this.history.pending.map((command) => command.toChangeSetEntry());
    }

    clearPendingChanges(): void {
        this.history.clear();
    }

    getTargets(): EditTarget[] {
        return [...this.targets.values()];
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

    /** Debug overlay: shows the IIDM node each element stands on. */
    showIidmNodes(enabled: boolean): void {
        this.dom.setIidmOverlay(enabled ? this.model.collectNodeStatus() : new Map());
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

        const node = this.resolveNodeAt(event.target as Element | null);

        if (this.selection) {
            this.completeSelection(node);
            return;
        }

        const buildable = this.targetsAt(node).filter((target) => target.kind !== 'EQUIPMENT');
        if (buildable.length > 0) {
            this.onTargets?.({
                targets: buildable,
                trigger: 'click',
                position: { x: event.clientX, y: event.clientY },
                insertion: this.insertionAt(buildable, event),
            });
            return;
        }

        if (node) {
            this.selectEquipment(node);
        } else {
            this.clearSelection();
        }
    };

    handleContextMenu(event: MouseEvent): void {
        if (!this.onTargets) return;
        const targets = this.targetsAt(this.resolveNodeAt(event.target as Element | null));
        if (targets.length === 0) return;

        event.preventDefault();
        this.onTargets({
            targets,
            trigger: 'contextmenu',
            position: { x: event.clientX, y: event.clientY },
            insertion: this.insertionAt(targets, event),
        });
    }

    beginLink(targetId: string, spec: CreateSpec): boolean {
        const first = this.targets.get(targetId);
        if (first?.kind !== 'NODE') return false;
        if (!creatableTypesFor('CREATE_SWITCH').has(spec.type)) return false;
        if (!availableOperations(first).includes('CREATE_SWITCH')) return false;
        if (this.isExistingEquipmentId(spec.provisionalId)) return false;

        const candidates = this.linkCandidates(first);
        if (candidates.length === 0) return false;

        this.selection = { operation: 'CREATE_SWITCH', first, candidates };
        this.pendingSpec = spec;
        document.addEventListener('keydown', this.onKeyDown);
        this.paintSelection();
        this.emit('selection:changed', { selection: this.selection });
        return true;
    }

    cancelSelection(): void {
        if (!this.selection) return;
        this.selection = null;
        this.pendingSpec = undefined;
        document.removeEventListener('keydown', this.onKeyDown);
        this.paintSelection();
        this.emit('selection:changed', { selection: null });
    }

    getSelection(): SelectionState | null {
        return this.selection;
    }

    private linkCandidates(first: NodeTarget): EditTarget[] {
        return [...this.targets.values()].filter((target): target is NodeTarget | BusbarTarget => {
            if (target.kind !== 'NODE' && target.kind !== 'BUSBAR') return false;
            if (target.vlId !== first.vlId || target.node === first.node) return false;
            return !this.model.hasSwitchBetween(first.vlId, first.node, target.node);
        });
    }

    private completeSelection(node: NodeMetadata | undefined): void {
        const selection = this.selection;
        const spec = this.pendingSpec;
        if (!selection || !spec) return;

        const eligible = new Set(selection.candidates.map((target) => target.id));
        const second = this.targetsAt(node).find((target) => eligible.has(target.id));
        if (!second || (second.kind !== 'NODE' && second.kind !== 'BUSBAR')) {
            this.cancelSelection();
            return;
        }

        const first = selection.first as NodeTarget;
        this.cancelSelection();
        this.history.push(
            new CreateLinkCommand(
                spec.provisionalId,
                spec.type,
                first.vlId,
                first.node,
                second.node,
                spec.properties,
                first.id,
                first.id,
            ),
        );
    }

    private paintSelection(): void {
        this.dom.setSelectionCandidates(
            this.selection?.first.id ?? null,
            this.selection?.candidates.map((target) => target.id) ?? [],
        );
    }

    private readonly onKeyDown = (event: KeyboardEvent): void => {
        if (event.key === 'Escape') this.cancelSelection();
    };

    private insertionAt(
        targets: readonly EditTarget[],
        event: MouseEvent,
    ): BayInsertion | undefined {
        const busbar = targets.find((target) => target.kind === 'BUSBAR');
        if (!busbar) return undefined;

        const x = this.dom.toDiagramX(event.clientX, event.clientY);
        if (x === undefined) return undefined;

        let left: NodeMetadata | undefined;
        let right: NodeMetadata | undefined;
        for (const feeder of this.model.feedersInSection(busbar)) {
            const feederX = this.dom.getDiagramX(feeder.id);
            if (feederX === undefined) continue;
            if (feederX <= x) left = feeder;
            else if (right === undefined) right = feeder;
        }

        const order = this.model.orderBetween(
            busbar,
            left?.order,
            right?.order,
            this.pendingOrders(busbar.vlId),
        );
        if (order === undefined) return undefined;

        return {
            order,
            afterEquipmentId: left?.equipmentId,
            beforeEquipmentId: right?.equipmentId,
        };
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

    private targetsAt(node: NodeMetadata | undefined): EditTarget[] {
        return node ? (this.targetsByNodeId.get(node.id) ?? []) : [];
    }

    private selectEquipment(node: NodeMetadata): void {
        const type = toElementType(node.componentType);
        if (!DELETABLE_TYPES.has(type)) {
            this.clearSelection();
            return;
        }

        const equipmentId = node.equipmentId ?? node.id;
        if (equipmentId === this.selectedEquipmentId) return;

        this.selectedEquipmentId = equipmentId;
        const nodes = node.equipmentId
            ? this.model.getNodesForEquipment(equipmentId)
            : [node];
        this.dom.setSelection(nodes.map((n) => n.id));
        this.emit('element:selected', { id: equipmentId, type });
    }

    private clearSelection(): void {
        if (this.selectedEquipmentId === null) return;
        this.dom.setSelection([]);
        this.selectedEquipmentId = null;
        this.emit('element:selected', { id: null, type: null });
    }

    private refreshTargets(): void {
        this.cancelSelection();
        const { consumed, markers } = this.pendingCreations();
        const targets = this.model
            .collectTargets()
            .filter((target) => !EXCLUSIVE_TARGETS.has(target.kind) || !consumed.has(target.id))
            .map((target) =>
                target.kind === 'EQUIPMENT' && consumed.has(target.id)
                    ? { ...target, pending: true }
                    : target,
            );

        this.targets = new Map(targets.map((target) => [target.id, target]));
        this.targetsByNodeId = this.indexByNode(targets);

        this.dom.setNodeTargets(
            targets.filter((target) => target.kind === 'NODE').map((target) => target.id),
        );
        this.dom.setPendingCreations(markers);
        this.emit('targets:changed', { targets });
    }

    private indexByNode(targets: readonly EditTarget[]): Map<string, EditTarget[]> {
        const byNode = new Map<string, EditTarget[]>();

        for (const target of targets) {
            switch (target.kind) {
                case 'NODE':
                case 'BUSBAR':
                    pushTo(byNode, target.id, target);
                    break;
                case 'EQUIPMENT':
                    for (const node of this.model.getNodesForEquipment(target.equipmentId)) {
                        pushTo(byNode, node.id, target);
                    }
                    break;
            }
        }
        return byNode;
    }

    private pendingCreations(): { consumed: Set<string>; markers: Map<string, string[]> } {
        const consumed = new Set<string>();
        const markers = new Map<string, string[]>();

        for (const command of this.history.pending) {
            const marker = command.pendingMarker;
            if (!marker) continue;
            if (marker.consumes !== false) consumed.add(marker.targetId);
            pushTo(markers, marker.nodeId, marker.label);
        }
        return { consumed, markers };
    }

    private pendingOrders(vlId: string, vacating?: string): PendingOrders {
        const claimed = new Map<number, number[]>();
        const vacated = new Set<string>(vacating ? [vacating] : []);

        for (const command of this.history.pending) {
            const claim = command.orderClaim;
            if (!claim || claim.vlId !== vlId) continue;
            pushTo(claimed, claim.sectionIndex, claim.order);
            if (claim.vacatedNodeId) vacated.add(claim.vacatedNodeId);
        }
        return { claimed, vacated };
    }

    private readonly emit: EditorEmit = (name, payload) => {
        if (this.destroyed) return;
        this.onEvent?.({ name, ...payload } as EditorEvent);
    };

    private readonly dropSelection = (equipmentId: string): void => {
        if (equipmentId === this.selectedEquipmentId) this.clearSelection();
    };

    private readonly onRenamed = (oldId: string, newId: string): void => {
        if (this.selectedEquipmentId !== oldId) return;
        this.selectedEquipmentId = newId;
        const node = this.model.getNodesForEquipment(newId)[0];
        this.emit('element:selected', { id: newId, type: toElementType(node?.componentType) });
    };

    private readonly syncSwitch = (equipmentId: string, changes: EquipmentProperties): void => {
        const open = changes.open;
        if (typeof open !== 'boolean') return;
        for (const node of this.model.getNodesForEquipment(equipmentId)) {
            if (isSwitchNode(node)) this.dom.setSwitchState(node.id, open);
        }
    };
}

