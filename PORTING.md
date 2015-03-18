# porting
Use this file to keep track of any changes/files added during the RIPE rpsl parser process

## Google Collections, Apache Commons and CIString (Benjamin)
the CIString class is used extensivly. It offers, for some reason, concurrent and immutable access. It is fairly selfcontained and will be kept.
CIString, along with many classes, depends on the google [collections](https://code.google.com/p/guava-libraries/) library. These offer immutable collections which CIString and other sections of code depend on. It has been added.
Apache Commons lang added for stringutils
