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

(css timeslot-item-css [hue kind]
     "flex center items-center"
     "text-sm font-medium select-none"
     "w-12 h-6"
     {"--pill" (case kind
                 :muted (gstring/format "oklch(80.93% 0.02 %s)" hue)
                 (gstring/format "oklch(80.93% 0.099 %s)" hue))
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

;; Components ------------------------------------------------------------------

(defn work-hour? [timestamp]
  (let [h (t/hour timestamp)]
    (and (>= h 8)
         (<= h 18))))

(defui time-slots [{:keys [hue times]}]
  ($ :ol {:class (timeslot-list-css)}
     (for [time times]
       ($ :li {:class (timeslot-item-css
                       hue
                       (if (work-hour? time) :active :muted))
               :key (str time)}
          ($ :span (t/format (t/formatter "HH:mm") time))))))

(defui root [{:keys []}]
  (let [my-timezones (day-hours "GMT+2")
        ist-timezones (->> (day-hours "UTC+02:00")
                           (map #(t/in % "UTC+05:30")))]
    ($ :div {:class (wrapper-css)}
       ($ time-slots {:times my-timezones
                      :hue "78.11"})
       ($ time-slots {:times ist-timezones
                      :hue "120.11"}))))
