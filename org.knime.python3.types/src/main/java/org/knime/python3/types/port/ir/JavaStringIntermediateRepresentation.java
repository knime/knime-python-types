package org.knime.python3.types.port.ir;

/**
 * Concrete implementation of StringIntermediateRepresentation for Java.
 */
public final class JavaStringIntermediateRepresentation implements StringIntermediateRepresentation {
    private final String m_representation;

    public JavaStringIntermediateRepresentation(final String representation) {
        m_representation = representation;
    }

    @Override
    public String getStringRepresentation() {
        return m_representation;
    }
}
