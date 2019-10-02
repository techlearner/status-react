(ns status-im.react-native.resources)

(def ui
  {:empty-hashtags      (js/require "./resources/images/ui/empty-hashtags.png")
   :empty-recent        (js/require "./resources/images/ui/empty-recent.png")
   :analytics-image     (js/require "./resources/images/ui/analytics-image.png")
   :welcome-image       (js/require "./resources/images/ui/welcome-image.png")
   :intro1              (js/require "./resources/images/ui/intro1.png")
   :intro2              (js/require "./resources/images/ui/intro2.png")
   :intro3              (js/require "./resources/images/ui/intro3.png")
   :sample-key          (js/require "./resources/images/ui/sample-key.png")
   :lock                (js/require "./resources/images/ui/lock.png")
   :tribute-to-talk     (js/require "./resources/images/ui/tribute-to-talk.png")
   :wallet-welcome      (js/require "./resources/images/ui/wallet-welcome.png")
   :hardwallet-card     (js/require "./resources/images/ui/hardwallet-card.png")
   :secret-keys         (js/require "./resources/images/ui/secret-keys.png")
   :keycard-lock        (js/require "./resources/images/ui/keycard-lock.png")
   :keycard             (js/require "./resources/images/ui/keycard.png")
   :keycard-logo        (js/require "./resources/images/ui/keycard-logo.png")
   :keycard-logo-blue   (js/require "./resources/images/ui/keycard-logo-blue.png")
   :keycard-logo-gray   (js/require "./resources/images/ui/keycard-logo-gray.png")
   :keycard-key         (js/require "./resources/images/ui/keycard-key.png")
   :keycard-empty       (js/require "./resources/images/ui/keycard-empty.png")
   :keycard-phone       (js/require "./resources/images/ui/keycard-phone.png")
   :keycard-connection  (js/require "./resources/images/ui/keycard-connection.png")
   :keycard-nfc-on      (js/require "./resources/images/ui/keycard-nfc-on.png")
   :keycard-wrong       (js/require "./resources/images/ui/keycard-wrong.png")
   :not-keycard         (js/require "./resources/images/ui/not-keycard.png")
   :status-logo         (js/require "./resources/images/ui/status-logo.png")
   :hold-card-animation (js/require "./resources/images/ui/hold-card-animation.gif")
   :warning-sign        (js/require "./resources/images/ui/warning-sign.png")
   :phone-nfc-on        (js/require "./resources/images/ui/phone-nfc-on.png")
   :phone-nfc-off       (js/require "./resources/images/ui/phone-nfc-off.png")
   :dapp-store          (js/require "./resources/images/ui/dapp-store.png")
   :ens-header          (js/require "./resources/images/ui/ens-header.png")})

(def loaded-images (atom {}))

(defn get-image [k]
  (if (contains? @loaded-images k)
    (get @loaded-images k)
    (get (swap! loaded-images assoc k
                (get ui k)) k)))