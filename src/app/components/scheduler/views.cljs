(ns app.components.scheduler.views
  (:require
   [app.utils.css.core :refer-macros [css]]
   [tick.core :as t]
   [tick.locale-en-us]
   [tick.timezone]
   [goog.string :as gstring]
   [uix.core :as uix :refer [$ defui]]))

;; Styles ----------------------------------------------------------------------

(css wrapper-css []
     "flex flex-col center gap-3"
     "p-3"
     "bg-white rounded-md overflow-hidden")

(css timeslot-list-css []
     "flex"
     "rounded-md overflow-hidden")

(css timeslot-item-css [kind]
     "flex center items-center"
     "text-sm font-medium select-none"
     "w-12 h-6"
     {"--pill" (case kind
                 :muted "oklch(80.93% 0.02 78.11)"
                 "oklch(80.93% 0.099 78.11)")
      :background "var(--pill)"
      :color "oklch(from var(--pill) calc(l - 0.4) c h)"
      "&:hover" {:background "oklch(from var(--pill) calc(l + 0.1) c h)"}})

;; Helpers ---------------------------------------------------------------------

(defn day-hours [timezone]
  (let [today (t/date)
        hours (->> (t/new-duration 1 :days)
                   (t/hours)
                   (range)
                   (map #(gstring/format "%02d:00" %))
                   (map #(t/at today (t/time %)))
                   (map #(t/in % timezone)))]
    hours))

(def hour-timestamp-formatter (t/formatter "HH:mm"))

(comment
  (-> (t/now) (t/hour))
  (-> (day-hours "GMT+2")
      (first))
  (let [a (-> (day-hours "GMT+2")
              (first))
        b (t/in a "UTC+05:30")]
    [a b])
  nil)

;; Components ------------------------------------------------------------------

(defui time-slots [{:keys [times]}]
  ($ :ol {:class (timeslot-list-css)}
     (for [time times]
       ($ :li {:class (timeslot-item-css (let [h (t/hour time)]
                                           (cond
                                             (< h 8) :muted
                                             (> h 18) :muted
                                             :else :active)))
               :key (str time)}
          ($ :span (t/format hour-timestamp-formatter time))))))

(defui root [{:keys []}]
  ($ :div {:class (wrapper-css)}
     ($ time-slots {:times (day-hours "GMT+2")})
     ($ time-slots {:times (->> (day-hours "UTC+02:00")
                                (map #(t/in % "UTC+05:30")))})))
