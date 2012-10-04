(ns philo.core)

(defn fork-indices
  [philo-id num-philo]
  [philo-id (mod (dec philo-id) num-philo)])

(defn grab-forks
  [philo-id table]
  (let [indices (fork-indices philo-id (count table))
        forks (map #(nth table %) indices)]
    (loop []
      (if (= [nil nil] (map deref forks))
        (doseq [f forks]
          (ref-set f philo-id))
        ;(println "oh well")
        ;(throw (Exception. "Couldn't get forks"))
        (let [p (promise)
              watch-fn (fn [k r o n] (deliver p nil))]
          (add-watch (first forks) nil watch-fn)
          (@p)
          (recur))))
    table))

(defn drop-forks
  [philo-id table]
  (let [indices (fork-indices philo-id (count table))
        forks (map #(nth table %) indices)]
    (when (= [philo-id philo-id] (map deref forks))
      (doseq [f forks]
        (ref-set f nil)))
    table))
