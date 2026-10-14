import { isPendingCreate, type Command } from '../commands/Command.ts';
import { CreateSwitchCommand } from '../commands/CreateSwitchCommand.ts';
import { RenameCommand } from '../commands/RenameCommand.ts';
import { ReplaceCommand } from '../commands/ReplaceCommand.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import type { BayPlacement } from '../layout/BayPlacement.ts';
import { pendingOrders } from '../commands/pending.ts';
import { pushTo } from '../utils.ts';
import {
    NODE_COMPONENT_TYPE,
    nodeComponentType,
    type BusbarTarget,
    type CreateSpec,
    type DiagramPoint,
    type DiagramSpan,
    type EditTarget,
    type ElementType,
    type NodeTarget,
} from '../types.ts';
import type { DiagramGeometry } from '../layout/DiagramGeometry.ts';
import type { SvgDomService } from '../dom/SvgDomService.ts';
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
} from '../dom/svgShapes.ts';

export class PendingPainter {
    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly geometry: DiagramGeometry,
        private readonly placement: BayPlacement,
        private readonly pending: () => readonly Command[],
        private readonly targets: () => ReadonlyMap<string, EditTarget>,
    ) {}

    paint(): void {
        const previews = this.pendingPreviews();
        this.dom.setPendingPreviews(previews);
        this.dom.setRenamedLabels(this.renamedLabels());
        this.dom.setPendingBadges(this.pendingBadges(new Set(previews.map(({ id }) => id))));
    }

    private renamedLabels(): Map<string, string> {
        const labels = new Map<string, string>();

        for (const command of this.pending()) {
            if (!(command instanceof RenameCommand || command instanceof ReplaceCommand)) continue;
            if (command.newId === command.equipmentId) continue;
            for (const node of this.model.getNodesForEquipment(command.newId)) {
                labels.set(node.id, command.newId);
            }
        }
        return labels;
    }

    private pendingBadges(drawn: ReadonlySet<string>): Map<string, PendingBadgeView[]> {
        const badges = new Map<string, PendingBadgeView[]>();

        for (const command of this.pending()) {
            const marker = command.pendingMarker;
            if (!marker || (marker.elementId && drawn.has(marker.elementId))) continue;
            pushTo(badges, marker.nodeId, { id: marker.elementId, label: marker.label });
        }
        return badges;
    }

    private pendingPreviews(): PendingPreview[] {
        const previews: PendingPreview[] = [];

        for (const command of this.pending()) {
            if (!isPendingCreate(command)) continue;

            if (command instanceof CreateSwitchCommand) {
                const preview = this.switchPreview(command);
                if (preview) previews.push(preview);
                continue;
            }

            const marker = command.pendingMarker;
            const elementId = marker?.elementId;
            const anchor = marker && this.targets().get(marker.targetId);
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
        const first = this.targets().get(command.ends.first);
        const second = this.targets().get(command.ends.second);
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
        return node && this.geometry.nodeCentre(node);
    }

    private bayPreview(busbar: BusbarTarget, spec: CreateSpec): FeederShape | undefined {
        const geometry =
            spec.order === undefined
                ? undefined
                : this.placement.bayGeometry(busbar, pendingOrders(this.pending(), busbar.vlId));
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
                span &&
                this.geometry.symbolFor(NODE_COMPONENT_TYPE.DISCONNECTOR, { open: node.id !== busbar.id }, node.id);
            if (symbol) crossed.push({ at: sign * (span.y - anchorY), symbol });
        }
        return crossed;
    }

    private feederPreview(target: NodeTarget, spec: CreateSpec): FeederShape | undefined {
        const point = this.nodePoint(target);
        if (!point) return undefined;

        const busbarY = this.geometry.busbarY(target.vlId);
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
        const levels = this.geometry.feederLevels(vlId, side === 'UP' ? 'TOP' : 'BOTTOM');
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

    private switchSize(): number {
        return this.geometry.componentSize(NODE_COMPONENT_TYPE.BREAKER).width;
    }

    private switchSymbol(type: ElementType, anchorNodeId: string, open = false): SymbolView | undefined {
        return this.geometry.symbolFor(NODE_COMPONENT_TYPE[type], { open, orientation: 'RIGHT' }, anchorNodeId);
    }

    private terminalSymbol(spec: CreateSpec, side: 'UP' | 'DOWN', anchorNodeId: string): SymbolView | undefined {
        const componentType = nodeComponentType(spec.type, spec.properties);
        return this.geometry.symbolFor(componentType, { orientation: side }, anchorNodeId);
    }
}

function isSpan(anchor: DiagramPoint | DiagramSpan): anchor is DiagramSpan {
    return 'left' in anchor;
}

function clampToSpan(span: DiagramSpan, x: number): number {
    return Math.min(Math.max(x, span.left), span.right);
}
