(ns slack-doi.core
  (:gen-class)
  (:require [org.httpkit.server :as server]
            [org.httpkit.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [environ.core :refer [env]]))

;; Look up a DOI

(defn doi-context [doi]
  (let [agency-url (str "http://api.crossref.org/v1/works/" doi "/agency")
        metadata-url (str "http://api.crossref.org/v1/works/" doi)]
    {:metadata (client/get metadata-url) :agency (client/get agency-url)}))

(defn error? [context]
  (let [{:keys [status error]} (-> context :agency deref)]
    (and (= status 500) (nil? error))))

(defn registered? [context]
  (not= (-> context :agency deref :status) 404))

(defn agency [context]
  (-> context :agency deref :body (json/read-str :key-fn keyword) :message :agency))

(defn message [doi context]
  (cond (error? context)
        {:text (str doi
                    "\nError when looking up data. Try again")}
        (and (registered? context) (= (-> context agency :id) "crossref"))
        (let [parsed-metadata (-> context
                                  :metadata deref :body
                                  (json/read-str :key-fn keyword)
                                  :message)]
          {:text (str doi
                      "\nCrossref DOI"
                      "\n" (-> parsed-metadata :title first)
                      "\n" (-> parsed-metadata :type)
                      (when (-> parsed-metadata :container-title empty? not)
                        (str " in " (-> parsed-metadata :container-title first)))
                      "\nMetadata Link: <http://api.crossref.org/v1/works/"
                      doi ">")})
        (registered? context)
        {:text (str doi
                    "\nRegistered by " (-> context agency :label))}
        :else
        {:text (str doi "\nNot registered by any RA")}))

(defn get-doi-info [doi]
  (->> doi doi-context (message doi)))

;; Routing

(defroutes app
  (GET "/slack" [text]
       {:status 200
        :headers {"Content-Type" "application/json"}
        :body (-> text str/trim get-doi-info json/write-str)})
  (route/not-found "No such route"))

;; Server gubbins

(def server-state (atom nil))
  
(defn start-server [port]
  (reset!
   server-state
   (server/run-server (-> app (wrap-defaults api-defaults)) {:port port})))

(defn stop-server []
  (when-not (nil? @server-state)
    (@server-state)
    (reset! server-state nil)))

(defn -main [& args]
  (start-server (-> :server-port env Integer/parseInt)))
