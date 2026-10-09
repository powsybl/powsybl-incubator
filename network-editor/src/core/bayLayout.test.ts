import { describe, expect, it } from 'vitest';
import { layoutBay, reflowCells, type LayoutCell } from './bayLayout';

/** MTPELP6 as powsybl draws it: MTPELL63TAMAR reaches no busbar and stands on a fictitious section 2. */
const MTPELP6: LayoutCell[] = [
    { id: 'TAMAR2', left: 75, width: 50, order: 10, section: 1 },
    { id: 'TAMAR1', left: 125, width: 50, order: 20, section: 1 },
    { id: 'PEYRO', left: 175, width: 50, order: 40, section: 1 },
    { id: 'ZP.TR', left: 225, width: 50, order: 60, section: 1 },
    { id: 'TR611', left: 275, width: 100, order: 70, section: 1 },
    { id: 'BALAR', left: 590, width: 50, order: 85, section: 1 },
    { id: 'TR632', left: 640, width: 50, order: 90, section: 1 },
    { id: 'TR633', left: 690, width: 50, order: 100, section: 1 },
    { id: 'TAMAR3', left: 740, width: 50, order: 30, section: 2 },
];

describe('reflowCells', () => {
    it('leaves a diagram with no edit where powsybl drew it, fictitious section included', () => {
        const lefts = reflowCells(MTPELP6);
        for (const cell of MTPELP6) expect(lefts.get(cell.id)).toBe(cell.left);
    });

    it('keeps the bay of the fictitious section out of a created bay', () => {
        const lefts = reflowCells(MTPELP6, [{ id: 'new', width: 50, order: 95, section: 1 }]);
        expect(lefts.get('TR632')).toBe(640);
        expect(lefts.get('new')).toBe(690);
        expect(lefts.get('TR633')).toBe(740);
        expect(lefts.get('TAMAR3')).toBe(790);
    });

    it('reorders the bays of a section among their own ranks', () => {
        const cells = MTPELP6.map((cell) => (cell.id === 'TAMAR2' ? { ...cell, order: 95 } : cell));
        const lefts = reflowCells(cells);
        expect(lefts.get('TAMAR1')).toBe(75);
        expect(lefts.get('TAMAR2')).toBe(690 - 50);
        expect(lefts.get('TR633')).toBe(690);
        expect(lefts.get('TAMAR3')).toBe(740);
    });
});

describe('layoutBay', () => {
    it('opens no place off the busbar', () => {
        const span = { left: 52.5, right: 727.5, y: 280 };
        const geometry = layoutBay(span, [
            { x: 665, order: 90 },
            { x: 715, order: 100 },
            { x: 765, order: 30 },
        ]);
        expect(geometry.gaps.map((gap) => gap.x)).toEqual([358.75, 690, 721.25]);
    });
});
