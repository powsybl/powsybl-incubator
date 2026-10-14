import { EditorModel } from './EditorModel';
import { buildActions, switchAction, type EditorAction } from './actions';
import { availableOperations, creatableTypesFor } from './operations';
import { pushTo } from './utils';
import { menuItemsFor } from './menu';
import { mergeFeederBayMoves, type NetworkModification } from './modifications';
import { CommandStack } from './commands/CommandStack';
import type { Command, PendingCreateCommand } from './commands/Command';
import {
    findAmendableRename,
    findPendingCreate,
    isPendingId,
    pendingBayPosition,
    pendingOrders,
    pendingScope,
} from './pending';
import { CreateCommand } from './commands/CreateCommand';
import { CreateSwitchCommand } from './commands/CreateSwitchCommand';
import { CreateSwitchedInjectionCommand } from './commands/CreateSwitchedInjectionCommand';
import { DeleteElementCommand } from './commands/DeleteElementCommand';
import { MoveBayCommand } from './commands/MoveBayCommand';
import { RenameCommand } from './commands/RenameCommand';
import { ReplaceCommand } from './commands/ReplaceCommand';
import { UpdateBayPositionCommand } from './commands/UpdateBayPositionCommand';
import { UpdatePropertiesCommand } from './commands/UpdatePropertiesCommand';
import { SvgDomService } from '../dom/SvgDomService';
import { DiagramGeometry } from '../dom/DiagramGeometry';
import { BayPlacement, nearestSlot } from './BayPlacement';
import { PendingPainter } from './PendingPainter';
import {
    DELETABLE_BAY_TYPES,
    DELETABLE_TYPES,
    INJECTION_TYPES,
    SWITCH_TYPES,
    isSwitchNode,
    toElementType,
    toDirection,
    type BayMoveGesture,
    type BayPosition,
    type BaySlot,
    type BaySlotCandidate,
    type BusbarTarget,
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
    type NodeMetadata,
    type NodeTarget,
    type PendingOrders,
    type PickedSwitch,
    type SwitchEnd,
    type SwitchGesture,
    type SymbolProvider,
    type DeleteScope,
    type OrderClaim,
} from './types';

const DRAG_THRESHOLD = 10;

export class EditorCore {
    private destroyed = false;

    private gesture: Gesture | null = null;

    private readonly history: CommandStack;
    private readonly geometry: DiagramGeometry;
    private readonly placement: BayPlacement;
    private readonly painter: PendingPainter;

    private targets = new Map<string, EditTarget>();
    private targetsByNodeId = new Map<string, EditTarget[]>();
    private nodeTargetIds: string[] = [];

