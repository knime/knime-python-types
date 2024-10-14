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
package org.knime.python3.types.port;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.python3.types.port.convert.KnimeToPyPortObjectConverter;
import org.knime.python3.types.port.convert.PortObjectConversionContext;
import org.knime.python3.types.port.convert.PortObjectSpecConversionContext;
import org.knime.python3.types.port.transfer.PythonPortObjectSpecTransfer;
import org.knime.python3.types.port.transfer.PythonPortObjectTransfer;

/**
 * WRemoves the generics from a {@link KnimeToPyPortObjectConverter} for subsequent use in the framework where the specific types are unknown.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference this class is non-public API and only meant to be used by the Python node framework
 * @noinstantiate this class is non-public API and only meant to be used by the Python node framework
 */
public final class UntypedKnimeToPyPortObjectConverter implements UntypedPythonPortObjectConverter {

    private final GenericAbsorbingKnimeToPyPortObjectConverter<?, ?> m_genericsAbsorber;

    private final Class<? extends PortObject> m_poClass;

    private final Class<? extends PortObjectSpec> m_specClass;

    private final PythonImplementation m_pythonImplementation;

    /**
     * @param <O> the type of PortObject
     * @param <S> the type of PortObjectSpec
     * @param typedConverter
     */
    public <O extends PortObject, S extends PortObjectSpec> UntypedKnimeToPyPortObjectConverter(
        final KnimeToPyPortObjectConverter<O, S> typedConverter, final PythonImplementation pythonImplementation) {
        m_genericsAbsorber = new GenericAbsorbingKnimeToPyPortObjectConverter<>(typedConverter);
        m_poClass = typedConverter.getPortObjectClass();
        m_specClass = typedConverter.getPortObjectSpecClass();
        m_pythonImplementation = pythonImplementation;
    }

    public PythonPortObjectSpecTransfer convertSpecToPython(final PortObjectSpec spec,
        final PortObjectSpecConversionContext context) {
        return m_genericsAbsorber.convertSpecToPython(spec, context);
    }

    public PythonPortObjectTransfer convertPortObjectToPython(final PortObject portObject,
        final PortObjectConversionContext context) {
        return m_genericsAbsorber.convertPortObjectToPython(portObject, context);
    }

    @Override
    public Class<? extends PortObject> getPortObjectClass() {
        return m_poClass;
    }

    @Override
    public Class<? extends PortObjectSpec> getPortObjectSpecClass() {
        return m_specClass;
    }

    public PythonImplementation getPythonImplementation() {
        return m_pythonImplementation;
    }


    private static final class GenericAbsorbingKnimeToPyPortObjectConverter<O extends PortObject, S extends PortObjectSpec> {
        private final KnimeToPyPortObjectConverter<O, S> m_typedConverter;

        GenericAbsorbingKnimeToPyPortObjectConverter(final KnimeToPyPortObjectConverter<O, S> typedConverter) {
            m_typedConverter = typedConverter;
        }

        @SuppressWarnings("unchecked")
        PythonPortObjectTransfer convertPortObjectToPython(final PortObject portObject,
            final PortObjectConversionContext context) {
            assert m_typedConverter.getPortObjectClass().isInstance(portObject) : "The provided portObject is not compatible with the converter.";
            return m_typedConverter.convertPortObjectToPython((O)portObject, context);
        }

        @SuppressWarnings("unchecked")
        PythonPortObjectSpecTransfer convertSpecToPython(final PortObjectSpec spec,
            final PortObjectSpecConversionContext context) {
            assert m_typedConverter.getPortObjectSpecClass().isInstance(spec) : "The provided spec is not compatible with the converter.";
            return m_typedConverter.convertSpecToPython((S)spec, context);
        }
    }

}