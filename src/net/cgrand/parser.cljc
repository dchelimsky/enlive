;   Copyright (c) Christophe Grand. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.cgrand.parser
  (:require [clojure.zip :as z]))

(defprotocol Parser
  (parse [input] "Given an object, return an Enlive data structure"))

(defrecord Element [tag attrs content])

(def tag #(:tag %))
(def attrs #(:attrs %))
(def content #(:content %))

(def tag? :tag)

(defn- document? 
  "Document nodes are a parsing impelentation details and should never leak
   outside of it."
  [x] (= :document (:type x)))

(defn comment? [x] (= :comment (:type x)))
(defn dtd? [x] (= :dtd (:type x)))

(defn element-zipper
 "Returns a zipper for enlive elements given a root element"
 [root]
  (z/zipper #(or (tag? %) (document? %))
    (comp seq :content) #(assoc %1 :content %2) root))

(defn insert-element [loc e]
  (-> loc (z/append-child e) z/down z/rightmost))

(defn merge-text-left [loc s]
  (or
    (when-let [l (-> loc z/down z/rightmost)]
      (when (-> l z/node string?)
        (-> l (z/edit str s) z/up)))
    (-> loc (z/append-child s))))

(defn is-xml?
  "Return true if the input string seems to be an XML document"
  [s]
  (re-find #"(?i)<\?xml" s))