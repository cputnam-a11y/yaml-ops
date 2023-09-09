package com.thiakil.yamlops.eoyaml.scalars;

import com.amihaiemil.eoyaml.Comment;
import com.amihaiemil.eoyaml.Node;
import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.YamlNode;
import com.mojang.serialization.DynamicOps;

public abstract class OpsScalar extends BaseYamlNode implements Scalar {
    private final Comment comment = new NullComment();

    @Override
    public final Node type() {
        return Node.SCALAR;
    }

    /**
     * Equality of two objects.
     * @param other Reference to the right hand Scalar
     * @return True if object are equal and False if are not.
     */
    @Override
    public boolean equals(final Object other) {
        final boolean result;
        if (!(other instanceof Scalar)) {
            result = false;
        } else if (this == other) {
            result = true;
        } else {
            result = this.compareTo((Scalar) other) == 0;
        }
        return result;
    }

    /**
     * Hash Code of this scalar.
     * @return Value of hashCode() of type int.
     */
    @Override
    public int hashCode() {
        return this.value().hashCode();
    }

    /**
     * Compare this Scalar to another node.<br><br>
     *
     * A Scalar is always considered less than a Sequence or a Mapping.<br>
     * If o is Scalar then their String values are compared lexicographically
     *
     * @param other The other AbstractNode.
     * @return
     *  a value < 0 if this < o <br>
     *   0 if this == o or <br>
     *  a value > 0 if this > o
     */
    @Override
    public int compareTo(final YamlNode other) {
        int result = -1;
        if (this == other) {
            result = 0;
        } else if (other == null) {
            result = 1;
        } else if (other instanceof Scalar) {
            final String value = this.value();
            final String otherVal = ((Scalar) other).value();
            if(value == null && otherVal == null) {
                result = 0;
            } else if(value != null && otherVal == null) {
                result = 1;
            } else if (value == null && otherVal != null) {
                result = -1;
            } else {
                result = this.value().compareTo(otherVal);
            }
        }
        return result;
    }

    @Override
    public final boolean isEmpty() {
        return this.value() == null || this.value().isEmpty();
    }

    public abstract <T> T convertTo(DynamicOps<T> ops);

    @Override
    public Comment comment() {
        return comment;
    }

    class NullComment implements Comment {

        @Override
        public YamlNode yamlNode() {
            return OpsScalar.this;
        }

        @Override
        public String value() {
            return "";
        }
    }
}
