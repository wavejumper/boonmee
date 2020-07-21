![Clojure CI](https://github.com/wavejumper/boonmee/workflows/Clojure%20CI/badge.svg?branch=master)
[![Clojars Project](https://img.shields.io/clojars/v/wavejumper/boonmee.svg)](https://clojars.org/wavejumper/boonmee)

# boonmee

boonmee is a language server for Clojure that focuses on features relating to host interop.

It is an attempt to bring first-class 'intellisense' to ClojureScript projects.

Goals:

* For now, focus on interop - there are other great tools that lint Clojure code already ([clj-kondo](https://github.com/borkdude/clj-kondo), [joker](https://github.com/candid82/joker) etc)
* Tooling-agnostic - you should be able to integrate boonmee into any IDE/editor tool

Right now boonmee only works on ClojureScript code (my personal frustration), but there are plans to target the JVM as well.

You can read [this](https://tscrowley.dev/#/posts/2020-20-07-boonmee) blog post about boonmee and its implementation details.

## Why

The biggest strength of Clojure is the fact that it is a hosted language.

Every Clojure codebase I have worked on leverages a host library at its core. 

And yet, most linting/editor tools (outside of Cursive for the JVM) consider the host language as an afterthought.

The guiding idea: in order to understand Clojure, you must first understand the host it is attached to.

## Features

### Editor functionality:

- [x] Quickinfo ([@jsdoc](https://jsdoc.app/) documentation, type signature, fn metadata etc)
- [x] Code completions (require, fn calls)
- [x] Go-to-definition

### Linting (WIP):

- [ ] Warn on deprecated methods (via @jsdoc convention)
- [ ] Warn on undefined es6 method call
- [ ] Incorrect arity on es6 method call
- [ ] Basic type-checking

## Installation

Download a binary from the [releases](https://github.com/wavejumper/boonmee/releases) page. 

Binaries get built via [GitHub Actions](https://github.com/wavejumper/boonmee/actions)

Refer to the [CI job](https://github.com/wavejumper/boonmee/blob/master/.github/workflows/clojure.yml) on how to compile boonmee as a native image from source.

## Dependencies 

boonmee requires [NodeJS](https://nodejs.org/en/), and the TypeScript standalone server (`tsserver`):

```
npm install -g typescript
```

By default, boonmee will use the `tsserver` found on your `$PATH`. However, you can also specify a custom path:

```
./boonmee --tsserver=/path/to/tsserver
````

## Usage

Interaction with boonmee happens via stdio:

`./boonmee`

Refer to the [Example RPC](#example-rpc) section for some examples of client requests.

If you would like to use boonmee directly from a Clojure project, bring in the following dependency:

```clojure
[wavejumper/boonmee "0.1.0-alpha1"]
```

```clojure
(require '[boonmee.client.clojure :as boonmee])
(require '[clojure.core.async :as async])

(def client (boonmee/client {}))

(async/put! (:req-ch client) {}) ;; Make a request
(async/<!! (:resp-ch client)) ;; Wait until there is a response...
(boonmee/stop client)
```

## Editor integration

WIP, no editor plugins exist for boonmee yet.

## ClojureScript 

### NPM deps

Note: boonmee analyses NPM dependencies found in a `node_modules` directory at your project's root. 

If you rely on cljsjs packages you're out of luck.

If you are a [shadow-cljs](http://shadow-cljs.org/) user, using boonmee should be a seamless experience.

### @types

boonmee's functionality comes from the [TypeScript](https://www.typescriptlang.org/) compiler. 

That means a `@types/*` package should be installed as a dev dependency, if the library you require is written in vanilla JavaScript:

```
npm install --save-dev @types/react
```

The [DefinitelyTyped/DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped) repo has many type definitions for popular npm dependencies.

**TODO**: infer/suggest possible `@types/` stubs.

### Globals

The `--env` switch tells boonmee which environment your ClojureScript project is targeting. 

This enables intellisense for `js/...` globals. 

Options are: `browser` (default) or `node`.

``` 
./boonmee --env=node
```

**Note** for the `node` env you will also need to `npm install --save-dev @types/node`

## Protocol

Specs for the boonmee protocol can be found in the [boonmee.protocol](https://github.com/wavejumper/boonmee/blob/master/src/boonmee/protocol.clj) namespace.

## Example RPC

Here's our example Clojure source code:

```clojure 
(ns tonal.core
  (:require ["@tonaljs/tonal" :refer [Midi]]))

(Midi/m ) ;; [4 7], left incomplete for our completions example


(Midi/midiToFreq 400) ;; [7 10], for our quickinfo and definitions example
```

Examples relate to the [tonaljs](https://github.com/tonaljs/tonal/blob/master/packages/midi/index.ts) npm package

For more examples, refer to boonmee's [integration tests](https://github.com/wavejumper/boonmee/tree/master/test/boonmee/integration)

### Completions

#### Request

```javascript 
{
  "command": "completions",
  "type": "request",
  "requestId": "12345",
  "arguments": {
    "projectRoot": "/path/to/project/root",
    "file": "/path/to/core.cljs",
    "line": 4,
    "offset": 7
  }
}
```

#### Response

```javascript
{
  "command": "completionInfo",
  "type": "response",
  "success": true,
  "interop": {
    "fragments": [
      "m"
    ],
    "isGlobal": false,
    "prevLocation": [
      4,
      1
    ],
    "nextLocation": [
      7,
      1
    ],
    "sym": "Midi",
    "usage": "method"
  },
  "data": {
    "isGlobalCompletion": false,
    "isMemberCompletion": true,
    "isNewIdentifierLocation": false,
    "entries": [
      {
        "name": "freqToMidi",
        "kind": "property",
        "kindModifiers": "declare",
        "sortText": "0"
      },
      {
        "name": "isMidi",
        "kind": "property",
        "kindModifiers": "declare",
        "sortText": "0"
      },
      {
        "name": "midiToFreq",
        "kind": "property",
        "kindModifiers": "declare",
        "sortText": "0"
      },
      {
        "name": "midiToNoteName",
        "kind": "property",
        "kindModifiers": "declare",
        "sortText": "0"
      },
      {
        "name": "toMidi",
        "kind": "property",
        "kindModifiers": "declare",
        "sortText": "0"
      }
    ]
  },
  "requestId": "12345"
}
```

### Quickinfo

#### Request

```javascript 
{
  "command": "quickinfo",
  "type": "request",
  "requestId": "12345",
  "arguments": {
    "file": "/path/to/core.cljs",
    "projectRoot": "/path/to/root",
    "line": 7,
    "offset": 10
  }
}
```

#### Response

```javascript
{
  "command": "quickinfo",
  "type": "response",
  "success": true,
  "data": {
    "kind": "property",
    "kindModifiers": "declare",
    "displayString": "(property) midiToFreq: (midi: number, tuning?: number) => number",
    "documentation": "",
    "tags": []
  },
  "interop": {
    "fragments": [
      "midiToFreq"
    ],
    "sym": "Midi",
    "isGlobal": false,
    "usage": "method",
    "prevLocation": [
      7,
      1
    ],
    "nextLocation": [
      7,
      18
    ]
  },
  "requestId": "12345"
}
```

### Definitions

#### Request

```javascript 
{
  "command": "definition",
  "type": "request",
  "requestId": "12345",
  "arguments": {
    "file": "/path/to/core/core.cljs",
    "projectRoot": "/path/to/project/root",
    "line": 7,
    "offset": 10
  }
}
```

#### Response

```javascript
{
  "command": "definition",
  "data": {
    "contextEnd": {
      "line": 69,
      "offset": 35
    },
    "contextStart": {
      "line": 69,
      "offset": 5
    },
    "end": {
      "line": 69,
      "offset": 15
    },
    "file": "/path/to/tonal/node_modules/@tonaljs/midi/dist/index.d.ts",
    "start": {
      "line": 69,
      "offset": 5
    }
  },
  "interop": {
    "fragments": [
      "midiToFreq"
    ],
    "isGlobal": false,
    "nextLocation": [
      7,
      18
    ],
    "prevLocation": [
      7,
      1
    ],
    "sym": "Midi",
    "usage": "method"
  },
  "requestId": "12345",
  "success": true,
  "type": "response"
}
```
