import type { EquipmentProperties, PropertyDescriptor } from '../src';

interface PropertyFormProps {
    schema: readonly PropertyDescriptor[];
    initial: EquipmentProperties;
    submitLabel: string;
    onSubmit: (values: EquipmentProperties) => void;
}

export function PropertyForm({ schema, initial, submitLabel, onSubmit }: PropertyFormProps) {
    return (
        <form
            onSubmit={(e) => {
                e.preventDefault();
                onSubmit({ ...initial, ...readValues(schema, new FormData(e.currentTarget)) });
            }}
        >
            {schema.map((descriptor) => (
                <label key={descriptor.key} className="field">
                    <span>
                        {descriptor.key}
                        {descriptor.unit && ` (${descriptor.unit})`}
                        {descriptor.required && ' *'}
                    </span>
                    {input(descriptor, initial[descriptor.key])}
                </label>
            ))}
            <button type="submit">{submitLabel}</button>
        </form>
    );
}

function input(descriptor: PropertyDescriptor, value: EquipmentProperties[string] | undefined) {
    if (descriptor.type === 'boolean') {
        return (
            <input
                type="checkbox"
                name={descriptor.key}
                defaultChecked={value === true}
                disabled={descriptor.readOnly}
            />
        );
    }
    if (descriptor.type === 'select') {
        return (
            <select
                name={descriptor.key}
                defaultValue={String(value ?? '')}
                required={descriptor.required}
                disabled={descriptor.readOnly}
            >
                {descriptor.options?.map((option) => (
                    <option key={option}>{option}</option>
                ))}
            </select>
        );
    }
    return (
        <input
            type={descriptor.type === 'number' ? 'number' : 'text'}
            name={descriptor.key}
            defaultValue={String(value ?? '')}
            required={descriptor.required}
            disabled={descriptor.readOnly}
            min={descriptor.min}
            max={descriptor.max}
            step="any"
        />
    );
}

/** Read-only fields are disabled, hence absent from the FormData: they come from `initial`. */
function readValues(
    schema: readonly PropertyDescriptor[],
    data: FormData,
): EquipmentProperties {
    const values: EquipmentProperties = {};

    for (const descriptor of schema) {
        if (descriptor.readOnly) continue;

        if (descriptor.type === 'boolean') {
            values[descriptor.key] = data.has(descriptor.key);
            continue;
        }

        const raw = data.get(descriptor.key);
        if (raw === null || raw === '') continue; // An emptied field stays empty, not 0.

        values[descriptor.key] = descriptor.type === 'number' ? Number(raw) : String(raw);
    }
    return values;
}
