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
 *   2 August 2024 (Ivan Prigarin): created
 */
package org.knime.python3.types.port;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * A registry that manages conversion of {@link PortObject}s and {@link PortObjectSpec}s from KNIME to Python and back.
 * This is done by using {@link PortObjectConverters} and {@link PortObjectSpecConverters} respectively.
 *
 * The registry maintains two maps with converters for Port Objects and Port Object Specs, as well as a map linking
 * fully qualified Java class names of KNIME-native Port Objects and Port Object Specs to their actual {@link Class}es.
 * The latter is needed since we get the string of the class name from objects returned from Python, and we need the
 * corresponding class for inheritance resolution in the converter maps.
 *
 ** @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 ** @author Ivan Prigarin, KNIME GmbH, Konstanz, Germany
 */
public final class PythonPortTypeRegistry {

    //    private static final NodeLogger LOGGER = NodeLogger.getLogger(PythonPortObjectTypeRegistry.class);

    // Lazy-loaded singleton initialized on first access
    private static class InstanceHolder {
        private static final PythonPortTypeRegistry INSTANCE = new PythonPortTypeRegistry();
    }

    private final Map<String, Class<?>> m_classNameToClassMap = new HashMap<>();

    // TODO make keys type-safer
    private final Map<Class<?>, PythonPortType> m_objClassToPortType = new HashMap<>();

    // TODO make keys type-safer
    private final Map<Class<?>, PythonPortType> m_specClassToPyPortType = new HashMap<>();

    private PythonPortTypeRegistry() {
        PythonExtensionTypeExtensionPointParser.parseExtensionPoint().forEach(this::addPythonPortType);
    }

    private void addPythonPortType(final PythonPortType pyPortType) {
        var portType = pyPortType.getPortType();
        m_objClassToPortType.put(portType.getPortObjectClass(), pyPortType);
        m_specClassToPyPortType.put(portType.getPortObjectSpecClass(), pyPortType);
    }

    /**
     * Converts the provided {@link PortObjectSpec} implementor to the corresponding {@link PythonPortObjectSpec}
     * wrapper.
     *
     * @param spec KNIME-native {@link PortObjectSpec}
     * @return The Port Object Spec wrapped in {@link PythonPortObjectSpec}, which can be provided to the Python proxy
     */
    public static PythonPortObjectSpec convertPortObjectSpecToPython(final PortObjectSpec spec,
        final PortObjectSpecConversionContext context) {
        if (spec == null) {
            return null;
        }

        var registry = InstanceHolder.INSTANCE;
        var pyPortType = findPortTypeForClass(spec.getClass(), registry.m_specClassToPyPortType);
        checkCanConvertToPython(pyPortType);
        return pyPortType.convertToPython(spec, context);
    }

    private static void checkCanConvertToPython(final PythonPortType pyPortType) {
        if (!pyPortType.canConvertToPython()) {
            throwConversionDirectionException(pyPortType, "KNIME to Python");
        }
    }

    private static void checkCanConvertFromPython(final PythonPortType pyPortType) {
        if (!pyPortType.canConvertToPython()) {
            throwConversionDirectionException(pyPortType, "Python to KNIME");
        }
    }

    private static void throwConversionDirectionException(final PythonPortType pyPortType, final String direction) {
        throw new IllegalStateException("The registered Python type for '%s' does not support %s conversion."
            .formatted(pyPortType.getPortType(), direction));
    }

    /**
     * Converts the provided {@link PythonPortObjectSpec} received from Python to the corresponding KNIME-native
     * {@link PortObjectSpec}.
     *
     * @param pythonSpec The Port Object Spec to be converted back to its KNIME-native {@link PortObjectSpec}
     *            counterpart
     * @return The KNIME-native {@link PortObjectSpec} extracted from the JSON encoding of the
     *         {@link PythonPortObjectSpec}
     */
    public static PortObjectSpec convertPortObjectSpecFromPython(final PythonPortObjectSpec pythonSpec,
        final PortObjectSpecConversionContext context) {
        if (pythonSpec == null) {
            return null;
        }
        var registry = InstanceHolder.INSTANCE;
        var specClass = registry.getClassFromClassName(pythonSpec.getJavaClassName());
        var pyPortType = findPortTypeForClass(specClass, registry.m_specClassToPyPortType);
        checkCanConvertFromPython(pyPortType);
        return pyPortType.convertFromPython(pythonSpec, context);

    }

