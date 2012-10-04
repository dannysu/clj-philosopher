(ns philo.core-test
  (:use clojure.test
        philo.core))

(deftest make-free-fork-test
         (is (= true
                (realized? (deref (make-free-fork))))))

(deftest make-taken-fork-test
         (is (= false
                (realized? (deref (make-taken-fork))))))

(deftest fork-indices-test
         (is (= [0 4] (fork-indices 0 5)))
         (is (= [1 0] (fork-indices 1 5))))

(deftest grab-forks-test
         (is (= [false true true true false]
                (dosync
                  (map free?
                       (grab-forks 0 (repeatedly 5 make-free-fork))))))
         (is (= [false false true true true]
                (let [busy-table (concat (repeatedly 2 make-taken-fork) (repeatedly 3 make-free-fork))]
                  (dosync
                    (map free?
                         ; provide a timeout for test purposes because grab-forks is expected to block
                         (grab-forks 0 busy-table 100)))))))

(deftest drop-forks-test
         (is (= [true true true true true]
                (let [busy-table (concat (repeatedly 2 make-taken-fork) (repeatedly 3 make-free-fork))]
                  (dosync
                    (map free?
                         (drop-forks 1 busy-table))))))
         (is (= [true true true false false]
                (let [busy-table (concat (repeatedly 3 make-free-fork) (repeatedly 2 make-taken-fork))]
                  (dosync
                    (map free?
                         (drop-forks 1 busy-table)))))))

(deftest eat-logging 
	(is 
		(= [2 :eating 200]
			(let [log (agent [])]
				(eat 2 log 200)
				(rest (first @log))))))

(deftest think-test
	(is 
		(= [2 :thinking 150]
			(let [log (agent [])]
				(think 2 log 150)
				(rest (first @log))))))

(deftest philosopher-test
	(is
		(= 20
			(let [log (agent [])]
				(philosopher 1 10 log (repeatedly 5 make-free-fork))
				(Thread/sleep 1000)
				(count @log)))))

(deftest dinner-test
	(is
		(= 50
			(let [log (agent [])]
				(dinner 5 5 log)
				(Thread/sleep 1000)
				(count @log)))))
