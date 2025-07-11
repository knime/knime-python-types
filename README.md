# KNIME® Python Type - Extension Points

This repository is maintained by the [KNIME Team Rakete](mailto:team-rakete@knime.com).

This repository contains the extension points which allow adding new data types and 
port objects to Python in a way that they can be communicated back and forth between
Python and Java.

See the repository [knime-python-types-example](https://bitbucket.org/KNIME/knime-python-types-example) for an example.

[![Jenkins](https://jenkins.knime.com/buildStatus/icon?job=knime-python-types%2Fmaster)](https://jenkins.knime.com/job/knime-python-types/job/master/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KNIME_knime-python-types&metric=alert_status&token=55129ac721eacd76417f57921368ed587ad8339d)](https://sonarcloud.io/summary/new_code?id=KNIME_knime-python-types)


## Content

This repository contains the source code for the KNIME Python type extension points.
The code is organized as follows:

* _org.knime.features.python3.types_: The feature is the installable unit in the KNIME AP
* _org.knime.python3.types_: The plugin containing the extension points for extension types and port objects
* _org.knime.update.python.types_: A small update site containing the feature and plugin

## Development Notes

You can find instructions on how to work with our code or develop extensions for KNIME Analytics Platform in the _knime-sdk-setup_ repository on [BitBucket](https://bitbucket.org/KNIME/knime-sdk-setup) or [GitHub](http://github.com/knime/knime-sdk-setup).

## Join the Community

* [KNIME Forum](https://forum.knime.com)