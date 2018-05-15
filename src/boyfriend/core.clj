(ns boyfriend.core
  (:require [instaparse.core :as insta]))

;;;;;;;;;;;;;;;;;;;
;; THE GRAMMAR
;;;;;;;;;;;;;;;;;;;

(defn- parse-string
  []
  "S = EXPR+
  <EXPR> = LOOP | PRINT | INCVAL | DECVAL | MOVRIGHT | MOVLEFT
  LOOP = <'['> EXPR+ <']'>
  PRINT = <','>
  INCVAL = <'+'>
  DECVAL = <'-'>
  MOVRIGHT = <'>'>
  MOVLEFT = <'<'>")

(def parser
  "Instantiate the bf parser"
  (insta/parser (parse-string)))

;;;;;;;;;;;;;;;;;;;
;; BASIC OPERATIONS
;;;;;;;;;;;;;;;;;;;

(defn- val-at-ptr
  [[tape ptr]]
  (get tape ptr))

(defn- modify-tape-val*
  [tape idx func]
  (update tape idx func))

(defn- dec-pointer
  [[tape pointer-idx]]
  [(modify-tape-val* tape pointer-idx dec)  pointer-idx])

(defn- inc-pointer
  [[tape pointer-idx]]
  [(modify-tape-val* tape pointer-idx inc) pointer-idx])

(defn- move-pointer*
  [tape oldidx func]
  (let [newidx (mod
                (func oldidx)
                (count tape))]
    [tape newidx]))

(defn- move-pointer-left
  [[tape pointer-idx]]
  (move-pointer* tape pointer-idx dec))

(defn- move-pointer-right
  [[tape pointer-idx]]
  (move-pointer* tape pointer-idx inc))

;;;;;;;;;;;;;;;;;;;
;; EVAL FNs
;;;;;;;;;;;;;;;;;;;

(defn- new-tape
  "Create a new BF env of [tape ptr]"
  []
  [(into [] (repeat 10 0)) 0])

(declare do-loop)
(defn- single-expr
  "Evaluate a single BF expression, taking and receiving a [tape ptr] env."
  [current [expr-type & exprs]]
  (cond
    (= expr-type :LOOP) (do-loop current exprs)
    (= expr-type :PRINT) (do
                           (print (val-at-ptr current))
                           current)
    (= expr-type :INCVAL) (inc-pointer current)
    (= expr-type :DECVAL) (dec-pointer current)
    (= expr-type :MOVRIGHT) (move-pointer-right current)
    (= expr-type :MOVLEFT) (move-pointer-left current)))

(defn- parse-exprs*
  "Parse a seq of BF expressions, passing along the env (which is [tape ptr])"
  [env expr-coll]
  (reduce single-expr env expr-coll))

(defn- parse-exprs-w-env
  [env expr-coll]
  (parse-exprs* env expr-coll))

(defn- parse-fresh-exprs
  [expr-coll]
  (parse-exprs* (new-tape) (rest expr-coll)))

(defn eval-boyfriend
  "Fully evaluate a boyfriend string, returning the tape and current pointer val."
  [input-str]
  (-> input-str parser parse-fresh-exprs))

;;;;;;;;;;;;;;;;;;;
;; LOOPING
;;;;;;;;;;;;;;;;;;;

(defn- do-loop
  "at the beginning of a LOOP, check whether tape[ptr] == 0.  If yes, return the tape.
  If no, run to the end of the loop and then test again with the resulting tape but initial pointer."
  [[init-tape ptr] loop-exprs]
  (loop [exprs loop-exprs tape init-tape constant-ptr ptr]
    (let [ptrval (val-at-ptr [tape constant-ptr])]
      (cond
        (= 0 ptrval)
        [tape ptr]
        (or (> -500 ptrval) (> ptrval 500))
        (throw (Exception. (str "The pointer val was too big or too small! Exiting to prevent infinite loop. Val was " ptrval)))
        :default
        (let [[new-tape _] (parse-exprs-w-env [tape constant-ptr] exprs)]
          (recur exprs new-tape constant-ptr))))))

(comment

  (parse-exprs-w-env [[1 0 1] 0] [[:DECVAL]])

  (val-at-ptr [[1 0 1] 0])

  (do-loop
   [[2 0 1] 0]
   [[:DECVAL] [:DECVAL]])

  (parse-exprs-w-env
   [[1 0 1] 0]
   [[:DECVAL] [:DECVAL]])

  (def loop-tape
    [[0 1 0 0 0 0 0] 1])

  (def loop-test
    [[:DECVAL "-"] [:DECVAL "-"] [:MOVRIGHT ">"] [:INCVAL "+"]])

  (do-loop loop-tape loop-test)

  (eval-bf "+++[,-]")

  "This is a comment.")
