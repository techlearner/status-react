(ns status-im.chat.models.link-preview
  (:require [re-frame.core :as re-frame]
            [status-im.utils.fx :as fx]
            [status-im.multiaccounts.update.core :as multiaccounts.update]
            [status-im.native-module.core :as native-module]))

(fx/defn set-link-preview
  [{{:keys [multiaccount]} :db :as cofx} site enabled?]
  (fx/merge cofx
            (multiaccounts.update/multiaccount-update
             :link-previews-enabled-sites
             (if enabled?
               (conj (get multiaccount :link-previews-enabled-sites #{}) site)
               (disj (get multiaccount :link-previews-enabled-sites #{}) site))
             {})))

(fx/defn cache-link-preview-data
  [{{:keys [multiaccount]} :db :as cofx} site data]
  (multiaccounts.update/optimistic
   cofx
   :link-previews-cache
   (assoc (get multiaccount :link-previews-cache {}) site data)))

(fx/defn should-suggest-link-preview
  [{:keys [db] :as cofx} enabled?]
  (multiaccounts.update/multiaccount-update
   cofx
   :can-ask-to-preview-links (boolean enabled?)
   {}))

(re-frame/reg-fx
 ::get-link-preview-whitelist
 (fn []
   (native-module/link-preview-whitelist #(do
                                            (re-frame/dispatch [::link-preview-whitelist-received %])))))

(fx/defn request-link-preview-whitelist
  [_]
  {::get-link-preview-whitelist nil})

(fx/defn save-link-preview-whitelist
  {:events [::link-preview-whitelist-received]}
  [cofx whitelist]
  (fx/merge cofx
            (multiaccounts.update/multiaccount-update
             :link-previews-whitelist whitelist {})))