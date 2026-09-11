import { EditorModel } from './EditorModel';
import { buildActions, switchAction, type EditorAction } from './actions';
import { availableOperations, creatableTypesFor, isSwitchNode } from './operations';
import { pushTo } from './utils';
import { CommandStack } from './commands/CommandStack';
import { isPendingCreate, type Command, type PendingCreateCommand } from './commands/Command';
import { CreateCommand } from './commands/CreateCommand';
import { CreateSwitchCommand } from './commands/CreateSwitchCommand';
import { CreateSwitchedInjectionCommand } from './commands/CreateSwitchedInjectionCommand';
import { DeleteElementCommand } from './commands/DeleteElementCommand';
import { MoveBayCommand } from './commands/MoveBayCommand';
import { RenameCommand } from './commands/RenameCommand';
import { UpdateBayPositionCommand } from './commands/UpdateBayPositionCommand';
import { UpdatePropertiesCommand } from './commands/UpdatePropertiesCommand';
import { SvgDomService } from '../dom/SvgDomService';
import type {
    FeederShape,
    PendingBadgeView,
    PendingPreview,
    SwitchPreview,
} from '../dom/svgShapes';
import {
    BAY_SLOT_CLASS,
    DELETABLE_BAY_TYPES,
    DELETABLE_TYPES,
    NODE_COMPONENT_TYPE,
    SWITCH_TYPES,
    toElementType,
    type BayColumn,
    type BayGeometry,
    type BayInsertion,
    type BayMoveGesture,
    type BayPosition,
    type BaySlot,
    type BaySlotCandidate,
    type BusbarTarget,
    type ChangeSet,
    type CreateSpec,
    type DiagramPoint,
    type DiagramSpan,
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
    type NodeMetadata,
    type NodeTarget,
    type PendingOrders,
    type PickedSwitch,
    type SwitchEnd,
    type SwitchGesture,
    type TargetEvent,
} from './types';

const DRAG_THRESHOLD = 10;

