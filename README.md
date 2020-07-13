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
- [x] Auto-install/suggest `@types` packages

### Linting (WIP):

- [ ] Warn on unused imports
- [ ] Warn on deprecated methods
- [ ] Warn on no such method
- [ ] Basic type-checking

## Installation


## ClojureScript 

### NPM deps

Note: boonmee analyses NPM dependencies found in a `package.json` file at your project's root. 

If you rely on cljsjs you're out of luck.

If you are a [shadow-cljs](http://shadow-cljs.org/) user, using boonmee should be a seamless experience.

### @types

boonmee's functionality comes from the [TypeScript](https://www.typescriptlang.org/) compiler. 

That means a `@types/*` package should be installed as a dev dependency, if the library you require is written in vanllia JavaScript (a rarity these days!):

```
npm install --save-dev @types/react
```

## Example RPC
