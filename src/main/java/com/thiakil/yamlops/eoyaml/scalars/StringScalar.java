package com.thiakil.yamlops.eoyaml.scalars;

import com.mojang.serialization.DynamicOps;

public class StringScalar extends OpsScalar {
    private final String value;

    public StringScalar(String value) {
        this.value = value;
    }

    @Override
    public <T> T convertTo(DynamicOps<T> ops) {
        return ops.createString(value);
    }

    @Override
    public String value() {
        return value;
    }
}
