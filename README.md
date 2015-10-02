# rpsl4j-generator #
A framework for configuring routing services and devices using the Routing Policy Specification Language. The framework generates an in memory model of an RPSL document using the `rpsl4j-parser` library, provides a modular interface for developing application specific configuration emitters (see rpsl4j-opendaylight) and a small command-line front end.

## How to use rpsl4j ##
The following information on using and deploying the framework can be found in the docs directory:
 + [RPSL Language Support and Internal Model](docs/rpsl-support.md)
 + [Writing a new emitter for rpsl4j](docs/implementing-new-emitter.md)
 + [Deploying rpsl4j-generator to Maven Central](docs/deploying.md)

## Requirements ##
 + Java Development Kit 7+
 + Maven

## Building and Running ##
```
$ mvn package
$ mvn dependency:copy-dependencies
$ java -cp dependencies/*:rpsl4j-generator.jar org.rpsl4j.App --help
Usage: rpsl4j-app [options]
  Options:
    -e, --emitter
       Emitter to use to format output

    -h, --help
       Dispaly usage information

    -i, --input
       Input path (omit for stdin)

    --list-emitters
       List emitters available to format output with

    --list-arguments
       List valid arguments for provided emitter

    -o, --output
       Output path (omit for stdout)

    -m
       Emitter parameters (optional depending on emitter)
       Syntax: -m key=value
$ java -cp dependencies/*:rpsl4j-generator.jar org.rpsl4j.App --list-emitters
Available emitters: org.rpsl4j.emitters.NullEmitter, org.rpsl4j.emitters.XMLEmitter
```

## License ##
This project is licensed under the GNU Affero General Public License.
