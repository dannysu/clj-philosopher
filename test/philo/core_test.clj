(ns philo.core-test
  (:use clojure.test
        philo.core))

(deftest fork-indices-test
         (is (= [0 4] (fork-indices 0 5)))
         (is (= [1 0] (fork-indices 1 5))))

(deftest grab-forks-test
         (is (= [0 nil nil nil 0]
                (dosync
                  (map deref (grab-forks 0 (map ref [nil nil nil nil nil]))))))
         (is (= [1 1 nil nil nil]
                (dosync
                  (map deref (grab-forks 0 (map ref [1 1 nil nil nil]))))))
         )

(deftest drop-forks-test
         (is (= [nil nil nil nil nil]
                (dosync
                  (map deref (drop-forks 0 (map ref [0 nil nil nil 0])))))))

;(deftest eat-test
         ;(is (= 0 1)))

;(deftest think-test
         ;(is (= 0 1)))
