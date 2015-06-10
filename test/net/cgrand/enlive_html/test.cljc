;   Copyright (c) Christophe Grand, 2009. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.cgrand.enlive-html.test
  #?(:cljs (:require-macros [net.cgrand.enlive-html.test :refer [is-same]]))
  (:require
    [clojure.zip :as zip]
    [net.cgrand.enlive-html :as e]
    [net.cgrand.parser :as xml]
    [clojure.string :as str]
    #?(:clj  [clojure.test :refer [deftest is are]]
       :cljs [cljs.test :refer-macros [deftest is are]])
    [net.cgrand.parser :as p]))

;; test utilities
(defn- normalize [x]
  (if (string? x)
    (e/html-snippet x)
    (e/html-resource x)))

(defn- same? [& xs]
  (apply = (map normalize xs)))

#?(:clj
   (defmacro #^{:private true}
    is-same
    [& forms]
     (if (resolve 'cljs.test/run-tests)
       `(~'cljs.test/is (same? ~@forms))
       `(~'clojure.test/is (same? ~@forms)))))

(defn- test-step [expected pred node]
  (= expected (boolean (pred (p/element-zipper node)))))

(defn- elt
 ([tag] (elt tag nil))
 ([tag attrs & content]
   {:tag tag
    :attrs attrs
    :content content}))

(deftest tag=-test
  (are [_1 _2 _3] (test-step _1 _2 _3)
    true (e/tag= :foo) (elt :foo)
    false (e/tag= :bar) (elt :foo)))

(deftest id=-test
  (are [_1 _2 _3] (test-step _1 _2 _3)
    true (e/id= "foo") (elt :div {:id "foo"})
    false (e/id= "bar") (elt :div {:id "foo"})
    false (e/id= "foo") (elt :div)))

(deftest attr?-test
  (are [_1 _2 _3] (test-step _1 _2 _3)
    true (e/attr? :href) (elt :a {:href "http://cgrand.net/"})
    false (e/attr? :href) (elt :a {:name "toc"})
    false (e/attr? :href :title) (elt :a {:href "http://cgrand.net/"})
    true (e/attr? :href :title) (elt :a {:href "http://cgrand.net/" :title "home"})))

(deftest attr=-test
  (are [_1 _2] (test-step _1 _2 (elt :a {:href "http://cgrand.net/" :title "home"}))
    true (e/attr= :href "http://cgrand.net/")
    false (e/attr= :href "http://clojure.org/")
    false (e/attr= :href "http://cgrand.net/" :name "home")
    false (e/attr= :href "http://cgrand.net/" :title "homepage")
    true (e/attr= :href "http://cgrand.net/" :title "home")))

(deftest attr-starts-test
  (are [_1 _2] (test-step _1 _2 (elt :a {:href "http://cgrand.net/" :title "home"}))
    true (e/attr-starts :href "http://cgr")
    false (e/attr-starts :href "http://clo")
    false (e/attr-starts :href "http://cgr" :name "ho")
    false (e/attr-starts :href "http://cgr" :title "x")
    true (e/attr-starts :href "http://cgr" :title "ho")))

(deftest attr-ends-test
  (are [_1 _2] (test-step _1 _2 (elt :a {:href "http://cgrand.net/" :title "home"}))
    true (e/attr-ends :href "d.net/")
    false (e/attr-ends :href "e.org/")
    false (e/attr-ends :href "d.net/" :name "me")
    false (e/attr-ends :href "d.net/" :title "hom")
    true (e/attr-ends :href "d.net/" :title "me")))

(deftest attr-contains-test
  (are [_1 _2] (test-step _1 _2 (elt :a {:href "http://cgrand.net/" :title "home"}))
    true (e/attr-contains :href "rand")
    false (e/attr-contains :href "jure")
    false (e/attr-contains :href "rand" :name "om")
    false (e/attr-contains :href "rand" :title "pa")
    true (e/attr-contains :href "rand" :title "om")))

(deftest nth-child-test
  (are [_1 _2] (same? _2 (e/sniptest "<dl><dt>1<dt>2<dt>3<dt>4<dt>5" _1 (e/add-class "foo")))
    [[:dt (e/nth-child 2)]] "<dl><dt>1<dt class=foo>2<dt>3<dt>4<dt>5"
    [[:dt (e/nth-child 2 0)]] "<dl><dt>1<dt class=foo>2<dt>3<dt class=foo>4<dt>5"
    [[:dt (e/nth-child 3 1)]] "<dl><dt class=foo>1<dt>2<dt>3<dt class=foo>4<dt>5"
    [[:dt (e/nth-child -1 3)]] "<dl><dt class=foo>1<dt class=foo>2<dt class=foo>3<dt>4<dt>5"
    [[:dt (e/nth-child 3 -1)]] "<dl><dt>1<dt class=foo>2<dt>3<dt>4<dt class=foo>5"))

(deftest nth-last-child-test
  (are [_1 _2] (same? _2 (e/sniptest "<dl><dt>1<dt>2<dt>3<dt>4<dt>5" _1 (e/add-class "foo")))
    [[:dt (e/nth-last-child 2)]] "<dl><dt>1<dt>2<dt>3<dt class=foo>4<dt>5"
    [[:dt (e/nth-last-child 2 0)]] "<dl><dt>1<dt class=foo>2<dt>3<dt class=foo>4<dt>5"
    [[:dt (e/nth-last-child 3 1)]] "<dl><dt>1<dt class=foo>2<dt>3<dt>4<dt class=foo>5"
    [[:dt (e/nth-last-child -1 3)]] "<dl><dt>1<dt>2<dt class=foo>3<dt class=foo>4<dt class=foo>5"
    [[:dt (e/nth-last-child 3 -1)]] "<dl><dt class=foo>1<dt>2<dt>3<dt class=foo>4<dt>5"))

(deftest nth-of-type-test
  (are [_1 _2] (same? _2 (e/sniptest "<dl><dt>1<dd>def #1<dt>2<dt>3<dd>def #3<dt>4<dt>5" _1 (e/add-class "foo")))
    [[:dt (e/nth-of-type 2)]] "<dl><dt>1<dd>def #1<dt class=foo>2<dt>3<dd>def #3<dt>4<dt>5"
    [[:dt (e/nth-of-type 2 0)]] "<dl><dt>1<dd>def #1<dt class=foo>2<dt>3<dd>def #3<dt class=foo>4<dt>5"
    [[:dt (e/nth-of-type 3 1)]] "<dl><dt class=foo>1<dd>def #1<dt>2<dt>3<dd>def #3<dt class=foo>4<dt>5"
    [[:dt (e/nth-of-type -1 3)]] "<dl><dt class=foo>1<dd>def #1<dt class=foo>2<dt class=foo>3<dd>def #3<dt>4<dt>5"
    [[:dt (e/nth-of-type 3 -1)]] "<dl><dt>1<dd>def #1<dt class=foo>2<dt>3<dd>def #3<dt>4<dt class=foo>5"))

(deftest nth-last-of-type-test
  (are [_1 _2] (same? _2 (e/sniptest "<dl><dt>1<dd>def #1<dt>2<dt>3<dd>def #3<dt>4<dt>5" _1 (e/add-class "foo")))
    [[:dt (e/nth-last-of-type 2)]] "<dl><dt>1<dd>def #1<dt>2<dt>3<dd>def #3<dt class=foo>4<dt>5"
    [[:dt (e/nth-last-of-type 2 0)]] "<dl><dt>1<dd>def #1<dt class=foo>2<dt>3<dd>def #3<dt class=foo>4<dt>5"
    [[:dt (e/nth-last-of-type 3 1)]] "<dl><dt>1<dd>def #1<dt class=foo>2<dt>3<dd>def #3<dt>4<dt class=foo>5"
    [[:dt (e/nth-last-of-type -1 3)]] "<dl><dt>1<dd>def #1<dt>2<dt class=foo>3<dd>def #3<dt class=foo>4<dt class=foo>5"
    [[:dt (e/nth-last-of-type 3 -1)]] "<dl><dt class=foo>1<dd>def #1<dt>2<dt>3<dd>def #3<dt class=foo>4<dt>5"))

(deftest has-test
  (is-same "<div><p>XXX<p class='ok'><a>link</a><p>YYY"
    (e/sniptest "<div><p>XXX<p><a>link</a><p>YYY"
      [[:p (e/has [:a])]] (e/add-class "ok"))))

(deftest but-test
  (is-same "<div><p>XXX<p><a class='ok'>link</a><p>YYY"
    (e/sniptest "<div><p>XXX<p><a>link</a><p>YYY"
      [:div (e/but :p)] (e/add-class "ok")))

  (is-same "<div><p class='ok'>XXX<p><a>link</a><p class='ok'>YYY"
    (e/sniptest "<div><p>XXX<p><a>link</a><p>YYY"
      [[:p (e/but (e/has [:a]))]] (e/add-class "ok"))))

(deftest left-test
  (are [_1 _2] (same? _2 (e/sniptest "<div><h1>T1<h2>T2<h3>T3<p>XXX" _1 (e/add-class "ok")))
    [[:h3 (e/left :h2)]] "<div><h1>T1<h2>T2<h3 class=ok>T3<p>XXX"
    [[:h3 (e/left :h1)]] "<div><h1>T1<h2>T2<h3>T3<p>XXX"
    [[:h3 (e/left :p)]] "<div><h1>T1<h2>T2<h3>T3<p>XXX"))

(deftest lefts-test
  (are [_1 _2] (same? _2 (e/sniptest "<div><h1>T1<h2>T2<h3>T3<p>XXX" _1 (e/add-class "ok")))
    [[:h3 (e/lefts :h2)]] "<div><h1>T1<h2>T2<h3 class=ok>T3<p>XXX"
    [[:h3 (e/lefts :h1)]] "<div><h1>T1<h2>T2<h3 class=ok>T3<p>XXX"
    [[:h3 (e/lefts :p)]] "<div><h1>T1<h2>T2<h3>T3<p>XXX"))

(deftest right-test
  (are [_1 _2] (same? _2 (e/sniptest "<div><h1>T1<h2>T2<h3>T3<p>XXX" _1 (e/add-class "ok")))
    [[:h2 (e/right :h3)]] "<div><h1>T1<h2 class=ok>T2<h3>T3<p>XXX"
    [[:h2 (e/right :p)]] "<div><h1>T1<h2>T2<h3>T3<p>XXX"
    [[:h2 (e/right :h1)]] "<div><h1>T1<h2>T2<h3>T3<p>XXX"))

(deftest rights-test
  (are [_1 _2] (same? _2 (e/sniptest "<div><h1>T1<h2>T2<h3>T3</h3><p>XXX" _1 (e/add-class "ok")))
    [[:h2 (e/rights :h3)]] "<div><h1>T1<h2 class=ok>T2<h3>T3</h3><p>XXX"
    [[:h2 (e/rights :p)]] "<div><h1>T1<h2 class=ok>T2<h3>T3</h3><p>XXX"
    [[:h2 (e/rights :h1)]] "<div><h1>T1<h2>T2<h3>T3</h3><p>XXX"))

(deftest any-node-test
  (is (= 3 (-> "<html><body><i>this</i> is a <i>test</i>" e/html-snippet
             (e/select [:body :> e/any-node]) count))))

(deftest transform-test
  (is-same "<div>" (e/sniptest "<div><span>" [:span] nil))
  (is-same "<!-- comment -->" (e/sniptest "<!-- comment -->" [:span] nil))
  (is-same "\n<feed xml:lang=\"en-us\">\n <title>1</title>\n <id>1</id>\n <link href=\"./\" />\n <link rel=\"self\" href=\"\" />\n <subtitle>1</subtitle>\n <updated>1</updated>\n\n<entry xml:base=\"http://orcloud.org/\">\n  <title>1</title>\n  <link href=\"1\" />\n  <id>1</id>\n  <published>1</published>\n  \n  <category term=\"devops\" scheme=\"http://hugoduncan.org/tags\">1</category>\n  <summary type=\"xhtml\"><div>1</div></summary>\n  <content type=\"xhtml\"><div>1</div></content>\n</entry><entry xml:base=\"http://orcloud.org/\">\n  <title>1</title>\n  <link href=\"1\" />\n  <id>1</id>\n  <published>1</published>\n  \n  <category term=\"devops\" scheme=\"http://hugoduncan.org/tags\">1</category>\n  <summary type=\"xhtml\"><div>1</div></summary>\n  <content type=\"xhtml\"><div>1</div></content>\n</entry>\n</feed>\n"
           (e/sniptest "<?xml version='1.0' encoding='UTF-8'?>
<feed xml:lang='en-us' xmlns='http://www.w3.org/2005/Atom'>
 <title>Feed title</title>
 <id>feed id</id>
 <link href='./'/>
 <link href='' rel='self'/>
 <subtitle>subtitle</subtitle>
 <updated></updated>

<entry xml:base='http://orcloud.org/'>
  <title>title</title>
  <link href=''/>
  <id></id>
  <published>2010-05-11T20:00:00.000000-04:00</published>
  <updated>2010-05-11T20:00:00.000000-04:00</updated>
  <category scheme='http://hugoduncan.org/tags' term='devops'/>
  <summary type='xhtml'><div xmlns='http://www.w3.org/1999/xhtml'></div></summary>
  <content type='xhtml'><div xmlns='http://www.w3.org/1999/xhtml'></div></content>
</entry>
</feed>
" [:feed :title] (e/content "1")
  [:feed :id] (e/content "1")
  [:feed :subtitle] (e/content "1")
  [:feed :updated] (e/content "1")
  [[:* (e/attr? :xmlns)]] (e/remove-attr :xmlns)
  [:entry] (e/clone-for [x ["one" "two"]]
                      [:published] (e/content "1")
                      [:updated] nil
                      [:category] (e/content "1")
                      [:link] (e/set-attr :href "1")
                      [:id] (e/content "1")
                      [:summary :div] (e/content "1")
                      [:content :div] (e/content "1"))
            )))

(deftest clone-for-test
  ;; node selector
  (is-same "<ul><li>one<li>two"
    (e/sniptest "<ul><li>" [:li] (e/clone-for [x ["one" "two"]] (e/content x))))
  ;; fragment selector
  (is-same "<dl><dt>term #1<dd>desc #1<dt>term #2<dd>desc #2"
    (e/sniptest "<dl><dt>Sample term<dd>sample description"
      {[:dt] [:dd]} (e/clone-for [[t d] {"term #1" "desc #1" "term #2" "desc #2"}]
                      [:dt] (e/content t)
                      [:dd] (e/content d)))))

(deftest move-test
  (are [_1 _2] (same?
                _2
                (e/sniptest "<body><span>1</span><div id=target>here</div><span>2</span>"
                          (e/move [:span] [:div] _1) ))
    e/substitute "<body><span>1</span><span>2</span>"
    e/content "<body><div id=target><span>1</span><span>2</span></div>"
    e/after "<body><div id=target>here</div><span>1</span><span>2</span>"
    e/before "<body><span>1</span><span>2</span><div id=target>here</div>"
    e/append "<body><div id=target>here<span>1</span><span>2</span></div>"
    e/prepend "<body><div id=target><span>1</span><span>2</span>here</div>")
  (are [_1 _2] (same?
                _2
                (e/sniptest "<div><h1>Title1</h1><p>blabla</p><hr><h2>Title2</h2><p>blibli"
                          (e/move {[:h1] [:p]} {[:h2] [:p]} _1) ))
    e/substitute "<div><hr><h1>Title1</h1><p>blabla"
    e/after "<div><hr><h2>Title2</h2><p>blibli<h1>Title1</h1><p>blabla"
    e/before "<div><hr><h1>Title1</h1><p>blabla<h2>Title2</h2><p>blibli")
  (are [_1 _2] (same?
                _2
                (e/sniptest "<div><h1>Title1</h1><p>blabla</p><hr><h2>Title2</h2><p>blibli"
                          (e/move {[:h1] [:p]} [:h2] _1) ))
    e/substitute "<div><hr><h1>Title1</h1><p>blabla</p><p>blibli"
    e/content "<div><hr><h2><h1>Title1</h1><p>blabla</p></h2><p>blibli"
    e/after "<div><hr><h2>Title2</h2><h1>Title1</h1><p>blabla</p><p>blibli"
    e/before "<div><hr><h1>Title1</h1><p>blabla</p><h2>Title2</h2><p>blibli"
    e/append "<div><hr><h2>Title2<h1>Title1</h1><p>blabla</p></h2><p>blibli"
    e/prepend "<div><hr><h2><h1>Title1</h1><p>blabla</p>Title2</h2><p>blibli")
  (are [_1 _2] (same? _2
                      (e/sniptest "<div><h1>Title1</h1><p>blabla</p><hr><h2>Title2</h2><p>blibli"
                                (e/move [:h1] {[:h2] [:p]} _1) ))
    e/substitute "<div><p>blabla<hr><h1>Title1"
    e/after "<div><p>blabla</p><hr><h2>Title2</h2><p>blibli</p><h1>Title1"
    e/before "<div><p>blabla</p><hr><h1>Title1</h1><h2>Title2</h2><p>blibli"))

(deftest wrap-test
  (is-same "<dl><ol><dt>Sample term</dt></ol><dd>sample description</dd></dl>"
    (e/sniptest "<dl><dt>Sample term<dd>sample description" [:dt] (e/wrap :ol)))
  (is-same "<dl><ol><dt>Sample term</dt><dd>sample description</dd></ol></dl>"
    (e/sniptest "<dl><dt>Sample term<dd>sample description" {[:dt] [:dd]} (e/wrap :ol))))

(deftest select-test
  (is (= 1 (-> "<html><body><h1>hello</h1>" e/html-snippet (e/select [:html :body :*]) count))))

(deftest emit*-test
  (is (= "<h1>hello&lt;<script>if (im < bad) document.write('&lt;')</script></h1>"
         (e/sniptest "<h1>hello&lt;<script>if (im < bad) document.write('&lt;')"))))

(deftest transform-content-test
  (is-same "<div><div class='bar'><div>"
    (e/sniptest "<div><div><div>"
      [:> :div] (e/transform-content [:> :div] (e/add-class "bar")))))

;
;(e/deftemplate case-insensitive-doctype-template "resources/templates/doctype_case.html"
;  [])
;
;(deftest case-insensitive-doctype-test
;  (is (.startsWith (apply str (case-insensitive-doctype-template)) "<!DOCTYPE")))
;
;(deftest templates-return-seqs
;  (is (seq? (case-insensitive-doctype-template))))

(deftest hiccup-like
  (is-same "<div><b>world"
    (e/sniptest "<div>"
      [:div] (e/content (e/html [:b "world"]))))
  (is-same "<div><b id=foo>world"
      (e/sniptest "<div>"
        [:div] (e/content (e/html [:b#foo "world"]))))
  (is-same "<div><a id=foo class=\"link home\" href=\"http://clojure.org/\">world"
    (e/sniptest "<div>"
      [:div] (e/content (e/html [:a.link#foo.home {:href "http://clojure.org/"}
                             "world"]))))
  (is-same "<div><ul><li>a<li>b<li>c"
    (e/sniptest "<div>"
      [:div] (e/content (e/html [:ul (for [s ["a" "b" "c"]] [:li s])])))))

(deftest hiccup-mixed
  (is-same "<div><p><i>big</i><b>world</b></p>"
    (e/sniptest "<div>"
      [:div] (e/content (e/html [:p '({:tag :i :content ["big"]}
                                      {:tag :b :content ["world"]})])))
    (e/sniptest "<div>"
      [:div] (e/content (e/html [:p {:tag :i :content ["big"]}
                                 {:tag :b :content ["world"]}])))
    (e/sniptest "<div>"
      [:div] (e/content (e/html {:tag :p
                                 :content [[:i "big"] [:b "world"]]}))))
  (is-same "<div><a href='http://clojure.org/'><i>big</i><b>world</b></a>"
    (e/sniptest "<div>"
      [:div] (e/content (e/html [:a {:href "http://clojure.org/"} {:tag :i :content ["big"]}
                                 {:tag :b :content ["world"]}])))))

(deftest replace-vars-test
  (is-same "<div><h1>untouched ${name}<p class=hello>hello world"
           (e/sniptest "<div><h1>untouched ${name}<p class=\"${class}\">hello ${name}"
      #{[:p] [:p e/any-node]} (e/replace-vars {:name "world" :class "hello"})))
  (is (= ((e/replace-vars {:a "A" :b "B"}) "${a} ${b}")
        "A B"))
  (is (= ((e/replace-words {"Donald" "Mickey" "Duck" "Mouse"}) "Donald Duckling Duck")
        "Mickey Duckling Mouse")))


#?(:cljs (enable-console-print!))

(defn -main
  "Entry point for running tests (until *.cljc tools catch up)"
  []
  #?(:clj
     #_(println "orig:" (e/html-snippet "<div><h1>Title1<p>blabla<hr><h2>Title2<p>blibli"))
     (clojure.test/run-tests 'net.cgrand.enlive-html.test)
     :cljs
     (cljs.test/run-tests 'net.cgrand.enlive-html.test)))

;; Run tests at the root level, in CLJS
#?(:cljs
   (do (-main)
       (.exit js/phantom)))


(comment


  (are [_1 _2] (same? _2 (e/sniptest "<div><h1>T1<h2>T2<h3>T3<p>XXX" _1 (e/add-class "ok")))
      [[:h2 (e/rights :h3)]] "<div><h1>T1<h2 class=ok>T2<h3>T3<p>XXX"
      [[:h2 (e/rights :p)]] "<div><h1>T1<h2 class=ok>T2<h3>T3<p>XXX"
      [[:h2 (e/rights :h1)]] "<div><h1>T1<h2>T2<h3>T3<p>XXX")


  (are [_1 _2] (same? _2 (e/sniptest "<dl><dt>1<dt>2<dt>3<dt>4<dt>5" _1 (e/add-class "foo")))
          [[:dt (e/nth-child 2)]] "<dl><dt>1<dt class=foo>2<dt>3<dt>4<dt>5"
          [[:dt (e/nth-child 2 0)]] "<dl><dt>1<dt class=foo>2<dt>3<dt class=foo>4<dt>5"
          [[:dt (e/nth-child 3 1)]] "<dl><dt class=foo>1<dt>2<dt>3<dt class=foo>4<dt>5"
          [[:dt (e/nth-child -1 3)]] "<dl><dt class=foo>1<dt class=foo>2<dt class=foo>3<dt>4<dt>5"
          [[:dt (e/nth-child 3 -1)]] "<dl><dt>1<dt class=foo>2<dt>3<dt>4<dt class=foo>5")


  )