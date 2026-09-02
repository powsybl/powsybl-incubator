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
import { SvgDomService, type PendingMarkerView } from '../dom/SvgDomService';
import {
    BAY_SLOT_CLASS,
    DELETABLE_TYPES,
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

const SLOT_PITCH = 50;

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
        this.cancelGesture();
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

        this.history.push(
            new UpdateBayPositionCommand(target, node, slot, position, this.dom, toX),
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
        if (!node || !this.isOperationAllowed(node, operation)) {
            return undefined;
        }

        const scope =
            kind === 'bay'
                ? this.model.collectBay(equipmentId)
                : this.model.collectElementScope(equipmentId);
        if (scope.nodes.length === 0) return undefined;

        return new DeleteElementCommand(
            equipmentId,
            toElementType(node.componentType),
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
        return this.selectedEquipmentIds[0] ?? null;
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

        const node = this.resolveNodeAt(event.target as Element | null);

        const buildable = event.shiftKey
            ? []
            : this.targetsAt(node).filter((target) => target.kind !== 'EQUIPMENT');
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
            this.selectEquipment(node, event.shiftKey);
        } else if (!event.shiftKey) {
            this.setSelection([]);
        }
    };

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
        if (!node || !slot) return false;

        const candidates = this.baySlotCandidates(slot, node.id);
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

    private baySlotCandidates(slot: BaySlot, movingNodeId: string): BaySlotCandidate[] {
        const y = this.busbarY(slot);
        if (y === undefined) return [];

        const pending = this.pendingOrders(slot.vlId, movingNodeId);
        const columns = this.feederColumns(slot, movingNodeId);
        const positions = gapPositions(columns);
        const candidates: BaySlotCandidate[] = [];

        for (let gap = 0; gap < positions.length; gap += 1) {
            const order = this.model.orderBetween(
                slot,
                columns[gap - 1]?.node.order,
                columns[gap]?.node.order,
                pending,
            );
            if (order === undefined) continue;
            candidates.push({ id: `ne-slot-${order}`, order, x: positions[gap], y });
        }
        return candidates;
    }

    private feederColumns(
        slot: BaySlot,
        excludeNodeId?: string,
    ): { node: NodeMetadata; x: number }[] {
        return this.model.feedersInSection(slot).flatMap((node) => {
            if (node.id === excludeNodeId) return [];
            const x = this.dom.getDiagramX(node.id);
            return x === undefined ? [] : [{ node, x }];
        });
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
        this.dom.setBaySlots(bayMove?.candidates ?? []);
    }

    private busbarY(slot: BaySlot): number | undefined {
        const busbar = [...this.targets.values()].find(
            (target) =>
                target.kind === 'BUSBAR' &&
                target.vlId === slot.vlId &&
                target.sectionIndex === slot.sectionIndex,
        );
        return busbar && this.dom.getDiagramPoint(busbar.id)?.y;
    }

    private readonly onKeyDown = (event: KeyboardEvent): void => {
        if (event.key === 'Escape') this.cancelGesture();
    };

    private insertionAt(
        targets: readonly EditTarget[],
        event: MouseEvent,
    ): BayInsertion | undefined {
        const busbar = targets.find((target) => target.kind === 'BUSBAR');
        if (!busbar) return undefined;

        const x = this.dom.toDiagramX(event.clientX, event.clientY);
        if (x === undefined) return undefined;

        const columns = this.feederColumns(busbar);
        const gap = columns.filter((column) => column.x <= x).length;
        const left = columns[gap - 1]?.node;
        const right = columns[gap]?.node;

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
        const { consumed, created, markers } = this.pendingCreations();

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

    private pendingCreations(): {
        consumed: Set<string>;
        created: Set<string>;
        markers: Map<string, PendingMarkerView[]>;
    } {
        const consumed = new Set<string>();
        const created = new Set<string>();
        const markers = new Map<string, PendingMarkerView[]>();

        for (const command of this.history.pending) {
            if (isPendingCreate(command)) created.add(command.equipmentId);

            const marker = command.pendingMarker;
            if (!marker) continue;
            consumed.add(marker.targetId);
            pushTo(markers, marker.nodeId, { id: marker.elementId, label: marker.label });
        }
        return { consumed, created, markers };
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

function gapPositions(columns: readonly { x: number }[]): number[] {
    if (columns.length === 0) return [];

    let pitch = SLOT_PITCH;
    for (let i = 1; i < columns.length; i += 1) {
        pitch = Math.min(pitch, columns[i].x - columns[i - 1].x);
    }
    const half = pitch / 2;

    const between = columns.slice(1).map((column, i) => (columns[i].x + column.x) / 2);
    return [columns[0].x - half, ...between, columns[columns.length - 1].x + half];
}
