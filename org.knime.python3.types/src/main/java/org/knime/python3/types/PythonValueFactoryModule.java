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
 *   Aug 27, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.types;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * Module holding one or more {@link PythonValueFactory PythonValueFactories}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public final class PythonValueFactoryModule implements Iterable<PythonValueFactory>, PythonModule {

    private final Path m_modulePath;

    private final String m_moduleName;

    private final List<PythonValueFactory> m_factories;

    private final List<FromPandasColumnConverter> m_fromPandasColumnConverters;

    private final List<ToPandasColumnConverter> m_toPandasColumnConverters;

    PythonValueFactoryModule(final Path modulePath, final String moduleName, final PythonValueFactory[] factories,
        final FromPandasColumnConverter[] fromPandasColumnConverters,
        final ToPandasColumnConverter[] toPandasColumnConverters) {
        m_modulePath = modulePath;
        m_moduleName = moduleName;
        m_factories = List.of(factories);
        m_fromPandasColumnConverters = List.of(fromPandasColumnConverters);
        m_toPandasColumnConverters = List.of(toPandasColumnConverters);
    }

    @Override
    public Path getParentDirectory() {
        return m_modulePath;
    }

    @Override
    public String getModuleName() {
        return m_moduleName;
    }

    @Override
    public Iterator<PythonValueFactory> iterator() {
        return m_factories.iterator();
    }

    /**
     * @return A list of column converters that should be applied before a pandas DataFrame is passed to KNIME
     */
    public List<FromPandasColumnConverter> getFromPandasColumnConverters() {
        return m_fromPandasColumnConverters;
    }

    /**
     * @return A list of column converters that should be applied before a table coming from KNIME is handed to the user
     *         as pandas DataFrame
     */
    public List<ToPandasColumnConverter> getToPandasColumnConverters() {
        return m_toPandasColumnConverters;
    }
}
