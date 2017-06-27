(ns net.cgrand.xml
  "Deprecated, provided for backwards compatibility

  Most actual functionality has been moved into the
  net.cgrand.parser NS."
  (:require [net.cgrand.parser :as p]
           [net.cgrand.parser.sax :as sax]))

(def tag p/tag)
(def attrs p/attrs)
(def content p/content)

(def tag? p/tag?)

(def comment? p/comment?)
(def dtd? p/dtd?)

(def xml-zip p/element-zipper)

(def startparse-sax sax/startparse-sax)

(def parse sax/parse)
