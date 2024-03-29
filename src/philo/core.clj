(ns philo.core
  (:use [clojure.tools.cli :only [cli]]
        [clojure.data.json :only [json-str]])
  (:gen-class))

(defn now [] (System/currentTimeMillis))

(defn make-free-fork
  []
  (ref (deliver (promise) :delivered)))

(defn make-taken-fork
  []
  (ref (promise)))

(defn fork-indices
  "Determine fork indices for a philosopher"
  [philo-id num-philo]
  [philo-id (mod (dec philo-id) num-philo)])

(def deref-x2 (comp deref deref))
(defn deref-x2-timeout
  [timeout]
  (fn [x] (deref (deref x) timeout nil)))

(def not-nil? (comp not nil?))

(def free? (comp realized? deref))
(def taken? (comp not free?))

(defn grab-forks
  "Thread-safe function to grab 2 forks needed by a philosopher"
  [philo-id table & [timeout]]
  (let [indices (fork-indices philo-id (count table))
        forks (map #(nth table %) indices)
        deref-fn (if (nil? timeout) deref-x2 (deref-x2-timeout timeout))]

    ; deref and potentially wait for all required forks to become available
    (let [fork1 (deref-fn (first forks))
          fork2 (deref-fn (second forks))]
      ; Claim the forks by swapping in new promises to put them back.
      ; It's a race for everyone! Whoever commits the transaction wins.
      ; Everyone else retries and gets blocked until promises are delivered.
      (when (and (not-nil? fork1) (not-nil? fork2))
        (doseq [f forks]
          (ref-set f (promise)))
        ))

    ; return resulting state of the world
    table))

(defn drop-forks
  [philo-id table]
  (let [indices (fork-indices philo-id (count table))
        forks (map #(nth table %) indices)]
    (when (= [true true] (map taken? forks))
      (doseq [f forks]
        (deliver (deref f) :delivered)))
    table))

(defn action [action philo-id log duration]
	(send-off log conj [(now) philo-id action duration])
	(Thread/sleep duration))

(def eat (partial action :eating))
(def think (partial action :thinking))

(defn philosopher [philo-id numtimes log table]
  (dotimes [_ numtimes]
    (think philo-id log (rand-int 200))
    (dosync 
      (grab-forks philo-id table))
    (eat philo-id log (rand-int 200))
    (dosync
      (drop-forks philo-id table))))

(defn dinner [philocount numtimes log]
	(let [table (repeatedly philocount make-free-fork)]
		(doall 
			(pmap (fn [philo-id] 
				(philosopher philo-id numtimes log table)) 
				(range philocount)))))

(defn run 
  "Run the simulation and print the output"
  [philocount times]
	(let [log (agent [])]
		  (println (str "Table of " philocount " philosophers, each eating " times " times" ))
		  (dinner philocount times log)
		  (Thread/sleep 1000)
		  (println (json-str @log)))
	(shutdown-agents))

(defn -main [& args]
	(let [[opts args banner]
		 	(cli args
			     ["-s" "--philocount" "number of philosophers/forks" :parse-fn #(Integer. %)] 
			     ["-n" "--times" "number of iterations each philosopher eats" :parse-fn #(Integer. %)])]
		(if
	        (and (:philocount opts) (:times opts))
      		(run (:philocount opts) (:times opts))
      		(println banner))))
