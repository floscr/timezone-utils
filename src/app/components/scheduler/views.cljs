(ns app.components.scheduler.views
  (:require
   ["@js-joda/core" :as js-joda]
   ["react-select-me$default" :as select]
   [ballpark.core :as bp]
   [app.utils.css.core :refer-macros [css]]
   [goog.string :as gstring]
   [js.Array]
   [tick.core :as t]
   [tick.locale-en-us]
   [tick.timezone]
   [uix.core :as uix :refer [$ defui]]
   [clojure.string :as str]))

;; Styles ----------------------------------------------------------------------

(css input-css []
     "flex"
     "h-9 w-full px-3 py-1"
     "text-sm text-primary shadow-sm rounded-md bg-background border border-solid border-input"
     ["&::placeholder" "text-muted-foreground"]
     ["&:disabled" "cursor-not-allowed opacity-50"]
     ["&:focus-visible" "ring-0"])

(css wrapper-css []
     "p-3"
     "bg-white rounded-md")

(css timeslots-wrapper-css []
     "relative flex flex-col center gap-3")

(css timeslot-list-css []
     "flex"
     "rounded-md overflow-hidden"
     ["li[data-type='overlapping']:first" {:background "red !important"}])

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

(css select-css []
     {"--color-tuna" "#373c43;"
      "--color-seashell" "#f1f1f1"}
     [".dd__wrapper" {:position "relative"
                      :max-width "300px"}]
     [".dd__opened" {:border-radius "3px"
                     :border-color "var(--color-tuna)"
                     :color "var(--color-tuna);"}]
     [".dd__selectControl" "flex items-center overflow-hidden"]
     [".dd__list" {:position "absolute"
                   :z-index 1
                   :background "white"
                   :overflow "auto"
                   "-webkit-overflow-scrolling" "touch"
                   :min-width "100%"
                   :border "1px solid var(--color-tuna)"
                   :border-radius "3px"
                   :box-shadow "0 3px 7px 0 rgba(0, 0, 0, 0.08)"
                   :will-change "transform"}]
     [".dd__list.dd__opened" "block"]
     [".dd__selected" "flex"]
     {".dd__option" {:padding "2px"
                     :white-space "nowrap"
                     :cursor "pointer"}}
     [".dd__option:hover" {:background "var(--color-seashell)"}])

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

(def zones (.getAvailableZoneIds (.-ZoneId js-joda)))

(defui timezones-select [{:keys []}]
  (let [[options set-options!] (uix/use-state zones)
        on-search (fn [query]
                    (if (empty? (str/trim query))
                      (set-options! options)
                      (-> (js.Array/filter zones (fn [zone] (bp/quick-score zone query)))
                          (set-options!))))]
    ($ :div {:class (select-css)}
       ($ select {:options options
                  :on-search on-search
                  :searchable true
                  :on-close #(set-options! zones)}))))

(defui time-slots [{:keys [hue times overlaps]}]
  ($ :ol {:class (timeslot-list-css)}
     (for [[time overlaps?] (map vector times overlaps)]
       (let [type (cond
                    overlaps? :overlapping
                    (work-hour? time) :active
                    :else :muted)]
         ($ :li {:data-type (name type)
                 :class (timeslot-item-css hue type)
                 :key (str time)}
            ($ :span (t/format (t/formatter "HH:mm") time)))))))

(defui root [{:keys []}]
  (let [my-timezones (day-hours "Europe/Vienna")
        ist-timezones (map #(t/in % "Asia/Kolkata") my-timezones)
        overlaps (work-hours-intersections [my-timezones ist-timezones])
        time-offset (time-percentage-of-current-day (t/time))]
    ($ :div {:class (wrapper-css)}
       ($ timezones-select)
       ($ :div {:class (timeslots-wrapper-css)}
          ($ :div {:class (time-marker-css)
                   :style {:left (str time-offset "%")}})
          ($ time-slots {:times my-timezones
                         :hue 78
                         :overlaps overlaps})
          ($ time-slots {:times ist-timezones
                         :hue 120
                         :overlaps overlaps})))))
