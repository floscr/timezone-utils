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
     "p-3"
     "bg-white rounded-md overflow-hidden")

(css timeslots-wrapper-css []
     "relative flex flex-col center gap-3")

(css timeslot-list-css []
     "flex"
     "rounded-md overflow-hidden")

(css timeslot-item-css [hue kind]
     "flex center items-center"
     "text-sm font-medium select-none"
     "w-12 h-6"
     {"--pill" (case kind
                 :active (gstring/format "oklch(85% 0.09 %s)" hue)
                 :muted (gstring/format "oklch(85% 0.01 %s)" hue)
                 (gstring/format "oklch(81% 0.13 %s)" hue))
      :background "var(--pill)"
      :color "oklch(from var(--pill) calc(l - 0.4) c h)"
      "&:hover" {:background "oklch(from var(--pill) calc(l + 0.08) c h)"}})

(css time-marker-css []
     "absolute"
     {:width "1px"
      :top 0
      :bottom 0
      :background "red"})

;; Helpers ---------------------------------------------------------------------

(defn day-hours [timezone]
  (let [today (t/date)
        hours (->> (t/new-duration 1 :days)
                   (t/hours)
                   (range)
                   (eduction
                    (map #(gstring/format "%02d:00" %))
                    (map #(t/at today (t/time %)))
                    (map #(t/in % timezone))))]
    hours))

(defn in-seconds [time]
  (+ (* (t/hour time) 3600)
     (* (t/minute time) 60)
     (t/second time)))

(defn day-seconds []
  (->> (t/new-duration 1 :days)
       (t/seconds)))

(defn time-percentage-of-current-day [time]
  (-> (/ (in-seconds time) (day-seconds))
      (/ 1)
      (* 100)))

(defn logical-intersect [colls]
  (apply map (fn [& vals] (every? true? vals)) colls))

;; Logic -----------------------------------------------------------------------

(defn work-hour? [timestamp]
  (let [h (t/hour timestamp)]
    (and (>= h 8)
         (<= h 18))))

(defn work-hours-intersections
  "Returns a list of booleans that will mark every overlapping working hours as true."
  [times]
  (->> times
       (map (fn [hours] (map work-hour? hours)))
       (logical-intersect)))

;; Components ------------------------------------------------------------------

(defui time-slots [{:keys [hue times overlaps]}]
  ($ :ol {:class (timeslot-list-css)}
     (for [[time overlaps?] (map vector times overlaps)]
       ($ :li {:class (timeslot-item-css
                       hue
                       (cond
                         overlaps? :overlapping
                         (work-hour? time) :active
                         :else :muted))
               :key (str time)}
          ($ :span (t/format (t/formatter "HH:mm") time))))))

(defui root [{:keys []}]
  (let [my-timezones (day-hours "UTC+02:00")
        ist-timezones (map #(t/in % "UTC+05:30") my-timezones)
        overlaps (work-hours-intersections [my-timezones ist-timezones])
        time-offset (time-percentage-of-current-day (t/time))]
    ($ :div {:class (wrapper-css)}
       ($ :div {:class (timeslots-wrapper-css)}
          ($ :div {:class (time-marker-css)
                   :style {:left (str time-offset "%")}})
          ($ time-slots {:times my-timezones
                         :hue 78
                         :overlaps overlaps})
          ($ time-slots {:times ist-timezones
                         :hue 120
                         :overlaps overlaps})))))
