import type { FeederDirection } from './types';

/**
 * How a bay turns over its busbars. Distances are measured from the busbar block (the band between
 * the highest and the lowest busbar), and both sides list the same anchors in the same order:
 * `[0, switch?, feeder]`. The two sides do not share their levels, so an anchor of the source side
 * lands on the matching anchor of the target side, and anything in between is interpolated.
 */
export interface BayFlip {
    busTop: number;
    busBottom: number;
    from: FeederDirection;
    source: readonly number[];
    target: readonly number[];
}

/** Where a y of the bay lands once flipped. What sits on the busbar block does not move. */
export function flipY(y: number, flip: BayFlip): number {
    const { busTop, busBottom, from } = flip;

    if (from === 'TOP' && y < busTop) {
        return busBottom + toTarget(busTop - y, flip);
    }
    if (from === 'BOTTOM' && y > busBottom) {
        return busTop - toTarget(y - busBottom, flip);
    }
    return y;
}

/** The flip that puts the bay back where it was. */
export function invertFlip(flip: BayFlip): BayFlip {
    return {
        busTop: flip.busTop,
        busBottom: flip.busBottom,
        from: flip.from === 'TOP' ? 'BOTTOM' : 'TOP',
        source: flip.target,
        target: flip.source,
    };
}

function toTarget(distance: number, flip: BayFlip): number {
    const { source, target } = flip;

    for (let i = 0; i < source.length - 1; i++) {
        const start = source[i];
        const end = source[i + 1];
        if (distance > end || end === start) continue;

        const ratio = (distance - start) / (end - start);
        return target[i] + ratio * (target[i + 1] - target[i]);
    }

    // Past the last anchor: keep the offset of the last anchor.
    const last = source.length - 1;
    return distance + target[last] - source[last];
}
