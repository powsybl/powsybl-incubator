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
import { isPendingCreate, type Command, type PendingCreateCommand } from './commands/Command';
import { CreateCommand } from './commands/CreateCommand';
import { CreateLinkCommand } from './commands/CreateLinkCommand';
import { CreateSwitchedInjectionCommand } from './commands/CreateSwitchedInjectionCommand';
import { DeleteElementCommand } from './commands/DeleteElementCommand';
import { MoveBayCommand } from './commands/MoveBayCommand';
import { RenameCommand } from './commands/RenameCommand';
import { UpdateBayPositionCommand } from './commands/UpdateBayPositionCommand';
import { UpdatePropertiesCommand } from './commands/UpdatePropertiesCommand';
import {
    SvgDomService,
    type DiagramSpan,
    type PendingCreateView,
    type PendingMarkerView,
} from '../dom/SvgDomService';
import {
    BAY_SLOT_CLASS,
    DELETABLE_TYPES,
    NODE_COMPONENT_TYPE,
    SWITCH_TYPES,
    toElementType,
    type BayInsertion,
    type BayMoveGesture,
    type BayPosition,
    type BaySlot,
    type BaySlotCandidate,
    type BusbarTarget,
    type ChangeSet,
    type CreateSpec,
    type EditOperation,
    type EditTarget,
    type ElementType,
    type EquipmentTarget,
    type EquipmentProperties,
    type EditorEmit,
    type EditorEvent,
    type EditorEventListener,
    type FeederDirection,
    type Gesture,
    type LinkEnd,
    type LinkGesture,
    type NodeMetadata,
    type NodeTarget,
    type PendingOrders,
    type TargetEvent,
} from './types';

const DRAG_THRESHOLD = 10;

const DEFAULT_SWITCH_SIZE = 12;

interface BayColumn {
    x: number;
    order?: number;
}

interface BayGeometry {
    y: number;
    columns: BayColumn[];
    gaps: { x: number; leftOrder?: number; rightOrder?: number }[];
}

type StubShape = Omit<PendingCreateView, 'id' | 'label'>;

type BuildableTarget = Exclude<EditTarget, { kind: 'EQUIPMENT' }>;

const CREATE_OPERATIONS: Record<BuildableTarget['kind'], EditOperation> = {
    NODE: 'CREATE_INJECTION',
    BUSBAR: 'CREATE_FEEDER_BAY',
};

export class EditorCore {
    private destroyed = false;

    private gesture: Gesture | null = null;

    private readonly history = new CommandStack((state) => {
        if (this.destroyed) return;
        this.refreshTargets();
        this.emit('history:changed', state);
        this.emit('model:changed', { changeSet: this.getPendingChanges() });
    });

    private targets = new Map<string, EditTarget>();
    private targetsByNodeId = new Map<string, EditTarget[]>();
    private nodeTargetIds: string[] = [];

