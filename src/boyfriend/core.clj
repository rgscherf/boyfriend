(ns boyfriend.core
  (:require [instaparse.core :as insta]))

;;;;;;;;;;;;;;;;;;;
;; THE GRAMMAR
;;;;;;;;;;;;;;;;;;;

(defn- parse-string
  []
  "S = EXPR+
  <EXPR> = INVOKE | FUNC | LOOP | PRINT | INCVAL | DECVAL | MOVRIGHT | MOVLEFT
  LOOP = <'['> EXPR+ <']'>
  FUNC = <'{'> EXPR+ <'}'>
  INVOKE = ';'
  PRINT = <'.'>
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
  [{:keys [tape ptr]}]
  (get tape ptr))

(defn- modify-tape-val*
  [tape idx func]
  (update tape idx func))

(defn- dec-pointer
  [{:keys [tape ptr] :as input}]
  (assoc input :tape (modify-tape-val* tape ptr dec)))

(defn- inc-pointer
  [{:keys [tape ptr] :as input}]
  (assoc input :tape (modify-tape-val* tape ptr inc)))

(defn- move-pointer*
  [tape oldidx func]
  (mod (func oldidx) (count tape)))

(defn- move-pointer-left
  [{:keys [tape ptr] :as input}]
  (assoc input :ptr (move-pointer* tape ptr dec)))

(defn- move-pointer-right
  [{:keys [tape ptr] :as input}]
  (assoc input :ptr (move-pointer* tape ptr inc)))

;;;;;;;;;;;;;;;;;;;
;; EVAL FNs
;;;;;;;;;;;;;;;;;;;

(defn- new-tape
  "Create a new BF env of [tape ptr]"
  [debug]
  (if debug
    {:ptr 0
     :tape (into [] (repeat 10 0))
     :bindings {}}
    {:ptr 500
     :tape (into [] (repeat 1000 0))
     :bindings {}}))

(defn- print-char
  [current]
  (-> current
      val-at-ptr
      (#(mod % 127))
      Character/toChars
      String/valueOf
      print)
  current)

(declare do-loop)
(declare intern-fn)
(declare invoke-fn)
(defn- single-expr
  "Evaluate a single BF expression, taking and receiving a [tape ptr] env."
  [current [expr-type & exprs]]
  (cond
    (= expr-type :LOOP) (do-loop current exprs)
    (= expr-type :FUNC) (intern-fn current exprs)
    (= expr-type :INVOKE) (invoke-fn current)
    (= expr-type :PRINT) (print-char current)
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
  (parse-exprs* (new-tape true) (rest expr-coll)))

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
  [{:keys [tape ptr bindings] :as current}  loop-exprs]
  (loop [exprs loop-exprs tape tape constant-ptr ptr loop-bindings bindings]
    (let [ptrval (val-at-ptr current)]
      (cond
        (= 0 ptrval)
        [tape ptr]
        (or (> -500 ptrval) (> ptrval 500))
        (throw (Exception. (str "The pointer val was too big or too small! Exiting to prevent infinite loop. Val was " ptrval)))
        :default
        (let [new-env (parse-exprs-w-env {:tape tape :ptr constant-ptr :bindings loop-bindings}
                                         exprs)
              new-bindings (:bindings new-env)
              new-tape (:tape new-env)]
          (recur exprs new-tape constant-ptr new-bindings))))))

(defn- intern-fn
  "When a fn definition is encountered, save its exprs in the env with key = current tape value."
  [{:keys [bindings] :as current} fn-exprs]
  (let [envkey (val-at-ptr current)]
    (assoc current :bindings (assoc bindings envkey fn-exprs))))

(defn- lookup-fn
  "Look up function for current pointer value.
  If no fn exists for that value, throw an error."
  [env]
  (let [location (val-at-ptr env)
        result (get (:bindings env) location)]
    (if result
      result
      (throw (Exception. (str "No function stored at ptr location " location ". Current env map: " env))))))

(defn- invoke-fn
  "Lookup the current value at pointer and then apply its fn. 
  If no fn for that key, throw an error."
  [current]
  (let [called-exprs (lookup-fn current)]
    (parse-exprs-w-env current called-exprs)))

(def testenv
  {:ptr 0, :tape [1 0 0], :bindings {1 [[:DECVAL]]}})

(comment

  (eval-boyfriend ">>++>>")

  (printchar testenv)

  (String/valueOf (Character/toChars 65))

  (invoke-fn testenv)

  (lookup-fn testenv)

  (val-at-ptr {:ptr 0, :tape [1 0 0], :bindings {1 [[:DECVAL]]}})

  (parse-exprs-w-env {:ptr 0, :tape [1 0 0], :bindings {1 [[:DECVAL]]}} [[:DECVAL]])

  (eval-boyfriend "++{>++};;++{<<<-----}>++++;")

  (eval-boyfriend "+++..")

  (parser "++>{++}")

  "endcomment")
