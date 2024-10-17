/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 7, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.types.port.framework;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.python3.types.port.api.convert.PortObjectConversionContext;
import org.knime.python3.types.port.api.convert.PortObjectSpecConversionContext;
import org.knime.python3.types.port.api.convert.PyToKnimePortObjectConverter;
import org.knime.python3.types.port.api.transfer.PythonPortObjectSpecTransfer;
import org.knime.python3.types.port.api.transfer.PythonPortObjectTransfer;

/**
 * Strips the generics from a {@link PyToKnimePortObjectConverter} for subsequent use in the framework where the
 * specific types are unknown.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference this class is non-public API and only meant to be used by the Python node framework
 * @noinstantiate this class is non-public API and only meant to be used by the Python node framework
 */
public final class UntypedPyToKnimePortObjectConverter implements UntypedPythonPortObjectConverter {

    private final GenericsAbsorbingConverter<?, ?, ?, ?> m_absorbingConverter;

    private final Class<? extends PortObject> m_poClass;

    private final Class<? extends PortObjectSpec> m_specClass;

    /**
     * @param <T> the type of transfer used for the port objects
     * @param <S> the type of spec the converter uses
     * @param <V> the type of transfer used for the specs
     * @param typedConverter the converter wrapped by the new untyped converter
     */
    public <O extends PortObject, T extends PythonPortObjectTransfer, S extends PortObjectSpec, V extends PythonPortObjectSpecTransfer> UntypedPyToKnimePortObjectConverter(
        final PyToKnimePortObjectConverter<O, T, S, V> typedConverter) {
        m_absorbingConverter = new GenericsAbsorbingConverter<>(typedConverter);
        m_poClass = typedConverter.getPortObjectClass();
        m_specClass = typedConverter.getPortObjectSpecClass();
    }

    @Override
    public Class<? extends PortObject> getPortObjectClass() {
        return m_poClass;
    }

    @Override
    public Class<? extends PortObjectSpec> getPortObjectSpecClass() {
        return m_specClass;
    }

    /**
     * @param transfer the transfer object that is converted into the spec
     * @param context in which the conversion happens
     * @return the {@link PortObjectSpec} created from the transfer object
     */
    public PortObjectSpec convertSpecFromPython(final PythonPortObjectSpecTransfer transfer,
        final PortObjectSpecConversionContext context) {
        return m_absorbingConverter.convertSpecFromPython(transfer, context);
    }

    /**
     * @param transfer the object that is converted into the port object
     * @param spec the spec used by the port object
     * @param context in which the conversion happens
     * @return the {@link PortObject} created from the transfer object and the spec
     */
    public PortObject convertPortObjectFromPython(final PythonPortObjectTransfer transfer, final PortObjectSpec spec,
        final PortObjectConversionContext context) {
        return m_absorbingConverter.convertPortObjectFromPython(transfer, spec, context);
    }

    private static final class GenericsAbsorbingConverter<O extends PortObject, T extends PythonPortObjectTransfer, S extends PortObjectSpec, V extends PythonPortObjectSpecTransfer> {

        private final PyToKnimePortObjectConverter<O, T, S, V> m_typedConverter;

        private GenericsAbsorbingConverter(final PyToKnimePortObjectConverter<O, T, S, V> typedConverter) {
            m_typedConverter = typedConverter;
        }

        @SuppressWarnings("unchecked")
        PortObject convertPortObjectFromPython(final PythonPortObjectTransfer source, final PortObjectSpec spec,
            final PortObjectConversionContext context) {
            return m_typedConverter.convertPortObjectFromPython((T)source, (S)spec, context);
        }

        @SuppressWarnings("unchecked")
        PortObjectSpec convertSpecFromPython(final PythonPortObjectSpecTransfer source,
            final PortObjectSpecConversionContext context) {
            return m_typedConverter.convertSpecFromPython((V)source, context);
        }
    }

}