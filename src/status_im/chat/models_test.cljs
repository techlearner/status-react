(ns status-im.chat.models-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.utils.identicon :as identicon]
            [status-im.ethereum.json-rpc :as json-rpc]
            [status-im.utils.clocks :as utils.clocks]
            [status-im.chat.models :as chat]))

(deftest upsert-chat-test
  (testing "upserting a non existing chat"
    (let [chat-id        "some-chat-id"
          contact-name   "contact-name"
          chat-props     {:chat-id chat-id
                          :extra-prop "some"}
          cofx           {:now "now"
                          :db {:contacts/contacts {chat-id
                                                   {:name contact-name}}}}
          response      (chat/upsert-chat cofx chat-props nil)
          actual-chat   (get-in response [:db :chats chat-id])]
      (testing "it adds the chat to the chats collection"
        (is actual-chat))
      (testing "it adds the extra props"
        (is (= "some" (:extra-prop actual-chat))))
      (testing "it adds the chat id"
        (is (= chat-id (:chat-id actual-chat))))
      (testing "it pulls the name from the contacts"
        (is (= contact-name (:name actual-chat))))
      (testing "it sets the timestamp"
        (is (= "now" (:timestamp actual-chat))))
      (testing "it adds the contact-id to the contact field"
        (is (= chat-id (-> actual-chat :contacts first))))))
  (testing "upserting an existing chat"
    (let [chat-id        "some-chat-id"
          chat-props     {:chat-id chat-id
                          :name "new-name"
                          :extra-prop "some"}
          cofx           {:db {:chats {chat-id {:is-active true
                                                :name "old-name"}}}}
          response      (chat/upsert-chat cofx chat-props nil)
          actual-chat   (get-in response [:db :chats chat-id])]
      (testing "it adds the chat to the chats collection"
        (is actual-chat))
      (testing "it adds the extra props"
        (is (= "some" (:extra-prop actual-chat))))
      (testing "it updates existins props"
        (is (= "new-name" (:name actual-chat)))))))

(deftest add-public-chat
  (with-redefs [gfycat/generate-gfy (constantly "generated")
                identicon/identicon (constantly "generated")]
    (let [topic "topic"
          fx (chat/add-public-chat {:db {}} topic nil)
          chat (get-in fx [:db :chats topic])]
      (testing "it sets the name"
        (is (= topic (:name chat))))
      (testing "it sets the participants"
        (is (= #{} (:contacts chat))))
      (testing "it sets the chat-id"
        (is (= topic (:chat-id chat))))
      (testing "it sets the group-chat flag"
        (is (:group-chat chat)))
      (testing "it does not sets the public flag"
        (is (:public? chat))))))

(deftest clear-history-test
  (let [chat-id "1"
        cofx    {:db {:message-lists {chat-id [{:something "a"}]}
                      :chats {chat-id {:last-message            {:clock-value 10}
                                       :unviewed-messages-count 1}}}}]
    (testing "it deletes all the messages"
      (let [actual (chat/clear-history cofx chat-id)]
        (is (= {} (get-in actual [:db :messages chat-id])))))
    (testing "it deletes all the message groups"
      (let [actual (chat/clear-history cofx chat-id)]
        (is (= nil (get-in actual [:db :message-lists chat-id])))))
    (testing "it deletes unviewed messages set"
      (let [actual (chat/clear-history cofx chat-id)]
        (is (= 0 (get-in actual [:db :chats chat-id :unviewed-messages-count])))))
    (testing "it sets a deleted-at-clock-value equal to the last message clock-value"
      (let [actual (chat/clear-history cofx chat-id)]
        (is (= 10 (get-in actual [:db :chats chat-id :deleted-at-clock-value])))))
    (testing "it does not override the deleted-at-clock-value when there are no messages"
      (let [actual (chat/clear-history (update-in cofx
                                                  [:db :chats chat-id]
                                                  assoc
                                                  :last-message nil
                                                  :deleted-at-clock-value 100)
                                       chat-id)]
        (is (= 100 (get-in actual [:db :chats chat-id :deleted-at-clock-value])))))
    (testing "it set the deleted-at-clock-value to now the chat has no messages nor previous deleted-at"
      (with-redefs [utils.clocks/send (constantly 42)]
        (let [actual (chat/clear-history (update-in cofx
                                                    [:db :chats chat-id]
                                                    assoc
                                                    :last-message nil)
                                         chat-id)]
          (is (= 42 (get-in actual [:db :chats chat-id :deleted-at-clock-value]))))))
    (testing "it adds the relevant rpc calls"
      (let [actual (chat/clear-history cofx chat-id)]
        (is (::json-rpc/call actual))
        (is (= 2 (count (::json-rpc/call actual))))))))

(deftest remove-chat-test
  (let [chat-id "1"
        cofx    {:db {:messages {chat-id {"1" {:clock-value 1}
                                          "2" {:clock-value 10}
                                          "3" {:clock-value 2}}}
                      :chats {chat-id {:last-message {:clock-value 10}}}}}]
    (testing "it deletes all the messages"
      (let [actual (chat/remove-chat cofx chat-id)]
        (is (= {} (get-in actual [:db :messages chat-id])))))
    (testing "it sets a deleted-at-clock-value equal to the last message clock-value"
      (let [actual (chat/remove-chat cofx chat-id)]
        (is (= 10 (get-in actual [:db :chats chat-id :deleted-at-clock-value])))))
    (testing "it sets the chat as inactive"
      (let [actual (chat/remove-chat cofx chat-id)]
        (is (= false (get-in actual [:db :chats chat-id :is-active])))))
    (testing "it makes the relevant json-rpc calls"
      (let [actual (chat/remove-chat cofx chat-id)]
        (is (::json-rpc/call actual))
        (is (= 4 (count (::json-rpc/call actual))))))))

(deftest multi-user-chat?
  (let [chat-id "1"]
    (testing "it returns true if it's a group chat"
      (let [cofx {:db {:chats {chat-id {:group-chat true}}}}]
        (is (chat/multi-user-chat? cofx chat-id))))
    (testing "it returns true if it's a public chat"
      (let [cofx {:db {:chats {chat-id {:public? true :group-chat true}}}}]
        (is (chat/multi-user-chat? cofx chat-id))))
    (testing "it returns false if it's a 1-to-1 chat"
      (let [cofx {:db {:chats {chat-id {}}}}]
        (is (not (chat/multi-user-chat? cofx chat-id)))))))

(deftest group-chat?
  (let [chat-id "1"]
    (testing "it returns true if it's a group chat"
      (let [cofx {:db {:chats {chat-id {:group-chat true}}}}]
        (is (chat/group-chat? cofx chat-id))))
    (testing "it returns false if it's a public chat"
      (let [cofx {:db {:chats {chat-id {:public? true :group-chat true}}}}]
        (is (not (chat/group-chat? cofx chat-id)))))
    (testing "it returns false if it's a 1-to-1 chat"
      (let [cofx {:db {:chats {chat-id {}}}}]
        (is (not (chat/group-chat? cofx chat-id)))))))

(def test-db
  {:multiaccount {:public-key "me"}

   :messages {"status" {"4" {} "5" {} "6" {}}}
   :chats {"status" {:public? true
                     :group-chat true
                     :loaded-unviewed-messages-ids #{"6" "5" "4"}}
           "opened" {:loaded-unviewed-messages-ids #{}}
           "1-1"    {:loaded-unviewed-messages-ids #{"6" "5" "4"}}}})
