import { isPendingCreate, type Command, type PendingCreateCommand } from './Command.ts';
import { RenameCommand } from './RenameCommand.ts';
import { UpdateBayPositionCommand } from './UpdateBayPositionCommand.ts';
import type { OrderClaim, PendingOrders } from '../types.ts';

export function findPendingCreate(
    pending: readonly Command[],
    nodeId: string,
): PendingCreateCommand | undefined {
    return pending.find(
        (command): command is PendingCreateCommand =>
            isPendingCreate(command) && command.pendingMarker?.elementId === nodeId,
    );
}

export function pendingBayPosition(
    pending: readonly Command[],
    equipmentId: string,
): UpdateBayPositionCommand | undefined {
    return pending.findLast(
        (command): command is UpdateBayPositionCommand =>
            command instanceof UpdateBayPositionCommand && command.equipmentId === equipmentId,
    );
}

export function originalId(pending: readonly Command[], currentId: string): string {
    let id = currentId;
    for (let i = pending.length - 1; i >= 0; i--) {
        const command = pending[i];
        if (command instanceof RenameCommand && command.newId === id) id = command.equipmentId;
    }
    return id;
}

export function claimedTargets(pending: readonly Command[]): Set<string> {
    const claimed = new Set<string>();
    for (const command of pending) {
        if (command.pendingMarker) claimed.add(command.pendingMarker.targetId);
    }
    return claimed;
}

export function pendingOrders(
    pending: readonly Command[],
    vlId: string,
    vacating: readonly string[] = [],
    ignore?: Command,
): PendingOrders {
    const claims: OrderClaim[] = [];
    const vacated = new Set<string>(vacating);

    const lastClaims = new Map<string, OrderClaim>();

    for (const command of pending) {
        if (command === ignore) continue;
        for (const claim of command.orderClaims ?? []) {
            if (claim.vlId !== vlId) continue;
            if (!claim.vacatedNodeId) {
                claims.push(claim);
                continue;
            }
            vacated.add(claim.vacatedNodeId);

            if (!vacating.includes(claim.vacatedNodeId)) lastClaims.set(claim.vacatedNodeId, claim);
        }
    }
    return { claims: [...claims, ...lastClaims.values()], vacated };
}
