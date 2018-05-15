# boyfriend

Boyfriend (n): A brainfuck that keeps making the same mistakes.

Boyfriend is the Brainfuck language you know and love, with a couple of modern
conveniences.

## Features

Brainfuck operators +-,.[] work as you expect. 

The banner addition is functions. Enclose any expression(s) in `{}` to save
them as a function. The function is keyed to the **value of the tape at the
point where you defined that function**. Later, use `;` to call the function
keyed to the value at the tape's current position.

Boyfriend also supports copying the current value left or right to the next
cell on the tape. Use `(` and `)` respectively.

The tape has 1000 cells. Going outside the tape's index wraps the pointer
around to the other end.

Loops terminate with an error if the sentinel value is x < -500 or x > 500.

## Usage

From the Clojure repl:

```clojure
(require 'boyfriend.core :as bf)

(let [source-code "++.>>--."]
  (bf/eval-boyfriend source-code))
```

## License

Copyright © 2018 Robert Scherf

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