    /**
     * Converts the provided {@link PortObject} implementor to the corresponding {@link PythonPortObject} wrapper.
     *
     * @param portObject KNIME-native {@link PortObject}
     * @param context The conversion context providing objects needed during the conversion process
     * @return The Port Object wrapped in {@link PythonPortObject}, which can be provided to the Python proxy
     */
    public static PythonPortObject convertPortObjectToPython(final PortObject portObject,
        final PortObjectConversionContext context) {
        if (portObject == null) {
            throw new IllegalStateException("Cannot convert `null` portObject from KNIME to Python");
        }
        var registry = InstanceHolder.INSTANCE;
        var pyPortType = findPortTypeForClass(portObject.getClass(), registry.m_objClassToPortType);
        checkCanConvertToPython(pyPortType);
        return pyPortType.convertToPython(portObject, context);

    }

    /**
     * Converts the provided PurePythonPortObject-interfaced object received from Python to the corresponding
     * KNIME-native {@link PortObject}.
     *
     * @param purePythonPortObject The `PurePython` Port Object to be converted back to its KNIME-native
     *            {@link PortObject} counterpart
     * @param context The conversion context providing objects needed during the conversion process
     * @return The KNIME-native {@link PortObject} extracted from the `PurePython` object
     */
    public static PortObject convertPortObjectFromPython(final PythonPortObject purePythonPortObject,
        final PortObjectConversionContext context) {
        if (purePythonPortObject == null) {
            throw new IllegalStateException("Cannot convert 'null' portObject from Python to KNIME");
        }
        var registry = InstanceHolder.INSTANCE;
        var portObjectClass = registry.getClassFromClassName(purePythonPortObject.getJavaClassName());
        var pyPortType = findPortTypeForClass(portObjectClass, registry.m_objClassToPortType);
        checkCanConvertFromPython(pyPortType);
        return pyPortType.convertFromPython(purePythonPortObject, context);
    }

    /**
     * Searches for a registered converter for the given Port Object or Port Object Spec class by traversing its class
     * hierarchy until a match is found.
     *
     * If no exact match is found, interfaces the class implements are considered first, then the next superclass.
     *
     * @param <T> Converter type specified by the caller
     * @param targetClass The class object whose converter is to be found
     * @param classToConverterMap The map containing class-to-converter mappings
     * @return The converter instance if found; null otherwise
     */
    private static <T> T findPortTypeForClass(final Class<?> targetClass, final Map<Class<?>, T> classToConverterMap) {
        Class<?> currentClass = targetClass;

        while (currentClass != null) {
            T converter = classToConverterMap.get(currentClass);
            if (converter != null) {
                return converter;
            }

            // check for interfaces if no match found
            for (Class<?> interfaceClass : currentClass.getInterfaces()) {
                converter = classToConverterMap.get(interfaceClass);
                if (converter != null) {
                    return converter;
                }
            }

            // move to superclass
            // TODO: we can do an early stop if we reach some sensible traversal depth or by defining a "stop"/"base" Class
            // after which it no longer makes sense to keep the traversal going
            currentClass = currentClass.getSuperclass();
        }

        throw new IllegalStateException("No compatible Python PortType found for '%s'.".formatted(targetClass));
    }

    /**
     * Retrieves a {@link Class} object associated with the specified class name. If the class is not found in the cache
     * map, it attempts to load the class dynamically.
     *
     * We receive Java class names as strings from the Python side, and this method allows us to get the corresponding
     * Class object to then perform a class hieararchy-aware converter lookup.
     *
     * @param className The fully qualified name of the class to retrieve
     * @return The {@link Class} object corresponding to the given class name
     */
    private Class<?> getClassFromClassName(final String className) {
        Class<?> retrievedClass = m_classNameToClassMap.get(className);

        if (retrievedClass == null) {
            try {
                retrievedClass = Class.forName(className);
                m_classNameToClassMap.put(className, retrievedClass);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Could not find Java class for class name " + className);
            }
        }

        return retrievedClass;
    }
}
