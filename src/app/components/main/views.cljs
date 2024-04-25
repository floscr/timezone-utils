(ns app.components.main.views
  (:require
   [app.components.scheduler.views :as scheduler]
   [app.state.context :as state.context]
   [uix.core :as uix :refer [$ defui]]))

(defui main [_]
  ($ scheduler/root))

(defui provider []
  ($ state.context/provider {:middleware state.context/middleware}
     ($ main)))
