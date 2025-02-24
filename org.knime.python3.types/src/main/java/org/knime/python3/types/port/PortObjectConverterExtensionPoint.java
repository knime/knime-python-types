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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.python3.types.port.converter.PortObjectDecoder;
import org.knime.python3.types.port.converter.PortObjectEncoder;
import org.knime.python3.types.port.converter.UntypedDelegatingPortObjectDecoder;
import org.knime.python3.types.port.converter.UntypedDelegatingPortObjectEncoder;

/**
 * Parses the {@code org.knime.python3.types.PythonPortObjectConverter} extension point.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference this class is non-public API and only meant to be used by the Python node framework
 * @noinstantiate this class is non-public API and only meant to be used by the Python node framework
 */
public final class PortObjectConverterExtensionPoint {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PortObjectConverterExtensionPoint.class);

    private static final String KNIME_TO_PY_CONVERTER_KEY = "KnimeToPythonPortObjectConverter";

    private static final String PY_TO_KNIME_CONVERTER_KEY = "PythonToKnimePortObjectConverter";

    private static final String EXTENSION_POINT = "org.knime.python3.types.PythonPortObjectConverter";

    private static final PortObjectConverterExtensionPoint INSTANCE = new PortObjectConverterExtensionPoint();

    private List<PythonPortObjectConverterExtension<UntypedDelegatingPortObjectEncoder>> m_knimeToPyPortConverters;

    private List<PythonPortObjectConverterExtension<UntypedDelegatingPortObjectDecoder>> m_pyToKnimePortConverters;

    private PortObjectConverterExtensionPoint() {
        var knimeToPyPortConverters =
            new ArrayList<PythonPortObjectConverterExtension<UntypedDelegatingPortObjectEncoder>>();
        var pyToKnimePortConverters =
            new ArrayList<PythonPortObjectConverterExtension<UntypedDelegatingPortObjectDecoder>>();

        var registry = Platform.getExtensionRegistry();
        var extPoint = registry.getExtensionPoint(EXTENSION_POINT);
        var moduleConfigElements = Arrays.stream(extPoint.getExtensions()) //
            .flatMap(e -> Arrays.stream(e.getConfigurationElements())) //
            .toArray(IConfigurationElement[]::new);

        // Loop over module configurations
        for (var moduleConfigElement : moduleConfigElements) {
            var contributorName = getContributor(moduleConfigElement);

            if (!"Module".equals(moduleConfigElement.getName())) {
                LOGGER.errorWithFormat("Invalid extension point configuration by '%s': Expected 'Module' but got '%s'.",
                    contributorName, moduleConfigElement.getName());
                continue;
            }

            var moduleName = moduleConfigElement.getAttribute("moduleName");
            var modulePath = extractModulePath(moduleConfigElement, moduleConfigElement.getAttribute("modulePath"));
            if (modulePath.isEmpty()) {
                // Note: We just skip this module and continue with the next one
                // extractModulePath already logs the error
                continue;
            }

            // Loop over converters
            for (var converterConfigElement : moduleConfigElement.getChildren()) {
                var pythonConverterClass = converterConfigElement.getAttribute("PythonConverterClass");
                var pythonImplementation = new PythonImplementation(modulePath.get(), moduleName, pythonConverterClass);

                if (KNIME_TO_PY_CONVERTER_KEY.equals(converterConfigElement.getName())) {
                    instantiateJavaConverter(converterConfigElement, PortObjectEncoder.class) // get the java converter
                        .map(UntypedDelegatingPortObjectEncoder::new) // strip types
                        .map(encoder -> new PythonPortObjectConverterExtension<>(encoder, pythonImplementation,
                            contributorName)) // create the extension
                        .ifPresent(knimeToPyPortConverters::add); // add it to the list
                } else if (PY_TO_KNIME_CONVERTER_KEY.equals(converterConfigElement.getName())) {
                    instantiateJavaConverter(converterConfigElement, PortObjectDecoder.class) // get the java converter
                        .map(UntypedDelegatingPortObjectDecoder::new) // strip types
                        .map(decoder -> new PythonPortObjectConverterExtension<>(decoder, pythonImplementation,
                            contributorName)) // create the extension
                        .ifPresent(pyToKnimePortConverters::add); // add it to the list
                } else {
                    LOGGER.warn("Unknown converter type '%s' in module '%s'."
                        .formatted(converterConfigElement.getName(), moduleName));
                }
            }
        }

        m_knimeToPyPortConverters = Collections.unmodifiableList(knimeToPyPortConverters);
        m_pyToKnimePortConverters = Collections.unmodifiableList(pyToKnimePortConverters);
    }

    /**
     * @return the unmodifiable list of registered extensions for converting from KNIME to Python
     */
    public static List<PythonPortObjectConverterExtension<UntypedDelegatingPortObjectEncoder>>
        getKnimeToPyConverters() {
        return INSTANCE.m_knimeToPyPortConverters;
    }

    /**
     * @return the unmodifiable list of registered extensions for converting from Python to KNIME
     */
    public static synchronized List<PythonPortObjectConverterExtension<UntypedDelegatingPortObjectDecoder>>
        getPyToKnimeConverters() {
        return INSTANCE.m_pyToKnimePortConverters;
    }

    private static <T> Optional<T> instantiateJavaConverter(final IConfigurationElement configElement,
        final Class<T> clazz) {
        try {
            return Optional.of(clazz.cast(configElement.createExecutableExtension("JavaConverterClass")));
        } catch (Exception ex) { // NOSONAR: We catch everything because we do not want to fail initialization
            LOGGER.error("Failed to instantiate %s provided by %s.".formatted(clazz.getSimpleName(),
                getContributor(configElement)), ex);
            return Optional.empty();
        }
    }

    private static String getContributor(final IConfigurationElement element) {
        return element.getContributor().getName();
    }

    private static Optional<Path> extractModulePath(final IConfigurationElement element, final String resourcePath) {
        final String contributor = element.getContributor().getName();
        final var bundle = Platform.getBundle(contributor);
        try {
            final URL moduleUrl = FileLocator.find(bundle, new org.eclipse.core.runtime.Path(resourcePath), null);//NOSONAR

            if (moduleUrl == null) {
                LOGGER.coding("Could not find module path '%s' in bundle '%s'.".formatted(resourcePath, contributor)
                    + " Make sure that the extension is configured correctly and that the Python files are included "
                    + "in your build.properties");
                return Optional.empty();
            }
            final URL moduleFileUrl = FileLocator.toFileURL(moduleUrl);//NOSONAR
            return Optional.of(FileUtil.resolveToPath(moduleFileUrl));
        } catch (IOException | URISyntaxException ex) {
            LOGGER.error("Can't resolve module path of converter provided by %s.".formatted(contributor), ex);
            return Optional.empty();
        }
    }

}
