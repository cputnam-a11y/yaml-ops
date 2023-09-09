package com.thiakil.yamlops.eoyaml.scalars;

import com.amihaiemil.eoyaml.*;
import com.amihaiemil.eoyaml.exceptions.YamlPrintException;
import com.amihaiemil.eoyaml.exceptions.YamlReadingException;

import java.io.IOException;
import java.io.StringWriter;

abstract class BaseYamlNode implements YamlNode {

    @Override
    public final Scalar asScalar()
            throws YamlReadingException, ClassCastException {
        return this.asClass(Scalar.class, Node.SCALAR);
    }

    @Override
    public final YamlMapping asMapping()
            throws YamlReadingException, ClassCastException {
        return this.asClass(YamlMapping.class, Node.MAPPING);
    }

    @Override
    public final YamlSequence asSequence()
            throws YamlReadingException, ClassCastException {
        return this.asClass(YamlSequence.class, Node.SEQUENCE);
    }

    @Override
    public final YamlStream asStream()
            throws YamlReadingException, ClassCastException {
        return this.asClass(YamlStream.class, Node.STREAM);
    }

    @Override
    public final <T extends YamlNode> T asClass(final Class<T> clazz,
                                                final Node type)
            throws YamlReadingException, ClassCastException {
        if (this.type() != type) {
            throw new YamlReadingException(
                    "The YamlNode is not a " + clazz.getSimpleName() + '!');
        }
        return clazz.cast(this);
    }

    /**
     * Print this YamlNode using a StringWriter to create its
     * String representation.
     * @return String print of this YamlNode.
     * @throws YamlPrintException If there is any I/O problem
     *  when printing the YAML.
     *
     */
    @Override
    public final String toString() {
        final StringWriter writer = new StringWriter();
        final YamlPrinter printer = Yaml.createYamlPrinter(writer);
        try {
            printer.print(this);
            return writer.toString();
        } catch (final IOException ex) {
            throw new YamlPrintException(
                    "IOException when printing YAML", ex
            );
        }
    }

}