    private selectedEquipmentIds: string[] = [];
    private hoveredBusbarId: string | null = null;
    private hoverCandidates: readonly BaySlotCandidate[] = [];
    private shownSlot: BaySlotCandidate | undefined;
    private mouseDownX = 0;
    private mouseDownY = 0;

    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly symbols: SymbolProvider,
        private readonly onEvent?: EditorEventListener,
        private readonly isSupported: (action: EditorAction) => boolean = () => true,
    ) {
        this.geometry = new DiagramGeometry(model, dom, symbols);
        this.history = new CommandStack((state) => {
            if (this.destroyed) return;
            this.placement.reflow();
            this.refreshTargets();
            this.emit('history:changed', state);
            this.emit('modifications:changed', { modifications: this.getModifications() });
        });
        const pending = () => this.pending;
        this.placement = new BayPlacement(model, dom, this.geometry, pending);
        this.painter = new PendingPainter(model, dom, this.geometry, this.placement, pending, () => this.targets);
        const container: HTMLElement = this.dom.getContainer();
        container.addEventListener('mousedown', this.onMouseDown);
        container.addEventListener('mouseup', this.onMouseUp);
        container.addEventListener('mousemove', this.onMouseMove);
        this.placement.reflow();
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

    create(targetId: string, operation: EditOperation, spec: CreateSpec): boolean {
        const target = this.targets.get(targetId);
        if (!target || target.kind === 'EQUIPMENT' || !allows(target, operation)) return false;
        if (!creatableTypesFor(operation).has(spec.type)) return false;

        let bay: BayPosition | undefined;

        if (target.kind === 'BUSBAR') {
            const pending = pendingOrders(this.pending, target.vlId);
            const order =
                spec.order === undefined
                    ? this.model.nextOrderForBusbar(target, pending)
                    : this.model.isOrderAvailable(target, spec.order, pending)
                      ? spec.order
                      : undefined;
            if (order === undefined) return false;
            bay = { order, direction: spec.direction ?? 'TOP' };
        }

        if (target.kind === 'NODE') {
            const standing = pendingOrders(this.pending, target.vlId).claims
                .find((claim) => claim.iidmNode === target.node);
            if (standing) bay = { order: standing.order, direction: standing.direction ?? 'BOTTOM' };
        }

        if (!this.isFreeId(spec.provisionalId)) return false;

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

    replaceEquipment(equipmentId: string, spec: CreateSpec): boolean {
        const target = this.targetFor(equipmentId, 'EQUIPMENT', 'REPLACE');
        if (!target) return false;
        if (!INJECTION_TYPES.has(spec.type) || spec.type === target.type) return false;

        const newId = spec.provisionalId;
        if (newId !== equipmentId && !this.isFreeId(newId)) return false;

        const node = this.model.getNodesForEquipment(equipmentId)[0];
        if (!node) return false;

        this.history.push(
            new ReplaceCommand(
                equipmentId,
                newId,
                spec.type,
                spec.properties,
                node,
                this.model,
                this.dom,
                this.symbols,
                this.onRenamed,
            ),
        );
        return true;
    }

    renameEquipment(equipmentId: string, newId: string): boolean {
        if (!newId || newId === equipmentId) return false;

        const created = this.pendingCreate(equipmentId);
        if (created) {
            if (!this.isFreeId(newId, created)) return false;
            if (!this.amend(created, { provisionalId: newId })) return false;
            this.onRenamed(equipmentId, newId);
            return true;
        }

        const target = this.renameTarget(equipmentId);
        if (!target || !allows(target, 'RENAME')) return false;

        const renamed = findAmendableRename(this.pending, equipmentId);
        if (renamed) {
            if (newId === renamed.equipmentId) return this.history.remove(renamed);
            if (!this.isFreeId(newId, renamed)) return false;
            return this.history.replace(
                renamed,
                new RenameCommand(renamed.equipmentId, newId, this.model, this.onRenamed),
            );
        }

        if (!this.isFreeId(newId)) return false;

        this.history.push(new RenameCommand(equipmentId, newId, this.model, this.onRenamed));
        return true;
    }

    private renameTarget(equipmentId: string): EquipmentTarget | BusbarTarget | undefined {
        const target = this.targets.get(equipmentId);
        if (target?.kind === 'EQUIPMENT') return target;
        return [...this.targets.values()].find(
            (candidate): candidate is BusbarTarget =>
                candidate.kind === 'BUSBAR' && candidate.busbarSectionId === equipmentId,
        );
    }

    /** `NEW_LOAD`, then `NEW_LOAD_1`, `NEW_LOAD_2`… the first id not taken yet. */
    proposeEquipmentId(type: ElementType): string {
        const base = `NEW_${type}`;
        let equipmentId = base;
        for (let index = 1; !this.isFreeId(equipmentId); index++) {
            equipmentId = `${base}_${index}`;
        }
        return equipmentId;
    }

    private isFreeId(equipmentId: string, ignore?: Command): boolean {
        return (
            this.model.getNodesForEquipment(equipmentId).length === 0 &&
            !isPendingId(this.pending, equipmentId, ignore)
        );
    }

    private targetFor<K extends EditTarget['kind']>(
        id: string | undefined,
        kind: K,
        operation: EditOperation,
    ): Extract<EditTarget, { kind: K }> | undefined {
        const target = id === undefined ? undefined : this.targets.get(id);
        if (target?.kind !== kind || !allows(target, operation)) return undefined;
        return target as Extract<EditTarget, { kind: K }>;
    }

    private get pending(): readonly Command[] {
        return this.history.pending;
    }

    private pendingCreate(equipmentId: string): PendingCreateCommand | undefined {
        return findPendingCreate(this.pending, equipmentId);
    }

    moveDestinations(equipmentId: string): BusbarTarget[] {
        const feeder = this.targetFor(equipmentId, 'EQUIPMENT', 'MOVE_BAY');
        if (!feeder) return [];

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
        const node = this.model.feederNodeOf(feeder);
        if (!host || !node) return false;

        // The order must suit the target section: orders grow from one section to the next.
        const pending = pendingOrders(this.pending, destination.vlId, node.id);
        const order = this.model.nextOrderForBusbar(destination, pending);
        if (order === undefined) return false;

        const direction = this.getBayPosition(equipmentId)?.direction ?? 'BOTTOM';
        this.history.push(
            new MoveBayCommand(feeder, destination, host.id, { order, direction }, node.id, this.model.feederSide(node)),
        );
        return true;
    }

    actionsFor(target: EditTarget, insertion?: number): EditorAction[] {
        return buildActions(this, target, insertion).filter(this.isSupported);
    }

    proposedOrder(target: BusbarTarget, insertion?: number): number | undefined {
        return (
            insertion ??
            this.model.nextOrderForBusbar(target, pendingOrders(this.pending, target.vlId))
        );
    }

    getBayPosition(equipmentId: string): BayPosition | undefined {
        const pending = pendingBayPosition(this.pending, equipmentId);
        if (pending) return pending.position;

        const target = this.targets.get(equipmentId);
        if (target?.kind !== 'EQUIPMENT' || target.order === undefined) return undefined;
        return { order: target.order, direction: target.direction ?? 'BOTTOM' };
    }

    setBayPosition(equipmentId: string, position: BayPosition): boolean {
        const created = this.pendingCreate(equipmentId);
        if (created) return this.amendBayPosition(created, position);

        const feeder = this.movableFeeder(equipmentId);
        if (!feeder) return false;
        const { target, node, slot } = feeder;

        const pending = pendingOrders(this.pending, slot.vlId, node.id);
        if (!this.model.isOrderAvailable(slot, position.order, pending)) return false;

        const from = toDirection(node.direction) ?? 'BOTTOM';
        if (position.direction !== from) return this.flipBay(target, node, slot, position, from);

        this.history.push(new UpdateBayPositionCommand(target, node, slot, position, this.dom, this.model));
        return true;
    }

    private flipBay(
        target: EquipmentTarget,
        node: NodeMetadata,
        slot: BaySlot,
        position: BayPosition,
        from: FeederDirection,
    ): boolean {
        const cell = this.dom.bayCell(node.id);
        const cellFeeders = this.model
            .feedersInSection(slot)
            .filter((feeder) => this.dom.bayCell(feeder.id) === cell);
        const turn = cell ? this.geometry.bayTurn(node, cellFeeders, from, position.direction) : undefined;
        if (!turn) return false;

        const commands: UpdateBayPositionCommand[] = [
            new UpdateBayPositionCommand(target, node, slot, position, this.dom, this.model, turn),
        ];
        for (const sibling of cellFeeders) {
            if (sibling.id === node.id) continue;

            const siblingTarget = this.targetFor(sibling.equipmentId, 'EQUIPMENT', 'UPDATE_BAY_POSITION');
            if (siblingTarget?.node === undefined) return false;

            const siblingPosition = { order: sibling.order!, direction: position.direction };
            commands.push(
                new UpdateBayPositionCommand(siblingTarget, sibling, slot, siblingPosition, this.dom, this.model),
            );
        }

        this.history.pushAll(commands);
        return true;
    }

    applyProperties(equipmentId: string, changes: EquipmentProperties): boolean {
        if (Object.keys(changes).length === 0) return false;

        const created = this.pendingCreate(equipmentId);
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
            const created = this.pendingCreate(equipmentId);
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

    private standingBayClaims(equipmentId: string, scope: DeleteScope, kind: 'element' | 'bay'): OrderClaim[] {
        if (kind !== 'element') return [];
        if (this.model.collectBay(equipmentId).nodes.length <= scope.nodes.length) return [];

        const claims: OrderClaim[] = [];
        const movedPosition = pendingBayPosition(this.pending, equipmentId)?.position;
        for (const node of scope.nodes) {
            const order = movedPosition?.order ?? node.order;
            if (order === undefined) continue;
            const slot = this.model.slotOfFeeder(node);
            const x = this.geometry.nodeCentre(node)?.x;
            if (!slot || x === undefined) continue;

            const direction = movedPosition?.direction ?? toDirection(node.direction);
            claims.push({ ...slot, order, x, iidmNode: node.iidmNode, direction });
        }
        return claims;
    }

    private deleteCommandFor(equipmentId: string, kind: 'element' | 'bay'): Command | undefined {
        const operation: EditOperation = kind === 'bay' ? 'DELETE_BAY' : 'DELETE';
        const target = this.targets.get(equipmentId);
        if (target && !allows(target, operation)) return undefined;

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
            this.standingBayClaims(equipmentId, scope, kind),
            this.model,
            this.dom,
            this.dropSelection,
        );
    }

    private equipmentType(equipmentId: string): ElementType | undefined {
        const node = this.model.getNodesForEquipment(equipmentId)[0];
        return node && toElementType(node.componentType);
    }

    getModifications(): NetworkModification[] {
        return this.history.pendingSteps.flatMap((step) =>
            mergeFeederBayMoves(step.flatMap((command) => command.toModifications())),
        );
    }

    clearModifications(): void {
        this.history.clear();
    }

    getTargets(): EditTarget[] {
        return [...this.targets.values()];
    }

    getProperties(equipmentId: string): EquipmentProperties {
        return this.model.getProperties(equipmentId);
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

        const element = event.target as Element | null;
        const busbar = event.shiftKey ? undefined : this.busbarAt(element);
        if (busbar && this.emitTargets([busbar], 'click', event)) return;

        const node = this.resolveNodeAt(element);

        const buildable = event.shiftKey
            ? []
            : this.targetsAt(node).filter((target) => target.kind === 'NODE');
        if (this.emitTargets(buildable, 'click', event)) return;

        if (node) {
            this.selectEquipment(node, event.shiftKey);
        } else if (!event.shiftKey) {
            this.setSelection([]);
        }
    };

    private readonly onMouseMove = (event: MouseEvent): void => {
        const x = this.dom.toDiagramX(event.clientX, event.clientY);

        if (this.gesture?.kind === 'BAY_MOVE') {
            this.showSlot(nearestSlot(this.gesture.candidates, x));
            return;
        }
        if (this.gesture) return;

        const busbar = this.busbarAt(event.target as Element | null);
        if ((busbar?.id ?? null) !== this.hoveredBusbarId) {
            this.hoveredBusbarId = busbar?.id ?? null;
            this.hoverCandidates = busbar ? this.placement.slotCandidates(busbar) : [];
        }
        this.showSlot(nearestSlot(this.hoverCandidates, x));
    };

    private busbarAt(element: Element | null): BusbarTarget | undefined {
        if (element?.closest('text.sld-label')) return undefined;
        return this.targetsAt(this.resolveNodeAt(element)).find(
            (target): target is BusbarTarget => target.kind === 'BUSBAR',
        );
    }

    private showSlot(slot: BaySlotCandidate | undefined): void {
        if (slot === this.shownSlot) return;
        this.shownSlot = slot;
        this.dom.setBaySlots(slot ? [slot] : []);
    }

    private clearHoverSlots(): void {
        this.hoveredBusbarId = null;
        this.hoverCandidates = [];
        this.shownSlot = undefined;
    }

    /** Without a supported action, the event goes on to the viewer callbacks (onFeeder, onBus). */
    handleContextMenu(event: MouseEvent): void {
        if (this.gesture) return;
        const targets = this.targetsAt(this.resolveNodeAt(event.target as Element | null));
        if (!this.emitTargets(targets, 'contextmenu', event)) return;

        event.preventDefault();
        event.stopPropagation();
    }

    private emitTargets(
        targets: readonly EditTarget[],
        trigger: 'click' | 'contextmenu',
        event: MouseEvent,
    ): boolean {
        if (!this.onEvent || targets.length === 0) return false;

        const insertion = this.insertionAt(targets, event);
        const offered = targets.filter((target) => this.actionsFor(target, insertion).length > 0);
        if (offered.length === 0) return false;

        this.emit('menu:requested', {
            items: menuItemsFor(this, { targets: offered, insertion }),
            targets: offered,
            trigger,
            position: { x: event.clientX, y: event.clientY },
        });
        return true;
    }

    beginSwitch(targetId: string, type: ElementType): boolean {
        const first = this.targetFor(targetId, 'NODE', 'CREATE_SWITCH');
        if (!first || !creatableTypesFor('CREATE_SWITCH').has(type)) return false;

        const candidates = this.switchEndCandidates(first);
        if (candidates.length === 0) return false;

        return this.setGesture({ kind: 'SWITCH', first, candidates, type });
    }

    private switchAction(): EditorAction | null {
        const picked = this.pickedSwitch();
        if (!picked) return null;
        const action = switchAction(this, picked);
        return this.isSupported(action) ? action : null;
    }

    createSwitch(spec: CreateSpec): boolean {
        const picked = this.pickedSwitch();
        if (!picked) return false;
        if (!this.isFreeId(spec.provisionalId)) return false;

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
        const target = this.targetFor(targetId, 'NODE', 'CREATE_SWITCHED_INJECTION');
        if (!target || !creatableTypesFor('CREATE_SWITCHED_INJECTION').has(spec.type)) return false;

        const switchType = spec.switchType;
        if (!switchType || !SWITCH_TYPES.has(switchType)) return false;

        const switchId = `${spec.provisionalId}_${switchType}`;
        if (!this.isFreeId(spec.provisionalId) || !this.isFreeId(switchId)) return false;

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
        const busbar = this.pendingBusbar(command);
        if (!busbar) return false;

        const pending = pendingOrders(this.pending, busbar.vlId, undefined, command);
        if (!this.model.isOrderAvailable(busbar, position.order, pending)) return false;

        return this.amend(command, { order: position.order, direction: position.direction });
    }

    /** A bay on a fictitious busbar has no busbar to slide along. */
    canBeginBayMove(equipmentId: string): boolean {
        const created = this.pendingCreate(equipmentId);
        if (created) return this.pendingBusbar(created) !== undefined;

        const feeder = this.movableFeeder(equipmentId);
        return feeder !== undefined && this.busbarOf(feeder.slot) !== undefined;
    }

    beginBayMove(equipmentId: string): boolean {
        const created = this.pendingCreate(equipmentId);
        if (created) return this.beginPendingBayMove(created);

        const feeder = this.movableFeeder(equipmentId);
        const busbar = feeder && this.busbarOf(feeder.slot);
        if (!feeder || !busbar) return false;

        const pending = pendingOrders(this.pending, busbar.vlId, feeder.node.id);
        return this.startBayMove(equipmentId, busbar, feeder.target.direction ?? 'BOTTOM', pending);
    }

    private beginPendingBayMove(command: PendingCreateCommand): boolean {
        const busbar = this.pendingBusbar(command);
        if (!busbar) return false;

        const pending = pendingOrders(this.pending, busbar.vlId, undefined, command);
        return this.startBayMove(command.equipmentId, busbar, command.createSpec.direction ?? 'TOP', pending);
    }

    private startBayMove(
        equipmentId: string,
        busbar: BusbarTarget,
        direction: FeederDirection,
        pending: PendingOrders,
    ): boolean {
        const candidates = this.placement.slotCandidates(busbar, pending);
        if (candidates.length === 0) return false;
        return this.setGesture({ kind: 'BAY_MOVE', equipmentId, direction, candidates });
    }

    private pendingBusbar(command: PendingCreateCommand): BusbarTarget | undefined {
        const anchor = command.pendingMarker && this.targets.get(command.pendingMarker.targetId);
        return anchor?.kind === 'BUSBAR' ? anchor : undefined;
    }

    cancelGesture(): void {
        if (!this.gesture) return;
        this.gesture = null;
        document.removeEventListener('keydown', this.onKeyDown);
        this.paintTargets();
        this.emit('gesture:changed', { gesture: null, action: null });
    }

    private setGesture(gesture: Gesture): boolean {
        this.gesture = gesture;
        this.clearHoverSlots();
        document.addEventListener('keydown', this.onKeyDown);
        this.paintTargets();
        this.emit('gesture:changed', { gesture, action: this.switchAction() });
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
        this.placeBayAt(gesture, this.dom.toDiagramX(event.clientX, event.clientY));
    }

    /** Second click: keep the far end and let the host ask for the properties. */
    private pickSwitchEnd(gesture: SwitchGesture, node: NodeMetadata | undefined): void {
        const clicked = new Set(this.targetsAt(node).map((target) => target.id));
        const second = gesture.candidates.find((end) => clicked.has(end.id));
        if (!second) return;

        this.setGesture({ ...gesture, kind: 'SWITCH', second });
    }

    private placeBayAt(bayMove: BayMoveGesture, x: number | undefined): void {
        const chosen = nearestSlot(bayMove.candidates, x);
        if (!chosen) return;

        const { equipmentId, direction } = bayMove;
        this.setBayPosition(equipmentId, { order: chosen.order, direction });
    }


    private movableFeeder(
        equipmentId: string,
    ): { target: EquipmentTarget; node: NodeMetadata; slot: BaySlot } | undefined {
        const target = this.targetFor(equipmentId, 'EQUIPMENT', 'UPDATE_BAY_POSITION');
        if (target?.node === undefined) return undefined;

        const node = this.model.feederNodeOf(target);
        const slot = node && this.model.slotOfFeeder(node);
        return node && slot ? { target, node, slot } : undefined;
    }

    private paintTargets(): void {
        const gesture = this.gesture;
        const newSwitch = gesture?.kind === 'SWITCH' ? gesture : undefined;

        const ends = newSwitch?.second ? [newSwitch.second] : (newSwitch?.candidates ?? []);

        this.dom.setNodeTargets(gesture ? [] : this.nodeTargetIds);
        this.dom.setSwitchEnds(newSwitch?.first.id ?? null, ends.map((end) => end.id));
        this.dom.setBaySlots(this.shownSlot ? [this.shownSlot] : []);
        this.dom.setMovingBay(gesture?.kind === 'BAY_MOVE' ? this.movingElementId(gesture) : null);
    }

    private movingElementId({ equipmentId }: BayMoveGesture): string | null {
        const created = this.pendingCreate(equipmentId);
        if (created) return created.pendingMarker?.elementId ?? null;
        return this.movableFeeder(equipmentId)?.node.id ?? null;
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
    ): number | undefined {
        const busbar = targets.find((target) => target.kind === 'BUSBAR');
        const x = this.dom.toDiagramX(event.clientX, event.clientY);
        if (busbar?.kind !== 'BUSBAR' || x === undefined) return undefined;

        return nearestSlot(this.placement.slotCandidates(busbar), x)?.order;
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
        if (!DELETABLE_TYPES.has(type) && type !== 'BUS') {
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
            elements: this.selectedEquipmentIds.flatMap((id) => {
                const type = this.equipmentType(id);
                return type ? [{ id, type }] : [];
            }),
        });
    }

    private refreshTargets(): void {
        this.cancelGesture();
        this.clearHoverSlots();
        const { created, claimed } = pendingScope(this.pending);

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

        this.painter.paint();
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

function allows(target: EditTarget, operation: EditOperation): boolean {
    return availableOperations(target).includes(operation);
}
