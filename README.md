# boonmee

The biggest strength of Clojure is the fact that it is a hosted language.

Every professional Clojure codebase I have worked on leverages a hosted library at its core (whether it be Kafka Streams or React). 

And yet, most linting/developer tools (outside of Cursive for JVM) consider the host language as an afterthought.

boonmee is an attempt to bring first-class 'intellisense' for Clojure interop.

Goals:

* Focus on interop only - there are other great tools that lint Clojure code already (clj-kondo, joker etc)
* Tooling-agnostic - you should be able to integrate boonmee into any editor

Right now boonmee only works on ClojureScript code (my personal frustration), but there are plans to target the JVM as well.

## Features

### Editor functionality:

- [x] Quickinfo (documentation, type hints, jsdoc -- deprecation/etc)
- [x] Completions (require, fn calls)
- [x] Go-to-definition

### Linting

- [ ] Warn on unused imports
- [ ] Warn on deprecated methods
- [ ] Warn on no such method
- [ ] Basic type-checking

## Installation


## Clojurescript 

Note: boonmee analyses `npm-deps` only. If you rely on cljsjs you're out of luck.

## Example RPC
