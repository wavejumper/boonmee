# boonmee

boonmee is a language server for Clojure that focuses on features relating to host interop.

It is an attempt to bring first-class 'intellisense' to ClojureScript projects.

Goals:

* For now, focus on interop - there are other great tools that lint Clojure code already (clj-kondo, joker etc)
* Tooling-agnostic - you should be able to integrate boonmee into any IDE/editor tool

Right now boonmee only works on ClojureScript code (my personal frustration), but there are plans to target the JVM as well.

You can read [this]() blog post about boonmee and its implementation detials.

## Why

The biggest strength of Clojure is the fact that it is a hosted language.

Every Clojure codebase I have worked on leverages a host library at its core. 

And yet, most linting/editor tools (outside of Cursive for the JVM) consider the host language as an afterthought.

## Features

### Editor functionality:

- [x] Quickinfo (@jsdoc documentation, type signature, fn metadata etc)
- [x] Code completions (require, fn calls)
- [x] Go-to-definition

### Linting (WIP):

- [ ] Warn on deprecated methods (via @jsdoc convention)
- [ ] Warn on undefined es6 method call
- [ ] Incorrect arity on es6 method call
- [ ] Basic type-checking

## Installation

Download a binary from the [releases]() page. 

Binaries are built via [CircleCI]() - you can view the CI job to verify the SHA hash.

Refer to the CI job on how to compile boonmee as a native image from source.

## Usage

Interaction with boonmee happens either via stdio (default) or TCP

`./boonmee --client=stdio`

`./boonmee --client=tcp --port=9000`

Refer to the [Example RPC](#example-rpc) section for some examples of client requests.

If you would like to use boonmee directly from a Clojure project, bring in the following dependency:

```clojure
```

```clojure
(require '[boonmee.client.clojure :as boonmee])
(require '[clojure.core.async :as async])

(def client (boonmee/client {}))

(async/put! (:req-ch client) {}) ;; Make a request
(async/<!! (:resp-ch client)) ;; Wait until there is a response...
(boonmee/stop client)
```

Note: when calling from Clojure directly, you will need `tsserver` on your `$PATH`:

```
npm install -g typescript
```

## ClojureScript 

### NPM deps

Note: boonmee analyses NPM dependencies found in a `node_modules` directory at your project's root. 

If you rely on cljsjs you're out of luck.

If you are a [shadow-cljs](http://shadow-cljs.org/) user, using boonmee should be a seamless experience.

### @types

boonmee's functionality comes from the [TypeScript](https://www.typescriptlang.org/) compiler. 

That means a `@types/*` package should be installed as a dev dependency, if the library you require is written in vanllia JavaScript (a rarity these days!):

```
npm install --save-dev @types/react
```

## Protocol

Specs for the boonmee protocol can be found in the `boonmee.protocol` namespace.

## Example RPC

Here's our example Clojure source code:

```clojure 
(ns tonal.core
  (:require ["@tonaljs/tonal" :refer [Midi]]))

(Midi/m )
```

Our examples will be querying at loc `[4 8]` 

### Completions

#### Request

```javascript 
{}
```

#### Response

```javascript
{}
``

### Completions

#### Request

```javascript 
{}
```

#### Response

```javascript
{}
``

### Definitions

#### Request

```javascript 
{}
```

#### Response

```javascript
{}
``