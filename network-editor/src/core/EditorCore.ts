import type { SldComponentOptions } from '@powsybl/network-viewer-core';

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
import { UpdateBayPositionCommand, type BayTurn } from './commands/UpdateBayPositionCommand';
import { UpdatePropertiesCommand } from './commands/UpdatePropertiesCommand';
import { buildBayFlip, invertFlip, type BayFlip, type FeederLevels } from './bayFlip';
import {
    busbarStretch,
    cellWidthFromColumns,
    layoutBay,
    reflowCells,
    type AddedCell,
    type BayColumn,
    type BayGeometry,
    type LayoutCell,
} from './bayLayout';
import { SvgDomService, type CellFlip } from '../dom/SvgDomService';
import {
    alignedExtent,
    defaultFeederLength,
    defaultSwitchAt,
    type FeederShape,
    type PendingBadgeView,
    type PendingPreview,
    type PlacedSymbol,
    type SwitchPreview,
    type SymbolView,
} from '../dom/svgShapes';
import {
    BAY_SLOT_CLASS,
    DELETABLE_BAY_TYPES,
    DELETABLE_TYPES,
    NODE_COMPONENT_TYPE,
    SWITCH_TYPES,
    isCreatedNodeId,
    nodeComponentType,
    toElementType,
    toDirection,
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
    type SymbolProvider,
    type TargetEvent, type DeleteScope, type OrderClaim,
} from './types';

const DRAG_THRESHOLD = 10;

export class EditorCore {
    private destroyed = false;

    private gesture: Gesture | null = null;

