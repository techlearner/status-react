(ns status-im.communities.core
  (:require
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   [status-im.utils.fx :as fx]
   [status-im.constants :as constants]
   [status-im.chat.models :as models.chat]
   [status-im.data-store.chats :as data-store.chats]
   [status-im.ethereum.json-rpc :as json-rpc]))

(def access-no-membership 1)
(def access-invitation-only 2)
(def access-on-request 3)

(defn <-rpc [{:keys [description] :as c}]
  (let [identity (:identity description)]
    (-> c
        (assoc-in [:description :identity] {:display-name (:display_name identity)
                                            :description (:description identity)}))))
(fx/defn handle-chats [cofx chats]
  (models.chat/ensure-chats cofx chats))

(fx/defn handle-removed-chats [{:keys [db]} chat-ids]
  {:db (reduce (fn [db chat-id]
                 (update db :chats dissoc chat-id))
               db
               chat-ids)})

(fx/defn handle-community
  [{:keys [db]} {:keys [id] :as community}]
  {:db (assoc-in db [:communities id] (<-rpc community))})

(fx/defn handle-fetched
  {:events [::fetched]}
  [{:keys [db]} communities]
  {:db (reduce (fn [db {:keys [id] :as community}]
                 (assoc-in db [:communities id] (<-rpc community)))
               db
               communities)})

(fx/defn handle-response [cofx response]
  (log/info "RESPONSE" response)
  (fx/merge cofx
            (handle-removed-chats (:removedChats response))
            (handle-chats (map #(-> %
                                    (data-store.chats/<-rpc)
                                    (dissoc :unviewed-messages-count))
                               (:chats response)))
            (handle-fetched (:communities response))))

(fx/defn left
  {:events [::left]}
  [cofx response]
  (handle-response cofx response))

(fx/defn joined
  {:events [::joined]}
  [cofx response]
  (handle-response cofx response))

(fx/defn join
  {:events [::join]}
  [cofx community-id]
  {::json-rpc/call [{:method "wakuext_joinCommunity"
                     :params [community-id]
                     :on-success #(re-frame/dispatch [::joined %])
                     :on-error #(do
                                  (log/error "failed to join community" community-id %)
                                  (re-frame/dispatch [::failed-to-join %]))}]})

(fx/defn leave
  {:events [::leave]}
  [cofx community-id]
  {::json-rpc/call [{:method "wakuext_leaveCommunity"
                     :params [community-id]
                     :on-success #(re-frame/dispatch [::left %])
                     :on-error #(do
                                  (log/error "failed to leave community" community-id %)
                                  (re-frame/dispatch [::failed-to-leave %]))}]})

(fx/defn fetch [_]
  {::json-rpc/call [{:method "wakuext_communities"
                     :params []
                     :on-success #(re-frame/dispatch [::fetched %])
                     :on-error #(do
                                  (log/error "failed to fetch communities" %)
                                  (re-frame/dispatch [::failed-to-fetch %]))}]})

(fx/defn chat-created
  {:events [::chat-created]}
  [cofx community-id user-pk]
  {::json-rpc/call [{:method "wakuext_sendChatMessage"
                     :params [{:chatId user-pk
                               :text "Upgrade here to see an invitation to community"
                               :communityId community-id
                               :contentType constants/content-type-community}]
                     :on-success
                     #(re-frame/dispatch [:transport/message-sent % 1])
                     :on-failure #(log/error "failed to send a message" %)}]})

(fx/defn invite-user [cofx
                      community-id
                      user-pk
                      on-success-event
                      on-failure-event]

  (fx/merge cofx
            {::json-rpc/call [{:method "wakuext_inviteUserToCommunity"
                               :params [community-id
                                        user-pk]
                               :on-success #(re-frame/dispatch [on-success-event %])
                               :on-error #(do
                                            (log/error "failed to invite-user community" %)
                                            (re-frame/dispatch [on-failure-event %]))}]}
            (models.chat/upsert-chat {:chat-id user-pk
                                      :active (get-in cofx [:db :chats user-pk :active])}
                                     #(re-frame/dispatch [::chat-created community-id user-pk]))))

(defn create [community-name
              community-description
              on-success-event
              on-failure-event]
  {::json-rpc/call [{:method "wakuext_createCommunity"
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
  {::json-rpc/call [{:method "wakuext_createCommunityChat"
                     :params [community-id
                              {:identity {:display_name community-channel-name
                                          :description community-channel-description}
                               :permissions {:access access-no-membership}}]
                     :on-success #(re-frame/dispatch [on-success-event %])
                     :on-error #(do
                                  (log/error "failed to create community channel" %)
                                  (re-frame/dispatch [on-failure-event %]))}]})

(def no-membership-access 1)
(def invitation-only-access 2)
(def on-request-access 3)

(defn require-membership? [permissions]
  (println "PERMI" permissions)
  (not= no-membership-access (:access permissions)))

;; TODO: test this
(defn can-post? [{:keys [admin] :as community} pk local-chat-id]
  (let [chat-id (keyword (subs local-chat-id 68))]
    (or admin
        (and (not (require-membership? (get-in community [:description :permissions])))
             (not (require-membership? (get-in community [:description :chats chat-id :permissions]))))
        (get-in community [:description :chats chat-id :members pk]))))
