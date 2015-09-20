# Deploying rpsl4j-generator to Maven Central
In order to deploy rpsl4j artifacts to Maven Central the project has been registered on [Sonatype OSS](https://oss.sonatype.org/). In order to deploy a version to the central repository, it must first be packaged, signed and staged to the Sonatype repository.

## Configuring your machine to deploy rpsl4j-generator
All official rpsl4j packages are signed using GPG with the [rpsl4j signing key](https://pgp.mit.edu/pks/lookup?op=vindex&search=0xDBD365D9036FA654). In order to deploy rpsl4j packages you must first ensure you have the private key in your local GPG keyring. It is also encouraged that you start `gpg-agent` before deploying as the signing process prompts the user for the passphrase multiple times.

The credentials of a Sonatype OSS account authorized to deploy rpsl4j packages must also be configured on the users machine. This can be done by appending the following to your local `~/.m2/settings.xml`
```xml
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
Please refer to the Maven page on [password encryption](https://maven.apache.org/guides/mini/guide-encryption.html) for advice on securing your OSS password.

_Existing maintainers can request authorizations on behalf of others by appending the relevant [Jira ticket](https://issues.sonatype.org/browse/OSSRH-15835) on Sonatype_

## Deploying and promoting a release
Now that your machine has been configured, deploying rpsl4j packages is relatively straight forward:

  1. Promote the version in `pom.xml` from a `-SNAPSHOT` to a full release (ie 0.1), commit and `git --tag` the commit with the release version.
  2. Compile and deploy the package to the OSS staging repository with `mvn clean deploy`. This will sign the packages using the rpsl4j signing key; prompting you for any required passphrase.
  3. Check the newly created staging repository at [OSSRH](https://oss.sonatype.org/), then
    + If the packages are complete and ready for deployment promote them to the Central Repository using the OSS web interface or by calling `mvn nexus-staging:release`
    + Otherwise, drop the staged release with `mvn nexus-staging:drop`

_package `-SNAPSHOT` versions can be deployed to OSS however cannot be promoted to the central repository_
