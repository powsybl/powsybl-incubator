import type { DiagramSpan } from './types';


export interface LayoutCell {
    id: string;
    left: number;
    width: number;
    order?: number;
    section?: number;
}

/** A bay waiting to be created */
export interface AddedCell {
    id: string;
    width: number;
    order: number;
    section: number;
}

interface Rank {
    cell: LayoutCell | AddedCell;
    space: number;
}

/**
 * Returns the new left edge of every cell, drawn and added.
 */
export function reflowCells(
    cells: readonly LayoutCell[],
    added: readonly AddedCell[] = [],
    removed: readonly LayoutCell[] = [],
): Map<string, number> {
    const originals = [...cells, ...removed].sort((a, b) => a.left - b.left);
    const ranks: Rank[] = originals.map((cell, index) => {
        const next = originals[index + 1];
        return { cell, space: next ? next.left - (cell.left + cell.width) : 0 };
    });
    for (const cell of removed) removeRank(ranks, cell);
    for (const cell of [...added].sort((a, b) => a.order - b.order)) insertRank(ranks, cell);

    const bySection = new Map<number | undefined, (LayoutCell | AddedCell)[]>();
    for (const cell of [...cells.filter((cell) => cell.order !== undefined), ...added]) {
        const section = bySection.get(cell.section) ?? [];
        section.push(cell);
        bySection.set(cell.section, section);
    }
    for (const section of bySection.values()) section.sort((a, b) => a.order! - b.order!);

    const lefts = new Map<string, number>();
    let x = originals[0]?.left ?? 0;
    for (const rank of ranks) {
        const cell =
            rank.cell.order === undefined ? rank.cell : (bySection.get(rank.cell.section)?.shift() ?? rank.cell);
        lefts.set(cell.id, x);
        x += cell.width + rank.space;
    }
    return lefts;
}

function removeRank(ranks: Rank[], cell: LayoutCell): void {
    const index = ranks.findIndex((rank) => rank.cell === cell);
    if (index > 0) ranks[index - 1].space += ranks[index].space;
    ranks.splice(index, 1);
}

/** A created bay goes right after the last bay of its section, then takes its place by order. */
function insertRank(ranks: Rank[], cell: AddedCell): void {
    const rank: Rank = { cell, space: 0 };

    const last = ranks.findLastIndex(
        (candidate) => candidate.cell.order !== undefined && (candidate.cell.section ?? 0) <= cell.section,
    );
    if (last !== -1) {
        rank.space = ranks[last].space;
        ranks[last].space = 0;
        ranks.splice(last + 1, 0, rank);
        return;
    }

    const first = ranks.findIndex((candidate) => candidate.cell.order !== undefined);
    ranks.splice(first === -1 ? ranks.length : first, 0, rank);
}

/**
 * Created bays push the busbars of later sections and lengthen the busbar of their own;
 * deleted bays do the opposite.
 */
export function busbarStretch(
    section: number,
    added: readonly AddedCell[],
    removed: readonly LayoutCell[] = [],
): { dx: number; dWidth: number } {
    let dx = 0;
    let dWidth = 0;
    const resize = (cellSection: number | undefined, width: number): void => {
        if (cellSection === undefined) return;
        if (cellSection < section) dx += width;
        if (cellSection === section) dWidth += width;
    };
    for (const cell of added) resize(cell.section, cell.width);
    for (const cell of removed) resize(cell.section, -cell.width);
    return { dx, dWidth };
}

export interface BayGeometry {
    y: number;
    columns: BayColumn[];
    gaps: { x: number; leftOrder?: number; rightOrder?: number }[];
}

export interface BayColumn {
    x: number;
    order?: number;
}

/** Places left between the bays of a busbar, from one end to the other. */
export function layoutBay(span: DiagramSpan, columns: BayColumn[]): BayGeometry {
    columns = columns.filter((column) => column.x >= span.left && column.x <= span.right);
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

/** Width of a cell whose outer columns are centred at `minX` and `maxX`. */
export function cellWidthFromColumns(minX: number, maxX: number, cellWidth: number): number {
    const columns = Math.round((maxX - minX) / cellWidth) + 1;
    return columns * cellWidth;
}
