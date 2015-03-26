# rpsl-parser
This library will provide parsing of _"Routing Policy Specification Language"_ to  abstract syntax trees (AST). Output to other formats may also be provided.

## aims
 + Import the minumum number of packages/classes required to compile RIPE's RPSL parser.
 + Implement unit tests for the parser
 + Provide a text based interface for testing
 + Implement an XML output backend
 + Compile/Package with Maven
 + Upload to Maven Central

## derived work
This module is based on code from RIPE's [whois](https://github.com/RIPE-NCC/whois). We keep a [mirror](https://gitlab.cecs.anu.edu.au/abn-comp3100/ripe-whois-client) of this on our gitlab.

whois is provided under the GPL license. As such make sure to add the following copyright notice to the beginning of any imported files:

    /*
     * Copyright (c) 2013 RIPE NCC
     * All rights reserved.
     */

When importing an entire package, run the following command from within the folder:

```bash
for f in `find . -name \*.java`; do                                                       ⏎
sed -i.bak '1i\
/*\
\ * Copyright (c) 2013 RIPE NCC\
\ * All rights reserved.\
\ */\
\
' $f; done
```

## mvn
the project includes a mvn pom.xm;. This defines it's dependencies and build plugin (findbugs, victims-enforcer).

### mvn package
Build jar file, will get dependecies, compile and test.

### mvn depency:resolve
Download required packages

### mvn findbugs:findbugs
Run [findbugs](https://github.com/h3xstream/find-sec-bugs/wiki/Maven-configuration) and generate report. Does not run by default but does require classes to have been compiled (compile phase)

## mvn findbugs:gui
open findbugs report in a gui viewer

### [victims-enforcer](https://github.com/victims/victims-enforcer)
Checks for exploitable library dependencies. Downloads a db of package vuln's and checks each build.

## license
Excluding code already licensed by RIPE under the BSD license, the source code is licensed under the GNU Affero General Public License.
