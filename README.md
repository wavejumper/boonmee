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

* Unused imports
* Warn on deprecated methods
* Docstrings
* Autocompletion (require, fn calls)
* Go-to-definition
* Basic type-checking

## Usage

## Clojurescript 

Note: boonmee analyses `npm-deps` only. If you rely on cljsjs you're out of luck.

## Usage

File:
```
(ns tonal.core
  (:require ["@tonaljs/tonal" :refer [Midi]]))
Midi/ ;; <--- your completions go here
```

Send some RPC to my new project:
```
(request! {:command   "open"
            :arguments {:file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs"}})
```

Then request some completions:
```
(request! {:command   "completions"
            :arguments {:file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs" :line 4 :offset 6}})
```

gets response:
````
{"seq":0,"type":"response","command":"completionInfo","request_seq":6,"success":true,"body":{"isGlobalCompletion":false,"isMemberCompletion":true,"isNewIdentifierLocation":false,"entries":[{"name":"freqToMidi","kind":"property","kindModifiers":"declare","sortText":"0"},{"name":"isMidi","kind":"property","kindModifiers":"declare","sortText":"0"},{"name":"midiToFreq","kind":"property","kindModifiers":"declare","sortText":"0"},{"name":"midiToNoteName","kind":"property","kindModifiers":"declare","sortText":"0"},{"name":"toMidi","kind":"property","kindModifiers":"declare","sortText":"0"}]}}
```
