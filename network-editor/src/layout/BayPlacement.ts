import type { EditorModel } from '../model/EditorModel.ts';
import { isPendingCreate, type Command } from '../commands/Command.ts';
import { CreateCouplingCommand } from '../commands/CreateCouplingCommand.ts';
import { CreateSwitchCommand } from '../commands/CreateSwitchCommand.ts';
import { pendingOrders } from '../commands/pending.ts';
import {
    bayGaps,
    busbarStretch,
    cellWidthFromColumns,
    forkSide,
    reflowCells,
    type AddedCell,
    type BayBlock,
    type LayoutCell,
} from './bayLayout.ts';
import {
    BUSBAR_SECTION_TYPE,
    type BaySlotCandidate,
    type BusbarTarget,
    type DiagramPoint,
    type NodeMetadata,
    type OrderClaim,
    type PendingOrders,
} from '../types.ts';
import type { SvgDomService } from '../dom/SvgDomService.ts';
import type { DiagramGeometry } from './DiagramGeometry.ts';

const COUPLING_ORDER = Number.MAX_SAFE_INTEGER / 2;

export class BayPlacement {
    private readonly createdBayXs = new Map<string, number>();
    private readonly forkXs = new Map<string, number>();
    private readonly couplingXs = new Map<string, number>();
    private readonly drawnCells = new WeakMap<Element, Omit<LayoutCell, 'id' | 'order'>>();

    constructor(
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly geometry: DiagramGeometry,
        private readonly pending: () => readonly Command[],
    ) {}

    reflow(): void {
        this.createdBayXs.clear();
        this.forkXs.clear();
        this.couplingXs.clear();
        for (const vlId of this.model.voltageLevelIds()) this.reflowVoltageLevel(vlId);
    }

    createdBayX(vlId: string, order: number): number | undefined {
        return this.createdBayXs.get(createdBayKey(vlId, order));
    }

    /** Column of the branch of a preview that forks off its node, if it forks at all. */
    forkX(elementId: string): number | undefined {
        return this.forkXs.get(elementId);
    }

    /** Centre of the two columns a pending coupling stands on. */
    couplingX(previewId: string): number | undefined {
        return this.couplingXs.get(previewId);
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

        const bays: AddedCell[] = pending.claims.filter(isCreatedBay).map((claim) => ({
            id: createdBayKey(vlId, claim.order),
            width: cellWidth,
            order: claim.order,
            section: claim.sectionIndex,
        }));
        const forks = this.forkColumns(vlId, cells, layout, cellWidth);
        const couplings = this.couplingCells(vlId, cellWidth);
        const added = [...bays, ...forks, ...couplings];

        const lefts = reflowCells(layout, added, removed);
        for (const { id, left } of layout) {
            this.dom.setCellShift(cells[Number(id)], lefts.get(id)! - left);
        }
        for (const { id, width } of bays) {
            this.createdBayXs.set(id, lefts.get(id)! + width / 2);
        }
        for (const { id, width } of forks) {
            this.forkXs.set(id, lefts.get(id)! + width / 2);
        }
        for (const { id, width } of couplings) {
            this.couplingXs.set(id, lefts.get(id)! + width / 2);
        }
        for (const busbar of busbars) {
            if (busbar.sectionIndex === undefined) continue;
            const { dx, dWidth } = busbarStretch(busbar.sectionIndex, added, removed);
            this.dom.setBusbarStretch(busbar.id, dx, dWidth);
        }
    }

    /** Two columns at the end of the section, one per busbar, for every pending coupling. */
    private couplingCells(vlId: string, cellWidth: number): AddedCell[] {
        return this.pending()
            .filter((command): command is CreateCouplingCommand => command instanceof CreateCouplingCommand)
            .filter((command) => command.first.vlId === vlId)
            .map((command, rank) => ({
                id: command.previewId,
                width: 2 * cellWidth,
                order: COUPLING_ORDER + rank,
                section: command.first.sectionIndex,
            }));
    }

    /**
     * One more column for every preview that forks off its node, as powsybl gives every feeder of a cell its own
     * column. A column is a cell added right before or after its own: the reflow makes room for it.
     */
    private forkColumns(
        vlId: string,
        cells: readonly Element[],
        layout: readonly LayoutCell[],
        cellWidth: number,
    ): AddedCell[] {
        const anchored = new Set<string>();
        const counts = new Map<string, number>();
        const columns: AddedCell[] = [];

        for (const command of this.pending()) {
            // A switch preview joins two nodes, a bay preview stands on its busbar: neither leaves a node.
            if (!isPendingCreate(command) || command instanceof CreateSwitchCommand) continue;

            const marker = command.pendingMarker;
            const node = marker && this.model.getNodeById(marker.nodeId);
            const centre = node && this.geometry.nodeCentre(node);
            if (!marker?.elementId || !node || !centre || node.vid !== vlId) continue;
            if (node.componentType === BUSBAR_SECTION_TYPE) continue;

            const alreadyAnchored = anchored.has(node.id);
            anchored.add(node.id);
            if (!alreadyAnchored && !this.hasBranch(node, centre, vlId)) continue;

            // Without a bay cell (coupling), there is no column to add.
            const element = this.dom.bayCell(node.id);
            const cell = element && layout.find(({ id }) => id === String(cells.indexOf(element)));
            if (!cell || cell.order === undefined || cell.section === undefined) continue;

            const side = forkSide(centre.x, cell);
            const key = `${cell.id}:${side}`;
            const rank = (counts.get(key) ?? 0) + 1;
            counts.set(key, rank);
            // Between the cell and its neighbour, a little further out for each fork on the same side.
            const offset = rank / (rank + 1);
            columns.push({
                id: marker.elementId,
                width: cellWidth,
                order: side === 'LEFT' ? cell.order - offset : cell.order + offset,
                section: cell.section,
            });
        }
        return columns;
    }

    /** A branch already leaves the node on the side the preview goes: the stub would lie over it. */
    private hasBranch(node: NodeMetadata, centre: DiagramPoint, vlId: string): boolean {
        const sign = this.geometry.feederSide(vlId, centre) === 'UP' ? -1 : 1;
        return this.model.neighbours(node.id).some((other) => {
            const y = this.geometry.nodeCentre(other)?.y;
            return y !== undefined && sign * (y - centre.y) > 0;
        });
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
