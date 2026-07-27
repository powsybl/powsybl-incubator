import { useEffect, useMemo, useState, type CSSProperties } from 'react';
import type { EquipmentProperties } from '../src';
import type { PropertyDescriptor } from './properties/properties.ts';
import {
    buildForm,
    computeChanges,
    fieldError,
    formatFieldError,
    type FieldError,
    type FormState,
} from './properties/propertyForm.ts';

interface PropertyPanelProps {
    equipmentId: string | null;
    schema: PropertyDescriptor[];
    values: EquipmentProperties;
    onApply: (changes: EquipmentProperties) => void;
    translate?: (key: string) => string;
}

export function PropertyPanel({
    equipmentId,
    schema,
    values,
    onApply,
    translate = (key) => key,
}: PropertyPanelProps) {
    const baseline = useMemo(() => buildForm(schema, values), [schema, values]);
    const [form, setForm] = useState<FormState>(baseline);

    useEffect(() => setForm(baseline), [baseline]);

    if (equipmentId === null) {
        return (
            <p style={{ color: '#888' }}>
                Select an equipment to edit its properties.
            </p>
        );
    }

    const errors = schema.map((descriptor) => fieldError(descriptor, form[descriptor.key]));
    const hasError = errors.some((error) => error !== null);
    const changes = computeChanges(schema, form, baseline);
    const hasChanges = Object.keys(changes).length > 0;

    const setField = (key: string, value: string | boolean) =>
        setForm((prev) => ({ ...prev, [key]: value }));

    return (
        <>
            <p style={{ color: '#666', fontSize: 12 }}>{equipmentId}</p>
            {schema.length === 0 && (
                <p style={{ color: '#888' }}>No editable property for this type.</p>
            )}
            <form
                onSubmit={(event) => {
                    event.preventDefault();
                    if (!hasError && hasChanges) onApply(changes);
                }}
            >
                {schema.map((descriptor, index) => {
                    const error = errors[index];
                    return (
                        <label key={descriptor.key} style={fieldStyle}>
                            <span>
                                {translate(descriptor.key)}
                                {descriptor.unit ? ` (${descriptor.unit})` : ''}
                            </span>
                            {renderInput(descriptor, form[descriptor.key], setField, error)}
                            {error && (
                                <span style={{ color: '#dc3545', fontSize: 11 }}>
                                    {formatFieldError(error)}
                                </span>
                            )}
                        </label>
                    );
                })}
                <button
                    type="submit"
                    disabled={hasError || !hasChanges}
                    style={{
                        marginTop: 8,
                        padding: '8px 16px',
                        cursor: hasError || !hasChanges ? 'not-allowed' : 'pointer',
                    }}
                >
                    Apply
                </button>
            </form>
        </>
    );
}

function renderInput(
    descriptor: PropertyDescriptor,
    value: string | boolean,
    setField: (key: string, value: string | boolean) => void,
    error: FieldError | null,
) {
    if (descriptor.type === 'boolean') {
        return (
            <input
                type="checkbox"
                checked={Boolean(value)}
                onChange={(e) => setField(descriptor.key, e.target.checked)}
            />
        );
    }
    if (descriptor.type === 'select') {
        return (
            <select
                value={String(value ?? '')}
                onChange={(e) => setField(descriptor.key, e.target.value)}
                style={inputStyle(error)}
            >
                {(descriptor.options ?? []).map((option) => (
                    <option key={option} value={option}>
                        {option}
                    </option>
                ))}
            </select>
        );
    }
    return (
        <input
            type={descriptor.type === 'number' ? 'number' : 'text'}
            value={String(value ?? '')}
            min={descriptor.min}
            max={descriptor.max}
            onChange={(e) => setField(descriptor.key, e.target.value)}
            style={inputStyle(error)}
        />
    );
}

const inputStyle = (error: FieldError | null): CSSProperties => ({
    border: `1px solid ${error ? '#dc3545' : '#ccc'}`,
    borderRadius: 4,
    padding: '4px 6px',
});

const fieldStyle: CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
    marginBottom: 10,
};
