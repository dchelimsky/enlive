(ns net.cgrand.parser.dom
  (:require [net.cgrand.parser :as p]))

;; Polyfill for PhantomJS/older browsers
(let [proto (.-prototype js/DOMParser)]
  (set! (.-parseFromStringOriginal proto) (.-parseFromString proto))
  (try
    (when (nil? (.parseFromString (js/DOMParser.) "<div/>" "text/html"))
      (throw (js/Error. "need a polyfill!")))
    (catch js/Error e
      (set! (.-parseFromString proto)
        (fn [markup type]
          ;; Only polyfill for html, otherwise fall back to native impl
          (if (= "text/html" (.toLowerCase type))
            (let [doc (.createHTMLDocument (.-implementation js/document) "")]
              (if (< -1 (.indexOf (.toLowerCase markup) "<!doctype"))
                (set! (.-innerHTML (.-documentElement doc)) markup)
                (set! (.-innerHTML (.-body doc)) markup))
              doc)
            (do
              (.parseFromStringOriginal (js/DOMParser.) markup type))))))))

(defn to-seq
  "Given a JS object with a .-length and .item API, return a seq of it"
  [obj]
  (loop [s []
         i 0]
    (if (< i (.-length obj))
      (recur (conj s (.item obj i)) (inc i))
      (seq s))))

(def ^:private ->key #(-> % (.toLowerCase) keyword))

(extend-protocol ISeqable
  js/NamedNodeMap
  (-seq [nnm] (to-seq nnm))
  js/HTMLCollection
  (-seq [hc] (to-seq hc))
  js/NodeList
  (-seq [nl] (to-seq nl)))

(defprotocol IEnlive
  (->nodes [obj] "Convert object into Enlive node(s)."))

(extend-protocol IEnlive
  js/Document
  (->nodes [d]
    (->nodes (.-documentElement d)))

  js/Element
  (->nodes [e]
    {:tag     (->key (.-tagName e))
     :attrs   (into {} (map ->nodes (.-attributes e)))
     :content (not-empty (filter identity (map ->nodes (.-childNodes e))))}
    #_(p/map->Element
      {:tag     (->key (.-tagName e))
       :attrs   (into {} (map ->nodes (.-attributes e)))
       :content (not-empty (map ->nodes (.-childNodes e)))}))

  js/Attr
  (->nodes [a]
    [(->key (.-name a)) (.-value a)])

  js/Comment
  (->nodes [c]
    {:type :comment :data (.-data c)})


  js/DocumentType
  (->nodes [dtd]
    ;  {:type :dtd :data (not-empty (map ->nodes (.-attributes dtd)))}
    nil
    )

  js/Text
  (->nodes [tn] (.-wholeText tn))

  nil
  (->nodes [obj] ))

(defn parser
  "Parse the given string as HTML.

  Does rudimentary type detection based on whether the input contains an XML declaration "
  [s]
  (if-not (= js/String (type s))
    (throw (ex-info "DOM parser can only parse strings" {:obj s})))
  (let [type (if (p/is-xml? s)
               "application/xml"
               "text/html")
        doc (.parseFromString (js/DOMParser.) s type)
        nodes (->nodes doc)]
    nodes))