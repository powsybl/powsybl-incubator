import { describe, expect, it } from 'vitest';
import { bayGaps, busbarStretch, cellWidthFromColumns, reflowCells } from './bayLayout.ts';

const SPAN = { left: 0, right: 300, y: 0 };

describe('bayGaps', () => {
    it('offers the middle of an empty busbar', () => {
        expect(bayGaps(SPAN, [])).toEqual([{ x: 150 }]);
    });

    it('offers the common edge of two touching cells', () => {
        const gaps = bayGaps(SPAN, [
            { left: 100, right: 150, order: 10 },
            { left: 150, right: 200, order: 20 },
        ]);

        expect(gaps).toEqual([
            { x: 50, rightOrder: 10 },
            { x: 150, leftOrder: 10, rightOrder: 20 },
            { x: 250, leftOrder: 20 },
        ]);
    });

    it('offers no place over a coupling at the start of the busbar', () => {
        const gaps = bayGaps(SPAN, [
            { left: -10, right: 90 },
            { left: 90, right: 140, order: 20 },
        ]);

        expect(gaps[0]).toEqual({ x: 90, rightOrder: 20 });
    });

    it('offers the place right after the last bay when a coupling ends the busbar', () => {
        const gaps = bayGaps(SPAN, [
            { left: 50, right: 100, order: 20 },
            { left: 200, right: 300 },
        ]);

        expect(gaps.at(-1)).toEqual({ x: 150, leftOrder: 20 });
    });

    it('offers the place right before the next bay when a coupling stands between two bays', () => {
        const gaps = bayGaps(SPAN, [
            { left: 0, right: 50, order: 10 },
            { left: 100, right: 150 },
            { left: 200, right: 250, order: 20 },
        ]);

        expect(gaps).toEqual([
            { x: 0, rightOrder: 10 },
            { x: 175, leftOrder: 10, rightOrder: 20 },
            { x: 275, leftOrder: 20 },
        ]);
    });

    it('ignores the cells of another busbar', () => {
        expect(bayGaps(SPAN, [{ left: 400, right: 450, order: 10 }])).toEqual([{ x: 150 }]);
    });

    it('ignores a cell that only touches the busbar edge', () => {
        expect(bayGaps(SPAN, [{ left: -50, right: 0, order: 5 }])).toEqual([{ x: 150 }]);
    });

    it('offers the place after a lone coupling', () => {
        expect(bayGaps(SPAN, [{ left: 100, right: 200 }])).toEqual([{ x: 250 }]);
    });

    it('does not depend on the order of the given cells', () => {
        const cells = [
            { left: 150, right: 200, order: 20 },
            { left: 100, right: 150, order: 10 },
        ];
        expect(bayGaps(SPAN, cells)).toEqual(bayGaps(SPAN, [...cells].reverse()));
    });
});

describe('reflowCells', () => {
    const A = { id: 'A', left: 0, width: 10, order: 1 };
    const B = { id: 'B', left: 20, width: 10, order: 2 };

    it('moves nothing when the orders are unchanged', () => {
        expect(reflowCells([A, B])).toEqual(new Map([['A', 0], ['B', 20]]));
    });

    it('swaps two cells whose orders are swapped', () => {
        const lefts = reflowCells([{ ...A, order: 2 }, { ...B, order: 1 }]);
        expect(lefts).toEqual(new Map([['B', 0], ['A', 20]]));
    });

    it('keeps a coupling at its rank', () => {
        const coupling = { id: 'C', left: 20, width: 10 };
        const lefts = reflowCells([{ ...A, order: 2 }, coupling, { ...B, left: 40, order: 1 }]);
        expect(lefts).toEqual(new Map([['B', 0], ['C', 20], ['A', 40]]));
    });

    it('draws an added cell after the last bay of its section', () => {
        const lefts = reflowCells([{ ...A, section: 1 }, { ...B, section: 1 }], [{ id: 'N', width: 5, order: 3, section: 1 }]);
        expect(lefts).toEqual(new Map([['A', 0], ['B', 20], ['N', 30]]));
    });

    it('places an added cell by its order', () => {
        const lefts = reflowCells([{ ...A, section: 1 }, { ...B, section: 1 }], [{ id: 'N', width: 5, order: 0, section: 1 }]);
        expect(lefts.get('N')).toBe(0);
        expect(lefts.get('A')! < lefts.get('B')!).toBe(true);
    });

    it('closes the place of a removed cell', () => {
        expect(reflowCells([B], [], [A])).toEqual(new Map([['B', 0]]));
    });

    it('returns nothing for no cell', () => {
        expect(reflowCells([])).toEqual(new Map());
    });
});

describe('busbarStretch', () => {
    it('pushes later sections and lengthens its own, removed cells counting negatively', () => {
        const added = [
            { id: 'X', width: 5, order: 1, section: 0 },
            { id: 'Y', width: 7, order: 1, section: 1 },
            { id: 'Z', width: 9, order: 1, section: 2 },
        ];
        const removed = [
            { id: 'R', left: 0, width: 3, section: 0 },
            { id: 'U', left: 0, width: 4 },
        ];
        expect(busbarStretch(1, added, removed)).toEqual({ dx: 2, dWidth: 7 });
    });

    it('does not move without change', () => {
        expect(busbarStretch(1, [])).toEqual({ dx: 0, dWidth: 0 });
    });
});

describe('cellWidthFromColumns', () => {
    it('counts the columns between the outer ones', () => {
        expect(cellWidthFromColumns(0, 20, 10)).toBe(30);
        expect(cellWidthFromColumns(5, 5, 10)).toBe(10);
        expect(cellWidthFromColumns(0, 19, 10)).toBe(30);
    });
});
