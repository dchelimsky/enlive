;   Copyright (c) Christophe Grand, 2009. All rights reserved.
;   Copyright (c) Baishampayan Ghose, 2013. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.cgrand.parser.jsoup
  "JSoup based parser backend."
  (:require [net.cgrand.parser :as p])
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Attribute Attributes Comment DataNode Document
            DocumentType Element Node TextNode XmlDeclaration]
           [org.jsoup.parser Parser Tag]))

(def ^:private ->key (comp keyword #(.. % toString toLowerCase)))

(defprotocol IEnlive
  (->nodes [d] "Convert object into Enlive node(s)."))

(extend-protocol IEnlive
  Attribute
  (->nodes [a] [(->key (.getKey a)) (.getValue a)])

  Attributes
  (->nodes [as] (not-empty (into {} (map ->nodes as))))

  Comment
  (->nodes [c] {:type :comment :data (.getData c)})

  DataNode
  (->nodes [dn] (str dn))

  Document
  (->nodes [d] (not-empty (map ->nodes (.childNodes d))))

  DocumentType
  (->nodes [dtd] {:type :dtd :data ((juxt :name :publicid :systemid) (->nodes (.attributes dtd)))})

  Element
  (->nodes [e] (p/map->Element {:tag     (->key (.tagName e))
                                :attrs   (->nodes (.attributes e))
                                :content (not-empty (map ->nodes (.childNodes e)))}))

  TextNode
  (->nodes [tn] (.getWholeText tn))

  nil
  (->nodes [_] nil))

(defmulti parse
  "Load the input using the appropriate parse signature"
  type)

(defmethod parse :default
  [input]
  (throw (ex-info (format "Don't know how to parse input of type %s using Jsoup" (class input))
           {:input input})))

(defmethod parse String
  [input]
  (Jsoup/parse input))

(defmethod parse java.io.File
  [input]
  (Jsoup/parse input "UTF-8"))

(defmethod parse java.io.InputStream
  [input]
  (with-open [^java.io.Closeable input input]
    (Jsoup/parse input "UTF-8")))

(defmethod parse java.net.URL
  [input]
  (parse (.openStream input)))

(defmethod parse java.net.URI
  [input]
  (parse (.toURL input)))

(defn parser
  "Parse using jsoup"
  [input]
  (->nodes (parse input)))