const DEFAULT_SWITCH_SIZE = 12;

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
        this.cancelGesture();
        this.dom.clear();
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

    create(targetId: string, operation: EditOperation, spec: CreateSpec): boolean {
        const target = this.targets.get(targetId);
        if (!target || target.kind === 'EQUIPMENT') return false;

        if (!creatableTypesFor(operation).has(spec.type)) return false;
        if (!availableOperations(target).includes(operation)) return false;

        // TODO
        if (operation === 'CREATE_BUSBAR') return false;

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
            new UpdatePropertiesCommand(
                equipmentId,
                type,
                changes,
                this.model,
                this.repaintSwitchState,
            ),
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
        const deletable = kind === 'bay' ? DELETABLE_BAY_TYPES : DELETABLE_TYPES;
        if (!target && !deletable.has(type)) return undefined;

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
        this.model.replaceProperties(equipmentId, values);
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
            if (!this.pickedSwitch()) this.completeGesture(event);
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

    beginSwitch(targetId: string, type: ElementType): boolean {
        const first = this.targets.get(targetId);
        if (first?.kind !== 'NODE') return false;
        if (!creatableTypesFor('CREATE_SWITCH').has(type)) return false;
        if (!availableOperations(first).includes('CREATE_SWITCH')) return false;

        const candidates = this.switchEndCandidates(first);
        if (candidates.length === 0) return false;

        return this.setGesture({ kind: 'SWITCH', first, candidates, type });
    }

    switchAction(): EditorAction | null {
        const picked = this.pickedSwitch();
        return picked && switchAction(this, picked);
    }

    createSwitch(spec: CreateSpec): boolean {
        const picked = this.pickedSwitch();
        if (!picked) return false;
        if (this.isExistingEquipmentId(spec.provisionalId)) return false;

        const { first, second } = picked;
        this.cancelGesture();
        this.history.push(
            new CreateSwitchCommand(
                spec.provisionalId,
                picked.type,
                first.vlId,
                first.node,
                second.node,
                spec.properties,
                { first: first.id, second: second.id },
                this.model,
            ),
        );
        return true;
    }

    private pickedSwitch(): PickedSwitch | null {
        const gesture = this.gesture;
        if (gesture?.kind !== 'SWITCH' || !gesture.second) return null;
        return { ...gesture, second: gesture.second };
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

        return this.setGesture({
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

    private setGesture(gesture: Gesture): boolean {
        this.gesture = gesture;
        this.clearHoverSlots();
        document.addEventListener('keydown', this.onKeyDown);
        this.paintTargets();
        this.emit('gesture:changed', { gesture });
        return true;
    }

    private switchEndCandidates(first: NodeTarget): SwitchEnd[] {
        const seen = new Set<number>();
        return [...this.targets.values()].filter((target): target is SwitchEnd => {
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

        if (gesture.kind === 'SWITCH') {
            this.pickSwitchEnd(gesture, this.resolveNodeAt(target));
            return;
        }

        this.cancelGesture();
        this.moveBay(gesture, target);
    }

    /** Second click: keep the far end and let the host ask for the properties. */
    private pickSwitchEnd(gesture: SwitchGesture, node: NodeMetadata | undefined): void {
        const clicked = new Set(this.targetsAt(node).map((target) => target.id));
        const second = gesture.candidates.find((end) => clicked.has(end.id));
        if (!second) return;

        this.setGesture({ ...gesture, kind: 'SWITCH', second });
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
        const x = this.dom.getDiagramPoint(node.id)?.x;
        if (x === undefined) return undefined;

        return x + this.model.componentSize(node.componentType).width / 2;
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
        const newSwitch = gesture?.kind === 'SWITCH' ? gesture : undefined;
        const bayMove = gesture?.kind === 'BAY_MOVE' ? gesture : undefined;

        const ends = newSwitch?.second ? [newSwitch.second] : (newSwitch?.candidates ?? []);

        this.dom.setNodeTargets(gesture ? [] : this.nodeTargetIds);
        this.dom.setSwitchEnds(newSwitch?.first.id ?? null, ends.map((end) => end.id));
        this.dom.setBaySlots(bayMove?.candidates ?? this.hoverSlots);
    }

    private paintPending(): void {
        const previews = this.pendingPreviews();
        this.dom.setPendingPreviews(previews);
        this.dom.setPendingBadges(this.pendingBadges(new Set(previews.map(({ id }) => id))));
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
        const { created, claimed } = this.pendingScope();

        const targets = this.model.collectTargets().map((target) => {
            if (target.kind !== 'EQUIPMENT') return target;
            if (created.has(target.equipmentId)) return { ...target, created: true };
            return claimed.has(target.id) ? { ...target, claimed: true } : target;
        });

        this.targets = new Map(targets.map((target) => [target.id, target]));
        this.targetsByNodeId = this.indexByNode(targets);

        this.nodeTargetIds = targets
            .filter((target) => target.kind === 'NODE')
            .map((target) => target.id);

        this.paintTargets();

        this.paintPending();
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

    private pendingScope(): { created: Set<string>; claimed: Set<string> } {
        const created = new Set<string>();
        const claimed = new Set<string>();

        for (const command of this.history.pending) {
            if (isPendingCreate(command)) created.add(command.equipmentId);
            if (command.pendingMarker) claimed.add(command.pendingMarker.targetId);
        }
        return { created, claimed };
    }

    private pendingBadges(drawn: ReadonlySet<string>): Map<string, PendingBadgeView[]> {
        const badges = new Map<string, PendingBadgeView[]>();

        for (const command of this.history.pending) {
            const marker = command.pendingMarker;
            if (!marker || (marker.elementId && drawn.has(marker.elementId))) continue;
            pushTo(badges, marker.nodeId, { id: marker.elementId, label: marker.label });
        }
        return badges;
    }

    private pendingPreviews(): PendingPreview[] {
        const previews: PendingPreview[] = [];

        for (const command of this.history.pending) {
            if (!isPendingCreate(command)) continue;

            if (command instanceof CreateSwitchCommand) {
                const preview = this.switchPreview(command);
                if (preview) previews.push(preview);
                continue;
            }

            const marker = command.pendingMarker;
            const elementId = marker?.elementId;
            const anchor = marker && this.targets.get(marker.targetId);
            if (!elementId || !anchor) continue;

            const spec = command.createSpec;
            const shape =
                anchor.kind === 'BUSBAR'
                    ? this.bayPreview(anchor, spec)
                    : anchor.kind === 'NODE'
                      ? this.feederPreview(anchor, spec)
                      : undefined;
            if (!shape) continue;

            previews.push({ kind: 'FEEDER', ...shape, id: elementId, label: command.equipmentId });
        }
        return previews;
    }

    private switchPreview(command: CreateSwitchCommand): SwitchPreview | undefined {
        const first = this.targets.get(command.ends.first);
        const second = this.targets.get(command.ends.second);
        const from = first?.kind === 'NODE' ? this.nodePoint(first) : undefined;
        const far = second && this.endAnchor(second);
        if (!from || !far) return undefined;

        return {
            kind: 'SWITCH',
            id: command.pendingMarker.elementId,
            label: command.equipmentId,
            from,
            to: isSpan(far) ? { x: clampToSpan(far, from.x), y: far.y } : far,
            switchSize: this.switchSize(),
        };
    }

    private endAnchor(target: EditTarget): DiagramPoint | DiagramSpan | undefined {
        if (target.kind === 'BUSBAR') return this.dom.getDiagramSpan(target.id);
        return target.kind === 'NODE' ? this.nodePoint(target) : undefined;
    }

    /** The centre of a node's symbol, where a preview starts. */
    private nodePoint(target: NodeTarget): DiagramPoint | undefined {
        const node = this.model.getNodeById(target.id);
        const point = node && this.dom.getDiagramPoint(node.id);
        if (!node || !point) return undefined;

        const size = this.model.componentSize(node.componentType);
        return { x: point.x + size.width / 2, y: point.y + size.height / 2 };
    }

    private bayPreview(busbar: BusbarTarget, spec: CreateSpec): FeederShape | undefined {
        const geometry = spec.order === undefined ? undefined : this.bayGeometry(busbar);
        const column = geometry?.columns.find((candidate) => candidate.order === spec.order);
        if (!geometry || !column) return undefined;

        return {
            x: column.x,
            y: geometry.y,
            side: spec.direction === 'TOP' ? 'UP' : 'DOWN',
            withSwitch: true,
            switchSize: this.switchSize(),
        };
    }

    private feederPreview(target: NodeTarget, spec: CreateSpec): FeederShape | undefined {
        const point = this.nodePoint(target);
        if (!point) return undefined;

        const busbarY = this.busbarY(target.vlId);

        return {
            x: point.x,
            y: point.y,
            side: busbarY !== undefined && point.y < busbarY ? 'UP' : 'DOWN',
            withSwitch: spec.switchType !== undefined,
            switchSize: this.switchSize(),
        };
    }

    private busbarY(vlId: string): number | undefined {
        for (const node of this.model.busbarNodes(vlId)) {
            const span = this.dom.getDiagramSpan(node.id);
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

    private readonly repaintSwitchState = (
        equipmentId: string,
        changes: EquipmentProperties,
    ): void => {
        const open = changes.open;
        if (typeof open !== 'boolean') return;
        for (const node of this.model.getNodesForEquipment(equipmentId)) {
            if (isSwitchNode(node)) this.dom.setSwitchState(node.id, open);
        }
    };
}

function insertPendingColumn(columns: BayColumn[], order: number, span: DiagramSpan): void {
    const after = columns.findIndex((column) => column.order !== undefined && column.order > order);
    const index = after === -1 ? columns.length : after;

    const left = columns[index - 1]?.x ?? span.left;
    const right = columns[index]?.x ?? span.right;
    columns.splice(index, 0, { x: (left + right) / 2, order });
}

function isSpan(anchor: DiagramPoint | DiagramSpan): anchor is DiagramSpan {
    return 'left' in anchor;
}

/** A switch drops onto a busbar, never past its ends. */
function clampToSpan(span: DiagramSpan, x: number): number {
    return Math.min(Math.max(x, span.left), span.right);
}
