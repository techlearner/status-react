(ns status-im.ui.screens.communities.views (:require-macros [status-im.utils.views :as views])
    (:require
     [reagent.core :as reagent]
     [re-frame.core :as re-frame]
     [quo.core :as quo]
     [status-im.i18n :as i18n]
     [status-im.utils.core :as utils]
     [status-im.utils.fx :as fx]
     [status-im.communities.core :as communities]
     [status-im.ui.screens.home.views :as home.views]
     [status-im.ui.components.list.views :as list]
     [status-im.ui.components.topbar :as topbar]
     [status-im.ui.components.colors :as colors]
     [status-im.ui.components.chat-icon.screen :as chat-icon.screen]
     [status-im.ui.components.toolbar :as toolbar]
     [status-im.ui.components.bottom-sheet.core :as bottom-sheet]
     [status-im.ui.components.react :as react]))

(defn hide-sheet-and-dispatch [event]
  (re-frame/dispatch [:bottom-sheet/hide])
  (re-frame/dispatch event))

(defn community-list-item [{:keys [id description]}]
  (let [identity (:identity description)]
    [quo/list-item
     {:icon                      [chat-icon.screen/chat-icon-view-chat-list
                                  id
                                  true
                                  (:display-name identity)
                                  ;; TODO: should be derived by id
                                  (or (:color identity)
                                      (rand-nth colors/chat-colors))
                                  false
                                  false]
      :title                     [react/view {:flex-direction :row
                                              :flex           1}
                                  [react/view {:flex-direction :row
                                               :flex           1
                                               :padding-right  16
                                               :align-items    :center}
                                   [quo/text {:weight              :medium
                                              :accessibility-label :community-name-text
                                              :ellipsize-mode      :tail
                                              :number-of-lines     1}
                                    (utils/truncate-str (:display-name identity) 30)]]]
      :title-accessibility-label :community-name-text
      :subtitle                  [react/view {:flex-direction :row}
                                  [react/view {:flex 1}
                                   [quo/text
                                    (utils/truncate-str (:description identity) 30)]]]
      :on-press                  #(do
                                    (re-frame/dispatch [:dismiss-keyboard])
                                    (re-frame/dispatch [:navigate-to :community id]))}]))

(views/defview communities []
  (views/letsubs [communities [:communities]]
    [react/view {:flex 1}
     [topbar/topbar {:title (i18n/label :t/communities)}]
     [react/scroll-view {:style                   {:flex 1}
                         :content-container-style {:padding-vertical 8}}]
     [list/flat-list
      {:key-fn                       :id
       :keyboard-should-persist-taps :always
       :data                         (vals communities)
       :render-fn                    (fn [community] [community-list-item community])}]
     [toolbar/toolbar
      {:show-border? true
       :center [quo/button {:on-press #(re-frame/dispatch [::create-pressed])}
                (i18n/label :t/create-a-community)]}]]))

(fx/defn create-pressed
  {:events [::create-pressed]}
  [cofx]
  (bottom-sheet/show-bottom-sheet cofx {:view :create-community}))

;; TODO: that's probably a better way to do this
(defonce community-id (atom nil))

(fx/defn invite-people-pressed
  {:events [::invite-people-pressed]}
  [cofx id]
  (reset! community-id id)
  (bottom-sheet/show-bottom-sheet cofx {:view :invite-people-community}))

(fx/defn create-channel-pressed
  {:events [::create-channel-pressed]}
  [cofx id]
  (reset! community-id id)
  (bottom-sheet/show-bottom-sheet cofx {:view :create-community-channel}))

(fx/defn community-created
  {:events [::community-created]}
  [cofx response]
  (fx/merge cofx
            {:dispatch [:bottom-sheet/hide]}
            (communities/handle-response response)))

(fx/defn people-invited
  {:events [::people-invited]}
  [cofx response]
  (fx/merge cofx
            {:dispatch [:bottom-sheet/hide]}
            (communities/handle-response response)))

(fx/defn community-channel-created
  {:events [::community-channel-created]}
  [cofx response]
  (fx/merge cofx
            {:dispatch [:bottom-sheet/hide]}
            (communities/handle-response response)))

(fx/defn create-confirmation-pressed
  {:events [::create-confirmation-pressed]}
  [cofx community-name community-description]
  (communities/create
   community-name
   community-description
   ::community-created
   ::failed-to-create-community))

(fx/defn create-channel-confirmation-pressed
  {:events [::create-channel-confirmation-pressed]}
  [cofx community-channel-name community-channel-description]
  (communities/create-channel
   @community-id
   community-channel-name
   community-channel-description
   ::community-channel-created
   ::failed-to-create-community-channel))

(fx/defn invite-people-confirmatino-pressed
  {:events [::invite-people-confirmation-pressed]}
  [cofx user-pk]
  (communities/invite-user
   @community-id
   user-pk
   ::people-invited
   ::failed-to-invite-people))

(defn valid? [community-name community-description]
  (and (not= "" community-name)
       (not= "" community-description)))

(defn create []
  (let [community-name (reagent/atom "")
        community-description (reagent/atom "")]
    (fn []
      [react/view {:style {:padding-left    16
                           :padding-right   8}}
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label          (i18n/label :t/name-your-community)
          :placeholder    (i18n/label :t/name-your-community-placeholder)
          :on-change-text #(reset! community-name %)
          :auto-focus     true}]]
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label           (i18n/label :t/give-a-short-description-community)
          :placeholder     (i18n/label :t/give-a-short-description-community)
          :multiline       true
          :number-of-lines 4
          :on-change-text  #(reset! community-description %)}]]

       [react/view {:style {:padding-top 20
                            :padding-horizontal 20}}
        [quo/button {:disabled  (not (valid? @community-name @community-description))
                     :on-press #(re-frame/dispatch [::create-confirmation-pressed @community-name @community-description])}
         (i18n/label :t/create)]]])))

