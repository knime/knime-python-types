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
 *   Aug 9, 2024 (adrian.nembach): created
 */
package org.knime.python3.types.port;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 * Implementation of a {@link PythonPortType} that is based on {@link Converter Converters}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class PythonPortTypeImpl implements PythonPortType {

    private final PortType m_portType;

    private final KnimeToPythonPortObjectConverter<?, ?> m_toPyObjConverter;

    private final KnimeToPythonPortObjectSpecConverter<?, ?> m_toPySpecConverter;

    private final PythonToKnimePortObjectConverter<?, ?> m_fromPyObjConverter;

    private final PythonToKnimePortObjectSpecConverter<?, ?> m_fromPySpecConverter;

    private static <T> T checkNotNull(final T obj, final String label) {
        return CheckUtils.checkArgumentNotNull(obj, "The %s must not be null.", label);
    }

    PythonPortTypeImpl(final PortType portType,
        final KnimeToPythonPortObjectConverter<?, ?> toPyObjConverter,//
        final KnimeToPythonPortObjectSpecConverter<?, ?> toPySpecConverter,//
        final PythonToKnimePortObjectConverter<?, ?> fromPyObjConverter,//
        final PythonToKnimePortObjectSpecConverter<?, ?> fromPySpecConverter) {
        m_portType = checkNotNull(portType, "KNIME Port Type");

        CheckUtils.checkArgument(areBothPresentOrMissing(toPyObjConverter, toPySpecConverter),
            "KNIME to Python conversion not fully implemented.");
        m_toPyObjConverter = toPyObjConverter;
        m_toPySpecConverter = toPySpecConverter;
        CheckUtils.checkArgument(areBothPresentOrMissing(fromPyObjConverter, fromPySpecConverter),
            "Python to KNIME conversion not fully implemented.");
        m_fromPyObjConverter = fromPyObjConverter;
        m_fromPySpecConverter = fromPySpecConverter;
    }

    private static boolean areBothPresentOrMissing(final Object objConverter, final Object specConverter) {
        if (objConverter == null) {
            return specConverter == null;
        } else {
            return specConverter != null;
        }
    }

    @Override
    public boolean canConvertToPython() {
        return m_toPyObjConverter != null;
    }

    @Override
    public PythonPortObject convertToPython(final PortObject obj, final PortObjectConversionContext context) {
        return uncheckedConvert(m_toPyObjConverter::convert, obj, context);
    }

    @Override
    public PythonPortObjectSpec convertToPython(final PortObjectSpec spec,
        final PortObjectSpecConversionContext context) {
        return uncheckedConvert(m_toPySpecConverter::convert, spec, null);
    }

    @Override
    public boolean canConvertFromPython() {
        return m_fromPyObjConverter != null;
    }

    @Override
    public PortObject convertFromPython(final PythonPortObject pyObj, final PortObjectConversionContext context) {
        return uncheckedConvert(m_fromPyObjConverter::convert, pyObj, context);
    }

    @Override
    public PortObjectSpec convertFromPython(final PythonPortObjectSpec pySpec,
        final PortObjectSpecConversionContext context) {
        return uncheckedConvert(m_fromPySpecConverter::convert, pySpec, null);
    }

    @SuppressWarnings("unchecked")
    private static <S, T, C> T uncheckedConvert(final Converter<S, T, C> converter, final Object source, final C ctx) {
        return converter.convert((S)source, ctx);
    }

    @Override
    public PortType getPortType() {
        return m_portType;
    }

}
