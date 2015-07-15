# parser-app
This module implements a commandline rpsl parser and transformer. You must have the rpsl4j library installed in your maven repository.

## mvn
the project includes a mvn pom.xml. This defines it's dependencies and build plugin (findbugs, victims-enforcer).
make sure to install the [Maven Intergration for Eclipse](http://www.eclipse.org/m2e/) plugin.

## javahome
the project is set to use java1.7. make sure your JAVAHOME environmental variable is set to a JDK >= 1.7.

on OSX with non-stock JDK's installed adding this to your .bashrc/.zshrc seems to do the trick:
```bash
export JAVA_HOME=`/usr/libexec/java_home`
```

### mvn generate-sources
parses the lexer and parser templates and generates the corresponding classes

### mvn compile
compiles the classes into the target directory

### mvn package
Build jar file, will get dependecies, compile and test.

### mvn depency:resolve
Download required packages

### mvn findbugs:findbugs
Run [findbugs](https://github.com/h3xstream/find-sec-bugs/wiki/Maven-configuration) and generate report. Does not run by default but does require classes to have been compiled (compile phase)

### mvn findbugs:gui
open findbugs report in a gui viewer

### [victims-enforcer](https://github.com/victims/victims-enforcer)
Checks for exploitable library dependencies. Downloads a db of package vuln's and checks each build.

## Deploying
  1. Make sure you have the signing key (0x036FA654) in your GPG keyring.
  2. Configure your user account details for OSSRH (`~/.m2/settings.xml`)
    ```
    <settings>
      <servers>
        <server>
          <id>ossrh</id>
          <username>your-jira-id</username>
          <password>your-jira-pwd</password>
        </server>
      </servers>
    </settings>
    ```
  3. Update the version number in `pom.xml`, commit and tag (`git tag v1.80`)
  4. Deploy to the staging repository (`mvn clean deploy`)
  5. Check the staging repository [OSSRH](https://oss.sonatype.org/)
    + To deploy to the central repository: `mvn nexus-staging:release`
    + To drop the staged release: `mvn nexus-staging:drop`

__note:__ versions ending in -SNAPSHOT will be uploaded the the snapshot repository

## license
Excluding code already licensed by RIPE under the BSD license, the source code is licensed under the GNU Affero General Public License.
