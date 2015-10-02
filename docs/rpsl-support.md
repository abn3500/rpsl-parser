# RPSL Language Support and Internal Model #
The rpsl4j project doesn't presently support the entire RPSL specification.
Particular meaning is also attributed to certain classes of RPSL objects within the `BGPRpslDocument` and related classes.
This document serves to outline the subset of supported RPSL and expected behaviors.


## aut-num ##
The aut-num class specifies the peering policy for a single Autonomous System.
This includes the routes, route-sets, aut-nums and as-sets exported, to which peers and from which routers.
At this point in time, rpsl4j only supports the export of routes to other peers; import and default routing policies are not handled.

### Supported attributes ###
 + __aut-num__: as-number
 + __as-name__: object-name
 + __member-of (optional)__: list of as-set-names
 + __export (optional)__: see below

### Example ###
```
aut-num: 1
as-name: example-as
member-of: as-examples
export:
    to AS2 2.2.2.2 at 1.1.1.1 action pref=10;
    announce 1.0.0.0/24, rs-example-set, as-examples
```

### rpsl4j behavior ###
the RPSL aut-num class is mapped to the rpsl4j-generator class __BGPAutNum__.
Along with standard information such as the name and number of the autonomous system, this class also generates an in memory map of the exported routing policy (`BGPAutNum#includedRouteMap`).
The type of this map is `Multimap<Pair<Long, String>, BGPRoute>`, or rather `(Destination-Autnum, Peer-IP) -> [Exported Route]` (Peer-IP will be "0.0.0.0" for routes exported to an entire AS).
This map is populated by the export statements of the particular aut-num class and is further described in the following, dedicated section.


## aut-num: export statement ##
RPSL export statements are the building blocks of an aut-num's outgoing peering policy.
rpsl4j does not presently support the set based filter expressions or structural policy specifications outlined in the RPSL specification.
It only allows for the exporting of prefixes, route-sets and as-sets.

### Example ###
```
(top of aut-num)
export:
    to AS2 2.2.2.2 at 1.1.1.1 action pref=10;
    announce 1.0.0.0/24, rs-example-set, as-examples
```

### rpsl4j behavior ###
Parsing of an aut-num's export statements occurs in `BGPAutNum#generateRouteMaps` and can be roughly broken down into three stages.

#### Find the export peers ###
First the peering specification of the export statement is parsed (`BGPAutNum#getExportPeers`) and a set of "export peers" is returned.
The type of this set is `Set<Pair<Pair<Long, String>, String>>` and represents the type `[((Destination-Autnum, Peer-Ip), Exporting-Router-Ip)]`.
For example, the peering specification `AS2 2.2.2.2 at 1.1.1.1` would result in the export peer `((2, "2.2.2.2"), "1.1.1.1")`. Routes exported to an entire AS (`AS2 at 1.1.1.1` will have a "Peer-Ip" of "0.0.0.0".

It is important to mention that the "exporting-router-ip" is used as the origin of the exported routes and is used by the `BGPInetRtr` class to determine which routes it will export.
This is discussed later in this document.

#### Parse the export actions ####
After identifying the peers, the actions of an export statement are parsed.
An example of an action statement is `action pref = 10;`. These are passed onto and applied to the routes to be exported and is implemented by `BGPAutNum#resolveActions`.

#### Resolve the routes ####
Having identified the peers and action statements, the export attribute is then passed to `BGPRoute#resolveRoutes`.
This method returns the set of individual `BGPRoute` objects exported by the current export statement; each encapsulating the prefix and origin of the route.
This set of routes is then added to the `BGPAutNum`'s route map.


## inet-rtr ##
The RPSL inet-rtr class is used to designate internet routing devices, the autonomous systems they operate within, their interface addresses and peers.
rpsl4j associates special meaning with these objects as declarations of BGP speakers and identifiers of peering sessions.

### Supported attributes ###
 + __inet-rtr__: dns-name
 + __local-as__: as-number
 + __ifaddr (optional, multiple)__: router interface specification
 + __peer (optional, multiple)__: peers of router

### Example ###
```
inet-rtr: router.test.net
local-as: AS1
ifaddr: 1.1.1.1 netmask 24
ifaddr: 1.1.2.1 netmask 24
peer: BGP4 2.2.2.1
peer: BGP4 3.3.3.1 asno(3)
```
_note: the actual protocol of peer is ignored during processing._

### rpsl4j behavior ###
For each `ifaddr` of an inet-rtr object an instance of `BGPInetRtr` is instantiated, representing a BGP speaker instance.
Each `BGPInetRtr` instance also instantiates a `BGPPeer` for each listed peer.
If the peer does not specify its own local-as (using the `asno` annotation), the BGPInetRtr's corresponding `BGPAutNum` instance is queried for the first matching AS associated with the peer's IP in an export statement (see `BGPInetRtr#addPeer`).
The `BGPPeer` instance will then query and store the routes exported to it by the policy of the speaker's AutNum (see `BGPPeer#BGPPeer`).
This is the final expression in rpsl4j of a routing policy, and any emmitters implemented on the library are encouraged to use `BGPPeer` and `BGPInetRtr` to instantiate and configure BGP peering sessions.


## route ##
The RPSL route class specifies a particular internet routing prefix.
It's use is mainly in the resolution of route-sets exported by aut-num objects.

### Supported attributes ###
 + __route__: address-prefix
 + __origin__: as-number
 + __withdrawn (optional)__: date
 + __member-of (optional)__: list of route-set-names
 + __mnt-by (optional)__: name of maintainer object or class

### Example ###
```
route: 1.1.1.0/24
origin: AS1
member-of: rs-example-set
mnt-by: rpsl4j-team-members
```

### rpsl4j behavior ###
Parsing route objects into `BGPRpslRoute` instances is the first thing `BGPRpslDocument` when constructed. This is because they are required for the construction and resolution of `route-set` objects.
Based on their `origin`, `member` of and `mnt-by` values, they are "cached" in different maps for lookup by set objects.
Withdrawn routes are dropped.

## route-set and as-set ##
These classes are used to group together route prefixes.
Subject to the omision of range operations discussed below, they are handled largely as specified by the RPSL specification and will not be discussed further.

## Special notes ##
 + The range operations (`^`, `+` etc) defined by the RPSL specifiation for application to set members and exported prefixes are not supported by rpsl4j.
 + RPSL classes not covered by this document (such as `mntnr`), while not handled by rpsl4j, may still be included in the stream of objects given to `BGPRpslDocument` as they will simply be ignored.
