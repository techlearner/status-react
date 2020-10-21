(ns status-im.ui.screens.chat.image.preview.views
  (:require [status-im.ui.components.colors :as colors]
            [status-im.ui.components.react :as react]
            [re-frame.core :as re-frame]
            [quo.core :as quo]
            [quo.react-native :as rn]
            [quo.animated :as animated]
            [status-im.i18n :as i18n]
            [status-im.ui.components.icons.vector-icons :as icons]
            [status-im.ui.screens.chat.sheets :as sheets]
            [quo.components.safe-area :as safe-area]
            ["react-native-image-pan-zoom" :default pan-zoom]))

(defn preview-image []
  (let []
    (fn [{{:keys [content] :as message} :message
         visible                       :visible
         on-close                      :on-close
         dimensions                    :dimensions}]
      (let [{screen-width  :width
             screen-height :height} @(re-frame/subscribe [:dimensions/window])
            k                       (if (< screen-height (second dimensions))
                                      (/ (first dimensions) screen-width)
                                      (/ (second dimensions) screen-height))
            width                   (/ (first dimensions) k)
            height                  (/ (second dimensions) k)
            animated-y              (animated/value 0)
            on-move                 (fn [^js move]
                                      (when (= (.-type move) "onPanResponderMove")
                                        (animated/set-value animated-y (.-positionY move))))
            reset                   (fn []
                                      (animated/set-value animated-y 0))
            opacity                 (animated/interpolate animated-y {:inputRange  [0 (/ screen-height 2)]
                                                                      :outputRange [1 0]
                                                                      :extrapolate (:clamp animated/extrapolate)})]
        [rn/modal {:visible         visible
                   :on-dismiss      reset
                   :on-equest-close on-close
                   :transparent     true}
         [safe-area/consumer
          (fn [insets]
            [react/view {:style {:flex 1}}
             [animated/view {:style {:position         "absolute"
                                     :top              0
                                     :bottom           0
                                     :left             0
                                     :right            0
                                     :background-color colors/black-persist
                                     :opacity          opacity}}]
             [:> pan-zoom {:crop-width        screen-width
                           :crop-height       screen-height
                           :image-width       width
                           :image-height      height
                           :enable-swipe-down true
                           :on-move           on-move
                           :responder-release #(js/setTimeout reset 100)
                           :on-swipe-down     (fn []
                                                (on-close))
                           :useNativeDriver   true}
              [react/image {:source {:uri (:image content)}
                            :style  {:width  width
                                     :height height}}]]
             [react/view {:style {:position       "absolute"
                                  :left           0
                                  :right          0
                                  :bottom         0
                                  :padding-bottom (:bottom insets)}}
              [react/view {:flex-direction     :row
                           :padding-horizontal 8
                           :justify-content    :space-between
                           :align-items        :center}
               [react/view {:width 64}]
               [quo/button {:on-press   on-close
                            :type       :secondary
                            :text-style {:color colors/white-persist}}
                (i18n/label :t/close)]

               [react/touchable-highlight
                {:on-press #(re-frame/dispatch [:bottom-sheet/show-sheet
                                                {:content (sheets/image-long-press message true)
                                                 :height  64}])}
                [icons/icon :main-icons/more {:container-style {:width  24
                                                                :height 24
                                                                :margin 20}
                                              :color           colors/white-persist}]]]]])]]))))
