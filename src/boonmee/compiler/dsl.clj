(ns boonmee.compiler.dsl
  (:require [boonmee.compiler.core :as compiler]))

(defn es6-import
  []
  (compiler/->Es6Import))

(defn es6-symbol
  [opts]
  (compiler/map->Es6Sym opts))