    private readonly history = new CommandStack((state) => {
        if (this.destroyed) return;
        this.reflowBays();
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
    private readonly createdBayXs = new Map<string, number>();
    private readonly drawnCells = new WeakMap<Element, Omit<LayoutCell, 'id' | 'order'>>();
    private mouseDownX = 0;
    private mouseDownY = 0;

    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly symbols: SymbolProvider,
        private readonly onEvent?: EditorEventListener,
        private readonly onTargets?: (event: TargetEvent) => void,
    ) {
        const container: HTMLElement = this.dom.getContainer();
        container.addEventListener('mousedown', this.onMouseDown);
        container.addEventListener('mouseup', this.onMouseUp);
        container.addEventListener('mousemove', this.onMouseMove);
        this.reflowBays();
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
            bay = { order, direction: spec.direction ?? 'TOP' };
        }

        if (target.kind === 'NODE') {
            const standing = this.pendingOrders(target.vlId).claims
                .find((claim) => claim.iidmNode === target.node);
            if (standing) bay = { order: standing.order, direction: standing.direction ?? 'BOTTOM' };
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

    actionsFor(target: EditTarget, insertion?: number): EditorAction[] {
        return buildActions(this, target, insertion);
    }

    proposedOrder(target: BusbarTarget, insertion?: number): number | undefined {
        return (
            insertion ??
            this.model.nextOrderForBusbar(target, this.pendingOrders(target.vlId))
        );
    }

    getBayPosition(equipmentId: string): BayPosition | undefined {
        const pending = this.pendingBayPosition(equipmentId);
        if (pending) return pending.bayPosition;

        const target = this.targets.get(equipmentId);
        if (target?.kind !== 'EQUIPMENT' || target.order === undefined) return undefined;
        return { order: target.order, direction: target.direction ?? 'BOTTOM' };
    }

    setBayPosition(equipmentId: string, position: BayPosition): boolean {
        const created = this.findPendingCreate(equipmentId);
        if (created) return this.amendBayPosition(created, position);

        const feeder = this.movableFeeder(equipmentId);
        if (!feeder) return false;
        const { target, node, slot } = feeder;

        const pending = this.pendingOrders(slot.vlId, node.id);
        if (!this.model.isOrderAvailable(slot, position.order, pending)) return false;

        const from = toDirection(node.direction) ?? 'BOTTOM';
        if (position.direction !== from) return this.flipBay(target, node, slot, position, from);

        this.history.push(new UpdateBayPositionCommand(target, node, slot, position, this.dom, this.model));
        return true;
    }

    private pendingBayPosition(equipmentId: string): UpdateBayPositionCommand | undefined {
        return this.history.pending.findLast(
            (command): command is UpdateBayPositionCommand =>
                command instanceof UpdateBayPositionCommand && command.equipmentId === equipmentId,
        );
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
        const turn = cell ? this.bayTurn(node, cellFeeders, from, position.direction) : undefined;
        if (!turn) return false;

        const commands: UpdateBayPositionCommand[] = [
            new UpdateBayPositionCommand(target, node, slot, position, this.dom, this.model, turn),
        ];
        for (const sibling of cellFeeders) {
            if (sibling.id === node.id) continue;

            const siblingTarget = sibling.equipmentId ? this.targets.get(sibling.equipmentId) : undefined;
            if (siblingTarget?.kind !== 'EQUIPMENT' || siblingTarget.node === undefined) return false;
            if (!availableOperations(siblingTarget).includes('UPDATE_BAY_POSITION')) return false;

            const siblingPosition = { order: sibling.order!, direction: position.direction };
            commands.push(
                new UpdateBayPositionCommand(siblingTarget, sibling, slot, siblingPosition, this.dom, this.model),
            );
        }

        this.history.pushAll(commands);
        return true;
    }

    private bayTurn(
        feeder: NodeMetadata,
        cellFeeders: readonly NodeMetadata[],
        from: FeederDirection,
        to: FeederDirection,
    ): BayTurn | undefined {
        const vlId = feeder.vid ?? '';
        const busbarYs = this.model.busbarNodes(vlId).flatMap((busbar) => {
            const span = this.dom.getDiagramSpan(busbar.id);
            return span ? [span.y] : [];
        });
        const source = this.levelsOf(feeder);
        if (busbarYs.length === 0 || !source) return undefined;

        const flip = buildBayFlip(busbarYs, source, this.feederLevels(vlId, to) ?? source, from, to);

        const heights = new Map<string, number>();
        for (const id of this.dom.bayCellElementIds(feeder.id)) {
            const componentType =
                this.model.getNodeById(id)?.componentType ??
                this.model.getFeederInfoById(id)?.componentType;
            if (componentType) heights.set(id, this.componentSize(componentType).height);
        }

        return {
            forth: this.cellFlip(feeder, cellFeeders, to, flip, heights),
            back: this.cellFlip(feeder, cellFeeders, from, invertFlip(flip), heights),
            feederNodeIds: cellFeeders.map((cellFeeder) => cellFeeder.id),
            from,
        };
    }

    private cellFlip(
        feeder: NodeMetadata,
        cellFeeders: readonly NodeMetadata[],
        direction: FeederDirection,
        y: BayFlip,
        heights: ReadonlyMap<string, number>,
    ): CellFlip {
        const orientation = direction === 'TOP' ? 'UP' : 'DOWN';
        const feeders = new Map<string, string | null>();
        const arrows = new Map<string, string | null>();

        for (const cellFeeder of cellFeeders) {
            const symbol = this.symbols.createSymbol(cellFeeder.componentType, { orientation });
            feeders.set(cellFeeder.id, symbol.firstElementChild?.getAttribute('transform') ?? null);

            for (const info of this.model.getFeederInfosForEquipment(cellFeeder.equipmentId ?? '')) {
                const { width, height } = this.componentSize(info.componentType);
                arrows.set(info.id, direction === 'BOTTOM' ? `rotate(180.0,${width / 2},${height / 2})` : null);
            }
        }
        return { feederNodeId: feeder.id, direction, y, heights, feeders, arrows };
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

    private standingBayClaims(equipmentId: string, scope: DeleteScope, kind: 'element' | 'bay'): OrderClaim[] {
        if (kind !== 'element') return [];
        if (this.model.collectBay(equipmentId).nodes.length <= scope.nodes.length) return [];

        const claims: OrderClaim[] = [];
        const movedPosition = this.pendingBayPosition(equipmentId)?.bayPosition;
        for (const node of scope.nodes) {
            const order = movedPosition?.order ?? node.order;
            if (order === undefined) continue;
            const slot = this.model.slotOfFeeder(node);
            const x = this.nodeCentre(node)?.x;
            if (!slot || x === undefined) continue;

            const direction = movedPosition?.direction ?? toDirection(node.direction);
            claims.push({ ...slot, order, x, iidmNode: node.iidmNode, direction });
        }
        return claims;
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
            insertion: slot.order,
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
        const busbar = this.pendingBusbar(command);
        if (!busbar) return false;

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
        const created = this.findPendingCreate(equipmentId);
        if (created) return this.beginPendingBayMove(created);

        const feeder = this.movableFeeder(equipmentId);
        const busbar = feeder && this.busbarOf(feeder.slot);
        if (!feeder || !busbar) return false;

        const pending = this.pendingOrders(busbar.vlId, feeder.node.id);
        return this.startBayMove(equipmentId, busbar, feeder.target.direction ?? 'BOTTOM', pending);
    }

    private beginPendingBayMove(command: PendingCreateCommand): boolean {
        const busbar = this.pendingBusbar(command);
        if (!busbar) return false;

        const pending = this.pendingOrders(busbar.vlId, undefined, command);
        return this.startBayMove(command.equipmentId, busbar, command.createSpec.direction ?? 'TOP', pending);
    }

    private startBayMove(
        equipmentId: string,
        busbar: BusbarTarget,
        direction: FeederDirection,
        pending: PendingOrders,
    ): boolean {
        const candidates = this.baySlotCandidates(busbar, pending);
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
        this.placeBayAtSlot(gesture, target);
    }

    /** Second click: keep the far end and let the host ask for the properties. */
    private pickSwitchEnd(gesture: SwitchGesture, node: NodeMetadata | undefined): void {
        const clicked = new Set(this.targetsAt(node).map((target) => target.id));
        const second = gesture.candidates.find((end) => clicked.has(end.id));
        if (!second) return;

        this.setGesture({ ...gesture, kind: 'SWITCH', second });
    }

    private placeBayAtSlot(bayMove: BayMoveGesture, target: Element | null): void {
        const clicked = target?.closest<SVGGElement>(`g.${BAY_SLOT_CLASS}`)?.id;
        const chosen = bayMove.candidates.find((candidate) => candidate.id === clicked);
        if (!chosen) return;

        const { equipmentId, direction } = bayMove;
        this.setBayPosition(equipmentId, { order: chosen.order, direction });
    }


    private bayGeometry(busbar: BusbarTarget, pending: PendingOrders): BayGeometry | undefined {
        const span = this.dom.getDiagramSpan(busbar.id);
        if (!span) return undefined;

        return layoutBay(span, this.feederColumns(busbar, pending));
    }

    private baySlotCandidates(
        busbar: BusbarTarget,
        pending: PendingOrders = this.pendingOrders(busbar.vlId),
    ): BaySlotCandidate[] {
        const geometry = this.bayGeometry(busbar, pending);
        if (!geometry) return [];

        return geometry.gaps.flatMap((gap) => {
            const order = this.model.orderBetween(busbar, gap.leftOrder, gap.rightOrder, pending);
            if (order === undefined) return [];
            return [{ id: `ne-slot-${order}`, order, x: gap.x, y: geometry.y }];
        });
    }

    private feederColumns(slot: BaySlot, { claims, vacated }: PendingOrders): BayColumn[] {
        const columns: BayColumn[] = [];
        for (const node of this.model.feedersInSection(slot)) {
            if (vacated.has(node.id)) continue;
            const x = this.nodeCentre(node)?.x;
            if (x !== undefined) columns.push({ x, order: node.order });
        }
        for (const claim of claims) {
            if (claim.sectionIndex !== slot.sectionIndex) continue;
            const x = claim.x ?? this.reflowedX(claim);
            if (x !== undefined) columns.push({ x, order: claim.order });
        }
        return columns.sort((a, b) => a.x - b.x);
    }

    private reflowBays(): void {
        this.createdBayXs.clear();
        for (const vlId of this.model.voltageLevelIds()) this.reflowVoltageLevel(vlId);
    }

    private reflowVoltageLevel(vlId: string): void {
        const cellWidth = this.model.cellWidth();
        const busbars = this.model.busbarNodes(vlId);
        const cells = busbars[0] ? this.dom.voltageLevelCells(busbars[0].id) : [];
        const pending = this.pendingOrders(vlId);

        const movedOrders = new Map<string, number>();
        for (const claim of pending.claims) {
            if (claim.vacatedNodeId) movedOrders.set(claim.vacatedNodeId, claim.order);
        }

        const layout: LayoutCell[] = [];
        const removed: LayoutCell[] = [];
        cells.forEach((cell, index) => {
            const id = String(index);
            const measured = this.measureCell(cell, movedOrders, cellWidth);
            const drawn = this.drawnCells.get(cell);
            if (measured) {
                layout.push({ id, ...measured });
                if (!drawn) {
                    const { left, width, section } = measured;
                    this.drawnCells.set(cell, { left, width, section });
                }
            } else if (drawn) {
                removed.push({ id, ...drawn });
            }
        });

        const added: AddedCell[] = pending.claims.filter(isCreatedBay).map((claim) => ({
            id: createdBayKey(vlId, claim.order),
            width: cellWidth,
            order: claim.order,
            section: claim.sectionIndex,
        }));

        const lefts = reflowCells(layout, added, removed);
        for (const { id, left } of layout) {
            this.dom.setCellShift(cells[Number(id)], lefts.get(id)! - left);
        }
        for (const { id, width } of added) {
            this.createdBayXs.set(id, lefts.get(id)! + width / 2);
        }
        for (const busbar of busbars) {
            if (busbar.sectionIndex === undefined) continue;
            const { dx, dWidth } = busbarStretch(busbar.sectionIndex, added, removed);
            this.dom.setBusbarStretch(busbar.id, dx, dWidth);
        }
    }

    /** Where the cell stands before any editor shift, and the order it takes in the layout. */
    private measureCell(
        cell: Element,
        movedOrders: ReadonlyMap<string, number>,
        cellWidth: number,
    ): Omit<LayoutCell, 'id'> | undefined {
        const extern = cell.classList.contains('sld-extern-cell');
        const xs: number[] = [];
        let order: number | undefined;
        let section: number | undefined;

        for (const id of this.dom.cellElementIds(cell)) {
            const node = this.model.getNodeById(id);
            const x = node && this.nodeCentre(node)?.x;
            if (!node || x === undefined) continue;
            xs.push(x);

            const nodeOrder = movedOrders.get(node.id) ?? node.order;
            if (!extern || nodeOrder === undefined) continue;
            order = Math.min(order ?? nodeOrder, nodeOrder);
            section ??= this.model.slotOfFeeder(node)?.sectionIndex;
        }
        if (xs.length === 0) return undefined;

        const minX = Math.min(...xs);
        const maxX = Math.max(...xs);
        return {
            left: minX - cellWidth / 2 - this.dom.getCellShift(cell),
            width: cellWidthFromColumns(minX, maxX, cellWidth),
            order,
            section,
        };
    }

    private reflowedX(claim: OrderClaim): number | undefined {
        if (claim.vacatedNodeId === undefined) {
            return this.createdBayXs.get(createdBayKey(claim.vlId, claim.order));
        }
        const node = this.model.getNodeById(claim.vacatedNodeId);
        return node && this.nodeCentre(node)?.x;
    }

    private movableFeeder(
        equipmentId: string,
    ): { target: EquipmentTarget; node: NodeMetadata; slot: BaySlot } | undefined {
        const target = this.targets.get(equipmentId);
        if (target?.kind !== 'EQUIPMENT' || target.node === undefined) return undefined;
        if (!availableOperations(target).includes('UPDATE_BAY_POSITION')) return undefined;

        const node = this.model.feederNodeOf(target);
        const slot = node && this.model.slotOfFeeder(node);
        return node && slot ? { target, node, slot } : undefined;
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
    ): number | undefined {
        const busbar = targets.find((target) => target.kind === 'BUSBAR');
        const x = this.dom.toDiagramX(event.clientX, event.clientY);
        if (busbar?.kind !== 'BUSBAR' || x === undefined) return undefined;

        const nearest = this.baySlotCandidates(busbar).reduce<BaySlotCandidate | undefined>(
            (best, candidate) =>
                best && Math.abs(best.x - x) <= Math.abs(candidate.x - x) ? best : candidate,
            undefined,
        );
        return nearest?.order;
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
        const open = command.createSpec.properties.open === true;

        if (!from || !far) return undefined;

        return {
            kind: 'SWITCH',
            id: command.pendingMarker.elementId,
            label: command.equipmentId,
            from,
            to: isSpan(far) ? { x: clampToSpan(far, from.x), y: far.y } : far,
            symbol: first && this.switchSymbol(command.createSpec.type, first.id, open),
        };
    }

    private endAnchor(target: EditTarget): DiagramPoint | DiagramSpan | undefined {
        if (target.kind === 'BUSBAR') return this.dom.getDiagramSpan(target.id);
        return target.kind === 'NODE' ? this.nodePoint(target) : undefined;
    }

    private nodePoint(target: NodeTarget): DiagramPoint | undefined {
        const node = this.model.getNodeById(target.id);
        return node && this.nodeCentre(node);
    }

    private bayPreview(busbar: BusbarTarget, spec: CreateSpec): FeederShape | undefined {
        const geometry =
            spec.order === undefined
                ? undefined
                : this.bayGeometry(busbar, this.pendingOrders(busbar.vlId));
        const column = geometry?.columns.find((candidate) => candidate.order === spec.order);
        if (!geometry || !column) return undefined;

        const side = spec.direction === 'TOP' ? 'UP' : 'DOWN';
        const from = { x: column.x, y: geometry.y };
        const busbars = this.crossedBusbars(busbar, from.y, side);
        return this.feederShape(busbar.vlId, from, side, spec, spec.switchType ?? 'BREAKER', busbar.id, busbars);
    }

    private crossedBusbars(busbar: BusbarTarget, anchorY: number, side: 'UP' | 'DOWN'): PlacedSymbol[] {
        const sign = side === 'UP' ? -1 : 1;
        const crossed: PlacedSymbol[] = [];
        for (const node of this.model.busbarNodes(busbar.vlId)) {
            if (node.sectionIndex !== busbar.sectionIndex) continue;
            const span = this.dom.getDiagramSpan(node.id);
            const symbol =
                span && this.symbolFor(NODE_COMPONENT_TYPE.DISCONNECTOR, { open: node.id !== busbar.id }, node.id);
            if (symbol) crossed.push({ at: sign * (span.y - anchorY), symbol });
        }
        return crossed;
    }

    private feederPreview(target: NodeTarget, spec: CreateSpec): FeederShape | undefined {
        const point = this.nodePoint(target);
        if (!point) return undefined;

        const busbarY = this.busbarY(target.vlId);
        const side = busbarY !== undefined && point.y < busbarY ? 'UP' : 'DOWN';
        return this.feederShape(target.vlId, point, side, spec, spec.switchType, target.id);
    }

    private feederShape(
        vlId: string,
        from: DiagramPoint,
        side: 'UP' | 'DOWN',
        spec: CreateSpec,
        switchType: ElementType | undefined,
        anchorNodeId: string,
        busbars?: PlacedSymbol[],
    ): FeederShape {
        const terminal = this.terminalSymbol(spec, side, anchorNodeId);
        const switchSize = switchType === undefined ? undefined : this.switchSize();
        const levels = this.feederLevels(vlId, side === 'UP' ? 'TOP' : 'BOTTOM');
        const extent = alignedExtent(levels, from.y, side, terminal?.height ?? 0, switchSize);

        const shape: FeederShape = {
            ...from,
            side,
            length: extent.length ?? defaultFeederLength(switchSize),
            terminal,
            busbars,
        };
        const symbol = switchType === undefined ? undefined : this.switchSymbol(switchType, anchorNodeId);
        if (symbol && switchSize !== undefined) {
            shape.switch = { at: extent.switchAt ?? defaultSwitchAt(switchSize), symbol };
        }
        return shape;
    }

    private feederLevels(vlId: string, direction: FeederDirection): FeederLevels | undefined {
        const feeder = this.model.feederNodes(vlId, direction).find((node) => !isCreatedNodeId(node.id));
        return feeder && this.levelsOf(feeder);
    }

    private levelsOf(feeder: NodeMetadata): FeederLevels | undefined {
        const feederY = this.nodeCentre(feeder)?.y;
        if (feederY === undefined) return undefined;

        const breaker = this.model.bayBreaker(feeder);
        return { feederY, switchY: breaker && this.nodeCentre(breaker)?.y };
    }

    private nodeCentre(node: NodeMetadata): DiagramPoint | undefined {
        const point = this.dom.getDiagramPoint(node.id);
        if (!point) return undefined;

        const { width, height } = this.componentSize(node.componentType);
        return { x: point.x + width / 2, y: point.y + height / 2 };
    }

    private busbarY(vlId: string): number | undefined {
        for (const node of this.model.busbarNodes(vlId)) {
            const span = this.dom.getDiagramSpan(node.id);
            if (span) return span.y;
        }
        return undefined;
    }

    private switchSize(): number {
        return this.componentSize(NODE_COMPONENT_TYPE.BREAKER).width;
    }

    private componentSize(componentType: string): { width: number; height: number } {
        return this.symbols.componentSize(componentType);
    }

    private switchSymbol(type: ElementType, anchorNodeId: string, open = false,): SymbolView | undefined {
        return this.symbolFor(NODE_COMPONENT_TYPE[type], { open: open, orientation: 'RIGHT' }, anchorNodeId);
    }

    private terminalSymbol(spec: CreateSpec, side: 'UP' | 'DOWN', anchorNodeId: string): SymbolView | undefined {
        return this.symbolFor(nodeComponentType(spec.type, spec.properties), { orientation: side }, anchorNodeId);
    }

    private symbolFor(
        componentType: string,
        options: SldComponentOptions,
        anchorNodeId: string,
    ): SymbolView | undefined {
        const { width, height } = this.componentSize(componentType);
        if (width <= 0 || height <= 0) return undefined;
        const element = this.symbols.createSymbol(componentType, options);
        element.classList.add(...this.dom.getVoltageClasses(anchorNodeId));
        return { element, width, height };
    }

    private pendingOrders(vlId: string, vacating?: string, ignore?: Command): PendingOrders {
        const claims: OrderClaim[] = [];
        const vacated = new Set<string>(vacating ? [vacating] : []);

        const lastClaims = new Map<string, OrderClaim>();

        for (const command of this.history.pending) {
            if (command === ignore) continue;
            for (const claim of command.orderClaims ?? []) {
                if (claim.vlId !== vlId) continue;
                if (!claim.vacatedNodeId) {
                    claims.push(claim);
                    continue;
                }
                vacated.add(claim.vacatedNodeId);

                if (claim.vacatedNodeId !== vacating) lastClaims.set(claim.vacatedNodeId, claim);
            }
        }
        return { claims: [...claims, ...lastClaims.values()], vacated };
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

function isSpan(anchor: DiagramPoint | DiagramSpan): anchor is DiagramSpan {
    return 'left' in anchor;
}

function clampToSpan(span: DiagramSpan, x: number): number {
    return Math.min(Math.max(x, span.left), span.right);
}

function isCreatedBay(claim: OrderClaim): boolean {
    return claim.vacatedNodeId === undefined && claim.x === undefined;
}

function createdBayKey(vlId: string, order: number): string {
    return `${vlId}:${order}`;
}
