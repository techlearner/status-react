(ns status-im.ui.screens.home.styles
  (:require [status-im.ui.components.colors :as colors]))

(def last-message-text
  {:flex        1
   :align-self  :stretch
   :line-height 22
   :color       colors/gray})

(def public-unread
  {:background-color colors/blue
   :border-radius    6
   :margin-right     5
   :margin-bottom    5
   :width            12
   :height           12})

(def datetime-text
  {:color          colors/text-gray
   :font-size      10
   :text-align     :right
   :letter-spacing 0.4
   :align-items    :center
   :line-height    12})

(defn chat-tooltip []
  {:align-items   :center
   :border-color  colors/gray-lighter
   :border-width  1
   :border-radius 16
   :margin        16
   :margin-bottom 68})

(def no-chats-text
  {:margin-top        50
   :margin-horizontal 16
   :line-height       22
   :text-align        :center})

(def welcome-view
  {:flex            1
   :justify-content :flex-end})

(def welcome-text
  {:typography :header
   :text-align :center})

(def welcome-blank-text
  {:font-size   15
   :width       270
   :line-height 22
   :text-align  :center
   :color       colors/gray})

(def welcome-text-description
  {:margin-top        16
   :margin-bottom     32
   :text-align        :center
   :margin-horizontal 40
   :color             colors/gray})

(def empty-chats-header-container
  {:align-items     :center
   :justify-content :center})

(defn hr-wrapper []
  {:position         :absolute
   :width            "100%"
   :height           1
   :top              10
   :background-color colors/gray-lighter})

(defn or-text []
  {:width            40
   :background-color colors/white
   :font-size        12
   :line-height      20
   :text-align       :center
   :color            colors/gray})

(def tags-wrapper
  {:margin-top      10
   :margin-bottom   18})

(defn close-icon-container []
  {:width            24
   :height           24
   :border-radius    12
   :background-color colors/gray
   :align-items      :center
   :justify-content  :center})

(def home-container
  (merge
   {:flex 1}))
