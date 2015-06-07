(ns net.cgrand.parser.sax
  (:require [clojure.zip :as z]
            [net.cgrand.parser :refer [->Element insert-element merge-text-left element-zipper]])
  (:import (org.xml.sax ContentHandler Attributes SAXException XMLReader)
           (org.xml.sax.ext DefaultHandler2)
           (javax.xml.parsers SAXParser SAXParserFactory)
           (java.io InputStream FileInputStream)
           (java.io ByteArrayInputStream)
           (java.nio.charset Charset)))

(defn- handler [loc metadata]
  (proxy [DefaultHandler2] []
    (startElement [uri local-name q-name ^Attributes atts]
      (let [e (->Element
                (keyword q-name)
                (when (pos? (. atts (getLength)))
                  (reduce #(assoc %1 (keyword (.getQName atts %2)) (.getValue atts (int %2)))
                    {} (range (.getLength atts))))
                nil)]
        (swap! loc insert-element e)))
    (endElement [uri local-name q-name]
      (swap! loc z/up))
    (characters [ch start length]
      (swap! loc merge-text-left (String. ^chars ch (int start) (int length))))
    (ignorableWhitespace [ch start length]
      (swap! loc merge-text-left (String. ^chars ch (int start) (int length))))
    (comment [ch start length]
      (swap! loc z/append-child {:type :comment :data (String. ^chars ch (int start) (int length))}))
    (startDTD [name publicId systemId]
      (swap! loc z/append-child {:type :dtd :data [name publicId systemId]}))
    (resolveEntity
      ([name publicId baseURI systemId]
       (doto (org.xml.sax.InputSource.)
         (.setSystemId systemId)
         (.setPublicId publicId)
         (.setCharacterStream (java.io.StringReader. ""))))
      ([publicId systemId]
       (let [^DefaultHandler2 this this]
         (proxy-super resolveEntity publicId systemId))))))

(defn startparse-sax [s ch]
  (-> (SAXParserFactory/newInstance)
    (doto
      (.setValidating false)
      (.setFeature "http://xml.org/sax/features/external-general-entities" false)
      (.setFeature "http://xml.org/sax/features/external-parameter-entities" false))
    .newSAXParser
    (doto
      (.setProperty "http://xml.org/sax/properties/lexical-handler" ch))
    (.parse s ch)))

(defmulti to-stream
  "Coerce the input to an InputStream"
  type)

(defmethod to-stream :default
  [input]
  (throw (ex-info (format "Don't know how to coerce type %s into an InputStream"
                    (class input))
           {:input input})))

(defmethod to-stream String
  [input]
  (ByteArrayInputStream.
    (.getBytes input (Charset/forName "UTF-8"))))

(defmethod to-stream java.io.File
  [input]
  (FileInputStream. input))

(defmethod to-stream InputStream
  [input]
  input)

(defmethod to-stream java.net.URL
  [input]
  (.openStream input))

(defmethod to-stream java.net.URI
  [input]
  (to-stream (.toURL input)))

(defmethod to-stream java.io.Reader
  [input]
  ;; inefficient, but satisfies the contract
  (to-stream (slurp input)))

(defn parse
  "Parses and loads the source s, which can be a File, InputStream or
   String naming a URI. Returns a seq of Enlive nodes. Other parsers can be
   supplied by passing startparse, a fn taking a source and a ContentHandler
   and returning a parser"
  ([s] (parse s startparse-sax))
  ([s startparse]
   (let [loc             (atom (-> {:type :document :content nil} element-zipper))
         metadata        (atom {})
         content-handler (handler loc metadata)]
     (startparse s content-handler)
     (map #(if (instance? clojure.lang.IObj %) (vary-meta % merge @metadata) %)
       (-> @loc z/root :content)))))

(defn parser
  "Loads and parse an HTML resource and closes the stream"
  [input]
  (with-open [^java.io.Closeable stream (to-stream input)]
    (parse (org.xml.sax.InputSource. stream))))