    private selectedEquipmentIds: string[] = [];
    private hoveredBusbarId: string | null = null;
    private hoverSlots: readonly BaySlotCandidate[] = [];
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
        container.addEventListener('mousemove', this.onMouseMove);
        this.refreshTargets();
    }

    destroy(): void {
        this.destroyed = true;
        const container: HTMLElement = this.dom.getContainer();
        container.removeEventListener('mousedown', this.onMouseDown);
        container.removeEventListener('mouseup', this.onMouseUp);
        container.removeEventListener('mousemove', this.onMouseMove);
        this.clearHoverSlots();
        this.dom.setBaySlots([]);
        this.cancelGesture();
        this.dom.setSelection([]);
        this.dom.setNodeTargets([]);
        this.dom.setPendingCreations(new Map());
        this.dom.setPendingStubs([]);
        this.history.clear();
    }

    undo(): void {
        this.history.undo();
    }

    redo(): void {
        this.history.redo();
    }

    deleteElement(equipmentId: string): boolean {
        return this.deleteElements([equipmentId], 'element');
    }

    deleteFeederBay(equipmentId: string): boolean {
        return this.deleteElements([equipmentId], 'bay');
    }

    create(targetId: string, spec: CreateSpec): boolean {
        const target = this.targets.get(targetId);
        if (!target || target.kind === 'EQUIPMENT') return false;

        const operation = CREATE_OPERATIONS[target.kind];
        if (!creatableTypesFor(operation).has(spec.type)) return false;
        if (!availableOperations(target).includes(operation)) return false;

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
                this.model,
                bay,
            ),
        );
        return true;
    }

    renameEquipment(equipmentId: string, newId: string): boolean {
        if (!newId || newId === equipmentId) return false;

        const created = this.findPendingCreate(equipmentId);
        if (created) {
            if (this.isExistingEquipmentId(newId, created)) return false;
            if (!this.amend(created, { provisionalId: newId })) return false;
            this.onRenamed(equipmentId, newId);
            return true;
        }

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

    private isExistingEquipmentId(equipmentId: string, ignore?: Command): boolean {
        return (
            this.model.getNodesForEquipment(equipmentId).length > 0 ||
            this.history.pending.some(
                (command) => command !== ignore && command.equipmentId === equipmentId,
            )
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

    setBayPosition(equipmentId: string, position: BayPosition, toX?: number): boolean {
        const created = this.findPendingCreate(equipmentId);
        if (created) return this.amendBayPosition(created, position);

        const target = this.targets.get(equipmentId);
        if (target?.kind !== 'EQUIPMENT' || target.node === undefined) return false;
        if (!availableOperations(target).includes('UPDATE_BAY_POSITION')) return false;

        const node = this.feederNodeOf(target);
        const slot = node && this.model.slotOfFeeder(node);
        if (!node || !slot) return false;

        const pending = this.pendingOrders(slot.vlId, node.id);
        if (!this.model.isOrderAvailable(slot, position.order, pending)) return false;

        const axis = this.feederAxisX(node);
        const dx = toX === undefined || axis === undefined ? 0 : toX - axis;

        this.history.push(
            new UpdateBayPositionCommand(target, node, slot, position, this.dom, dx),
        );
        return true;
    }

    applyProperties(equipmentId: string, changes: EquipmentProperties): boolean {
        if (Object.keys(changes).length === 0) return false;

        const created = this.findPendingCreate(equipmentId);
        if (created) {
            return this.amend(created, {
                properties: { ...created.createSpec.properties, ...changes },
            });
        }

        const type = this.equipmentType(equipmentId);
        if (!type) return false;

        this.history.push(
            new UpdatePropertiesCommand(equipmentId, type, changes, this.model, this.syncSwitch),
        );
        return true;
    }

    deleteElements(equipmentIds: readonly string[], kind: 'element' | 'bay'): boolean {
        const creations: PendingCreateCommand[] = [];
        const commands: Command[] = [];

        for (const equipmentId of equipmentIds) {
            const created = this.findPendingCreate(equipmentId);
            if (created) {
                creations.push(created);
                continue;
            }
            const command = this.deleteCommandFor(equipmentId, kind);
            if (!command) return false;
            commands.push(command);
        }

        for (const created of creations) {
            this.dropSelection(created.equipmentId);
            this.history.remove(created);
        }
        this.history.pushAll(commands);
        return creations.length + commands.length > 0;
    }

    selectedTargets(): EquipmentTarget[] {
        return this.selectedEquipmentIds.flatMap((equipmentId) => {
            const target = this.targets.get(equipmentId);
            return target?.kind === 'EQUIPMENT' ? target : [];
        });
    }

    private deleteCommandFor(equipmentId: string, kind: 'element' | 'bay'): Command | undefined {
        const operation: EditOperation = kind === 'bay' ? 'DELETE_BAY' : 'DELETE';
        const target = this.targets.get(equipmentId);
        if (target && !availableOperations(target).includes(operation)) return undefined;

        const node = this.model.getNodesForEquipment(equipmentId)[0];
        if (!node) return undefined;

        const type = toElementType(node.componentType);
        if (!target && !operationsForEquipment(type).includes(operation)) return undefined;

        const scope =
            kind === 'bay'
                ? this.model.collectBay(equipmentId)
                : this.model.collectElementScope(equipmentId);
        if (scope.nodes.length === 0) return undefined;

        return new DeleteElementCommand(
            equipmentId,
            type,
            scope,
            kind,
            this.model,
            this.dom,
            this.dropSelection,
        );
    }

    private equipmentType(equipmentId: string): ElementType | undefined {
        const node = this.model.getNodesForEquipment(equipmentId)[0];
        return node && toElementType(node.componentType);
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

    getSelectedEquipmentIds(): readonly string[] {
        return this.selectedEquipmentIds;
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

        if (this.gesture) {
            this.completeGesture(event);
            return;
        }

        if (this.openHoveredSlot(event)) return;

        const node = this.resolveNodeAt(event.target as Element | null);

        const buildable = event.shiftKey
            ? []
            : this.targetsAt(node).filter((target) => target.kind === 'NODE');
        if (buildable.length > 0) {
            this.onTargets?.({
                targets: buildable,
                trigger: 'click',
                position: { x: event.clientX, y: event.clientY },
            });
            return;
        }

        if (node) {
            this.selectEquipment(node, event.shiftKey);
        } else if (!event.shiftKey) {
            this.setSelection([]);
        }
    };

    private readonly onMouseMove = (event: MouseEvent): void => {
        if (this.gesture) return;

        const element = event.target as Element | null;
        if (element?.closest(`g.${BAY_SLOT_CLASS}`)) return;

        this.showHoverSlots(
            this.targetsAt(this.resolveNodeAt(element)).find(
                (target): target is BusbarTarget => target.kind === 'BUSBAR',
            ),
        );
    };

    private showHoverSlots(busbar?: BusbarTarget): void {
        if ((busbar?.id ?? null) === this.hoveredBusbarId) return;

        this.hoveredBusbarId = busbar?.id ?? null;
        this.hoverSlots = busbar ? this.baySlotCandidates(busbar) : [];
        this.paintTargets();
    }

    private clearHoverSlots(): void {
        this.hoveredBusbarId = null;
        this.hoverSlots = [];
    }

    private openHoveredSlot(event: MouseEvent): boolean {
        const slotId = (event.target as Element | null)?.closest<SVGGElement>(
            `g.${BAY_SLOT_CLASS}`,
        )?.id;
        const slot = this.hoverSlots.find((candidate) => candidate.id === slotId);
        const busbar = this.hoveredBusbarId ? this.targets.get(this.hoveredBusbarId) : undefined;
        if (!slot || busbar?.kind !== 'BUSBAR') return false;

        this.onTargets?.({
            targets: [busbar],
            trigger: 'click',
            position: { x: event.clientX, y: event.clientY },
            insertion: { order: slot.order },
        });
        return true;
    }

    handleContextMenu(event: MouseEvent): void {
        if (!this.onTargets || this.gesture) return;
        const targets = this.targetsAt(this.resolveNodeAt(event.target as Element | null));
        if (targets.length === 0) return;

        event.preventDefault();
        event.stopPropagation();
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

        return this.arm({ kind: 'LINK', first, candidates, spec });
    }

    createSwitchedInjection(targetId: string, spec: CreateSpec): boolean {
        const target = this.targets.get(targetId);
        if (target?.kind !== 'NODE') return false;
        if (!creatableTypesFor('CREATE_SWITCHED_INJECTION').has(spec.type)) return false;
        if (!availableOperations(target).includes('CREATE_SWITCHED_INJECTION')) return false;

        const switchType = spec.switchType;
        if (!switchType || !SWITCH_TYPES.has(switchType)) return false;

        const switchId = `${spec.provisionalId}_${switchType}`;
        if (this.isExistingEquipmentId(spec.provisionalId)) return false;
        if (this.isExistingEquipmentId(switchId)) return false;

        this.history.push(
            new CreateSwitchedInjectionCommand(
                spec.provisionalId,
                spec.type,
                target.vlId,
                target.node,
                switchType,
                switchId,
                spec.properties,
                target.id,
                this.model,
            ),
        );
        return true;
    }

    private amend(command: PendingCreateCommand, patch: Partial<CreateSpec>): boolean {
        return this.history.replace(command, command.withSpec({ ...command.createSpec, ...patch }));
    }

    private amendBayPosition(command: PendingCreateCommand, position: BayPosition): boolean {
        const busbar = command.pendingMarker && this.targets.get(command.pendingMarker.targetId);
        if (busbar?.kind !== 'BUSBAR') return false;

        const pending = this.pendingOrders(busbar.vlId, undefined, command);
        if (!this.model.isOrderAvailable(busbar, position.order, pending)) return false;

        return this.amend(command, { order: position.order, direction: position.direction });
    }

    private findPendingCreate(equipmentId: string): PendingCreateCommand | undefined {
        return this.history.pending.find(
            (command): command is PendingCreateCommand =>
                isPendingCreate(command) && command.equipmentId === equipmentId,
        );
    }

    beginBayMove(equipmentId: string): boolean {
        const target = this.targets.get(equipmentId);
        if (target?.kind !== 'EQUIPMENT') return false;
        if (!availableOperations(target).includes('UPDATE_BAY_POSITION')) return false;

        const node = this.feederNodeOf(target);
        const slot = node && this.model.slotOfFeeder(node);
        const busbar = slot && this.busbarOf(slot);
        if (!node || !slot || !busbar) return false;

        const candidates = this.baySlotCandidates(busbar, node.id);
        if (candidates.length === 0) return false;

        return this.arm({
            kind: 'BAY_MOVE',
            equipmentId,
            slot,
            direction: target.direction ?? 'BOTTOM',
            candidates,
        });
    }

    cancelGesture(): void {
        if (!this.gesture) return;
        this.gesture = null;
        document.removeEventListener('keydown', this.onKeyDown);
        this.paintTargets();
        this.emit('gesture:changed', { gesture: null });
    }

    getGesture(): Gesture | null {
        return this.gesture;
    }

    private arm(gesture: Gesture): boolean {
        this.gesture = gesture;
        this.clearHoverSlots();
        document.addEventListener('keydown', this.onKeyDown);
        this.paintTargets();
        this.emit('gesture:changed', { gesture });
        return true;
    }

    private linkCandidates(first: NodeTarget): LinkEnd[] {
        const seen = new Set<number>();
        return [...this.targets.values()].filter((target): target is LinkEnd => {
            if (target.kind !== 'NODE' && target.kind !== 'BUSBAR') return false;
            if (target.vlId !== first.vlId || target.node === first.node) return false;
            if (seen.has(target.node)) return false;
            if (this.model.hasSwitchBetween(first.vlId, first.node, target.node)) return false;
            seen.add(target.node);
            return true;
        });
    }

    private completeGesture(event: MouseEvent): void {
        const gesture = this.gesture;
        if (!gesture) return;

        const target = event.target as Element | null;
        this.cancelGesture();

        if (gesture.kind === 'LINK') this.createLink(gesture, this.resolveNodeAt(target));
        else this.moveBay(gesture, target);
    }

    private createLink(link: LinkGesture, node: NodeMetadata | undefined): void {
        const clicked = new Set(this.targetsAt(node).map((target) => target.id));
        const second = link.candidates.find((end) => clicked.has(end.id));
        if (!second) return;

        const { first, spec } = link;
        this.history.push(
            new CreateLinkCommand(
                spec.provisionalId,
                spec.type,
                first.vlId,
                first.node,
                second.node,
                spec.properties,
                first.id,
                this.model,
            ),
        );
    }

    private moveBay(bayMove: BayMoveGesture, target: Element | null): void {
        const clicked = target?.closest<SVGGElement>(`g.${BAY_SLOT_CLASS}`)?.id;
        const chosen = bayMove.candidates.find((candidate) => candidate.id === clicked);
        if (!chosen) return;

        const { equipmentId, direction } = bayMove;
        this.setBayPosition(equipmentId, { order: chosen.order, direction }, chosen.x);
    }

    /**
     * Where a bay can sit along the busbar: one gap midway between each pair of neighbours, the
     * bar's own two ends counting as neighbours, each with the orders that frame it. An empty
     * section therefore has a single gap, at the middle of the bar.
     */
    private bayGeometry(busbar: BusbarTarget, movingNodeId?: string): BayGeometry | undefined {
        const span = this.dom.getDiagramSpan(busbar.id);
        if (!span) return undefined;

        const columns = this.feederColumns(busbar, movingNodeId);
        const { claimed } = this.pendingOrders(busbar.vlId, movingNodeId);
        for (const order of [...(claimed.get(busbar.sectionIndex) ?? [])].sort((a, b) => a - b)) {
            insertPendingColumn(columns, order, span);
        }

        const bounds = [span.left, ...columns.map((column) => column.x), span.right];

        return {
            y: span.y,
            columns,
            gaps: bounds.slice(1).map((bound, gap) => ({
                x: (bounds[gap] + bound) / 2,
                leftOrder: columns[gap - 1]?.order,
                rightOrder: columns[gap]?.order,
            })),
        };
    }

    private baySlotCandidates(busbar: BusbarTarget, movingNodeId?: string): BaySlotCandidate[] {
        const geometry = this.bayGeometry(busbar, movingNodeId);
        if (!geometry) return [];

        const pending = this.pendingOrders(busbar.vlId, movingNodeId);
        return geometry.gaps.flatMap((gap) => {
            const order = this.model.orderBetween(busbar, gap.leftOrder, gap.rightOrder, pending);
            if (order === undefined) return [];
            return [{ id: `ne-slot-${order}`, order, x: gap.x, y: geometry.y }];
        });
    }

    private feederColumns(slot: BaySlot, excludeNodeId?: string): BayColumn[] {
        const { vacated } = this.pendingOrders(slot.vlId, excludeNodeId);

        return this.model
            .feedersInSection(slot)
            .flatMap((node) => {
                if (vacated.has(node.id)) return [];
                const x = this.feederAxisX(node);
                return x === undefined ? [] : [{ x, order: node.order }];
            })
            .sort((a, b) => a.x - b.x);
    }

    private feederAxisX(node: NodeMetadata): number | undefined {
        const x = this.dom.getDiagramX(node.id);
        return x === undefined ? undefined : x + this.model.componentSize(node.componentType).width / 2;
    }

    private feederNodeOf(target: EquipmentTarget): NodeMetadata | undefined {
        return this.model
            .getNodesForEquipment(target.equipmentId)
            .find(
                (candidate) =>
                    candidate.order === target.order && (candidate.vid ?? '') === target.vlId,
            );
    }

    private paintTargets(): void {
        const gesture = this.gesture;
        const link = gesture?.kind === 'LINK' ? gesture : undefined;
        const bayMove = gesture?.kind === 'BAY_MOVE' ? gesture : undefined;

        this.dom.setNodeTargets(gesture ? [] : this.nodeTargetIds);
        this.dom.setLinkEnds(link?.first.id ?? null, link?.candidates.map((end) => end.id) ?? []);
        this.dom.setBaySlots(bayMove?.candidates ?? this.hoverSlots);
    }

    private busbarOf(slot: BaySlot): BusbarTarget | undefined {
        return [...this.targets.values()].find(
            (target): target is BusbarTarget =>
                target.kind === 'BUSBAR' &&
                target.vlId === slot.vlId &&
                target.sectionIndex === slot.sectionIndex,
        );
    }

    private readonly onKeyDown = (event: KeyboardEvent): void => {
        if (event.key === 'Escape') this.cancelGesture();
    };

    private insertionAt(
        targets: readonly EditTarget[],
        event: MouseEvent,
    ): BayInsertion | undefined {
        const busbar = targets.find((target) => target.kind === 'BUSBAR');
        const x = this.dom.toDiagramX(event.clientX, event.clientY);
        if (busbar?.kind !== 'BUSBAR' || x === undefined) return undefined;

        const nearest = this.baySlotCandidates(busbar).reduce<BaySlotCandidate | undefined>(
            (best, candidate) =>
                best && Math.abs(best.x - x) <= Math.abs(candidate.x - x) ? best : candidate,
            undefined,
        );
        return nearest && { order: nearest.order };
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

    private selectEquipment(node: NodeMetadata, additive: boolean): void {
        const type = toElementType(node.componentType);
        if (!DELETABLE_TYPES.has(type)) {
            if (!additive) this.setSelection([]);
            return;
        }

        const equipmentId = node.equipmentId ?? node.id;
        const selected = this.selectedEquipmentIds;

        this.setSelection(
            !additive
                ? [equipmentId]
                : selected.includes(equipmentId)
                  ? selected.filter((id) => id !== equipmentId)
                  : [...selected, equipmentId],
        );
    }

    /** The one way in: paints the selection and publishes it, and never twice for nothing. */
    private setSelection(equipmentIds: readonly string[]): void {
        if (this.selectedEquipmentIds.join() === equipmentIds.join()) return;

        this.selectedEquipmentIds = [...equipmentIds];
        this.dom.setSelection(
            // An equipment is drawn by its nodes; a node with no equipmentId stands for itself.
            equipmentIds.flatMap((id) => {
                const nodes = this.model.getNodesForEquipment(id);
                return nodes.length > 0 ? nodes.map((node) => node.id) : [id];
            }),
        );
        this.emit('element:selected', {
            elements: this.selectedTargets().map(({ equipmentId, type }) => ({
                id: equipmentId,
                type,
            })),
        });
    }

    private refreshTargets(): void {
        this.cancelGesture();
        this.clearHoverSlots();
        const { consumed, created } = this.pendingScope();

        const targets = this.model.collectTargets().map((target) => {
            if (target.kind !== 'EQUIPMENT') return target;
            if (created.has(target.equipmentId)) return { ...target, created: true };
            return consumed.has(target.id) ? { ...target, pending: true } : target;
        });

        this.targets = new Map(targets.map((target) => [target.id, target]));
        this.targetsByNodeId = this.indexByNode(targets);

        this.nodeTargetIds = targets
            .filter((target) => target.kind === 'NODE')
            .map((target) => target.id);

        this.paintTargets();

        const stubs = this.pendingCreateViews();
        this.dom.setPendingStubs(stubs);
        this.dom.setPendingCreations(this.pendingMarkers(new Set(stubs.map((stub) => stub.id))));
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

    private pendingScope(): { consumed: Set<string>; created: Set<string> } {
        const consumed = new Set<string>();
        const created = new Set<string>();

        for (const command of this.history.pending) {
            if (isPendingCreate(command)) created.add(command.equipmentId);
            if (command.pendingMarker) consumed.add(command.pendingMarker.targetId);
        }
        return { consumed, created };
    }

    /** The labelled boxes, for every pending command the stubs do not already draw. */
    private pendingMarkers(drawn: ReadonlySet<string>): Map<string, PendingMarkerView[]> {
        const markers = new Map<string, PendingMarkerView[]>();

        for (const command of this.history.pending) {
            const marker = command.pendingMarker;
            if (!marker || (marker.elementId && drawn.has(marker.elementId))) continue;
            pushTo(markers, marker.nodeId, { id: marker.elementId, label: marker.label });
        }
        return markers;
    }

    /**
     * A stub for every creation that hangs off a node or a busbar, drawn where the equipment
     * would land. Creations of another shape — a switch between two nodes — keep a plain marker.
     */
    private pendingCreateViews(): PendingCreateView[] {
        const views: PendingCreateView[] = [];

        for (const command of this.history.pending) {
            if (!isPendingCreate(command)) continue;

            const elementId = command.pendingMarker?.elementId;
            const target = command.pendingMarker && this.targets.get(command.pendingMarker.targetId);
            if (!elementId || !target) continue;

            const spec = command.createSpec;
            // A switch dropped between two nodes is no feeder: it keeps a plain marker.
            if (SWITCH_TYPES.has(spec.type)) continue;

            const stub =
                target.kind === 'BUSBAR'
                    ? this.bayStub(target, spec)
                    : target.kind === 'NODE'
                      ? this.injectionStub(target, spec)
                      : undefined;
            if (!stub) continue;

            views.push({ ...stub, id: elementId, label: command.equipmentId });
        }
        return views;
    }

    /** A pending bay sits in the gap its claimed order falls into. */
    /** A pending bay is a column like any other: the preview just reads where it landed. */
    private bayStub(busbar: BusbarTarget, spec: CreateSpec): StubShape | undefined {
        const geometry = spec.order === undefined ? undefined : this.bayGeometry(busbar);
        const column = geometry?.columns.find((candidate) => candidate.order === spec.order);
        if (!geometry || !column) return undefined;

        return {
            x: column.x,
            y: geometry.y,
            towards: spec.direction === 'TOP' ? -1 : 1,
            withSwitch: true,
            switchSize: this.switchSize(),
        };
    }

    /** A pending injection hangs off its node, drawn away from the busbars of its voltage level. */
    private injectionStub(target: NodeTarget, spec: CreateSpec): StubShape | undefined {
        const node = this.model.getNodeById(target.id);
        const point = node && this.dom.getDiagramPoint(node.id);
        if (!node || !point) return undefined;

        const size = this.model.componentSize(node.componentType);
        const busbarY = this.busbarY(target.vlId);
        const y = point.y + size.height / 2;

        return {
            x: point.x + size.width / 2,
            y,
            towards: busbarY !== undefined && y < busbarY ? -1 : 1,
            withSwitch: spec.switchType !== undefined,
            switchSize: this.switchSize(),
        };
    }

    private busbarY(vlId: string): number | undefined {
        for (const target of this.targets.values()) {
            if (target.kind !== 'BUSBAR' || target.vlId !== vlId) continue;
            const span = this.dom.getDiagramSpan(target.id);
            if (span) return span.y;
        }
        return undefined;
    }

    /** The preview borrows the diagram's scale, never its symbols. */
    private switchSize(): number {
        return this.model.componentSize(NODE_COMPONENT_TYPE.BREAKER).width || DEFAULT_SWITCH_SIZE;
    }

    private pendingOrders(vlId: string, vacating?: string, ignore?: Command): PendingOrders {
        const claimed = new Map<number, number[]>();
        const vacated = new Set<string>(vacating ? [vacating] : []);

        for (const command of this.history.pending) {
            if (command === ignore) continue;
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
        this.setSelection(this.selectedEquipmentIds.filter((id) => id !== equipmentId));
    };

    private readonly onRenamed = (oldId: string, newId: string): void => {
        this.setSelection(this.selectedEquipmentIds.map((id) => (id === oldId ? newId : id)));
    };

    private readonly syncSwitch = (equipmentId: string, changes: EquipmentProperties): void => {
        const open = changes.open;
        if (typeof open !== 'boolean') return;
        for (const node of this.model.getNodesForEquipment(equipmentId)) {
            if (isSwitchNode(node)) this.dom.setSwitchState(node.id, open);
        }
    };
}

/**
 * A pending bay takes the middle of the interval its order falls into — the very slot it was
 * picked on. Inserted one by one, two bays of the same interval end up on either side of each
 * other rather than on the same spot.
 */
function insertPendingColumn(columns: BayColumn[], order: number, span: DiagramSpan): void {
    const after = columns.findIndex((column) => column.order !== undefined && column.order > order);
    const index = after === -1 ? columns.length : after;

    const left = columns[index - 1]?.x ?? span.left;
    const right = columns[index]?.x ?? span.right;
    columns.splice(index, 0, { x: (left + right) / 2, order });
}
