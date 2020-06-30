# scala-cinder-client [![license](http://img.shields.io/:license-MIT-blue.svg)](LICENSE)
[![Scaladoc](http://javadoc-badge.appspot.com/pt.tecnico.dsi/scala-cinder-client_2.13.svg?label=scaladoc&style=plastic&maxAge=604800)](https://ist-dsi.github.io/scala-cinder-client/api/latest/pt/tecnico/dsi/openstack/cinder/index.html)
[![Latest version](https://index.scala-lang.org/ist-dsi/scala-cinder-client/scala-cinder-client/latest.svg)](https://index.scala-lang.org/ist-dsi/scala-cinder-client/scala-cinder-client)

[![Build Status](https://travis-ci.org/ist-dsi/scala-cinder-client.svg?branch=master&style=plastic&maxAge=604800)](https://travis-ci.org/ist-dsi/scala-cinder-client)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/)](https://www.codacy.com/app/IST-DSI/scala-cinder-client?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ist-dsi/scala-vault&amp;utm_campaign=Badge_Grade)
[![BCH compliance](https://bettercodehub.com/edge/badge/ist-dsi/scala-cinder-client)](https://bettercodehub.com/results/ist-dsi/scala-cinder-client)

The Scala client for Openstack Cinder.

Currently supported endpoints:
  
- [Volumes](https://docs.openstack.org/api-ref/block-storage/v3/index.html?expanded=#volumes-volumes)
- [Quota sets extension](https://docs.openstack.org/api-ref/block-storage/v3/index.html?expanded=#quota-sets-extension-os-quota-sets)

[Latest scaladoc documentation](https://ist-dsi.github.io/scala-cinder-client/api/latest/pt/tecnico/dsi/openstack/cinder/index.html)

## Install
Add the following dependency to your `build.sbt`:
```sbt
libraryDependencies += "pt.tecnico.dsi" %% "scala-cinder-client" % "0.0.0"
```
We use [semantic versioning](http://semver.org).

## License
scala-cinder-client is open source and available under the [MIT license](LICENSE).
