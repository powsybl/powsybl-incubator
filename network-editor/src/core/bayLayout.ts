/**
 * Horizontal reflow of the cells of one voltage level, as powsybl-diagram lays them out.
 *
 * In powsybl-diagram (`BlockPositionner`), the vertical cells are sorted by order and placed
 * side by side: each cell takes `hSpan * cellWidth / 2` pixels and there is no extra gap.
 * Moving a bay only changes its order, so the cells keep their width and their drawing:
 * the reflow reorders them and slides each one to its new place.
 */
export interface LayoutCell {
    id: string;
    /** Left edge in diagram pixels, before any editor shift. */
    left: number;
    width: number;
    /** Order of an extern cell; `undefined` keeps the cell where it is (intern cells). */
    order?: number;
}

/**
 * Returns the new left edge of every cell.
 *
 * Cells with an order swap places by order; cells without one keep their rank.
 * The spaces between ranks are kept, so the total width, and so the busbars, do not change.
 */
export function reflowCells(cells: readonly LayoutCell[]): Map<string, number> {
    const ranks = [...cells].sort((a, b) => a.left - b.left);
    const ordered = ranks
        .filter((cell) => cell.order !== undefined)
        .sort((a, b) => a.order! - b.order!);

    const lefts = new Map<string, number>();
    let next = 0;
    let x = ranks[0]?.left ?? 0;
    for (let rank = 0; rank < ranks.length; rank++) {
        const cell = ranks[rank].order === undefined ? ranks[rank] : ordered[next++];
        lefts.set(cell.id, x);

        const following = ranks[rank + 1];
        const space = following ? following.left - (ranks[rank].left + ranks[rank].width) : 0;
        x += cell.width + space;
    }
    return lefts;
}

/** Width of a cell whose outer columns are centred at `minX` and `maxX`. */
export function cellWidthFromColumns(minX: number, maxX: number, cellWidth: number): number {
    const columns = Math.round((maxX - minX) / cellWidth) + 1;
    return columns * cellWidth;
}
