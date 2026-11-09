import { EditorModel } from '../model/EditorModel.ts';
import { buildActions, switchAction, type EditorAction } from '../actions/actions.ts';
import { availableOperations, creatableTypesFor } from '../actions/operations.ts';
import { pushTo } from '../utils.ts';
import { menuItemsFor } from '../actions/menu.ts';
import type { NetworkModification } from '../modifications/modifications.ts';
import { normalizeModifications } from '../modifications/normalize.ts';
import { CommandStack } from '../commands/CommandStack.ts';
import type { Command, PendingCreateCommand } from '../commands/Command.ts';
import { claimedTargets, findPendingCreate, pendingBayPosition, pendingOrders } from '../commands/pending.ts';
import { CreateCommand } from '../commands/CreateCommand.ts';
import { CreateSwitchCommand } from '../commands/CreateSwitchCommand.ts';
import { CreateSwitchedInjectionCommand } from '../commands/CreateSwitchedInjectionCommand.ts';
import { DeleteElementCommand } from '../commands/DeleteElementCommand.ts';
import { MoveBayCommand } from '../commands/MoveBayCommand.ts';
import { RenameCommand } from '../commands/RenameCommand.ts';
import { ReplaceCommand } from '../commands/ReplaceCommand.ts';
import { UpdateBayPositionCommand } from '../commands/UpdateBayPositionCommand.ts';
import { UpdatePendingBayCommand } from '../commands/UpdatePendingBayCommand.ts';
import { UpdatePropertiesCommand } from '../commands/UpdatePropertiesCommand.ts';
import { SvgDomService } from '../dom/SvgDomService.ts';
import { DiagramGeometry } from '../layout/DiagramGeometry.ts';
import { BayPlacement, nearestSlot } from '../layout/BayPlacement.ts';
import { PendingPainter } from './PendingPainter.ts';
import {
    DEFAULT_DIRECTION,
    DELETABLE_BAY_TYPES,
    DELETABLE_TYPES,
    INJECTION_TYPES,
    SWITCH_TYPES,
    isCreatedNodeId,
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
} from '../types.ts';

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
            this.emit('modifications:changed', { modifications: this.journal() });
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
            bay = { order, direction: spec.direction ?? DEFAULT_DIRECTION };
        }

        if (target.kind === 'NODE') {
            const standing = pendingOrders(this.pending, target.vlId).claims
                .find((claim) => claim.iidmNode === target.node);
            if (standing) bay = { order: standing.order, direction: standing.direction ?? DEFAULT_DIRECTION };
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
                spec.fields,
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

        if (!this.pendingCreate(equipmentId)) {
            const target = this.renameTarget(equipmentId);
            if (!target || !allows(target, 'RENAME')) return false;
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

    private isFreeId(equipmentId: string): boolean {
        return !this.model.isIdTaken(equipmentId);
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
        const node = this.model.getNodesForEquipment(equipmentId).find((candidate) => isCreatedNodeId(candidate.id));
        return node && findPendingCreate(this.pending, node.id);
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
        const pending = pendingOrders(this.pending, destination.vlId, [node.id]);
        const order = this.model.nextOrderForBusbar(destination, pending);
        if (order === undefined) return false;

        const direction = this.getBayPosition(equipmentId)?.direction ?? DEFAULT_DIRECTION;
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
        return { order: target.order, direction: target.direction ?? DEFAULT_DIRECTION };
    }

    setBayPosition(equipmentId: string, position: BayPosition): boolean {
        const created = this.pendingCreate(equipmentId);
        if (created) return this.setPendingBayPosition(created, position);

        const feeder = this.movableFeeder(equipmentId);
        const current = this.getBayPosition(equipmentId);
        if (!feeder || !current) return false;
        const { node, slot } = feeder;

        // A fork is one cell: all its feeders shift by the same delta, and turn together.
        const moves = this.cellMoves(node, slot, position.order - current.order);
        if (!moves) return false;
        const pending = pendingOrders(this.pending, slot.vlId, moves.map((move) => move.node.id));
        if (!moves.every((move) => this.model.isOrderAvailable(slot, move.order, pending))) return false;

        const from = toDirection(node.direction) ?? DEFAULT_DIRECTION;
        const turns = position.direction !== from;
        const turn =
            turns && this.dom.bayCell(node.id)
                ? this.geometry.bayTurn(node, moves.map((move) => move.node), from, position.direction)
                : undefined;
        if (turns && !turn) return false;

        this.history.pushAll(
            moves.map(
                (move) =>
                    new UpdateBayPositionCommand(
                        move.target,
                        move.node,
                        slot,
                        { order: move.order, direction: position.direction },
                        this.dom,
                        this.model,
                        move.node.id === node.id ? turn : undefined,
                    ),
            ),
        );
        return true;
    }

    private cellMoves(
        node: NodeMetadata,
        slot: BaySlot,
        delta: number,
    ): { target: EquipmentTarget; node: NodeMetadata; order: number }[] | undefined {
        const moves = [];
        for (const feeder of this.cellFeeders(node, slot)) {
            const target = this.targetFor(feeder.equipmentId, 'EQUIPMENT', 'UPDATE_BAY_POSITION');
            const current = target && this.getBayPosition(target.equipmentId);
            if (target?.node === undefined || !current) return undefined;
            moves.push({ target, node: feeder, order: current.order + delta });
        }
        return moves;
    }

    /** A feeder and those sharing its cell (a fork), smallest order first: the cell moves as one block. */
    private cellFeeders(node: NodeMetadata, slot: BaySlot): NodeMetadata[] {
        const cell = this.dom.bayCell(node.id);
        if (!cell) return [node];
        return this.model
            .feedersInSection(slot)
            .filter((feeder) => this.dom.bayCell(feeder.id) === cell)
            .sort((a, b) => a.order! - b.order!);
    }

    applyProperties(
        equipmentId: string,
        changes: EquipmentProperties,
        fields: Readonly<Record<string, unknown>> = {},
    ): boolean {
        if (Object.keys(changes).length === 0 && Object.keys(fields).length === 0) return false;

        const type = this.equipmentType(equipmentId);
        if (!type) return false;

        this.history.push(
            new UpdatePropertiesCommand(
                equipmentId,
                type,
                changes,
                this.model,
                this.repaintSwitchState,
                fields,
            ),
        );
        return true;
    }

    group(edit: () => boolean): boolean {
        return this.history.group(edit);
    }

    deleteElements(equipmentIds: readonly string[], kind: 'element' | 'bay'): boolean {
        const commands: Command[] = [];

        for (const equipmentId of equipmentIds) {
            const command = this.deleteCommandFor(equipmentId, kind);
            if (!command) return false;
            commands.push(command);
        }

        this.history.pushAll(commands);
        return commands.length > 0;
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
        if (this.pendingCreate(equipmentId)) kind = 'element';

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
        return normalizeModifications(this.journal());
    }

    private journal(): NetworkModification[] {
        return this.history.pending.flatMap((command) => command.toModifications());
    }

    clearModifications(): void {
        while (this.history.canUndo) this.history.undo();
        this.history.clear();
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
            items: menuItemsFor((target, at) => this.actionsFor(target, at), offered, insertion),
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

    private setPendingBayPosition(command: PendingCreateCommand, position: BayPosition): boolean {
        const busbar = this.pendingBusbar(command);
        const nodeId = command.pendingMarker?.elementId;
        if (!busbar || !nodeId) return false;

        const pending = pendingOrders(this.pending, busbar.vlId, [], command);
        if (!this.model.isOrderAvailable(busbar, position.order, pending)) return false;

        const equipmentId = command.createSpec.provisionalId;
        this.history.push(new UpdatePendingBayCommand(equipmentId, nodeId, position, busbar, this.model));
        return true;
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

        const feeders = this.cellFeeders(feeder.node, feeder.slot);
        const lead = feeders[0].equipmentId ?? equipmentId;
        const pending = pendingOrders(this.pending, busbar.vlId, feeders.map((node) => node.id));
        return this.startBayMove(lead, busbar, feeder.target.direction ?? DEFAULT_DIRECTION, pending);
    }

    private beginPendingBayMove(command: PendingCreateCommand): boolean {
        const busbar = this.pendingBusbar(command);
        if (!busbar) return false;

        const { provisionalId, direction } = command.createSpec;
        const pending = pendingOrders(this.pending, busbar.vlId, [], command);
        return this.startBayMove(provisionalId, busbar, direction ?? DEFAULT_DIRECTION, pending);
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
        const claimed = claimedTargets(this.pending);

        const targets = this.model.collectTargets().map((target) => {
            if (target.kind !== 'EQUIPMENT') return target;
            if (this.pendingCreate(target.equipmentId)) return { ...target, created: true };
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
