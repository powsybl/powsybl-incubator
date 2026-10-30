import type { EditorModel } from '../model/EditorModel.ts';
import type { Command } from '../commands/Command.ts';
import { pendingOrders } from '../commands/pending.ts';
import {
    busbarStretch,
    cellWidthFromColumns,
    layoutBay,
    reflowCells,
    type AddedCell,
    type BayColumn,
    type BayGeometry,
    type LayoutCell,
} from './bayLayout.ts';
import type { BaySlot, BaySlotCandidate, BusbarTarget, OrderClaim, PendingOrders } from '../types.ts';
import type { SvgDomService } from '../dom/SvgDomService.ts';
import type { DiagramGeometry } from './DiagramGeometry.ts';

export class BayPlacement {
    private readonly createdBayXs = new Map<string, number>();
    private readonly drawnCells = new WeakMap<Element, Omit<LayoutCell, 'id' | 'order'>>();

    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly geometry: DiagramGeometry,
        private readonly pending: () => readonly Command[],
    ) {}

    reflow(): void {
        this.createdBayXs.clear();
        for (const vlId of this.model.voltageLevelIds()) this.reflowVoltageLevel(vlId);
    }

    bayGeometry(busbar: BusbarTarget, pending: PendingOrders): BayGeometry | undefined {
        const span = this.dom.getDiagramSpan(busbar.id);
        if (!span) return undefined;

        return layoutBay(span, this.feederColumns(busbar, pending));
    }

    slotCandidates(
        busbar: BusbarTarget,
        pending: PendingOrders = pendingOrders(this.pending(), busbar.vlId),
    ): BaySlotCandidate[] {
        const geometry = this.bayGeometry(busbar, pending);
        if (!geometry) return [];

        return geometry.gaps.flatMap((gap) => {
            const order = this.model.orderBetween(busbar, gap.leftOrder, gap.rightOrder, pending);
            if (order === undefined) return [];
            return [{ order, x: gap.x, y: geometry.y }];
        });
    }

    private feederColumns(slot: BaySlot, { claims, vacated }: PendingOrders): BayColumn[] {
        const columns: BayColumn[] = [];
        for (const node of this.model.feedersInSection(slot)) {
            if (vacated.has(node.id)) continue;
            const x = this.geometry.nodeCentre(node)?.x;
            if (x !== undefined) columns.push({ x, order: node.order });
        }
        for (const claim of claims) {
            if (claim.sectionIndex !== slot.sectionIndex) continue;
            const x = claim.x ?? this.reflowedX(claim);
            if (x !== undefined) columns.push({ x, order: claim.order });
        }
        return columns.sort((a, b) => a.x - b.x);
    }

    private reflowVoltageLevel(vlId: string): void {
        const cellWidth = this.model.cellWidth();
        const busbars = this.model.busbarNodes(vlId);
        const cells = busbars[0] ? this.dom.voltageLevelCells(busbars[0].id) : [];
        const pending = pendingOrders(this.pending(), vlId);

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
            const x = node && this.geometry.nodeCentre(node)?.x;
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
        return node && this.geometry.nodeCentre(node)?.x;
    }
}

export function nearestSlot(
    candidates: readonly BaySlotCandidate[],
    x: number | undefined,
): BaySlotCandidate | undefined {
    if (x === undefined) return undefined;
    return candidates.reduce<BaySlotCandidate | undefined>(
        (best, candidate) =>
            best && Math.abs(best.x - x) <= Math.abs(candidate.x - x) ? best : candidate,
        undefined,
    );
}

function isCreatedBay(claim: OrderClaim): boolean {
    return claim.vacatedNodeId === undefined && claim.x === undefined;
}

function createdBayKey(vlId: string, order: number): string {
    return `${vlId}:${order}`;
}
