# Writing a new emitter for rpsl4j
The main role of the rpsl4j-generator package is to build an in-memory representation of an RPSL document that can be consumed and transformed by an "Output Emitter" in order to configure an external piece of software.
While the package provides two dummy emitters (XMLEmitter, NullEmitter) and the project provides emitters for OpenDaylight-BGPCEP (rpsl4j-opendaylight), it is possible for users to implement their own emitters for other platforms and load them at runtime.

## Implementing a basic emitter
All output emitters must implement the `org.rpsl4j.emitters.OutputEmitter` interface.
This consists of three methods:

### `String emit(Set<RpslObject> objects)`
This is the main method of any emitter.
The emitter is provided with a set of RpslObjects from either `org.rpsl4j.App` or another calling program which it should process in some fashion.
This processing could be:

 + Generating a configuration file using a templating engine,
 + Posting the objects to a RESTful interface,
 + Nothing at all (see NullEmitter), or
 + Anything you want!

The string returned by `emit` is output or written to a file by `org.rpsl4j.App` and is often used to return the final template/configuration file/desired information.
 If the emitter doesn't necessarily need to return any data, for example it POSTS directly to a web server, it may return the empty string.

### `void setArguments(Map<String, String> arguments)`
The `org.rpsl4j.App` class provides users the capability to provide arguments to an emitter in order to configure it or change it's behaviour; an IP of a remote server for example.

These arguments are passed to the emitter using the `setArguments` method with the name of the argument as the key of the `arguments` map.
Emitters do not need to accept any arguments and can leave the method empty.

### `Map<String, String> validArguments()`
The `org.rpsl4j.App` class also provides the users with a list arguments supported by the emitter by calling `validArguments()`.
This should return a map with the name of an accepted argument as the key and a short description of the argument as the value.
Emitters not accepting any arguments may return an empty map.

### Constructor
OutputEmitters may keep state but must implement a zero-argument constructor.
This is due to the method with which they are loaded, detailed later in this document.

### Making an emitter loadable by the application
rpsl4j-generator uses the [`java.util.ServiceLoader`] class to dynamically load emitters from JAR files on the class-path.
This mechanism requires that any emitter loaded by `OutputEmitterProvider` must be listed in a "provider-configuration" file within the resource directory `META-INF/services`.
In this case, it must be appended to `META-Inf/services/org.rpsl4j.emitters.OutputEmitter`.
For more information refer to the ServiceLoader [javadocs](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).

## Example: Using rpsl4j-generator to implement an emitter
So far only the interface of an emitter has been detailed and not the in-memory representation of rpsl the library provides.
The primary interface to this model is the `BGPRpslDocument` class which provides a high level view of the relationships between a particular set of RPSL Objects.
Observe the following scaffold for a basic emitter:

__src/main/java/org/rpsl4j/emitters/ScaffoldOutputEmitter.java__
```java
package org.rpsl4j.emitters;

public class ScaffoldOutputEmitter implements OutputEmitter {

    private String argumentOne = "default-value";

	@Override
	public String emit(Set<RpslObject> objects) {
		BGPRpslDocument doc = new BGPRpslDocument(objects);
        String emitterOutput = "";
        for(BGPInetRtr router : doc.getInetRtrSet()) {
            //Do something using the router object
        }

        for(BGPAutNum autSystem : doc.getAutNumSet()) {
            //Do something using the routing policy of the aut-num
        }

        //Return an output to the caller
        return emitterOutput;
	}

	@Override
	public void setArguments(Map<String, String> arguments) {
        if(arguments.containsKey("argumentOne"))
            argumentOne = arguments.get("argumentOne");
	}

	@Override
	public Map<String, String> validArguments(){
		Map<String, String> args = new HashMap<String, String>();
        args.put("argumentOne", "The first and only argument of the emitter");
        return args;
	}

}
```

BGPRpslDocument offers a logical entry into the processed RPSL objects
and can be used to retrieve various types, such as

 + BGPAutNum
 + BGPInetRtr
 + BGPPeer

Refer to the [specification](specification.md) for more information on what these types represent.

The emitter must now be appended to the service-provider file:
__src/main/resources/META-INF/services/org.rpsl4j.emitters.OutputEmitter__
```
org.rpsl4j.emitters.ScaffoldOutputEmitter
```

The project could now be compiled into a JAR called `rpsl4j-scaffold.jar`, added to the classpath and loaded by the App class

```
$ java -cp dependencies/*:rpsl4j-generator.jar:rpsl4j-scaffold.jar org.rpsl4j.App --list-emitters

Available emitters: org.rpsl4j.emitters.NullEmitter, org.rpsl4j.emitters.XMLEmitter, org.rpsl4j.emitters.ScaffoldOutputEmitter

$ java -cp dependencies/*:rpsl4j-generator.jar:rpsl4j-scaffold.jar org.rpsl4j.App --emitter org.rpsl4j.emitters.ScaffoldOutputEmitter --list-arguments

Available Arguments:
argumentOne: The first and only argument of the emitter
```
