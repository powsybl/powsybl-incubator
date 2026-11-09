import type { EditorModel } from '../model/EditorModel.ts';
import type { Command } from '../commands/Command.ts';
import { pendingOrders } from '../commands/pending.ts';
import {
    bayGaps,
    busbarStretch,
    cellWidthFromColumns,
    reflowCells,
    type AddedCell,
    type BayBlock,
    type LayoutCell,
} from './bayLayout.ts';
import type { BaySlotCandidate, BusbarTarget, OrderClaim, PendingOrders } from '../types.ts';
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

    createdBayX(vlId: string, order: number): number | undefined {
        return this.createdBayXs.get(createdBayKey(vlId, order));
    }

    slotCandidates(
        busbar: BusbarTarget,
        pending: PendingOrders = pendingOrders(this.pending(), busbar.vlId),
    ): BaySlotCandidate[] {
        const span = this.dom.getDiagramSpan(busbar.id);
        if (!span) return [];

        return bayGaps(span, this.blocks(busbar, pending)).flatMap((gap) => {
            const order = this.model.orderBetween(busbar, gap.leftOrder, gap.rightOrder, pending);
            return order === undefined ? [] : [{ order, x: gap.x, y: span.y }];
        });
    }

    private blocks(busbar: BusbarTarget, { claims, vacated }: PendingOrders): BayBlock[] {
        const cellWidth = this.model.cellWidth();
        const movedOrders = movedOrdersOf(claims);
        const moving = (id: string): boolean => vacated.has(id) && !movedOrders.has(id);

        const blocks: BayBlock[] = [];
        for (const cell of this.dom.voltageLevelCells(busbar.id)) {
            if (this.dom.cellElementIds(cell).some(moving)) continue;
            const measured = this.measureCell(cell, movedOrders, cellWidth);
            if (!measured) continue;
            const left = measured.left + this.dom.getCellShift(cell);
            blocks.push({ left, right: left + measured.width, order: measured.order });
        }
        for (const claim of claims.filter(isCreatedBay)) {
            const x = this.createdBayX(claim.vlId, claim.order);
            if (x !== undefined) blocks.push({ left: x - cellWidth / 2, right: x + cellWidth / 2, order: claim.order });
        }
        return blocks;
    }

    private reflowVoltageLevel(vlId: string): void {
        const cellWidth = this.model.cellWidth();
        const busbars = this.model.busbarNodes(vlId);
        const cells = busbars[0] ? this.dom.voltageLevelCells(busbars[0].id) : [];
        const pending = pendingOrders(this.pending(), vlId);

        const movedOrders = movedOrdersOf(pending.claims);

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

/** The order each moved node takes. */
function movedOrdersOf(claims: readonly OrderClaim[]): Map<string, number> {
    const orders = new Map<string, number>();
    for (const claim of claims) {
        if (claim.vacatedNodeId) orders.set(claim.vacatedNodeId, claim.order);
    }
    return orders;
}

function isCreatedBay(claim: OrderClaim): boolean {
    return claim.vacatedNodeId === undefined && claim.x === undefined;
}

function createdBayKey(vlId: string, order: number): string {
    return `${vlId}:${order}`;
}
