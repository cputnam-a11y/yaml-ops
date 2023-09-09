package com.thiakil.yamlops.eoyaml.scalars;

import com.mojang.serialization.DynamicOps;

public class BooleanScalar extends OpsScalar {

    private final boolean value;
    private final String strValue;

    public BooleanScalar(boolean value) {
        this.value = value;
        this.strValue = String.valueOf(value);
    }

    @Override
    public <T> T convertTo(DynamicOps<T> ops) {
        return ops.createBoolean(value);
    }

    @Override
    public String value() {
        return strValue;
    }

    public boolean rawValue() {
        return value;
    }
}
