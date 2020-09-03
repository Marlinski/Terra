# LibDTN - a lightweight and modular DTN library

Delay Tolerant Networking is a network architecture providing communications in and/or through highly stressed environments. Stressed networking environments include those with intermittent connectivity, large and/or variable delays, and high bit error rates.  Key capabilities of BP include:

* Ability to use physical mobility for the movement of data
* Ability to cope with intermittent connectivity, including cases where the sender and receiver are not concurrently present in the network
* Ability to take advantage of scheduled, predicted, and opportunistic connectivity, whether bidirectional or unidirectional, in addition to continuous connectivity
* Late binding of overlay network endpoint identifiers to underlying constituent network addresses

This library is still a work in progress - no stable version yet.

# Description

Libraries:
* libdtn-core is a modular and lightweight implementation of [bpbis-11](https://tools.ietf.org/html/draft-ietf-dtn-bpbis-11)

Modules:
* libdtn-module-stcp (SimpleTCP) - convergence layer adapter module, implementation of [draft-stcp-00](https://www.ietf.org/internet-drafts/draft-burleigh-dtn-stcp-00.txt)
* libdtn-module-ldcp (LibDtn Client Protocol) - application agent module to remote client
* libdtn-module-http core module for querying the dtn-node

binaries:
* terra - a full dtn node application daemon
* dtncat - netcat of dtn, it is light client that can register/recv/send bundles from a remote dtn node using LDCP.
* dtnping - work in progress

# Example running Terra and dtncat

## Run a full DTN node with Terra

Call the package.sh helper that will automatically build and package the program for you.
If it completes successfully, it will create a new directory linux-dtn.

You can run an instance of terra like so:

```
 $ ./linux-dtn/bin/terra -d -n cla-modules -a aa-modules -c core-modules -s VOLATILE
```

## Using dtncat to register to a sink and listen for bundles:

Now we can use dtncat to register to a sink, send or receive bundle. just like Terra, gradle has generated a build folder and scripts for dtncat:

```
 $ cd linux/dtncat/build/distribution
 $ tar xvf dtncat.tar 
 $ ./dtncat/bin/dtncat -h
```

dtncat is a light client and does not run DTN itself, it must connect to a DTN node like Terra to use dtn services. The following command will connect
dtncat to a running instance of Terra, register for a sink and wait for incomming bundles:

```
 $ ./dtncat/bin/dtncat 127.0.0.1 4557 -l /test/recv/bundle
 sink registered. cookie: 31cb71b7-783a-4009-9e63-32d25edce9c1

```

An application agent (AA) cannot register to a sink that was already registered by another (AA). The cookie is used to reattach to a registered sink.
For instance, if this process were killed, you could reattach to the registration like so:

```
 $ ./dtncat/bin/dtncat 127.0.0.1 4557 -l /test/recv/bundle -c 31cb71b7-783a-4009-9e63-32d25edce9c1
 re-attach to registered sink
 
```

## Using dtncat to send bundles

In another terminal, we will send a bundle to our listening instance of dtncat 

```
  $ echo "hello" | ./dtncat/bin/dtncat 127.0.0.1 4557 -D api:me/test/recv/bundle
  bundle successfully sent to 127.0.0.1:4557
```

the -D options will set the destination EID for the bundle that wraps around standard input (in this case "hello). api:me is a special EID
that identifies the node on which the application-agent is registering to.

If another instance of dtncat was running and actively listening for /test/recv/bundle, the bundle will be automatically delivered to it
and the payload will appear in the other terminal. Otherwise it will be either deleted or kept in storage until it connects later if Terra 
was running with the option -s.

## License

    Copyright 2018 RightMesh

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.






