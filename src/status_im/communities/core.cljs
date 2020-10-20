(ns status-im.communities.core
  (:require
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   [status-im.utils.fx :as fx]
   [status-im.chat.models :as models.chat]
   [status-im.data-store.chats :as data-store.chats]
   [status-im.ethereum.json-rpc :as json-rpc]))

(def access-no-membership 1)
(def access-invitation-only 2)
(def access-on-request 3)

(defn <-rpc [{:keys [description] :as c}]
  (let [identity (:identity description)]
    (-> c
        ;; TODO: to be removed
        (assoc :admin (:Admin c))
        (assoc-in [:description :identity] {:display-name (:display_name identity)
                                            :description (:description identity)}))))

(fx/defn handle-chats [cofx chats]
  (models.chat/ensure-chats cofx chats))

(fx/defn handle-fetched
  {:events [::fetched]}
  [{:keys [db]} communities]
  {:db (reduce (fn [db {:keys [id] :as community}]
                 (assoc-in db [:communities id] (<-rpc community)))
               db
               communities)})

(fx/defn handle-response [cofx response]
  (log/debug "RESPONSE" response)
  (fx/merge cofx
            (handle-chats (map #(-> %
                                    (data-store.chats/<-rpc)
                                    (dissoc :unviewed-messages-count))
                               (:chats response)))
            (handle-fetched (:organisations response))))

(fx/defn fetch [_]
  {::json-rpc/call [{:method "wakuext_organisations"
                     :params []
                     :on-success #(re-frame/dispatch [::fetched %])
                     :on-error #(do
                                  (log/error "failed to fetch communities" %)
                                  (re-frame/dispatch [::failed-to-fetch %]))}]})

(defn invite-user [community-id
                   user-pk
                   on-success-event
                   on-failure-event]
  {::json-rpc/call [{:method "wakuext_inviteUserToOrganisation"
                     :params [community-id
                              user-pk]
                     :on-success #(re-frame/dispatch [on-success-event %])
                     :on-error #(do
                                  (log/error "failed to invite-user community" %)
                                  (re-frame/dispatch [on-failure-event %]))}]})

(defn create [community-name
              community-description
              on-success-event
              on-failure-event]
  {::json-rpc/call [{:method "wakuext_createOrganisation"
                     :params [{:identity {:display_name community-name
                                          :description community-description}
                               :permissions {:access access-no-membership}}]
                     :on-success #(re-frame/dispatch [on-success-event %])
                     :on-error #(do
                                  (log/error "failed to create community" %)
                                  (re-frame/dispatch [on-failure-event %]))}]})

(defn create-channel [community-id
                      community-channel-name
                      community-channel-description
                      on-success-event
                      on-failure-event]
  {::json-rpc/call [{:method "wakuext_createOrganisationChat"
                     :params [community-id
                              {:identity {:display_name community-channel-name
                                          :description community-channel-description}
                               :permissions {:access access-no-membership}}]
                     :on-success #(re-frame/dispatch [on-success-event %])
                     :on-error #(do
                                  (log/error "failed to create community channel" %)
                                  (re-frame/dispatch [on-failure-event %]))}]})