(def create-sheet
  {:content create})

(defn create-channel []
  (let [channel-name (reagent/atom "")
        channel-description (reagent/atom "")]
    (fn []
      [react/view {:style {:padding-left    16
                           :padding-right   8}}
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label          (i18n/label :t/name-your-channel)
          :placeholder    (i18n/label :t/name-your-channel-placeholder)
          :on-change-text #(reset! channel-name %)
          :auto-focus     true}]]
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label           (i18n/label :t/give-a-short-description-channel)
          :placeholder     (i18n/label :t/give-a-short-description-channel)
          :multiline       true
          :number-of-lines 4
          :on-change-text  #(reset! channel-description %)}]]

       [react/view {:style {:padding-top 20
                            :padding-horizontal 20}}
        [quo/button {:disabled  (not (valid? @channel-name @channel-description))
                     :on-press #(re-frame/dispatch [::create-channel-confirmation-pressed @channel-name @channel-description])}
         (i18n/label :t/create)]]])))

(def create-channel-sheet
  {:content create-channel})

(defn invite-people []
  (let [user-pk (reagent/atom "")]
    (fn []
      [react/view {:style {:padding-left    16
                           :padding-right   8}}
       [react/view {:style {:padding-horizontal 20}}
        [quo/text-input
         {:label          (i18n/label :t/enter-user-pk)
          :placeholder    (i18n/label :t/enter-user-pk)
          :on-change-text #(reset! user-pk %)
          :auto-focus     true}]]
       [react/view {:style {:padding-top 20
                            :padding-horizontal 20}}
        [quo/button {:disabled  (= "" user-pk)
                     :on-press #(re-frame/dispatch [::invite-people-confirmation-pressed @user-pk])}
         (i18n/label :t/invite)]]])))

(def invite-people-sheet
  {:content invite-people})

(defn community-actions [id admin]
  [react/view
   (when admin
     [quo/list-item
      {:theme               :accent
       :title               (i18n/label :t/create-channel)
       :accessibility-label :community-create-channel
       :icon                :main-icons/check
       :on-press            #(hide-sheet-and-dispatch [::create-channel-pressed id])}])
   (when admin
     [quo/list-item
      {:theme               :accent
       :title               (i18n/label :t/invite-people)
       :accessibility-label :community-invite-people
       :icon                :main-icons/close
       :on-press            #(re-frame/dispatch [::invite-people-pressed id])}])])

(defn toolbar-content [id display-name color]
  [react/view {:style  {:flex           1
                        :align-items    :center
                        :flex-direction :row}}
   [react/view {:margin-right 10}
    [chat-icon.screen/chat-icon-view-toolbar
     id
     true
     display-name
     (or color
         (rand-nth colors/chat-colors))]]])

(defn topbar [id display-name color admin]
  [topbar/topbar
   {:content           [toolbar-content id display-name color]
    :navigation        {:on-press #(re-frame/dispatch [:navigate-to :home])}
    :right-accessories [{:icon                :main-icons/more
                         :accessibility-label :community-menu-button
                         :on-press
                         #(re-frame/dispatch [:bottom-sheet/show-sheet
                                              {:content (fn []
                                                          [community-actions id admin])
                                               :height  256}])}]}])

(views/defview community-channel-list [id]
  (views/letsubs [chats [:chats/by-community-id id]]
    [home.views/chats-list-2 chats false nil true]))

(defn community-channel-preview-list [chats]
  [react/view {:flex 1}
   [list/flat-list
    {:key-fn                       :chat-id
     :keyboard-should-persist-taps :always
     :data                         chats
     :render-fn                    (fn [chat] [community-list-item chat])}]])

(views/defview community [route]
  (views/letsubs [{:keys [id description joined admin]} [:communities/community (get-in route [:route :params])]]
    [react/view {:style {:flex 1}}
     [topbar
      id
      (get-in description [:identity :display-name])
      (get-in description [:identity :color])
      admin]
     (if joined
       [community-channel-list id]
       [community-channel-preview-list id (:chats description)])]))
