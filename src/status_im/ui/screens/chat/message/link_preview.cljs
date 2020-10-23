(ns status-im.ui.screens.chat.message.link-preview
  (:require [re-frame.core :as re-frame]
            [clojure.string :as string]
            [status-im.ui.components.react :as react]
            [quo.core :as quo]
            [status-im.utils.security :as security]
            [status-im.i18n :as i18n]
            [status-im.ui.screens.chat.message.styles :as styles]
            [status-im.react-native.resources :as resources]
            [status-im.chat.models.link-preview :as link-preview])
  (:require-macros [status-im.utils.views :refer [defview letsubs]]))

(defn merge-paragraphs-content [parsed-text]
  (reduce
   (fn [acc {:keys [type children]}]
     (if (= type "paragraph")
       (into [] (concat acc children))
       acc))
   []
   parsed-text))

(defn get-links-from-paragraphs-content [merged-paragraphs]
  (reduce
   (fn [acc {:keys [type destination]}]
     (if (= type "link")
       (conj acc destination)
       acc))
   []
   merged-paragraphs))

(defn get-link [parsed-text]
  (-> parsed-text
      (merge-paragraphs-content)
      (get-links-from-paragraphs-content)
      (first)))

(defn link-belongs-to-domain [link domain]
  (cond
    (string/starts-with? link (str "https://" domain)) true
    (string/starts-with? link (str "https://www." domain)) true
    :else false))

(defn link-previewable-context [link whitelist enabled-list]
  (let [domain-info (first (filter
                            #(link-belongs-to-domain link (:address %))
                            whitelist))]
    {:whitelisted (not (nil? domain-info))
     :enabled (contains? enabled-list (:title domain-info))}))

(defview link-preview-enable-request []
  [react/view styles/link-preview-request-wrapper
   [react/view {:margin 12}
    [react/image {:source (resources/get-theme-image :unfurl)
                  :style  styles/link-preview-request-image}]
    [quo/text {:size :small
               :align :center
               :style {:margin-top 6}}
     (i18n/label :t/enable-link-previews)]
    [quo/text {:size :small
               :color :secondary
               :align :center
               :style {:margin-top 2}}
     (i18n/label :t/once-enabled-share-metadata)]]
   [quo/separator]
   [quo/button {:on-press #(re-frame/dispatch [:navigate-to :link-preview-settings])
                :type     :secondary}
    (i18n/label :enable)]
   [quo/separator]
   [quo/button {:on-press #(re-frame/dispatch
                            [::link-preview/should-suggest-link-preview false])
                :type     :secondary}
    (i18n/label :t/dont-ask)]])

(defn load-and-cache-link-data [link])

(defview link-preview-loader [link outgoing]
  (letsubs [cache [:link-preview/cache]]
    (let [{:keys [site title thumbnailUrl] :as preview-data} (get cache link)]
      (if (not preview-data)
        (do
          (re-frame/dispatch
           [::link-preview/load-link-preview-data link])
          nil)

        [react/touchable-highlight
         {:on-press #(when (and (security/safe-link? link))
                       (re-frame/dispatch
                        [:browser.ui/message-link-pressed link]))}

         [react/view (styles/link-preview-wrapper outgoing)
          [react/image {:source              {:uri thumbnailUrl}
                        :style               (styles/link-preview-image outgoing)
                        :accessibility-label :member-photo}]
          [quo/text {:size :small
                     :style styles/link-preview-title}
           title]
          [quo/text {:size :small
                     :color :secondary
                     :style styles/link-preview-site}
           site]]]))))

(defview link-preview-wrapper [link outgoing]
  (letsubs
    [ask-user? [:link-preview/link-preview-request-enabled]
     whitelist [:link-preview/whitelist]
     enabled-sites   [:link-preview/enabled-sites]]
    (let [{:keys [whitelisted enabled]} (link-previewable-context link whitelist enabled-sites)]
      (when (and link whitelisted)
        (if enabled
          [link-preview-loader link outgoing]
          (when ask-user?
            [link-preview-enable-request]))))))