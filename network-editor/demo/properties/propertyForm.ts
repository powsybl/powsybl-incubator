import type { EquipmentProperties } from '../../src';
import type { PropertyDescriptor } from './properties.ts';

/** Form state: numbers/strings/selects held as raw text, booleans as booleans. */
export type FormState = Record<string, string | boolean>;

/** Builds the form baseline from stored values, falling back on defaults. */
export function buildForm(
    schema: PropertyDescriptor[],
    values: EquipmentProperties,
): FormState {
    const form: FormState = {};
    for (const descriptor of schema) {
        const raw = values[descriptor.key] ?? descriptor.defaultValue;
        form[descriptor.key] =
            descriptor.type === 'boolean'
                ? Boolean(raw)
                : raw === undefined
                  ? ''
                  : String(raw);
    }
    return form;
}

/** Parses a field to its typed value, or `undefined` when invalid/empty. */
export function parseField(
    descriptor: PropertyDescriptor,
    raw: string | boolean,
): number | string | boolean | undefined {
    if (descriptor.type === 'boolean') return Boolean(raw);
    if (descriptor.type === 'string' || descriptor.type === 'select') {
        return raw === '' ? undefined : String(raw);
    }
    if (raw === '') return undefined;
    const parsed = Number(raw);
    return Number.isNaN(parsed) ? undefined : parsed;
}

/**
 * Validation failure, as data rather than as a sentence: the host owns the
 * wording. `min`/`max` carry the offending bound so a translated message can
 * interpolate it (`intl.formatMessage({ id: 'error.' + code }, error)`).
 * {@link formatFieldError} renders the English fallback.
 */
export type FieldError =
    | { code: 'required' }
    | { code: 'notANumber' }
    | { code: 'min'; min: number }
    | { code: 'max'; max: number }
    | { code: 'unknownProperty' };

/** Validation error for a raw form field, or null when valid. */
export function fieldError(
    descriptor: PropertyDescriptor,
    raw: string | boolean,
): FieldError | null {
    if (descriptor.type === 'boolean') return null;
    if (raw === '' || raw === undefined)
        return descriptor.required ? { code: 'required' } : null;
    if (descriptor.type !== 'number') return null;
    return validateValue(descriptor, Number(raw));
}

/** English rendering of a {@link FieldError}, for hosts without i18n. */
export function formatFieldError(error: FieldError): string {
    switch (error.code) {
        case 'required':
            return 'value required';
        case 'notANumber':
            return 'invalid number';
        case 'min':
            return `must be ≥ ${error.min}`;
        case 'max':
            return `must be ≤ ${error.max}`;
        case 'unknownProperty':
            return 'property is not editable';
    }
}

/** Diff between the edited form and its baseline: only changed, valid keys. */
export function computeChanges(
    schema: PropertyDescriptor[],
    form: FormState,
    baseline: FormState,
): EquipmentProperties {
    const changes: EquipmentProperties = {};
    for (const descriptor of schema) {
        if (form[descriptor.key] === baseline[descriptor.key]) continue;
        const parsed = parseField(descriptor, form[descriptor.key]);
        if (parsed === undefined) continue; // invalid — kept out of the change set
        changes[descriptor.key] = parsed;
    }
    return changes;
}

/**
 * Every valid, typed value of the form. A creation carries the whole form,
 * where an edit only carries its diff: there is no stored value to fall back on.
 */
export function computeValues(
    schema: PropertyDescriptor[],
    form: FormState,
): EquipmentProperties {
    const values: EquipmentProperties = {};
    for (const descriptor of schema) {
        const parsed = parseField(descriptor, form[descriptor.key]);
        if (parsed !== undefined) values[descriptor.key] = parsed;
    }
    return values;
}

/**
 * Validation error for an already-typed value, or null when valid. Unlike
 * {@link fieldError} (which works on raw form input), this checks the typed
 * values that reach the core through `applyProperties`.
 */
export function validateValue(
    descriptor: PropertyDescriptor,
    value: number | string | boolean | undefined,
): FieldError | null {
    if (value === undefined) return descriptor.required ? { code: 'required' } : null;
    if (descriptor.type === 'number') {
        if (typeof value !== 'number' || Number.isNaN(value)) return { code: 'notANumber' };
        if (descriptor.min !== undefined && value < descriptor.min)
            return { code: 'min', min: descriptor.min };
        if (descriptor.max !== undefined && value > descriptor.max)
            return { code: 'max', max: descriptor.max };
    }
    return null;
}

/**
 * Validates a typed change set against its schema. Returns one error per
 * offending key (empty when all valid). Only keys present in `changes` are
 * checked — the core applies diffs, so a required key absent from the diff is
 * left to its already-stored value. Keys the schema does not declare are
 * rejected: they would end up in the change set and be refused by the backend
 * update methods.
 */
export function validateChanges(
    schema: PropertyDescriptor[],
    changes: EquipmentProperties,
): Array<FieldError & { key: string }> {
    const errors: Array<FieldError & { key: string }> = [];
    const known = new Map(schema.map((descriptor) => [descriptor.key, descriptor]));
    for (const key of Object.keys(changes)) {
        const descriptor = known.get(key);
        if (!descriptor) {
            errors.push({ key, code: 'unknownProperty' });
            continue;
        }
        const error = validateValue(descriptor, changes[key]);
        if (error) errors.push({ key, ...error });
    }
    return errors;
}
