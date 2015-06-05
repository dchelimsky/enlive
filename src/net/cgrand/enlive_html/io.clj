(ns net.cgrand.enlive-html.io
  (:refer-clojure :exclude [flatmap])
  (:require [net.cgrand.tagsoup :as tagsoup]
            [net.cgrand.xml :as xml]
            [clojure.string :as str]
            [clojure.zip :as z]))

(def ^{:dynamic true} *options* {:parser tagsoup/parser})

(defmacro with-options [m & body]
  `(binding [*options* (merge *options* ~m)]
     ~@body))

(defn ns-options
  ([] (ns-options *ns*))
  ([ns] (::options (meta ns) {})))

(defn set-ns-options!
  "Sets the default options to use by all templates and snippets in the
   declaring ns."
  [options]
  (alter-meta! *ns* assoc ::options options))

(defn alter-ns-options!
  "Sets the default options to use by all templates and snippets in the
   declaring ns."
  [f & args]
  (set-ns-options! (apply f (ns-options) args)))

(defn set-ns-parser!
  "Sets the default parser to use by all templates and snippets in the
   declaring ns."
  [parser]
  (alter-ns-options! assoc :parser parser))

(defn xml-parser
 "Loads and parse a XML resource and closes the stream."
 [stream]
  (with-open [^java.io.Closeable stream stream]
    (xml/parse (org.xml.sax.InputSource. stream))))

(defmulti ^{:arglists '([resource loader])} get-resource
 "Loads a resource, using the specified loader. Returns a seq of nodes."
 (fn [res _] (type res)))

(defmulti register-resource! type)

(defmethod register-resource! :default [_]
  #_(do nothing))

(defn html-resource
 "Loads an HTML resource, returns a seq of nodes."
 ([resource]
   (get-resource resource (:parser *options*)))
 ([resource options]
   (with-options options
     (html-resource resource))))

(defn xml-resource
 "Loads an XML resource, returns a seq of nodes."
 [resource]
  (get-resource resource xml-parser))

(defmethod get-resource clojure.lang.IPersistentMap
 [xml-data _]
  (list xml-data))

(defmethod get-resource clojure.lang.IPersistentCollection
 [nodes _]
  (seq nodes))

(defmethod get-resource String
 [path loader]
  (-> (clojure.lang.RT/baseLoader) (.getResourceAsStream path) loader))

(defmethod register-resource! String [path]
  (register-resource! (.getResource (clojure.lang.RT/baseLoader) path)))

(defmethod get-resource java.io.File
 [^java.io.File file loader]
  (loader (java.io.FileInputStream. file)))

(defmethod register-resource! java.io.File [^java.io.File file]
  (register-resource! (.toURL file)))

(defmethod get-resource java.io.Reader
 [reader loader]
  (loader reader))

(defmethod get-resource java.io.InputStream
 [stream loader]
  (loader stream))

(defmethod register-resource! java.net.URL
  [^java.net.URL url]
  (alter-meta! *ns* update-in [:net.cgrand.reload/deps] (fnil conj #{}) url))

(defmethod get-resource java.net.URL
 [^java.net.URL url loader]
  (loader (.getContent url)))

(defmethod get-resource java.net.URI
 [^java.net.URI uri loader]
  (get-resource (.toURL uri) loader))
