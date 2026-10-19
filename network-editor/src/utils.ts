
export function pushTo<K, V>(map: Map<K, V[]>, key: K, value: V): void {
    const existing = map.get(key);
    if (existing) {
        existing.push(value);
    } else {
        map.set(key, [value]);
    }
}

export function removeFrom<K, V>(map: Map<K, V[]>, key: K, value: V): void {
    const existing = map.get(key);
    if (!existing) return;

    const index = existing.indexOf(value);
    if (index !== -1) existing.splice(index, 1);
    if (existing.length === 0) map.delete(key);
}

export function removeById<T extends { id: string }>(
    array: T[],
    id: string,
): T | undefined {
    const index = array.findIndex((entry) => entry.id === id);
    if (index === -1) return undefined;
    return array.splice(index, 1)[0];
}
