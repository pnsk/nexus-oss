<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2008-2015 Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
# Configure Repositories

    orient:connect plocal:../sonatype-work/nexus/db/config admin admin
    orient:insert 'into repository_configuration SET repository_name="rawhosted", recipe_name="raw-hosted", attributes={"rawContent":{"strictContentTypeValidation":true}}'
    orient:insert 'into repository_configuration SET repository_name="rawproxy", recipe_name="raw-proxy", attributes={"rawContent":{"strictContentTypeValidation":false},"proxy":{"remoteUrl":"http://repo1.maven.org/maven2/junit/junit","artifactMaxAge":120}}'
    system:shutdown --force --reboot

# Interact

## Hosted

    curl -v --user 'admin:admin123' -H 'Content-Type: text/plain' --upload-file ./README.md http://localhost:8081/repository/rawhosted/README.md
    curl -v --user 'admin:admin123' --upload-file ./README.md http://localhost:8081/repository/rawhosted/no-type-README.md
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawhosted/
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawhosted/index.html
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawhosted/README.md
    curl -v --user 'admin:admin123' -X DELETE http://localhost:8081/repository/rawhosted/README.md
    curl -v --user 'admin:admin123' -X DELETE http://localhost:8081/repository/rawhosted/no-type-README.md

## Proxy

    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawproxy/4.12/junit-4.12.pom
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawproxy/maven-metadata.xml

# unproxied equivalents
    curl -v -X GET http://repo1.maven.org/maven2/junit/junit/4.12/junit-4.12.pom
    curl -v -X GET http://repo1.maven.org/maven2/junit/junit/maven-metadata.xml