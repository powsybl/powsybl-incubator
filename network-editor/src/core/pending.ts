import { isPendingCreate, type Command, type PendingCreateCommand } from './commands/Command';
import { RenameCommand } from './commands/RenameCommand';
import { UpdateBayPositionCommand } from './commands/UpdateBayPositionCommand';
import type { OrderClaim, PendingOrders } from './types';

export function findPendingCreate(
    pending: readonly Command[],
    equipmentId: string,
): PendingCreateCommand | undefined {
    return pending.find(
        (command): command is PendingCreateCommand =>
            isPendingCreate(command) && command.equipmentId === equipmentId,
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

export function findAmendableRename(
    pending: readonly Command[],
    equipmentId: string,
): RenameCommand | undefined {
    const renamed = pending.find(
        (command): command is RenameCommand =>
            command instanceof RenameCommand && command.newId === equipmentId,
    );
    if (!renamed) return undefined;

    const used = pending.some((command) => command.equipmentId === equipmentId);
    return used ? undefined : renamed;
}

export function isPendingId(
    pending: readonly Command[],
    equipmentId: string,
    ignore?: Command,
): boolean {
    return pending.some((command) => command !== ignore && command.equipmentId === equipmentId);
}

export function pendingScope(pending: readonly Command[]): { created: Set<string>; claimed: Set<string> } {
    const created = new Set<string>();
    const claimed = new Set<string>();

    for (const command of pending) {
        if (isPendingCreate(command)) created.add(command.equipmentId);
        if (command.pendingMarker) claimed.add(command.pendingMarker.targetId);
    }
    return { created, claimed };
}

export function pendingOrders(
    pending: readonly Command[],
    vlId: string,
    vacating?: string,
    ignore?: Command,
): PendingOrders {
    const claims: OrderClaim[] = [];
    const vacated = new Set<string>(vacating ? [vacating] : []);

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

            if (claim.vacatedNodeId !== vacating) lastClaims.set(claim.vacatedNodeId, claim);
        }
    }
    return { claims: [...claims, ...lastClaims.values()], vacated };
}
