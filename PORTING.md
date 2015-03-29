# porting
Use this file to keep track of any changes/files added during the RIPE rpsl parser process

## Google Collections, Apache Commons and CIString (Benjamin)
the CIString class is used extensively. It offers, for some reason, concurrent and immutable access. It is fairly self-contained and will be kept.
CIString, along with many classes, depends on the google [collections](https://code.google.com/p/guava-libraries/) library. These offer immutable collections which CIString and other sections of code depend on. It has been added.
Apache Commons lang added for stringutils

### NOTE: NestableRuntimeException replaced with RuntimeException in AuthenticationException.java
Nestable exceptions are discontinued in Apache commons v3, as Java has provided this functionality itself since Java 5.

From my investigations I'm reasonably confident this won't break anything; AuthenticationException appears not to be referenced in any code we're keeping, and I haven't found anything that handles a NestableRuntimeException - or anything that plainly appears to deal with exceptions thrown by AuthenticationException for that matter (the latter is easier to miss though). ip resource classes are the only suspects I identified for runtimeException handling, but further investigation of them didn't turn up anything.

(Nathan)


## JSR305 annotations
This library provides annotations for Null checking ETC. The library _has_ been added, however it mightn't be wanted.

## org.slf4j.Logger (Nathan)
Widely used logging package, used by IP resource classes.


## Joda date/time libraries (Nathan)
(Necessary, probably.. it's complicated)

Made before the days of Java 8, when (the developers say) Java's date/time
libraries were poor.
The developers now recommend anyone on Java 8 uses the standard date/time
libraries. (So we might lose these at some point if we move this parser
to Java 8- though from memory, OpenDaylight targets java 7, so it's
probably not a good idea to move this parser to Java 8 reliance any time soon).

Regardless of that, I'm keeping Joda for now, to avoid unexpected bugs due to
differences which I imagine may exist between the joda and Java 8 time apis.

## Diffutils
One of the rpsl classes provides unified diffs of objects. This could be useful so it has been retained
