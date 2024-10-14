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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.python3.types.port.convert.KnimeToPyPortObjectConverter;
import org.knime.python3.types.port.convert.PyToKnimePortObjectConverter;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class PythonExtensionTypeExtensionPointParser {

    private static final String PY_TO_KNIME_CONVERTER_KEY = "PythonToKnimePortObjectConverter";

    private static final String KNIME_TO_PY_CONVERTER_KEY = "KnimeToPythonPortObjectConverter";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PythonExtensionTypeExtensionPointParser.class);

    private static final String EXTENSION_POINT = "org.knime.python3.types.PythonExtensionType";

    private static List<PythonPortConverterExtension> PYTHON_PORT_CONVERTERS;

    public static synchronized List<PythonPortConverterExtension> getPythonPortConverters() {
        if (PYTHON_PORT_CONVERTERS == null) {
            PYTHON_PORT_CONVERTERS = parseExtensionPoint().toList();
        }
        return PYTHON_PORT_CONVERTERS;
    }

    static Stream<PythonPortConverterExtension> parseExtensionPoint() {
        var registry = Platform.getExtensionRegistry();
        var extPoint = registry.getExtensionPoint(EXTENSION_POINT);
        IExtension[] extensions = extPoint.getExtensions();
        return Stream.of(extensions)//
            .flatMap(PythonExtensionTypeExtensionPointParser::extractPythonPortTypes);
    }

    private static Stream<PythonPortConverterExtension> extractPythonPortTypes(final IExtension extension) {
        IConfigurationElement[] configurationElements = extension.getConfigurationElements();
        return Stream.of(configurationElements)//
            .map(PythonExtensionTypeExtensionPointParser::parsePortConverterTuple)//
            .filter(Objects::nonNull);
    }

    private static PythonPortConverterExtension parsePortConverterTuple(final IConfigurationElement configElement) {
        // NOSONAR the lambda is shorter than the function ref
        var knimeToPy =
            parseConverter(configElement.getChildren(KNIME_TO_PY_CONVERTER_KEY), e -> parseKnimeToPyConverter(e));

        // NOSONAR the lambda is shorter than the function ref
        var pyToKnime =
            parseConverter(configElement.getChildren(PY_TO_KNIME_CONVERTER_KEY), e -> parsePyToKnimeConverter(e));

        if (knimeToPy.isEmpty() && pyToKnime.isEmpty()) {
            // TODO add extension info
            LOGGER.coding(() -> "The extension %s provides neither a %s, nor a %s.".formatted(
                configElement.getContributor().getName(), KNIME_TO_PY_CONVERTER_KEY, PY_TO_KNIME_CONVERTER_KEY));
            return null;
        }

        if (!typesMatch(knimeToPy, pyToKnime, UntypedPythonPortObjectConverter::getPortObjectClass)) {
            LOGGER.coding(() -> "The converters provided by the extension %s do not have matching PortObject classes."
                .formatted(configElement.getContributor().getName()));
            return null;
        }

        if (!typesMatch(knimeToPy, pyToKnime, UntypedPythonPortObjectConverter::getPortObjectSpecClass)) {
            LOGGER.coding(() -> "The converters provided by the extension %s do not have matching PortObjectSpec classes."
            .formatted(configElement.getContributor().getName()));
            return null;
        }

        // TODO null is ugly but Optional fields are ugly too :/
        return new PythonPortConverterExtension(knimeToPy.orElse(null), pyToKnime.orElse(null), configElement.getContributor().getName());

    }

    private static boolean typesMatch(final Optional<UntypedKnimeToPyPortObjectConverter> knimeToPy,
        final Optional<UntypedPyToKnimePortObjectConverter> pyToKnime,
        final Function<UntypedPythonPortObjectConverter, Class<?>> typeExtractor) {
        var knimeToPyType = knimeToPy.map(typeExtractor);
        var pyToKnimeType = pyToKnime.map(typeExtractor);

        return knimeToPyType.equals(pyToKnimeType) // both present and types match or both are empty
                || (knimeToPyType.isPresent() ^ pyToKnimeType.isPresent()); // only one of the values is present
    }

    private static <T> Optional<T> parseConverter(final IConfigurationElement[] children,
        final Function<IConfigurationElement, T> parser) {
        if (children.length == 0) {
            return Optional.empty();
        }
        // TODO complain if there is more than one?
        return Optional.of(parser.apply(children[0]));
    }

    private static UntypedKnimeToPyPortObjectConverter
        parseKnimeToPyConverter(final IConfigurationElement configElement) {
        try {
            var javaConverter =
                (KnimeToPyPortObjectConverter<?, ?>)configElement.createExecutableExtension("JavaConverterClass");
            return new UntypedKnimeToPyPortObjectConverter(javaConverter, getPythonImplementation(configElement));
        } catch (CoreException ex) {
            // TODO clean up error handling
            throw new IllegalStateException(ex);
        }
    }

    private static PythonImplementation getPythonImplementation(final IConfigurationElement configElement) {
        var modulePath = extractModulePath(configElement, configElement.getAttribute("PythonModule"));
        return new PythonImplementation(modulePath, configElement.getAttribute("PythonConverterClass"));
    }

    private static UntypedPyToKnimePortObjectConverter
        parsePyToKnimeConverter(final IConfigurationElement configElement) {
        try {
            var javaConverter =
                (PyToKnimePortObjectConverter<?, ?, ?>)configElement.createExecutableExtension("JavaConverterClass");
            return new UntypedPyToKnimePortObjectConverter(javaConverter, getPythonImplementation(configElement));
        } catch (CoreException ex) {
            // TODO clean up error handling
            throw new IllegalStateException(ex);
        }
    }

    // TODO consolidate with PythonValueFactoryRegistry
    private static Path extractModulePath(final IConfigurationElement element, final String resourcePath) {
        final String contributor = element.getContributor().getName();
        final var bundle = Platform.getBundle(contributor);
        try {
            final URL moduleUrl = FileLocator.find(bundle, new org.eclipse.core.runtime.Path(resourcePath), null);//NOSONAR

            if (moduleUrl == null) {
                LOGGER.coding("Could not find module path '" + resourcePath + "' in bundle '" + contributor + "'."
                    + " Please make sure that the extension is configured correctly and that the Python files are included "
                    + "in your build.properties");
                return null;
            }
            final URL moduleFileUrl = FileLocator.toFileURL(moduleUrl);//NOSONAR
            return FileUtil.resolveToPath(moduleFileUrl);
        } catch (IOException | URISyntaxException ex) {
            LOGGER.error(String.format("Can't resolve KnimeArrowExtensionType provided by %s.", contributor), ex);
            return null;
        }
    }

}
