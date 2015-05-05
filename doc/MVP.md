% RPSL to ODL MVP
% Andrew Maxwell, Benjamin Roberts, Nathan Kelly
% COMP3100 2015

#Introduction

When generating OpenDaylight BGP Speaker configurations (hereafter referred to as the BGP configuration) from Routing Policy Specification Language documents (hereafter referred to as RPSL), there are several equalities and concessions that must be made between elements in the two formats.
This document proposes subset of RPSL that is suitable for use in the generation of the BGP configuration.
It also proposes a method of performing this generation

# Minimal Viable Product (MVP)
This is the initial subset of RPSL that we are seeking to implement.
IT omits some useful, but not necessary features.

## Omitted features
With reference to [rfc2280](https://tools.ietf.org/html/rfc2280), the list of RPSL classes of syntax features that are not targeted for support in the MVP is as follows.
This list can be broken down into omissions which are not relevant to the BGP configuration generation, and those which are relevant but not necessary.

### Irrelevant features
  + Contact information classes ([section 3](https://tools.ietf.org/html/rfc2280#section-3))
  + aut-num _import_ attribute ([section 6.1](https://tools.ietf.org/html/rfc2280#section-6.1))
  + aut-num _defaut_ attribtue ([section 6.5](https://tools.ietf.org/html/rfc2280#section-6.5))

### Unnecessary features
  + Route objects ([section 4](https://tools.ietf.org/html/rfc2280#section-4))
  + Set classes ([section 5](https://tools.ietf.org/html/rfc2280#section-5))
  + Structured Policy Specification ([section 6.6](https://tools.ietf.org/html/rfc2280#section-6.6))

The reason for the omission of these features is that they are not necessary when declaring routes to be exported to BGP peers.
They also introduce potentially recursive references that need to be resolved when determining their member list.
In order to simplify the MVP the resolution of these lists has been omitted, however it would likely be the feature added immediately after the MVP.

## RPSL Classes
In order to generate BGP configurations from RPSL documents only two classes are absolutely necessary.
The aut-num class is responsible for declaring the routes available to potential peers, and inet-rtr is responsible for generating the speaker and its configured peers.

### aut-num
The _aut-num_ class is responsible for declaring the routing policy of an autonomous system.
As we are generating a configuration that exports routes to peers, we are primarily concerned with the _export_ attribute.
The _aut-num_ object does not declare any instances of BGP speakers, nor does it declare the exact peers we will be connecting to.
It serves as a source of information BGP speakers can be initialised using.
The syntax of the _aut-num_ class can be found in [section 6](https://tools.ietf.org/html/rfc2280#section-6) of the RPSL RFC.

### export attribute
The export attributes of _aut-num_ objects are responsible for, in the MVP, the entirety of the routing policy.
Each instance of the export attribute specifies:

  + The list of routes to export,
  + The preferences of these routes,
  + The local router these routes are available at,
  + The list of ASN's the routes should be exported to, and
  + The specific peer routers the routes should be exported to

Whilst the local-router address is an optional part of the peering section ([section 6.1.1](https://tools.ietf.org/html/rfc2280#section-6.1.1)), in order to provide a next-hop for the exported route this attribute must become mandatory.

### inet-rtr
The _inet-rtr_ class represents an internet router which engages in routing and, optionally, peering.
The syntax of this class can be found in [section 9](https://tools.ietf.org/html/rfc2280#section-9) of the RPSL RFC.
When generating a BGP configuration, _inet-rtr_ instances are interpreted as BGP speaker instances to be instantiated.
The list of peers in the object are used to establish peering connections which the route export policy of the relevant _aut-num_ object are exported over.

The _protocol_ and _option_ fields of the peer attribute are not handled.

## BGP Configuration
Having defined the relevant subset of RPSL required to generate BGP configurations, the relationship between the two is now described.

### Peer registry, RIB and Tables
There are three types of objects in the BGP configuration that must be described before continuing.

Peer registries contain a list of BGP Peers which a BGP speaker should establish connections to.

The Routing Information Base (RIB) contains a serious of routing tables.
Each BGP peer is associated with a RIB and specifies which set of tables within the RIB should be exported to it.

### Speaker
A Speaker object (bgp-peer-acceptor) is added for each _inet-rtr_ instance in the RPSL document.
The IP address the speaker is to bind to is taken from the interfaces of the _inet-rtr_ object, similarly the ASN is also taken from the RPSL object.
Each speaker is provided with its own peer-registry which it's BGP Peers will be added to.

### Peers
A BGP Peer object (bgp-peer) is added for each address listed in the _inet-rtr_ object.
The peer will be added to the peer-registry of it's BGP speaker, will use the RIB dedicated to it's particular RIB and will export a table consisting of "general" routes as well as those specifically exported to itself.
