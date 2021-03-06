(in-ns 'game.core)

(def trash-program {:prompt "Choose a program to trash" :label "Trash a program"
                    :msg (msg "trash " (:title target))
                    :choices {:req #(= (:type %) "Program")}
                    :effect (effect (trash target {:cause :subroutine}))})

(def trash-hardware {:prompt "Choose a piece of hardware to trash" :label "Trash a piece of hardware"
                     :msg (msg "trash " (:title target))
                     :choices {:req #(= (:type %) "Hardware")}
                     :effect (effect (trash target {:cause :subroutine}))})

(def cards
  {"Accelerated Beta Test"
   {:optional {:prompt "Look at the top 3 cards of R&D?"
               :msg (msg (let [c (count (filter #(= (:type %) "ICE") (take 3 (:deck corp))))]
                           (str "install " c " ICE and trash " (- 3 c) " cards")))
               :effect (req (doseq [c (take 3 (:deck corp))]
                              (if (= (:type c) "ICE")
                                (corp-install state side c nil {:no-install-cost true :rezzed true})
                                (trash state side c))))}}

   "Access to Globalsec"
   {:effect (effect (gain :link 1)) :leave-play (effect (lose :link 1))}

   "Account Siphon"
   {:effect (effect (run :hq {:replace-access
                              {:msg (msg "force the Corp to lose " (min 5 (:credit corp))
                                         " [Credits], gain " (* 2 (min 5 (:credit corp)))
                                         " [Credits] and take 2 tags")
                               :effect (effect (gain :tag 2 :credit (* 2 (min 5 (:credit corp))))
                                               (lose :corp :credit (min 5 (:credit corp))))}} card))}

   "Activist Support"
   {:events
    {:corp-turn-begins {:req (req (not tagged)) :msg "take 1 tag" :effect (effect (gain :runner :tag 1))}
     :runner-turn-begins {:req (req (zero? (:bad-publicity corp))) :msg "give the Corp 1 bad publicity"
                          :effect (effect (gain :corp :bad-publicity 1))}}}

   "Adjusted Chronotype"
   {:events {:runner-loss {:req (req (some #{:click} target)) :once :per-turn
                           :msg "gain [Click]" :effect (effect (gain :runner :click 1))}}}

   "Adonis Campaign"
   {:data {:counter 12}
    :events {:corp-turn-begins {:msg "gain 3 [Credits]" :counter-cost 3
                                :effect (req (gain state :corp :credit 3)
                                             (when (zero? (:counter card)) (trash state :corp card)))}}}

   "Aesops Pawnshop"
   {:abilities [{:msg (msg "trash " (:title target) " and gain 3 [Credits]")
                 :choices {:req #(= (first (:zone %)) :rig)}
                 :effect (effect (gain :credit 3) (trash target))}]}

   "Aggressive Negotiation"
   {:req (req (:scored-agenda corp-reg)) :prompt "Choose a card" :choices (req (:deck corp))
    :effect (effect (move target :hand) (shuffle! :deck))}

   "Aggressive Secretary"
   {:advanceable :always
    :access
    {:optional
     {:req (req installed) :prompt "Pay 2 [Credits] to use Aggressive Secretary ability?"
      :cost [:credit 2]
      :effect (req (let [agg card]
                     (resolve-ability
                       state side (assoc (assoc-in trash-program [:choices :max] (req (:advance-counter agg)))
                                    :effect (effect (trash-cards targets))) agg nil)))}}}

   "Akamatsu Mem Chip"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))}


   "Akitaro Watanabe"
   {:events {:pre-rez {:req (req (and (= (:type target) "ICE")
                                        (= (card->server state card) (card->server state target))))
                       :effect (effect (rez-cost-bonus -2))}}}

   "Alix T4LB07"
   {:events {:corp-install {:effect (effect (add-prop card :counter 1))}}
    :abilities [{:cost [:click 1] :label "Gain 2 [Credits] for each counter on Alix T4LB07"
                 :msg (msg "gain " (* 2 (:counter card)) " [Credits]")
                 :effect (effect (gain :credit (* 2 (:counter card))) (trash card))}]}

   "All-nighter"
   {:abilities [{:cost [:click 1] :effect (effect (trash card {:cause :ability-cost}) (gain :click 2))
                 :msg "gain [Click][Click]"}]}

   "Allele Repression"
   {:advanceable :always
    :abilities [{:label "Swap 1 cards in HQ and Archives for each advancement token"
                 :effect (effect (trash card))
                 :msg (msg "Swap " (:advance-counter card) " cards in HQ and Archives")}]}

   "Amped Up"
   {:effect (effect (gain :click 3) (damage :brain 1 {:unpreventable true :card card}))}

   "Analog Dreamers"
   {:abilities [{:cost [:click 1] :msg "make a run on R&D"
                 :effect (effect (run :rd {:replace-access
                                           {:prompt "Choose a card to shuffle into R&D"
                                            :choices {:req #(and (not (= (:type %) "ICE"))
                                                                 (not (:rezzed %))
                                                                 (not (:advance-counter %)))}
                                            :effect (req (move state :corp target :deck)
                                                         (shuffle! state :corp :deck)
                                                         (swap! state update-in [:runner :prompt] rest)
                                                         (handle-end-run state side)) ; remove the replace-access prompt
                                            :msg "shuffle a card into R&D"}} card))}]}

   "Andromeda: Dispossessed Ristie"
   {:effect (effect (gain :link 1) (draw 4)) :mulligan (effect (draw 4))}

   "Angel Arena"
   {:prompt "How many power counters?" :choices :credit :msg (msg "add " target " power counters")
    :effect (effect (set-prop card :counter target))
    :abilities [{:counter-cost 1 :msg "look at the top card of Stack"
                 :effect (req (when (zero? (:counter card)) (trash state :runner card)))
                 :optional {:prompt (msg "Add " (:title (first (:deck runner))) " to bottom of Stack?")
                            :msg "add the top card of Stack to the bottom"
                            :effect (req (move state side (first (:deck runner)) :deck))}}]}

   "Anonymous Tip"
   {:effect (effect (draw 3))}

   "Archived Memories"
   {:prompt "Choose a card from Archives" :choices (req (:discard corp))
    :effect (effect (move target :hand)
                    (system-msg (str "adds " (if (:seen target) (:title target) "a card") " to HQ")))}

   "Armand \"Geist\" Walker"
   {:effect (effect (gain :link 1))
    :events {:trash {:optional {:req (req (and (= side :runner) (= (second targets) :ability-cost)))
                                :prompt "Draw a card?" :msg (msg "draw a card") :effect (effect (draw 1))}}}}

   "Armitage Codebusting"
   {:data {:counter 12}
    :abilities [{:cost [:click 1] :counter-cost 2 :msg "gain 2 [Credits]"
                 :effect (req (gain state :runner :credit 2)
                              (when (zero? (:counter card)) (trash state :runner card)))}]}

   "AstroScript Pilot Program"
   {:data {:counter 1}
    :abilities [{:counter-cost 1 :msg (msg "place 1 advancement token on "
                                           (if (:rezzed target) (:title target) "a card"))
                 :choices {:req #(or (= (:advanceable %) "always")
                                     (and (= (:advanceable %) "while-rezzed") (:rezzed %))
                                     (= (:type %) "Agenda"))}
                 :effect (effect (add-prop target :advance-counter 1))}]}

   "Argus Security: Protection Guaranteed"
   {:events {:agenda-stolen
             {:prompt "Take 1 tag or suffer 2 meat damage?"
              :choices ["1 tag" "2 meat damage"] :player :runner
              :msg "make the Runner take 1 tag or suffer 2 meat damage"
              :effect (req (if (= target "1 tag")
                             (do (gain state :runner :tag 1) (system-msg state side "takes 1 tag"))
                             (do (damage state :runner :meat 2 {:unboostable true :card card})
                                 (system-msg state side "suffers 2 meat damage"))))}}}

   "Ash 2X3ZB9CY"
   {:abilities [{:label "Trace 4 - Prevent the Runner from accessing cards other than Ash 2X3ZB9CY"
                 :trace {:base 4
                         :effect (req (max-access state side 0)
                                      (let [ash card]
                                        (swap! state update-in [:run :run-effect]
                                               #(assoc % :successful-run
                                                       {:effect (effect (handle-access [ash])) :card ash}))))
                         :msg "prevent the Runner from accessing cards other than Ash 2X3ZB9CY"}}]}

   "Astrolabe"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))
    :events {:server-created {:msg "draw 1 card" :effect (effect (draw :runner))}}}

   "Au Revoir"
   {:events {:jack-out {:effect (effect (gain :credit 1)) :msg "gain 1 [Credits]"}}}

   "Autoscripter"
   {:events {:runner-install {:req (req (and (= (:active-player @state) :runner)
                                             (has? target :type "Program")))
                              :once :per-turn :msg "gain [Click]" :effect (effect (gain :click 1))}
             :unsuccessful-run {:effect (effect (trash card)
                                                (system-msg "Autoscripter is trashed"))}}}

   "Bank Job"
   {:data {:counter 8}
    :abilities [{:label "Take any number of [Credits] on Bank Job"
                 :prompt "How many [Credits]?" :choices :counter :msg (msg "gain " target " [Credits]")
                 :effect (req (gain state side :credit target)
                              (when (= target (:counter card)) (trash state :runner card)))}]}

   "Beach Party"
   {:effect (effect (gain :max-hand-size 5)) :leave-play (effect (lose :max-hand-size 5))
    :events {:runner-turn-begins {:msg "lose [Click]" :effect (effect (lose :click 1))}}}

   "Beanstalk Royalties"
   {:effect (effect (gain :credit 3))}

   "Bernice Mai"
   {:events {:successful-run {:req (req this-server)
                              :trace {:base 5 :msg "give the Runner 1 tag"
                                      :effect (effect (gain :runner :tag 1))}}}}

   "Bioroid Efficiency Research"
   {:choices {:req #(and (= (:type %) "ICE") (has? % :subtype "Bioroid") (not (:rezzed %)))}
    :msg (msg "rez " (:title target) " at no cost")
    :effect (effect (rez target {:no-cost true})
                    (host (get-card state target) (assoc card :zone [:discard] :seen true)))}

   "Blue Level Clearance"
   {:effect (effect (gain :credit 5) (draw 2))}

   "Blue Sun: Powering the Future"
   {:abilities [{:msg (msg "add " (:title target) " to HQ and gain " (:cost target) " [Credits]")
                 :choices {:req #(:rezzed %)}
                 :effect (effect (gain :credit (:cost target)) (move target :hand))}]}

   "Big Brother"
   {:req (req tagged) :effect (effect (gain :runner :tag 2))}

   "Biotic Labor"
   {:effect (effect (gain :click 2))}

   "Blackguard"
   {:effect (effect (gain :memory 2)) :leave-play (effect (lose :memory 2))
    :events {:expose {:msg (msg "attempt to force the rez of " (:title target))
                      :effect (effect (rez :corp target))}}}

   "Blackmail"
   {:req (req (> (:bad-publicity corp) 0)) :prompt "Choose a server" :choices (req servers)
    :effect (effect (run target))}

   "Borrowed Satellite"
   {:effect (effect (gain :link 1 :max-hand-size 1))
    :leave-play (effect (lose :link 1 :max-hand-size 1))}

   "Box-E"
   {:effect (effect (gain :memory 2 :max-hand-size 2))
    :leave-play (effect (lose :memory 2 :max-hand-size 2))}

   "Bifrost Array"
   {:req (req (not (empty? (filter #(not= (:title %) "Bifrost Array") (:scored corp)))))
    :msg (msg "trigger the score ability on " (:title target))
    :prompt "Choose an agenda to trigger"
    :choices (req (filter #(not= (:title %) "Bifrost Array") (:scored corp)))
    :effect (effect (card-init target))}

   "Bishop"
   {:abilities [{:label "Host Bishop on a piece of ICE" :cost [:click 1]
                 :choices {:req #(and (= (:type %) "ICE")
                                      (= (last (:zone %)) :ices)
                                      (not (some (fn [c] (has? c :subtype "Caïssa")) (:hosted %))))}
                 :msg (msg "host it on " (if (:rezzed target) (:title target) "a piece of ICE"))
                 :effect (effect (host target card))}]
    :events {:pre-ice-strength
             {:req (req (and (= (:cid target) (:cid (:host card))) (:rezzed target)))
              :effect (effect (ice-strength-bonus -2))}}}

   "Brain Cage"
   {:effect (effect (damage :brain 1 {:card card}) (gain :max-hand-size 3))
    :leave-play (effect (lose :max-hand-size 3))}

   "Brain-Taping Warehouse"
   {:events {:pre-rez
             {:req (req (and (= (:type target) "ICE") (has? target :subtype "Bioroid")))
              :effect (effect (rez-cost-bonus (- (:click runner))))}}}

   "Braintrust"
   {:effect (effect (set-prop card :counter (quot (- (:advance-counter card) 3) 2)))
    :events {:pre-rez
             {:req (req (= (:type target) "ICE"))
              :effect (effect (rez-cost-bonus (- (:counter (get-card state card)))))}}}

   "Breaker Bay Grid"
   {:events {:pre-rez {:req (req (= (:zone card) (:zone target)))
                       :effect (effect (rez-cost-bonus -5))}}}

   "Breaking News"
   {:effect (effect (gain :runner :tag 2)) :msg "give the Runner 2 tags"
    :end-turn {:effect (effect (lose :runner :tag 2)) :msg "make the Runner lose 2 tags"}}

   "Bribery"
   {:prompt "How many [Credits]?" :choices :credit
    :msg (msg "increase the rez cost of the 1st unrezzed ice approached by " target " [Credits]")
    :effect (effect (resolve-ability {:prompt "Choose a server" :choices (req servers)
                                      :effect (effect (run target nil card))} card nil))}

   "Broadcast Square"
   {:abilities [{:label "Trace 3 - Avoid taking a bad publicity"
                 :trace {:base 3 :msg "avoid taking a bad publicity"
                         :effect (effect (lose :bad-publicity 1))}}]}

   "Cache"
   {:abilities [{:counter-cost 1 :effect (effect (gain :credit 1)) :msg "gain 1 [Credits]"}]
    :data {:counter 3}}

   "Calling in Favors"
   {:effect (effect (gain :credit (count (filter (fn [c] (has? c :subtype "Connection"))
                                                 (get-in runner [:rig :resource])))))}

   "Capital Investors"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit 2)) :msg "gain 2 [Credits]"}]}

   "Caprice Nisei"
   {:abilities [{:msg "start a Psi game"
                 :psi {:not-equal {:msg "end the run" :effect (effect (end-run))}}}]}

   "Career Fair"
   {:prompt "Choose a Resource to install"
    :choices (req (filter #(#{"Resource"} (:type %)) (:hand runner)))
    :effect  (effect
               (register-events {:pre-install
                                 {:effect (effect (install-cost-bonus -3) (unregister-events card))}}
                                (assoc card :zone '(:discard)))
               (runner-install target))
    :events {:pre-install nil}}

   "Celebrity Gift"
   {:choices {:max 5 :req #(and (:side % "Corp") (= (:zone %) [:hand]))}
    :msg (msg "reveal " (join ", " (map :title targets)) " and gain " (* 2 (count targets)) " [Credits]")
    :effect (effect (gain :credit (* 2 (count targets))))}

   "Cerebral Cast"
   {:psi {:not-equal {:player :runner :prompt "Take 1 tag or 1 brain damage?"
                      :choices ["1 tag" "1 brain damage"] :msg (msg "The Runner takes " target)
                      :effect (req (if (= target "1 tag")
                                     (gain state side :tag 1)
                                     (damage state side :brain 1 {:card card})))}}}

   "Cerebral Imaging: Infinite Frontiers"
   {:effect (req (add-watch state :cerebral-imaging
                            (fn [k ref old new]
                              (let [credit (get-in new [:corp :credit])]
                                (when (not= (get-in old [:corp :credit]) credit)
                                  (swap! ref assoc-in [:corp :max-hand-size] credit))))))}

   "Cerebral Overwriter"
   {:advanceable :always
    :access {:optional {:req (req installed)
                        :prompt "Pay 3 [Credits] to use Cerebral Overwriter ability?"
                        :cost [:credit 3] :msg (msg "do " (:advance-counter card) " brain damage")
                        :effect (effect (damage :brain (:advance-counter card) {:card card}))}}}

   "Chairman Hiro"
   {:effect (effect (lose :runner :max-hand-size 2))
    :leave-play (effect (gain :runner :max-hand-size 2))
    :trash-effect {:req (req (:access @state))
                   :effect (effect (move :runner card :scored) (gain :runner :agenda-point 2))}}

   "Chakana"
   {:leave-play (effect (update-all-advancement-costs))
    :events {:successful-run {:effect (effect (add-prop card :counter 1)) :req (req (= target :rd))}
             :pre-advancement-cost {:req (req (>= (get-virus-counters state side card) 3))
                                    :effect (effect (advancement-cost-bonus 1))}
             :counter-added
             {:req (req (or (= (:title target) "Hivemind") (= (:cid target) (:cid card))))
              :effect (effect (update-all-advancement-costs))}}}

   "Chaos Theory: Wünderkind"
   {:effect (effect (gain :memory 1))}

   "Character Assassination"
   {:prompt "Choose a resource to trash" :choices (req (get-in runner [:rig :resource]))
    :msg (msg "trash " (:title target)) :effect (effect (trash target))}

   "Chop Bot 3000"
   {:abilities [{:msg (msg "trash " (:title target))
                 :choices {:req #(= (first (:zone %)) :rig)}
                 :effect (effect (trash target)
                                 (resolve-ability
                                  {:prompt "Draw 1 card or remove 1 tag" :msg (msg (.toLowerCase target))
                                   :choices ["Draw 1 card" "Remove 1 tag"]
                                   :effect (req (if (= target "Draw 1 card")
                                                  (draw state side)
                                                  (lose state side :tag 1)))} card nil))}]}

   "Chrome Parlor"
   {:events
    {:pre-damage {:req (req (has? (second targets) :subtype "Cybernetic"))
                  :effect (effect (damage-prevent target Integer/MAX_VALUE))}}}

   "Chronos Project"
   {:effect (effect (move-zone :runner :discard :rfg))}

   "City Surveillance"
   {:events {:runner-turn-begins
             {:prompt "Pay 1 [Credits] or take 1 tag" :choices ["Pay 1 [Credits]" "Take 1 tag"]
              :player :runner :msg "make the Runner pay 1 [Credits] or take 1 tag"
              :effect (req (if-not (and (= target "Pay 1 [Credits]") (pay state side card :credit 1))
                             (do (gain state side :tag 1) (system-msg state side "takes 1 tag"))
                             (system-msg state side "pays 1 [Credits]")))}}}

   "Cloak"
   {:recurring 1}

   "Clone Chip"
   {:abilities [{:prompt "Choose a program to install" :msg (msg "install " (:title target))
                 :priority true
                 :choices (req (filter #(has? % :type "Program") (:discard runner)))
                 :effect (effect (trash card {:cause :ability-cost}) (runner-install target))}]}

   "Clone Retirement"
   {:msg "remove 1 bad publicity" :effect (effect (lose :bad-publicity 1))
    :stolen {:msg "force the Corp to take 1 bad publicity"
             :effect (effect (gain :corp :bad-publicity 1))}}

   "Closed Accounts"
   {:req (req tagged) :effect (effect (lose :runner :credit :all))}

   "Clot"
   {:events {:purge {:effect (effect (trash card))}}}

   "Code Siphon"
   {:effect (effect (run :rd
                      {:replace-access
                       {:prompt "Choose a program to install"
                        :msg (msg "Code Siphon to install " (:title target) " and take 1 tag")
                        :choices (req (filter #(and (has? % :type "Program")
                                                    (<= (:cost %) (:credit runner))) (:deck runner)))
                        :effect (effect (gain :tag 1)
                                        (runner-install target) (shuffle! :deck))}} card))}

   "Collective Consciousness"
   {:events {:rez {:req (req (= (:type target) "ICE")) :msg "draw 1 card"
                   :effect (effect (draw :runner))}}}

   "Comet"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))
    :events {:play-event
             {:optional {:prompt "Play another event?" :once :per-turn
                         :effect (effect (resolve-ability
                                          {:prompt "Choose an Event to play"
                                           :choices (req (filter #(has? % :type "Event") (:hand runner)))
                                           :msg (msg "play " (:title target))
                                           :effect (effect (play-instant target))} card nil))}}}}

   "Compromised Employee"
   {:recurring 1
    :events {:rez {:req (req (= (:type target) "ICE")) :msg "gain 1 [Credits]"
                   :effect (effect (gain :runner :credit 1))}}}

   "Commercialization"
   {:msg (msg "gain " (:advance-counter target) " [Credits]")
    :choices {:req #(has? % :type "ICE")} :effect (effect (gain :credit (:advance-counter target)))}

   "Constellation Protocol"
   {:events {:corp-turn-begins
             {:optional
              {:prompt "Move one advancement token between ICE?"
               :effect (effect
                         (resolve-ability
                           {:choices {:req #(and (= (:type %) "ICE") (:advance-counter %))}
                            :priority true
                            :effect (req
                                      (let [fr target]
                                           (resolve-ability
                                             state side
                                             {:priority true
                                              :prompt "Move to where?"
                                              :choices {:req #(and (= (:type %) "ICE")
                                                                   (not= (:cid fr) (:cid %))
                                                                   (or (= (:advanceable %) "always")
                                                                       (and (= (:advanceable %) "while-rezzed")
                                                                            (:rezzed %))))}
                                              :effect (effect (add-prop :corp target :advance-counter 1)
                                                              (add-prop :corp fr :advance-counter -1))} card nil)
                                           card nil))}
                           card nil))}}}}

   "Contract Killer"
   {:advanceable :always
    :abilities [{:label "Trash a connection" :cost [:click 1] :req (req (>= (:advance-counter card) 2))
                 :choices {:req #(has? % :subtype "Connection")}
                 :msg (msg "to trash " (:title target)) :effect (effect (trash card) (trash target))}
                {:cost [:click 1] :req (req (>= (:advance-counter card) 2))
                 :msg "do 2 meat damage" :effect (effect (trash card) (damage :meat 2 {:card card}))}]}

   "Corporate Shuffle"
   {:effect (effect (shuffle-into-deck :hand) (draw 5))}

   "Corporate Town"
   {:additional-cost [:forfeit]
    :events {:corp-turn-begins
             {:choices {:req #(and (= (:type %) "Resource"))} :msg (msg "trash " (:title target))
              :effect (effect (trash target))}}}

   "Corporate Troubleshooter"
   {:abilities [{:label "Add strength to a rezzed ICE protecting this server" :choices :credit
                 :prompt "How many credits?"
                 :effect (req (let [boost target]
                                (resolve-ability
                                 state side
                                 {:choices {:req #(and (has? % :type "ICE") (:rezzed %))}
                                  :msg (msg "add " boost " strength to " (:title target))
                                  :effect (req (update! state side (assoc card :troubleshooter-target target
                                                                          :troubleshooter-amount boost))
                                               (trash state side (get-card state card))
                                               (update-ice-strength state side target))} card nil)))}]
    :events {:pre-ice-strength nil :end-turn nil}
    :trash-effect
      {:effect (req (register-events
                      state side
                      (let [ct {:effect (req (unregister-events state side card)
                                             (update! state side (dissoc card :troubleshooter-target))
                                             (update-ice-strength state side (:troubleshooter-target card)))}]
                        {:pre-ice-strength
                          {:req (req (= (:cid target) (:cid (:troubleshooter-target card))))
                           :effect (effect (ice-strength-bonus (:troubleshooter-amount card)))}
                         :runner-turn-ends ct :corp-turn-ends ct}) card))}}

   "Corporate War"
   {:msg (msg (if (> (:credit corp) 6) "gain 7 [Credits]" "lose all credits"))
    :effect (req (if (> (:credit corp) 6)
                   (gain state :corp :credit 7) (lose state :corp :credit :all)))}

   "Cortez Chip"
   {:abilities [{:label "increase cost to rez a piece of ice by 2 [Credits]"
                 :prompt "Choose a piece of ice" :choices {:req #(and (not (:rezzed %)) (= (:type %) "ICE"))}
                 :effect (effect (update! (assoc card :cortez-target target))
                                 (trash (get-card state card) {:cause :ability-cost}))}]
    :trash-effect {:effect (effect (register-events {:pre-rez {:req (req (= (:cid target) (:cid (:cortez-target card))))
                                                               :effect (effect (rez-cost-bonus 2))}
                                                     :runner-turn-ends {:effect (effect (unregister-events card))}
                                                     :corp-turn-ends {:effect (effect (unregister-events card))}}
                                                    (get-card state card)))}
    :events {:pre-rez nil :runner-turn-ends nil :corp-turn-ends nil}}

   "Crash Space"
   {:prevent {:damage [:meat]}
    :recurring 2
    :abilities [{:label "Trash to prevent up to 3 meat damage"
                 :msg "prevent up to 3 meat damage"
                 :effect (effect (trash card {:cause :ability-cost}) (damage-prevent :meat 3))}]}

   "Crescentus"
   {:abilities [{:req (req current-ice) :msg (msg "derez " (:title current-ice))
                 :effect (effect (trash card {:cause :ability-cost}) (derez current-ice))}]}

   "Cybsoft MacroDrive"
   {:recurring 1}

   "Cyberdex Trial"
   {:effect (effect (purge))}

   "Cyberdex Virus Suite"
   {:access {:optional {:prompt "Purge viruses with Cyberdex Virus Suite?"
                        :msg (msg "purge viruses") :effect (effect (purge))}}
    :abilities [{:msg "purge viruses" :effect (effect (purge) (trash card))}]}

   "Cyberfeeder"
   {:recurring 1}

   "Cybernetics Division: Humanity Upgraded"
   {:effect (effect (lose :max-hand-size 1) (lose :runner :max-hand-size 1))}

   "CyberSolutions Mem Chip"
   {:effect (effect (gain :memory 2)) :leave-play (effect (lose :memory 2))}

   "D4v1d"
   {:data {:counter 3} :abilities [{:counter-cost 1 :msg "break 1 subroutine"}]}

   "Daily Business Show"
   {:events {:corp-draw
             {:msg "draw additional cards" :once :per-turn :once-key :daily-business-show
              :effect (req
                        (let [dbs (->> (:corp @state) :servers seq flatten (mapcat :content)
                                       (filter #(and (:rezzed %) (= (:title %) "Daily Business Show")))  count)
                              newcards (take dbs (:deck corp))
                              drawn (conj newcards (last (:hand corp)))]
                             (doseq [c newcards] (move state side c :hand))
                             (resolve-ability
                              state side
                              {:prompt (str "Choose " dbs " card" (if (> dbs 1) "s" "") " to add to the bottom of R&D")
                               :choices {:max dbs
                                         :req #(and (= (:zone %) [:hand]) (some (fn [c] (= (:cid c) (:cid %))) drawn))}
                               :msg (msg "add " dbs " card" (if (> dbs 1) "s" "") " to bottom of R&D")
                               :effect (req (doseq [c targets] (move state side c :deck)))} card targets)))}}}

   "Daily Casts"
   {:data {:counter 8}
    :events {:runner-turn-begins {:msg "gain 2 [Credits]" :counter-cost 2
                                  :effect (req (gain state :runner :credit 2)
                                               (when (zero? (:counter card)) (trash state :runner card)))}}}

   "Data Dealer"
   {:abilities [{:cost [:click 1 :forfeit] :effect (effect (gain :credit 9))
                 :msg (msg "gain 9 [Credits]")}]}

   "Data Folding"
   {:events {:runner-turn-begins {:req (req (>= (:memory runner) 2)) :msg "gain 1 [Credits]"
                                  :effect (effect (gain :credit 1))}}}

   "Data Leak Reversal"
   {:req (req (some #{:hq :rd :archives} (:successful-run runner-reg)))
    :abilities [{:req (req tagged) :cost [:click 1] :effect (effect (mill :corp))
                 :msg "force the Corp to trash the top card of R&D"}]}

   "Datasucker"
   {:events (let [ds {:effect (req (update! state side (dissoc card :datasucker-count)))}]
                 {:successful-run {:effect (effect (add-prop card :counter 1))
                                   :req (req (#{:hq :rd :archives} target))}
                  :pre-ice-strength {:req (req (and (= (:cid target) (:cid current-ice))
                                                    (:datasucker-count card)))
                                     :effect (req (let [c (:datasucker-count (get-card state card))]
                                                       (ice-strength-bonus state side (- c))))}
                  :pass-ice ds :run-ends ds})
    :abilities [{:counter-cost 1 :msg (msg "give -1 strength to " (:title current-ice))
                 :req (req current-ice)
                 :effect (req (update! state side (update-in card [:datasucker-count] (fnil #(+ % 1) 0)))
                              (update-ice-strength state side current-ice))}]}

   "Day Job"
   {:additional-cost [:click 3] :effect (effect (gain :credit 10))}

   "Decoy"
   {:abilities [{:msg "avoid 1 tag" :effect (effect (trash card {:cause :ability-cost}))}]}

   "Dedicated Response Team"
   {:events {:successful-run-ends {:req (req tagged) :msg "do 2 meat damage"
                                   :effect (effect (damage :meat 2 {:card card}))}}}

   "Dedicated Server"
   {:recurring 2}

   "Dedicated Technician Team"
   {:recurring 2}

   "Defective Brainchips"
   {:events {:pre-damage {:req (req (= target :brain)) :msg "to do 1 additional brain damage"
                          :once :per-turn :effect (effect (damage-bonus :brain 1))}}}

   "Déjà Vu"
   {:prompt "Choose a card to add to Grip" :choices (req (:discard runner))
    :msg (msg "add " (:title target) " to his Grip")
    :effect (req (move state side target :hand)
                 (when (has? target :subtype "Virus")
                   (resolve-ability state side
                                    {:prompt "Choose a virus to add to Grip"
                                     :msg (msg "add " (:title target) " to his Grip")
                                     :choices (req (filter #(has? % :subtype "Virus") (:discard runner)))
                                     :effect (effect (move target :hand))} card nil)))}

   "Demolition Run"
   {:prompt "Choose a server" :choices ["HQ" "R&D"] :effect (effect (run target))}

   "Desperado"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))
    :events {:successful-run {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Diversified Portfolio"
   {:effect (effect (gain :credit (count (filter #(not (empty? (:content %)))
                                                 (get-in corp [:servers :remote])))))}

   "Diesel"
   {:effect (effect (draw 3))}

   "Dinosaurus"
   {:abilities [{:req (req (empty? (:hosted card))) :cost [:click 1]
                 :prompt "Choose a non-AI icebreaker to install on Dinosaurus"
                 :choices (req (filter #(and (has? % :subtype "Icebreaker")
                                             (not (has? % :subtype "AI")))
                                       (:hand runner)))
                 :msg (msg "host " (:title target))
                 :effect (effect (gain :memory (:memoryunits target))
                                 (runner-install target {:host-card card})
                                 (update-breaker-strength target))}]
    :events {:pre-breaker-strength {:req (req (= (:cid target) (:cid (first (:hosted card)))))
                                    :effect (effect (breaker-strength-bonus 2))}}}

   "Dirty Laundry"
   {:prompt "Choose a server" :choices (req servers)
    :effect (effect (run target {:end-run {:req (req (:successful run)) :msg " gain 5 [Credits]"
                                           :effect (effect (gain :runner :credit 5))}} card))}

   "Djinn"
   {:abilities [{:label "Add a virus program to your Grip from your Stack"
                 :prompt "Choose a Virus" :msg (msg "adds " (:title target) " to his Grip")
                 :choices (req (filter #(and (= (:type %) "Program")
                                             (has? % :subtype "Virus"))
                                       (:deck runner)))
                 :cost [:click 1 :credit 1] :effect (effect (move target :hand) (shuffle! :deck))}
                {:label "Install a non Icebreaker program on Djin" :cost [:click 1]
                 :prompt "Choose a non Icebreaker program to install on Djin"
                 :choices (req (filter #(and (= (:type %) "Program")
                                             (not (has? % :subtype "Icebreaker"))
                                             (<= (:cost %) (:credit runner)))
                                       (:hand runner)))
                 :msg (msg "host " (:title target))
                 :effect (effect (gain :memory (:memoryunits target))
                                 (runner-install target {:host-card card}))}]}

   "Director Haas"
   {:effect (effect (gain :click 1 :click-per-turn 1)) :leave-play (effect (lose :click-per-turn 1))
    :trash-effect {:req (req (:access @state))
                   :effect (effect (move :runner card :scored) (gain :runner :agenda-point 2))}}

   "Docklands Crackdown"
   {:abilities [{:cost [:click 2] :msg "add 1 power counter" :effect (effect (add-prop card :counter 1))}]}

   "Domestic Sleepers"
   {:abilities [{:cost [:click 3] :msg "place 1 agenda counter on Domestic Sleepers"
                 :effect (req (when (zero? (:counter card))
                                (gain-agenda-point state side 1))
                              (set-prop state side card :counter 1 :agendapoints 1))}]}

   "Doppelgänger"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))
    :events {:successful-run-ends
             {:optional
              {:once :per-turn :prompt "Use Doppelgänger to run again?" :player :runner
               :effect (effect (resolve-ability {:prompt "Choose a server" :choices (req servers)
                                                 :msg (msg "to make a run on " target)
                                                 :effect (effect (run target))} card targets))}}}}

   "Dorm Computer"
   {:data {:counter 4}
    :abilities [{:counter-cost 1 :cost [:click 1]
                 :prompt "Choose a server" :choices (req servers)
                 :msg "make a run and avoid all tags for the remainder of the run"
                 :effect (effect (run target))}]}

   "Duggars"
   {:abilities [{:cost [:click 4] :effect (effect (draw 10)) :msg "draw 10 cards"}]}

   "Drive By"
   {:choices {:req #(and (= (second (:zone %)) :remote)
                         (= (last (:zone %)) :content)
                         (not (:rezzed %)))}
    :msg (msg "expose " (:title target) (when (#{"Asset" "Upgrade"} (:type target)) " and trash it"))
    :effect (req (expose state side target)
                 (when (#{"Asset" "Upgrade"} (:type target))
                   (trash state side (assoc target :seen true))))}

   "Dyson Fractal Generator"
   {:recurring 1}

   "Dyson Mem Chip"
   {:effect (effect (gain :link 1 :memory 1)) :leave-play (effect (lose :link 1 :memory 1))}

   "e3 Feedback Implants"
   {:abilities [{:cost [:credit 1] :msg "break 1 additional subroutine"}]}

   "Earthrise Hotel"
   {:data {:counter 3}
    :events {:runner-turn-begins {:msg "draw 2 cards" :counter-cost 1
                                  :effect (req (draw state :runner 2)
                                               (when (zero? (:counter card))
                                                 (trash state :runner card)))}}}

   "Early Bird"
   {:prompt "Choose a server" :choices (req servers) :effect (effect (gain :click 1) (run target))}

   "Easy Mark"
   {:effect (effect (gain :credit 3))}

   "Edge of World"
   {:access {:optional
             {:req (req installed) :cost [:credit 3]
              :prompt "Pay 3 [Credits] to use Edge of World ability?"
              :msg (msg "do " (count (get-in corp [:servers :remote (last (:server run)) :ices]))
                        " brain damage")
              :effect (req (damage state side :brain
                                   (count (get-in corp [:servers :remote (last (:server run)) :ices])) {:card card}))}}}

   "Edward Kim: Humanitys Hammer"
   {:effect (effect (gain :link 1))
    :events {:access {:req (req (= (:type target) "Operation")) :once :per-turn
                      :msg (msg "trash " (:title target)) :effect (effect (trash target))}}}

   "Efficiency Committee"
   {:effect (effect (add-prop card :counter 3))
    :abilities [{:cost [:click 1] :counter-cost 1 :effect (effect (gain :click 2))
                 :msg "gain [Click][Click]"}]}

   "Ekomind"
   {:effect (req (swap! state assoc-in [:runner :memory] (count (get-in @state [:runner :hand])))
                 (add-watch state :ekomind (fn [k ref old new]
                                             (let [hand-size (count (get-in new [:runner :hand]))]
                                               (when (not= (count (get-in old [:runner :hand])) hand-size)
                                                 (swap! ref assoc-in [:runner :memory] hand-size))))))
    :leave-play (req (remove-watch state :ekomind))}

   "Elizabeth Mills"
   {:effect (effect (lose :bad-publicity 1)) :msg "remove 1 bad publicity"
    :abilities [{:cost [:click 1] :label "Trash a location"
                 :msg (msg "trash " (:title target) " and take 1 bad publicity")
                 :choices {:req #(has? % :subtype "Location")}
                 :effect (effect (trash card) (trash target) (gain :bad-publicity 1))}]}

   "Elizas Toybox"
   {:abilities [{:cost [:click 3] :choices {:req #(not (:rezzed %))}
                 :label "Rez a card at no cost" :msg (msg "rez " (:title target) " at no cost")
                 :effect (effect (rez target {:no-cost true}))}]}

   "Emergency Shutdown"
   {:req (req (some #{:hq} (:successful-run runner-reg))) :msg (msg "derez " (:title target))
    :choices {:req #(and (has? % :type "ICE") (:rezzed %))} :effect (effect (derez target))}

   "Encrypted Portals"
   {:msg (msg "gain " (reduce (fn [c server]
                                (+ c (count (filter (fn [ice] (and (has? ice :subtype "Code Gate")
                                                                   (:rezzed ice))) (:ices server)))))
                              0 (flatten (seq (:servers corp))))
              " [Credits]")
    :effect (effect (gain :credit
                          (reduce (fn [c server]
                                    (+ c (count (filter (fn [ice] (and (has? ice :subtype "Code Gate")
                                                                       (:rezzed ice))) (:ices server)))))
                                  0 (flatten (seq (:servers corp))))))

    :events {:pre-ice-strength {:req (req (has? target :subtype "Code Gate"))
                                :effect (effect (ice-strength-bonus 1))}}}

   "Encryption Protocol"
   {:events {:pre-trash {:req (req (= (first (:zone target)) :servers))
                         :effect (effect (trash-cost-bonus 1))}}}

   "Enhanced Vision"
   {:events {:successful-run {:msg (msg "force the Corp to reveal " (:title (first (shuffle (:hand corp)))))
                              :once :per-turn}}}

   "Eureka!"
   {:effect
    (req (let [topcard (first (:deck runner))
               caninst (some #(= % (:type topcard)) '("Hardware" "Resource" "Program"))
               cost (min 10 (:cost topcard))]
              (when caninst
                    (do (gain state side :credit cost)
                        (runner-install state side topcard)))
              (when (get-card state topcard) ; only true if card was not installed
                    (do (system-msg state side (str "reveals and trashes " (:title topcard)))
                        (trash state side topcard)
                        (when caninst (lose state side :credit cost))))))}

   "Eve Campaign"
   {:data {:counter 16}
    :events {:corp-turn-begins {:msg "gain 2 [Credits]" :counter-cost 2
                                :effect (req (gain state :corp :credit 2)
                                             (when (zero? (:counter card)) (trash state :corp card)))}}}

   "Executive Boot Camp"
   {:abilities [{:prompt "Choose an asset to add to HQ" :msg (msg "add " (:title target) " to HQ")
                 :choices (req (filter #(has? % :type "Asset") (:deck corp)))
                 :cost [:credit 1] :label "Search R&D for an asset"
                 :effect (effect (trash card) (move target :hand) (shuffle! :deck))}]}

   "Executive Retreat"
   {:data {:counter 1} :effect (effect (shuffle-into-deck :hand))
    :abilities [{:cost [:click 1] :counter-cost 1 :msg "draw 5 cards" :effect (effect (draw 5))}]}

   "Executive Wiretaps"
   {:msg (msg "reveal cards in HQ: " (map :title (:hand corp)))}

   "Experiential Data"
   {:effect (req (update-ice-in-server state side (card->server state card)))
    :events {:pre-ice-strength {:req (req (= (card->server state card) (card->server state target)))
                                :effect (effect (ice-strength-bonus 1))}}
    :derez-effect {:effect (req (update-ice-in-server state side (card->server state card)))}
    :trash-effect {:effect (req (update-all-ice state side))}}

   "Expert Schedule Analyzer"
   {:abilities
    [{:cost [:click 1] :msg "make a run on HQ"
      :effect (effect (run :hq {:replace-access
                                {:msg (msg "reveal cards in HQ: " (map :title (:hand corp)))}} card))}]}

   "Exposé"
   {:advanceable :always
    :abilities [{:label "Remove 1 bad publicity for each advancement token on Exposé"
                 :msg (msg "remove " (:advance-counter card) " bad publicities")
                 :effect (effect (trash card) (lose :bad-publicity (:advance-counter card)))}]}

   "Express Delivery"
   {:prompt "Choose a card to add to your Grip" :choices (req (take 4 (:deck runner)))
    :effect (effect (move target :hand) (shuffle! :deck))}

   "Fall Guy"
   {:prevent {:trash [:resource]}
    :abilities [{:label "Prevent a resource from being trashed"
                 :effect (effect (trash-prevent :resource 1) (trash card {:unpreventable true :cause :ability-cost}))}
                {:effect (effect (trash card {:cause :ability-cost}) (gain :credit 2)) :msg "gain 2 [Credits]"}]}

   "False Lead"
   {:abilities [{:req (req (>= (:click runner) 2)) :msg "force the Runner to lose [Click][Click]"
                 :effect (effect (forfeit card) (lose :runner :click 2))}]}

   "Fast Track"
   {:prompt "Choose an Agenda" :choices (req (filter #(has? % :type "Agenda") (:deck corp)))
    :effect (effect (system-msg (str "adds " (:title target) " to HQ and shuffle R&D"))
                    (move target :hand) (shuffle! :deck))}

   "Feedback Filter"
   {:prevent {:damage [:net :brain]}
    :abilities [{:cost [:credit 3] :msg "prevent 1 net damage" :effect (effect (damage-prevent :net 1))}
                {:msg "prevent 2 brain damage" :effect (effect (trash card {:cause :ability-cost})
                                                               (damage-prevent :brain 2)) }]}

   "Feint"
   {:effect (effect (run :hq) (max-access 0))}

   "Fetal AI"
   {:access {:req (req (not= (first (:zone card)) :discard)) :msg "do 2 net damage"
             :effect (effect (damage :net 2 {:card card}))}
    :steal-cost [:credit 2]}

   "Fester"
   {:events {:purge {:msg "force the Corp to lose 2 [Credits] if able"
                     :effect (effect (pay :corp card :credit 2))}}}

   "Firmware Updates"
   {:data [:counter 3]
    :abilities [{:counter-cost 1 :choices {:req #(and (= (:type %) "ICE") (:advanceable %))}
                 :msg (msg "place 1 advancement token on " (if (:rezzed target) (:title target) "a card"))
                 :once :per-turn :effect (effect (add-prop target :advance-counter 1))}]}

   "Forged Activation Orders"
   {:choices {:req #(and (has? % :type "ICE") (not (:rezzed %)))}
    :effect (req (let [ice target]
                   (resolve-ability
                    state :corp
                    {:prompt (msg "Rez " (:title ice) " or trash it?") :choices ["Rez" "Trash"]
                     :effect (effect (resolve-ability
                                      (if (and (= target "Rez") (<= (:cost ice) (:credit corp)))
                                        {:msg (msg "force the rez of " (:title ice))
                                         :effect (effect (rez :corp ice))}
                                        {:msg "trash the ICE" :effect (effect (trash :corp ice))})
                                      card nil))}
                    card nil)))}

   "Forked"
   {:prompt "Choose a server" :choices (req servers) :effect (effect (run target))}

   "Forger"
   {:effect (effect (gain :link 1)) :leave-play (effect (lose :link 1))
    :abilities [{:msg "remove 1 tag"
                 :effect (effect (trash card {:cause :ability-cost}) (lose :tag 1))}]}

   "Foxfire"
   {:trace {:base 7 :prompt "Choose 1 card to trash" :not-distinct true
            :choices (req (filter #(or (has? % :subtype "Virtual") (has? % :subtype "Link"))
                                  (concat (get-in runner [:rig :hardware])
                                          (get-in runner [:rig :resource])
                                          (get-in runner [:rig :program]))))
            :msg (msg "trash " (:title target)) :effect (effect (trash target))}}

   "Frame Job"
   {:additional-cost [:forfeit] :effect (effect (gain :corp :bad-publicity 1))}

   "Freelance Coding Contract"
   {:choices {:max 5 :req #(and (has? % :type "Program") (= (:zone %) [:hand]))}
    :msg (msg "trash " (join ", " (map :title targets)) " and gain " (* 2 (count targets)) " [Credits]")
    :effect (effect (trash-cards targets) (gain :credit (* 2 (count targets))))}

   "Freelancer"
   {:req (req tagged) :msg (msg "trash " (join ", " (map :title targets)))
    :choices {:max 2 :req #(= (:zone %) [:rig :resource])} :effect (effect (trash-cards :runner targets))}

   "Gabriel Santiago: Consummate Professional"
   {:events {:successful-run {:msg "gain 2 [Credits]" :once :per-turn
                              :effect (effect (gain :credit 2)) :req (req (= target :hq))}}}

   "Game Day"
   {:msg (msg "draw " (- (:max-hand-size runner) (count (:hand runner))) " cards")
    :effect (effect (draw (- (:max-hand-size runner) (count (:hand runner)))))}

   "Genetic Resequencing"
   {:choices {:req #(= (last (:zone %)) :scored)} :msg (msg "add 1 agenda counter on " (:title target))
    :effect (effect (add-prop target :counter 1))}

   "Gang Sign"
   {:events {:agenda-scored {:msg "access 1 card from HQ"
                             :effect (req (let [c (first (shuffle (:hand corp)))]
                                            (system-msg state side (str "accesses " (:title c)))
                                            (handle-access state side [c])))}}}

   "Geothermal Fracking"
   {:data {:counter 2}
    :abilities [{:cost [:click 1] :counter-cost 1 :msg "gain 7 [Credits] and take 1 bad publicity"
                 :effect (effect (gain :credit 7 :bad-publicity 1))}]}

   "Ghost Branch"
   {:advanceable :always
    :access {:optional {:req (req installed) :prompt "Use Ghost Branch ability?"
                        :msg (msg "give the Runner " (:advance-counter card) " tag"
                                  (when (> (:advance-counter card) 1) "s"))
                        :effect (effect (gain :runner :tag (:advance-counter card)))}}}

   "Gila Hands Arcology"
   {:abilities [{:cost [:click 2] :effect (effect (gain :credit 3)) :msg "gain 3 [Credits]"}]}

   "Gorman Drip v1"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit (get-virus-counters state side card))
                                                  (trash card {:cause :ability-cost}))
                 :msg (msg "gain " (get-virus-counters state side card) " [Credits]")}]
    :events {:corp-click-credit {:effect (effect (add-prop :runner card :counter 1))}
             :corp-click-draw {:effect (effect (add-prop :runner card :counter 1))}}}

   "Government Contracts"
   {:abilities [{:cost [:click 2] :effect (effect (gain :credit 4)) :msg "gain 4 [Credits]"}]}

   "Government Takeover"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit 3)) :msg "gain 3 [Credits]"}]}

   "Grappling Hook"
   {:abilities [{:msg "break all but 1 subroutine" :effect (effect (trash card {:cause :ability-cost}))}]}

   "Gravedigger"
   {:events {:trash {:req (req (and (= (first (:zone target)) :servers) (= (:side target) "Corp")))
                     :effect (effect (add-prop :runner card :counter 1))}}
    :abilities [{:counter-cost 1 :cost [:click 1] :msg "force the Corp to trash the top card of R&D"
                 :effect (effect (mill :corp))}]}

   "Green Level Clearance"
   {:effect (effect (gain :credit 3) (draw))}

   "Grifter"
   {:events {:runner-turn-ends
             {:effect #(let [ab (if (get-in @%1 [:runner :register :successful-run])
                                  {:effect (effect (gain [:credit 1])) :msg "gain 1 [Credits]"}
                                  {:effect (effect (trash %3)) :msg "trash Grifter"})]
                         (resolve-ability %1 %2 ab %3 nil))}}}

   "Grimoire"
   {:effect (effect (gain :memory 2)) :leave-play (effect (lose :memory 2))
    :events {:runner-install {:req (req (has? target :subtype "Virus"))
                              :effect (effect (add-prop target :counter 1))}}}

   "GRNDL: Power Unleashed"
   {:effect (effect (gain :credit 5 :bad-publicity 1))}

   "GRNDL Refinery"
   {:advanceable :always
    :abilities [{:label "Gain 4 [Credits] for each advancement token on GRNDL Refinery"
                 :cost [:click 1] :msg (msg "gain " (* 4 (:advance-counter card)) " [Credits]")
                 :effect (effect (trash card) (gain :credit (* 4 (:advance-counter card))))}]}

   "Haas Arcology AI"
   {:advanceable :while-unrezzed
    :abilities [{:label "Gain [Click]" :once :per-turn :msg "gain [Click]"
                 :cost [:click 1] :advance-counter-cost 1 :effect (effect (gain :click 2))}]}

   "Haas-Bioroid: Engineering the Future"
   {:events {:corp-install {:once :per-turn :msg "gain 1 [Credits]"
                            :effect (effect (gain :credit 1))}}}

   "Haas-Bioroid: Stronger Together"
   {:events {:pre-ice-strength {:req (req (and (= (:type target) "ICE") (has? target :subtype "Bioroid")))
                                :effect (effect (ice-strength-bonus 1))}}}

   "Hacktivist Meeting"
   {:events {:rez {:req (req (not= (:type target) "ICE"))
                   :msg "force the Corp to trash 1 card from HQ at random"
                   :effect (effect (trash (first (shuffle (:hand corp)))))}}}

   "Hades Fragment"
   {:events {:corp-turn-begins
             {:optional
              {:prompt "Add 1 card from Archives to bottom of R&D?"
               :effect (effect (resolve-ability
                                {:prompt "Choose a card" :choices (:discard corp)
                                 :effect (effect (move target :deck))
                                 :msg (msg "add " (if (:seen target) (:title target) "a card")
                                           " to the bottom of R&D")} card target))}}}}

   "Hades Shard"
   {:abilities [{:msg "access all cards in Archives"
                 :effect (effect (trash card {:cause :ability-cost}) (handle-access (access state side [:archives])))}]}

   "Hard at Work"
   {:events {:runner-turn-begins {:msg "gain 2 [Credits] and lose [Click]"
                                  :effect (effect (lose :click 1) (gain :credit 2))}}}

   "Harmony Medtech: Biomedical Pioneer"
   {:effect (effect (lose :agenda-point-req 1) (lose :runner :agenda-point-req 1))}

   "Hayley Kaplan: Universal Scholar"
   {:events {:runner-install
             {:optional {:prompt (msg "Install another " (:type target) " from Grip?") :once :per-turn
                         :effect (req (let [type (:type target)]
                                        (resolve-ability
                                         state side
                                         {:prompt (msg "Choose a " type "to install")
                                          :choices (req (filter #(has? % :type type) (:hand runner)))
                                          :msg (msg "install " (:title target))
                                          :effect (effect (runner-install target))} card nil)))}}}}

   "Hedge Fund"
   {:effect (effect (gain :credit 9))}

   "Hemorrhage"
   {:events {:successful-run {:effect (effect (add-prop card :counter 1))}}
    :abilities [{:counter-cost 2 :cost [:click 1] :msg "force the Corp to trash 1 card from HQ"}]}

   "High-Risk Investment"
   {:data {:counter 1}
    :abilities [{:cost [:click 1] :counter-cost 1 :msg (msg "gain" (:credit runner) " [Credits]")
                 :effect (effect (gain :credit (:credit runner)))}]}

   "Hivemind"
   {:data {:counter 1 :counter-type "Virus"}
    :abilities [{:req (req (> (:counter card) 0)) :priority true
                 :prompt "Move a virus counter to which card?"
                 :choices {:req #(has? % :subtype "Virus")}
                 :effect (req (let [abilities (:abilities (card-def target))
                                    virus target]
                                (add-prop state :runner virus :counter 1)
                                (add-prop state :runner card :counter -1)
                                (if (= (count abilities) 1)
                                  (do (swap! state update-in [side :prompt] rest) ; remove the Hivemind prompt so Imp works
                                      (resolve-ability state side (first abilities) (get-card state virus) nil))
                                  (resolve-ability
                                   state side
                                   {:prompt "Choose an ability to trigger"
                                    :choices (vec (map :msg abilities))
                                    :effect (req (swap! state update-in [side :prompt] rest)
                                                 (resolve-ability
                                                  state side
                                                  (first (filter #(= (:msg %) target) abilities))
                                                  card nil))}
                                   (get-card state virus) nil))))
                 :msg (msg "to trigger an ability on " (:title target))}]}

   "Hokusai Grid"
   {:events {:successful-run {:req (req this-server) :msg "do 1 net damage"
                              :effect (req (damage state side :net 1 {:card card}))}}}

   "Hostage"
   {:prompt "Choose a Connection to install"
    :choices (req (filter #(and (has? % :subtype "Connection")
                                (<= (:cost %) (:credit runner))) (:deck runner)))
    :effect (effect (runner-install target) (shuffle! :deck))}

   "Hostile Infrastructure"
   {:events {:trash {:req (req (and (= (:side target) "Corp") (= side :runner)))
                     :msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}}
    :abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}]}

   "Hostile Takeover"
   {:effect (effect (gain :credit 7 :bad-publicity 1))}

   "Housekeeping"
   {:events {:runner-install {:req (req (= side :runner)) :choices (req (:hand runner))
                              :prompt "Choose a card to trash for Housekeeping" :once :per-turn
                              :msg (msg "to force the Runner to trash " (:title target) " from Grip")
                              :effect (effect (trash target))}}}

   "House of Knives"
   {:data {:counter 3}
    :abilities [{:counter-cost 1 :msg "do 1 net damage" :req (req (:run @state)) :once :per-run
                 :effect (effect (damage :net 1 {:card card}))}]}

   "Human First"
   {:events {:agenda-scored {:msg (msg "gain " (:agendapoints target) " [Credits]")
                             :effect (effect (gain :runner :credit (:agendapoints target)))}
             :agenda-stolen {:msg (msg "gain " (:agendapoints target) " [Credits]")
                             :effect (effect (gain :credit (:agendapoints target)))}}}

   "HQ Interface"
   {:effect (effect (gain :hq-access 1)) :leave-play (effect (lose :hq-access 1))}

   "Hyperdriver"
   {:abilities [{:label "Remove Hyperdriver from the game to gain [Click] [Click] [Click]"
                 :effect (effect (move card :rfg) (gain :memory 3 :click 3))
                 :msg "gain [Click] [Click] [Click]"}]}

   "Iain Stirling: Retired Spook"
   {:effect (effect (gain :link 1))
    :events {:runner-turn-begins {:req (req (> (:agenda-point corp) (:agenda-point runner)))
                                  :msg "to gain 2 [Credits]" :effect (effect (gain :credit 2))}}}

   "Ice Analyzer"
   {:events {:rez {:req (req (= (:type target) "ICE")) :msg "place 1 [Credits] on Ice Analyzer"
                   :effect (effect (add-prop :runner card :counter 1))}}
    :abilities [{:counter-cost 1 :effect (effect (gain :credit 1))
                 :msg "take 1 [Credits] to install programs"}]}

   "Ice Carver"
   {:events {:pre-ice-strength
             {:req (req (and (= (:cid target) (:cid current-ice)) (:rezzed target)))
              :effect (effect (ice-strength-bonus -1))}}}

   "Imp"
   {:data {:counter 2}
    :abilities [{:counter-cost 1 :msg "trash at no cost" :once :per-turn
                 :effect (effect (trash-no-cost))}]}

   "Incubator"
   {:events {:runner-turn-begins {:effect (effect (add-prop card :counter 1))}}
    :abilities [{:cost [:click 1]
                 :msg (msg "move " (:counter card) " virus counter to " (:title target))
                 :choices {:req #(and (= (first (:zone %)) :rig) (has? % :subtype "Virus"))}
                 :effect (effect (trash card {:cause :ability-cost}) (add-prop target :counter (:counter card)))}]}

   "Indexing"
   {:effect (effect (run :rd {:replace-access
                              {:msg "rearrange the top 5 cards of R&D"
                               :effect (req (doseq [c (take 5 (:deck corp))]
                                              (move state side c :play-area)))}} card))}

   "Industrial Genomics: Growing Solutions"
   {:events {:pre-trash {:effect (effect (trash-cost-bonus
                                           (count (filter #(not (:seen %)) (:discard corp)))))}}}

   "Infiltration"
   {:prompt "Gain 2 [Credits] or expose a card?" :choices ["Gain 2 [Credits]" "Expose a card"]
    :effect (effect (resolve-ability (if (= target "Expose a card")
                                       {:choices {:req #(= (first (:zone %)) :servers)}
                                        :effect (effect (expose target))
                                        :msg (msg "expose " (:title target))}
                                       {:msg "gain 2 [Credits]" :effect (effect (gain :credit 2))})
                                     card nil))}

   "Inject"
   {:effect (req (doseq [c (take 4 (get-in @state [:runner :deck]))]
                   (if (= (:type c) "Program")
                     (do (trash state side c) (gain state side :credit 1)
                         (system-msg state side (str "trashes " (:title c) " and gains 1 [Credits]")))
                     (do (move state side c :hand)
                         (system-msg state side (str "adds " (:title c) " to Grip"))))))}

   "Inside Job"
   {:prompt "Choose a server" :choices (req servers) :effect (effect (run target))}

   "Inside Man"
   {:recurring 2}

   "Interns"
   {:prompt "Install a card from Archives or HQ?" :choices ["Archives" "HQ"]
    :msg (msg "install a card from " target)
    :effect (effect (resolve-ability
                     {:prompt "Choose a card to install"
                      :choices (req (filter #(not= (:type %) "Operation")
                                            ((if (= target "HQ") :hand :discard) corp)))
                      :effect (effect (corp-install target nil {:no-install-cost true}))}
                     card targets))}

   "Invasion of Privacy"
   {:trace {:base 2 :msg (msg "reveal the Runner's Grip")
            :effect (req (doseq [c (:hand runner)]
                           (move state side c :play-area)))
            :unsuccessful {:msg "take 1 bad publicity" :effect (effect (gain :corp :bad-publicity 1))}}}

   "Isabel McGuire"
   {:abilities [{:label "Add an installed card to HQ" :choices {:req #(= (first (:zone %)) :servers)}
                 :msg (msg "move " (:title target) " to HQ") :effect (effect (move target :hand))}]}

   "IT Department"
   {:abilities [{:counter-cost 1 :label "Add strength to a rezzed ICE"
                 :choices {:req #(and (= (:type %) "ICE") (:rezzed %))}
                 :msg (msg "add strength to a rezzed ICE")
                 :effect (req (update! state side (update-in card [:it-targets (keyword (str (:cid target)))]
                                                             (fnil #(+ % 1) 0)))
                              (update-ice-strength state side target))}
                {:cost [:click 1] :msg "add 1 counter" :effect (effect (add-prop card :counter 1))}]
    :events
      (let [it {:req (req (:it-targets card))
                :effect (req (update! state side (dissoc card :it-targets))
                             (update-all-ice state side))}]
           {:pre-ice-strength {:req (req (get-in card [:it-targets (keyword (str (:cid target)))]))
                                :effect (effect (ice-strength-bonus
                                                  (* (get-in card [:it-targets (keyword (str (:cid target)))])
                                                     (inc (:counter card)))))}
            :runner-turn-ends it :corp-turn-ends it})}

   "Ive Had Worse"
   {:effect (effect (draw 3))
    :trash-effect {:req (req (#{:meat :net} target))
                   :effect (effect (draw :runner 3)) :msg "draw 3 cards"}}

   "Ixodidae"
   {:events {:corp-loss {:req (req (= (first target) :credit)) :msg "to gain 1 [Credits]"
                         :effect (effect (gain :runner :credit 1))}
             :purge {:effect (effect (trash card))}}}

   "Jackson Howard"
   {:abilities [{:cost [:click 1] :effect (effect (draw 2)) :msg "draw 2 cards"}
                {:effect (effect (move card :rfg)) :label "Remove Jackson Howard from the game"
                 :msg "shuffle up to 3 cards from Archives into R&D"}]}

   "Jinteki: Personal Evolution"
   {:events {:agenda-scored {:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}
             :agenda-stolen {:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}}}

   "Jinteki Biotech: Life Imagined"
   {:events {:pre-first-turn {:req (req (= side :corp))
                              :prompt "Choose a copy of Jinteki Biotech to use this game"
                              :choices ["The Brewery" "The Tank" "The Greenhouse"]
                              :effect (effect (update! (assoc card :biotech-target target))
                                              (system-msg (str "has chosen a copy of Jinteki Biotech for this game ")))}}
    :abilities [{:cost [:click 3]
                 :req (req (not (:biotech-used card)))
                 :effect (req (let [flip (:biotech-target card)]
                                (update! state side (assoc card :biotech-used true))
                                (case flip
                                  "The Brewery"
                                  (do (system-msg state side "uses The Brewery to do 2 net damage")
                                      (damage state side :net 2 {:card card}))
                                  "The Tank"
                                  (do (system-msg state side "uses The Tank to shuffle Archives into R&D")
                                      (shuffle-into-deck state side :discard))
                                  "The Greenhouse"
                                  (do (system-msg state side (str "uses The Greenhouse to place 4 advancement tokens "
                                                                  "on a card that can be advanced"))
                                      (resolve-ability
                                        state side
                                        {:prompt "Choose a card that can be advanced"
                                         :choices {:req #(or (= (:advanceable %) "always")
                                                             (and (= (:advanceable %) "while-rezzed") (:rezzed %))
                                                             (= (:type %) "Agenda"))}
                                         :effect (effect (add-prop target :advance-counter 4))} card nil)))))}]}

   "John Masanori"
   {:events {:successful-run {:msg "draw 1 card" :once :per-turn :once-key :john-masanori-draw
                              :effect (effect (draw))}
             :unsuccessful-run {:msg "take 1 tag" :once :per-turn :once-key :john-masanori-tag
                                :effect (effect (gain :runner :tag 1))}}}

   "Joshua B."
   {:events {:runner-turn-begins
             {:optional {:prompt "Use Joshua B. to gain [Click]?" :msg "gain [Click]"
                         :effect (effect (gain :click 1))
                         :end-turn {:effect (effect (gain :tag 1)) :msg "gain 1 tag"}}}}}

   "Investigative Journalism"
   {:req (req (> (:bad-publicity corp) 0))
    :abilities [{:cost [:click 4] :msg "give the Corp 1 bad publicity"
                 :effect (effect (gain :corp :bad-publicity 1) (trash card {:cause :ability-cost}))}]}

   "Kate \"Mac\" McCaffrey: Digital Tinker"
   {:effect (effect (gain :link 1))
    :events {:pre-install {:once :per-turn
                           :req (req (some #(= % (:type target)) '("Hardware" "Program")))
                           :msg (msg "reduce the install cost of " (:title target) " by 1 [Credits]")
                           :effect (effect (install-cost-bonus -1))}}}

   "Kati Jones"
   {:abilities
    [{:cost [:click 1] :msg "store 3 [Credits]" :once :per-turn
      :effect (effect (add-prop card :counter 3))}
     {:cost [:click 1] :msg (msg "gain " (:counter card) " [Credits]") :once :per-turn
      :label "Take all credits"
      :effect (effect (gain :credit (:counter card)) (set-prop card :counter 0))}]}

   "Ken \"Express\" Tenma: Disappeared Clone"
   {:events {:play-event {:req (req (has? target :subtype "Run")) :once :per-turn
                          :msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Keyhole"
   {:abilities [{:cost [:click 1] :msg "make a run on R&D"
                 :effect (effect (run :rd
                                   {:replace-access
                                    {:prompt "Choose a card to trash" :not-distinct true
                                     :msg (msg "trash " (:title target))
                                     :choices (req (take 3 (:deck corp))) :mandatory true
                                     :effect (effect (trash (assoc target :seen true))
                                                     (shuffle! :corp :deck))}} card))}]}

   "Knifed"
   {:prompt "Choose a server" :choices (req servers) :effect (effect (run target))}

   "Kraken"
   {:req (req (:stole-agenda runner-reg)) :prompt "Choose a server" :choices (req servers)
    :msg (msg "force the Corp to trash an ICE protecting " target)}

   "Labyrinthine Servers"
   {:data {:counter 2}
    :abilities [{:counter-cost 1 :effect (effect (prevent-jack-out))
                 :msg "prevent the Runner from jacking out"}]}

   "Lag Time"
   {:events {:pre-ice-strength {:effect (effect (ice-strength-bonus 1))}}}

   "Lamprey"
   {:events {:successful-run {:req (req (= target :hq)) :msg "to force the Corp to lose 1 [Credits]"
                              :effect (effect (lose :corp :credit 1))}
             :purge {:effect (effect (trash card))}}}

   "Lawyer Up"
   {:effect (effect (draw 3) (lose :tag 2))}

   "Leela Patel: Trained Pragmatist"
   {:events {:agenda-scored {:choices {:req #(not (:rezzed %))} :msg "add 1 unrezzed card to HQ"
                             :player :runner :effect (effect (move :corp target :hand))}
             :agenda-stolen {:choices {:req #(not (:rezzed %))} :msg "add 1 unrezzed card to HQ"
                             :effect (req (move state :corp target :hand)
                                          (swap! state update-in [:runner :prompt] rest)
                                          (handle-end-run state side))}}}

   "Legwork"
   {:effect (effect (run :hq) (access-bonus 2))}

   "Lemuria Codecracker"
   {:abilities [{:cost [:click 1 :credit 1] :req (req (some #{:hq} (:successful-run runner-reg)))
                 :choices {:req #(= (first (:zone %)) :servers)} :effect (effect (expose target))
                 :msg "expose 1 card"}]}

   "Leprechaun"
   {:abilities [{:req (req (<= (count (:hosted card)) 2)) :cost [:click 1]
                 :prompt "Choose a program to install on Leprechaun"
                 :choices (req (filter #(and (= (:type %) "Program")
                                             (<= (:cost %) (:credit runner)))
                                       (:hand runner)))
                 :msg (msg "host " (:title target))
                 :effect (effect (gain :memory (:memoryunits target))
                                 (runner-install target {:host-card card}))}]}

   "Leverage"
   {:player :corp
    :prompt "Take 2 bad publicity?"
    :choices ["Yes" "No"]
    :effect (req (if (= target "Yes")
                   (do (gain state :corp :bad-publicity 2) (system-msg state :corp "takes 2 bad publicity"))
                   (do (register-events state side
                                    {:pre-damage {:effect (effect (damage-prevent :net Integer/MAX_VALUE)
                                                                  (damage-prevent :meat Integer/MAX_VALUE)
                                                                  (damage-prevent :brain Integer/MAX_VALUE))}
                                     :runner-turn-begins {:effect (effect (unregister-events card))}}
                                    (assoc card :zone '(:discard)))
                       (system-msg state :runner "is immune to damage until the beginning of the runner's next turn"))))
    ; This :events is a hack so that the unregister-events above will fire.
    :events {:runner-turn-begins nil :pre-damage nil}}

   "Levy AR Lab Access"
   {:effect (effect (shuffle-into-deck :hand :discard) (draw 5)
                    (move (first (:play-area runner)) :rfg))}

   "Levy University"
   {:abilities [{:prompt "Choose an ICE" :msg (msg "adds " (:title target) " to HQ")
                 :choices (req (filter #(has? % :type "ICE") (:deck corp)))
                 :label "Search R&D for a piece of ICE"
                 :cost [:click 1 :credit 1] :effect (effect (move target :hand) (shuffle! :deck))}]}

   "Liberated Account"
   {:data {:counter 16}
    :abilities [{:cost [:click 1] :counter-cost 4 :msg "gain 4 [Credits]"
                 :effect (req (gain state :runner :credit 4)
                              (when (= (:counter card) 0) (trash state :runner card)))}]}

   "License Acquisition"
   {:prompt "Install a card from Archives or HQ?" :choices ["Archives" "HQ"]
    :msg (msg "install a card from " target)
    :effect (effect (resolve-ability
                     {:prompt "Choose a card to install" :msg (msg "install and rez " (:title target))
                      :choices (req (filter #(#{"Asset" "Upgrade"} (:type %))
                                            ((if (= target "HQ") :hand :discard) corp)))
                      :effect (effect (corp-install target nil {:rezzed true}))} card targets))}

   "LLDS Energy Regulator"
   {:prevent {:trash [:hardware]}
    :abilities [{:cost [:credit 3] :msg "prevent a hardware from being trashed"}
                {:effect (effect (trash card {:cause :ability-cost})) :msg "prevent a hardware from being trashed"}]}

   "LLDS Processor"
   {:events
             (let [llds {:effect (req (let [cards (:llds-target card)]
                                           (update! state side (dissoc card :llds-target))
                                           (doseq [c cards]
                                             (update-breaker-strength state side c))))}]
               {:runner-turn-ends llds :corp-turn-ends llds
                :runner-install {:req (req (has? target :subtype "Icebreaker"))
                                 :effect (effect (update! (update-in card [:llds-target] #(conj % target)))
                                                 (update-breaker-strength target))}
                :pre-breaker-strength {:req (req (some #(= (:cid target) (:cid %)) (:llds-target card)))
                                       :effect (effect (breaker-strength-bonus 1))}})}

   "Lockpick"
   {:recurring 1}

   "Logos"
   {:effect (effect (gain :memory 1 :max-hand-size 1))
    :leave-play (effect (lose :memory 1 :max-hand-size 1))
    :events {:agenda-scored
             {:player :runner :prompt "Choose a card" :msg (msg "add 1 card to Grip from Stack")
              :choices (req (:deck runner)) :effect (effect (move target :hand) (shuffle! :deck))}}}

   "London Library"
   {:abilities [{:label "Install a non-virus program on London Library" :cost [:click 1]
                 :prompt "Choose a non-virus program to install on London Library"
                 :choices (req (filter #(and (= (:type %) "Program")
                                             (not (has? % :subtype "Virus"))
                                             (<= (:memoryunits %) (:memory runner)))
                                       (:hand runner)))
                 :msg (msg "host " (:title target))
                 :effect (effect (runner-install target {:host-card card :no-cost true}))}
                {:label "Add a program hosted on London Library to your Grip" :cost [:click 1]
                 :choices {:req #(:host %)} :msg (msg "add " (:title target) "to his or her Grip")
                 :effect (effect (move target :hand))}]
    :events {:runner-turn-ends {:effect (req (doseq [c (:hosted card)]
                                               (trash state side c)))}}}

   "Lucky Find"
   {:effect (effect (gain :credit 9))}

   "Magnum Opus"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit 2)) :msg "gain 2 [Credits]"}]}

   "Mandatory Upgrades"
   {:effect (effect (gain :click 1 :click-per-turn 1))}

   "Manhunt"
   {:events {:successful-run {:once :per-turn :trace {:base 2 :msg "give the Runner 1 tag"
                                                      :effect (effect (gain :runner :tag 1))}}}}

   "Marcus Batty"
   {:abilities [{:msg "start a Psi game"
                 :psi {:not-equal {:req (req this-server)
                                   :choices {:req #(and (has? % :type "ICE") (:rezzed %))}
                                   :msg (msg "resolve a subroutine on " (:title target))
                                   :effect (effect (trash card {:cause :ability-cost}))}}}]}

   "Mark Yale"
   {:events {:agenda-counter-spent {:effect (effect (gain :credit 1))
                                    :msg "gain 1 [Credits]"}}
    :abilities [{:label "Trash to gain 2 [Credits]"
                 :msg "gain 2 [Credits]"
                 :effect (effect (gain :credit 2) (trash card))}
                {:label "Spend an agenda counter to gain 2 [Credits]"
                 :effect (req (resolve-ability
                                state side
                                {:prompt "Select an agenda with a counter"
                                 :choices {:req #(and (= (:type %) "Agenda")
                                                      (:counter %))}
                                 :effect (req (add-prop state side target :counter -1)
                                              (gain state :corp :credit 2)
                                              (trigger-event state side :agenda-counter-spent card))
                                 :msg (msg "spend an agenda token on " (:title target) " and gain 2 [Credits]")}
                                card nil))}]}

   "Marked Accounts"
   {:abilities [{:cost [:click 1] :msg "store 3 [Credits]"
                 :effect (effect (add-prop card :counter 3))}]
    :events {:corp-turn-begins {:msg "gain 1 [Credits]" :counter-cost 1
                                :effect (effect (gain :credit 1))}}}

   "Market Research"
   {:req (req tagged) :effect (effect (set-prop card :counter 1 :agendapoints 3))}

   "Mass Install"
   {:choices {:max 3 :req #(and (has? % :type "Program") (= (:zone %) [:hand]))}
    :msg (msg "install " (join ", " (map :title targets)))
    :effect (req (doseq [c targets] (runner-install state side c)))}

   "MaxX: Maximum Punk Rock"
   {:events {:runner-turn-begins {:msg "trash the top 2 cards from Stack and draw 1 card"
                                  :effect (effect (mill 2) (draw))}}}

   "Medical Breakthrough"
   {:effect (effect (update-all-advancement-costs))
    :stolen (effect (update-all-advancement-costs))
    :advancement-cost-bonus (req (- (count (filter #(= (:title %) "Medical Breakthrough")
                                                (concat (:scored corp) (:scored runner))))))}

   "Medical Research Fundraiser"
   {:effect (effect (gain :credit 8) (gain :runner :credit 3))}

   "Medium"
   {:events
    {:successful-run
     {:req (req (= target :rd))
      :effect (effect (add-prop card :counter 1)
                      (access-bonus (max 0 (dec (get-virus-counters state side (get-card state card))))))}}}

   "Melange Mining Corp."
   {:abilities [{:cost [:click 3] :effect (effect (gain :credit 7)) :msg "gain 7 [Credits]"}]}

   "MemStrips"
   {:effect (effect (gain :memory 3))}

   "Mental Health Clinic"
   {:effect (effect (gain :runner :max-hand-size 1))
    :leave-play (effect (lose :runner :max-hand-size 1))
    :events {:corp-turn-begins {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Midseason Replacements"
   {:req (req (:stole-agenda runner-reg))
    :trace {:base 6 :msg (msg "give the Runner " (- target (second targets)) " tags")
            :effect (effect (gain :runner :tag (- target (second targets))))}}

   "Modded"
   {:prompt "Choose a card to install"
    :choices (req (filter #(#{"Hardware" "Program"} (:type %)) (:hand runner)))
    :effect (effect
              (register-events {:pre-install
                                {:effect (effect (install-cost-bonus -3) (unregister-events card))}}
                               (assoc card :zone '(:discard)))
              (runner-install target))
    :events {:pre-install nil}}

   "Monolith"
   {:prevent {:damage [:net :brain]}
    :effect (effect (gain :memory 3)) :leave-play (effect (lose :memory 3))
    :abilities [{:msg (msg "prevent 1 brain or net damage by trashing " (:title target))
                 :priority true
                 :choices (req (filter #(= (:type %) "Program") (:hand runner)))
                 :prompt "Choose a program to trash" :effect (effect (trash target)
                                                                     (damage-prevent :brain 1)
                                                                     (damage-prevent :net 1))}]}

   "Motivation"
   {:events
    {:runner-turn-begins
     {:msg "look at the top card of his Stack"
      :effect (effect (prompt! card (str "The top card of your Stack is "
                                         (:title (first (:deck runner)))) ["OK"] {}))}}}
   "Mr. Li"
   {:abilities [{:cost [:click 1] :prompt "Card to keep?"
                 :choices (req (take 2 (:deck runner))) :msg "choose 1 card to draw"
                 :effect (req (move state side target :hand)
                              (if (= target (first (:deck runner)))
                                (move state side (second (:deck runner)) :deck)
                                (move state side (first (:deck runner)) :deck)))}]}

   "Muertos Gang Member"
   {:abilities [{:msg "draw 1 card"
                 :effect (effect (trash card {:cause :ability-cost}) (draw))}] }

   "Muresh Bodysuit"
   {:events {:pre-damage {:once :per-turn :once-key :muresh-bodysuit
                          :req (req (= target :meat))
                          :msg "prevent the first meat damage this turn"
                          :effect (effect (damage-prevent :meat 1))}}}

   "Mushin No Shin"
   {:prompt "Choose a card to install"
    :choices (req (filter #(#{"Asset" "Agenda" "Upgrade"} (:type %)) (:hand corp)))
    :effect (effect (corp-install (assoc target :advance-counter 3) "New remote"))}

   "NAPD Contract"
   {:steal-cost [:credit 4]
    :advancement-cost-bonus (req (:bad-publicity corp))}

   "NBN: Making News"
   {:recurring 2}

   "NBN: The World is Yours*"
   {:effect (effect (gain :max-hand-size 1))}

   "Near-Earth Hub: Broadcast Center"
   {:events {:server-created {:msg "draw 1 card" :once :per-turn :effect (effect (draw 1))}}}

   "NeoTokyo Grid"
   {:events {:advance {:req (req (= (butlast (:zone target)) (butlast (:zone card)))) :once :per-turn
                       :msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Nerve Agent"
   {:events
    {:successful-run
     {:req (req (= target :hq))
      :effect (effect (add-prop card :counter 1)
                      (access-bonus (max 0 (dec (get-virus-counters state side (get-card state card))))))}}}

   "Net Celebrity"
   {:recurring 1}

   "Net Shield"
   {:prevent {:damage [:net]}
    :abilities [{:cost [:credit 1] :once :per-turn :msg "prevent the first net damage this turn"
                 :effect (effect (damage-prevent :net 1))}]}

   "Networking"
   {:effect (effect (lose :tag 1))
    :optional {:cost [:credit 1] :prompt "Pay 1 [Credits] to add Networking to Grip?"
               :msg "add it to his Grip" :effect (effect (move (first (:discard runner)) :hand))}}

   "Neural EMP"
   {:req (req (:made-run runner-reg)) :effect (effect (damage :net 1 {:card card}))}

   "Net-Ready Eyes"
   {:effect (effect (damage :meat 2 {:unboostable true :card card})) :msg "suffer 2 meat damage"
    :events {:run {:choices {:req #(and (:installed %) (has? % :subtype "Icebreaker"))}
                   :msg (msg "give " (:title target) " +1 strength")
                   :effect (effect (pump target 1 :all-run))}}}

   "New Angeles City Hall"
   {:events {:agenda-stolen {:msg "trash itself" :effect (effect (trash card))}}
    :abilities [{:cost [:credit 2] :msg "avoid 1 tag" :effect (effect (lose :tag 1))}]}

   "Nisei Division: The Next Generation"
   {:events {:psi-game {:msg "gain 1 [Credits]" :effect (effect (gain :corp :credit 1))}}}

   "Nisei MK II"
   {:data {:counter 1} :abilities [{:counter-cost 1 :msg "end the run" :effect (effect (end-run))}]}

   "Noise: Hacker Extraordinaire"
   {:events {:runner-install {:msg "force the Corp to trash the top card of R&D" :effect (effect (mill :corp))
                              :req (req (has? target :subtype "Virus"))}}}

   "Notoriety"
   {:req (req (and (some #{:hq} (:successful-run runner-reg))
                   (some #{:rd} (:successful-run runner-reg))
                   (some #{:archives} (:successful-run runner-reg))))
    :effect (effect (gain-agenda-point 1) (move (first (:play-area runner)) :scored))}

   "Oaktown Grid"
   {:events {:pre-trash {:req (req (= (:zone card) (:zone target)))
                         :effect (effect (trash-cost-bonus 3))}}}

   "Oaktown Renovation"
   {:install-state :face-up
    :events {:advance {:req (req (= (:cid card) (:cid target)))
                       :effect (req (gain state side :credit
                                          (if (>= (:advance-counter (get-card state card)) 5) 3 2)))}}}

   "Off-Campus Apartment"
   {:abilities [{:prompt "Choose a connection to install on Off-Campus Apartment"
                 :choices (req (filter #(and (has? % :subtype "Connection")
                                             (<= (:cost %) (:credit runner)))
                                       (:hand runner)))
                 :msg (msg "host " (:title target) " and draw 1 card")
                 :effect (effect (runner-install target {:host-card card}) (draw))}]}

   "Oracle May"
   {:abilities [{:cost [:click 1] :once :per-turn :prompt "Choose card type"
                 :choices ["Event" "Hardware" "Program" "Resource"]
                 :effect #(let [c (first (get-in @%1 [:runner :deck]))]
                            (system-msg %1 %2 (str "uses Oracle May, names " (first %4)
                                                   " and reveals " (:title c)))
                            (if (= (:type c) (first %4))
                              (do (system-msg %1 %2 (str "gains 2 [Credits] and draws " (:title c)))
                                  (gain %1 %2 :credit 2) (draw %1 %2))
                              (do (system-msg %1 %2 (str "trashes " (:title c))) (mill %1 %2))))}]}

   "Origami"
   {:effect (effect (gain :max-hand-size
                          (dec (* 2 (count (filter #(= (:title %) "Origami")
                                                   (get-in runner [:rig :program])))))))
    :leave-play (effect (lose :max-hand-size
                              (dec (* 2 (count (filter #(= (:title %) "Origami")
                                                       (get-in runner [:rig :program])))))))}

   "Oversight AI"
   {:choices {:req #(and (= (:type %) "ICE") (not (:rezzed %)))}
    :msg (msg "rez " (:title target) " at no cost")
    :effect (effect (rez target {:no-cost true})
                    (host (get-card state target) (assoc card :zone [:discard] :seen true)))}

   "PAD Campaign"
   {:events {:corp-turn-begins {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Patch"
   {:choices {:req #(and (= (:type %) "ICE") (:rezzed %))}
    :effect (effect (host target (assoc card :zone [:discard] :seen true))
                    (update-ice-strength (get-card state target)))
    :events {:pre-ice-strength {:req (req (= (:cid target) (:cid (:host card))))
                                :effect (effect (ice-strength-bonus 2))}}}

   "Posted Bounty"
   {:optional {:prompt "Forfeit Posted Bounty to give the Runner 1 tag and take 1 bad publicity?"
               :msg "give the Runner 1 tag and take 1 bad publicity"
               :effect (effect (gain :bad-publicity 1) (gain :runner :tag 1) (forfeit card))}}

   "Precognition"
   {:effect (req (doseq [c (take 5 (:deck corp))] (move state side c :play-area)))}

   "Profiteering"
   {:choices ["0" "1" "2" "3"] :prompt "How many bad publicity?"
    :msg (msg "take " target " bad publicity and gain " (* 5 (Integer/parseInt target)) " [Credits]")
    :effect (effect (gain :credit (* 5 (Integer/parseInt target))
                          :bad-publicity (Integer/parseInt target)))}

   "Progenitor"
   {:abilities [{:cost [:click 1] :req (req (empty? (:hosted card)))
                 :prompt "Choose a Virus program to install on Progenitor"
                 :choices (req (filter #(and (= (:type %) "Program")
                                             (has? % :subtype "Virus")
                                             (<= (:cost %) (:credit runner)))
                                       (:hand runner)))
                 :msg (msg "host " (:title target))
                 :effect (effect (gain :memory (:memoryunits target))
                                 (runner-install target {:host-card card}))}]
    :events {:purge {:effect (req (when-let [c (first (:hosted card))]
                                    (add-prop state side c :counter 1)))}}}

   "Omni-Drive"
   {:recurring 1
    :abilities [{:label "Install a program of 1[Memory Unit] or less on Omni-Drive"
                 :req (req (empty? (:hosted card))) :cost [:click 1]
                 :prompt "Choose a program of 1[Memory Unit] or less to install on Omni-Drive"
                 :choices (req (filter #(and (= (:type %) "Program")
                                             (<= (:memoryunits %) 1)
                                             (<= (:cost %) (:credit runner)))
                                       (:hand runner)))
                 :msg (msg "host " (:title target))
                 :effect (effect (gain :memory (:memoryunits target))
                                 (runner-install target {:host-card card}))}]}

   "Order of Sol"
   {:effect (req (add-watch state :order-of-sol
                            (fn [k ref old new]
                              (when (and (not (zero? (get-in old [:runner :credit])))
                                         (zero? (get-in new [:runner :credit])))
                                (resolve-ability ref side {:msg "gain 1 [Credits]" :once :per-turn
                                                           :effect (effect (gain :credit 1))} card nil)))))
    :leave-play (req (remove-watch state :order-of-sol))}

   "Paintbrush"
   {:abilities [{:cost [:click 1] :msg (msg "give " (:title target)
                                            " sentry, code gate or barrier until the end of next run this turn")
                 :choices {:req #(and (= (first (:zone %)) :servers) (has? % :type "ICE") (:rezzed %))}}]}

   "Panic Button"
   {:init {:root "HQ"} :abilities [{:cost [:credit 1] :effect (effect (draw))
                                    :req (req (and run (= (first (:server run)) :hq)))}]}

   "Paper Tripping"
   {:effect (effect (lose :tag :all))}

   "Parasite"
   {:hosting {:req #(and (= (:type %) "ICE") (:rezzed %))}
    :effect (req (when-let [h (:host card)] (update-ice-strength state side h)))
    :events {:runner-turn-begins
             {:effect (req (add-prop state side card :counter 1))}
             :counter-added
             {:req (req (or (= (:title target) "Hivemind") (= (:cid target) (:cid card))))
              :effect (effect (update-ice-strength (:host card)))}
             :pre-ice-strength
             {:req (req (= (:cid target) (:cid (:host card))))
              :effect (effect (ice-strength-bonus (- (get-virus-counters state side card))))}
             :ice-strength-changed
             {:req (req (and (= (:cid target) (:cid (:host card))) (<= (:current-strength target) 0)))
              :effect (effect (trash target))
              :msg (msg "trash " (:title target))}}}

   "Paricia"
   {:recurring 2}

   "Pawn"
   {:abilities [{:label "Host Pawn on a piece of ICE" :cost [:click 1]
                 :choices {:req #(and (= (:type %) "ICE")
                                      (= (last (:zone %)) :ices)
                                      (not (some (fn [c] (has? c :subtype "Caïssa")) (:hosted %))))}
                 :msg (msg "host it on " (if (:rezzed target) (:title target) "a piece of ICE")) 
                 :effect (effect (host target card))}]}

   "Paywall Implementation"
   {:events {:successful-run {:msg "gain 1 [Credits]" :effect (effect (gain :corp :credit 1))}}}

   "Peak Efficiency"
   {:effect (effect (gain :credit
                          (reduce (fn [c server]
                                    (+ c (count (filter (fn [ice] (:rezzed ice)) (:ices server)))))
                                  0 (flatten (seq (:servers corp))))))}

   "Personal Workshop"
   (let [remove-counter
         {:req (req (not (empty? (:hosted card))))
          :msg (msg "remove 1 counter from " (:title target)) :choices {:req #(:host %)}
          :effect (req (if (= (:counter target) 1)
                         (runner-install state side (dissoc target :counter) {:no-cost true})
                         (add-prop state side target :counter -1)))}]
     {:abilities [{:label "Host a program or piece of hardware" :cost [:click 1]
                   :prompt "Choose a card to host on Personal Workshop"
                   :choices (req (filter #(#{"Program" "Hardware"} (:type %)) (:hand runner)))
                   :effect (effect (host card (assoc target :counter (:cost target))))
                   :msg (msg "host " (:title target) "")}
                  (assoc remove-counter
                         :label "Remove 1 counter from a hosted card" :cost [:credit 1])]
      :events {:runner-turn-begins remove-counter}})

   "Philotic Entanglement"
   {:msg (msg "do " (count (:scored runner)) " net damage")
    :effect (effect (damage :net (count (:scored runner)) {:card card}))}

   "Plan B"
   {:advanceable :always
    :access {:optional
             {:prompt "Score an Agenda from HQ?" :req (req installed)
              :effect (effect
                       (resolve-ability
                        {:prompt "Choose an Agenda to score"
                         :choices (req (filter #(and (has? % :type "Agenda")
                                                     (<= (:advancementcost %) (:advance-counter card)))
                                               (:hand corp)))
                         :msg (msg "score " (:title target))
                         :effect (effect (score (assoc target :advance-counter (:advancementcost target))))}
                        card targets))}}}

   "Planned Assault"
   {:msg (msg "play " (:title target))
    :choices (req (filter #(and (has? % :subtype "Run")
                                (<= (:cost %) (:credit runner))) (:deck runner)))
    :prompt "Choose a Run event" :effect (effect (play-instant target {:no-additional-cost true}))}

   "Plascrete Carapace"
   {:data [:counter 4]
    :prevent {:damage [:meat]}
    :abilities [{:counter-cost 1 :msg "prevent 1 meat damage"
                 :effect (req (damage-prevent state side :meat 1)
                              (when (= (:counter card) 0) (trash state side card)))}]}

   "Power Shutdown"
   {:req (req (:made-run runner-reg)) :prompt "Trash how many cards from the top R&D?"
    :choices {:number (req (count (:deck corp)))}
    :msg (msg "trash " target " cards from the top of R&D")
    :effect (req (mill state :corp target)
                 (let [n target]
                   (resolve-ability state :runner
                                    {:prompt "Choose a Program or piece of Hardware to trash"
                                     :choices {:req #(and (#{"Hardware" "Program"} (:type %))
                                                          (<= (:cost %) n))}
                                     :msg (msg "trash " (:title target)) :effect (effect (trash target))}
                                    card nil)))}

   "Priority Requisition"
   {:choices {:req #(and (= (:type %) "ICE") (not (:rezzed %)))}
    :msg (msg "rez " (:title target) " at no cost")
    :effect (effect (rez target {:no-cost true}))}

   "Private Contracts"
   {:data {:counter 14}
    :abilities [{:cost [:click 1] :counter-cost 2 :msg "gain 2 [Credits]"
                 :effect (req (gain state :corp :credit 2)
                              (when (= (:counter card) 0) (trash state :corp card)))}]}

   "Private Security Force"
   {:abilities [{:req (req tagged) :cost [:click 1] :effect (effect (damage :meat 1 {:card card}))
                 :msg "do 1 meat damage"}]}

   "Project Atlas"
   {:effect (effect (set-prop card :counter (max 0 (- (:advance-counter card) 3))))
    :abilities [{:counter-cost 1 :prompt "Choose a card" :label "Search R&D and add 1 card to HQ"
                 :msg (msg "add " (:title target) " to HQ from R&D")
                 :choices (req (:deck corp)) :effect (effect (move target :hand) (shuffle! :deck))}]}

   "Project Beale"
   {:effect (effect (set-prop card :counter (quot (- (:advance-counter card) 3) 2)
                              :agendapoints (+ 2 (quot (- (:advance-counter card) 3) 2))))}

   "Project Junebug"
   {:advanceable :always
    :access {:optional {:prompt "Pay 1 [Credits] to use Project Junebug ability?" :cost [:credit 1]
                        :req (req installed) :msg (msg "do " (* 2 (:advance-counter card)) " net damage")
                        :effect (effect (damage :net (* 2 (:advance-counter card)) {:card card}))}}}

   "Project Vitruvius"
   {:effect (effect (set-prop card :counter (- (:advance-counter card) 3)))
    :abilities [{:counter-cost 1 :prompt "Choose a card"
                 :msg (msg "add " (if (:seen target)
                                    (:title target) "an unseen card ") " to HQ from Archives")
                 :choices (req (:discard corp)) :effect (effect (move target :hand))}]}

   "Project Wotan"
   {:data [:counter 3]
    :abilities [{:counter-cost 1 :msg "add an 'End the run' subroutine to the approached ICE"}]}

   "Psychic Field"
   (let [ab {:psi {:req (req installed)
                   :not-equal {:msg (msg "do " (count (:hand runner)) " net damage")
                               :effect (effect (damage :net (count (:hand runner)) {:card card}))}}}]
     {:expose ab :access ab})

   "Public Sympathy"
   {:effect (effect (gain :max-hand-size 2)) :leave-play (effect (lose :max-hand-size 2))}

   "Public Terminal"
   {:recurring 1}

   "Power Nap"
   {:effect (effect (gain :credit (+ 2 (count (filter (fn [c] (has? c :subtype "Double"))
                                                      (:discard runner))))))}

   "Power Grid Overload"
   {:trace {:base 2 :msg (msg "trash 1 piece of hardware with install cost lower or equal to "
                              (- target (second targets)))
            :effect (req (let [max-cost (- target (second targets))]
                           (resolve-ability state side
                                            {:choices {:req #(and (has? % :type "Hardware")
                                                                  (<= (:cost %) max-cost))}
                                             :msg (msg "trash " (:title target))
                                             :effect (effect (trash target))}
                                            card nil)))}}

   "Power Tap"
   {:events {:trace {:msg "gain 1 [Credits]" :effect (effect (gain :runner :credit 1))}}}

   "Prepaid VoicePAD"
   {:recurring 1}

   "Primary Transmission Dish"
   {:recurring 3}

   "Professional Contacts"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit 1) (draw))
                 :msg "gain 1 [Credits] and draw 1 card"}]}

   "Psychographics"
   {:req (req tagged) :choices :credit :prompt "How many credits?"
    :effect (req (let [c (min target (:tag runner))]
                   (resolve-ability state side
                                    {:msg (msg "place " c " advancement tokens on "
                                               (if (:rezzed target) (:title target) "a card"))
                                     :choices {:req #(or (= (:type %) "Agenda") (:advanceable %))}
                                     :effect (effect (add-prop target :advance-counter c))} card nil)))}

   "Punitive Counterstrike"
   {:trace {:base 5 :msg (msg "do " (:stole-agenda runner-reg) " meat damage")
            :effect (effect (damage :meat (get-in runner [:register :stole-agenda]) {:card card}))}}

   "Push Your Luck"
   {:player :corp :prompt "Guess the amount the Runner will spend on Push Your Luck"
    :choices ["Even" "Odd"] :msg "make the Corp choose a guess"
    :effect (req (let [guess target]
                   (resolve-ability
                    state :runner
                    {:choices :credit :prompt "How many credits?"
                     :msg (msg "spend " target " [Credits]. The Corp guessed " guess)
                     :effect (req (when (or (and (= guess "Even") (odd? target))
                                            (and (= guess "Odd") (even? target)))
                                    (system-msg state :runner (str "gains " (* 2 target) " [Credits]"))
                                    (gain state :runner :credit (* 2 target))))} card nil)))}

   "Q-Coherence Chip"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))
    :events {:trash {:msg "trash itself" :req (req (= (last (:zone target)) :program))
                     :effect (effect (trash card))}}}

   "Quality Time"
   {:effect (effect (draw 5))}

   "Queens Gambit"
   {:choices ["0", "1", "2", "3"] :prompt "How many advancement tokens?"
    :effect (req (let [c (Integer/parseInt target)]
                   (resolve-ability
                    state side
                    {:choices {:req #(and (= (second (:zone %)) :remote)
                                          (= (last (:zone %)) :content)
                                          (not (:rezzed %)))}
                     :msg (msg "add " c " advancement tokens on a card and gain " (* 2 c) " [Credits]")
                     :effect (effect (gain :credit (* 2 c)) (add-prop :corp target :advance-counter c))}
                    card nil)))}

   "Quest Completed"
   {:req (req (and (some #{:hq} (:successful-run runner-reg))
                   (some #{:rd} (:successful-run runner-reg))
                   (some #{:archives} (:successful-run runner-reg))))
    :choices {:req #(= (first (:zone %)) :servers)} :msg (msg "access " (:title target))
    :effect (effect (handle-access targets))}

   "Quetzal: Free Spirit"
   {:abilities [{:once :per-turn :msg "break 1 barrier subroutine"}]}

   "R&D Interface"
   {:effect (effect (gain :rd-access 1)) :leave-play (effect (lose :rd-access 1))}

   "Rabbit Hole"
   {:effect
    (effect (gain :link 1)
            (resolve-ability
             {:optional {:req (req (some #(when (= (:title %) "Rabbit Hole") %) (:deck runner)))
                         :prompt "Install another Rabbit Hole?" :msg "install another Rabbit Hole"
                         :effect (req (when-let [c (some #(when (= (:title %) "Rabbit Hole") %)
                                                         (:deck runner))]
                                        (runner-install state side c)
                                        (shuffle! state :runner :deck)))}} card nil))
    :leave-play (effect (lose :link 1))}

   "Rachel Beckman"
   {:effect (req (gain state :runner :click 1 :click-per-turn 1)
                 (add-watch state :rachel-beckman
                            (fn [k ref old new]
                              (when (> (get-in new [:runner :tag]) 0)
                                (remove-watch state :rachel-beckman)
                                (trash ref :runner card)
                                (system-msg ref side "trashes Rachel Beckman for being tagged")))))
    :leave-play (effect (lose :click 1 :click-per-turn 1))}

   "Recon"
   {:prompt "Choose a server" :choices (req servers) :effect (effect (run target))}

   "Reclamation Order"
   {:prompt "Choose a card from Archives" :msg (msg "add copies of " (:title target) " to HQ")
    :choices (req (filter #(not= (:title %) "Reclamation Order") (:discard corp)))
    :effect (req (doseq [c (filter #(= (:title target) (:title %)) (:discard corp))]
                   (move state side c :hand)))}

   "Reina Roja: Freedom Fighter"
   {:effect (effect (gain :link 1))
    :events {:pre-rez
             {:req (req (= (:type target) "ICE")) :once :per-turn
              :effect (effect (rez-cost-bonus 1))}}}

   "Replicator"
   {:events {:runner-install
             {:optional {:req (req (= (:type target) "Hardware"))
                         :prompt "Use Replicator to add a copy?"
                         :msg (msg "add a copy of " (:title target) " to his Grip")
                         :effect (effect (move (some #(when (= (:title %) (:title target)) %)
                                                     (:deck runner)) :hand)
                                         (shuffle! :deck))}}}}

   "Research Station"
   {:init {:root "HQ"}
    :effect (effect (gain :max-hand-size 2)) :leave-play (effect (lose :max-hand-size 2))}

   "Restoring Face"
   {:prompt "Choose a Sysop, Executive or Clone to trash"
    :msg (msg "trash " (:title target) " to remove 2 bad publicity")
    :choices (req (filter #(and (:rezzed %)
                                (or (has? % :subtype "Clone") (has? % :subtype "Executive")
                                    (has? % :subtype "Sysop")))
                          (mapcat :content (flatten (seq (:servers corp))))))
    :effect (effect (lose :bad-publicity 2) (trash target))}

   "Restructure"
   {:effect (effect (gain :credit 15))}

   "Restructured Datapool"
   {:abilities [{:cost [:click 1]
                 :trace {:base 2 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}]}

   "Retrieval Run"
   {:effect (effect (run :archives
                      {:replace-access
                       {:prompt "Choose a program to install" :msg (msg "install " (:title target))
                        :choices (req (filter #(= (:type %) "Program") (:discard runner)))
                        :effect (effect (runner-install target {:no-cost true}))}} card))}

   "Reuse"
   {:choices {:max 100 :req #(and (:side % "Corp") (= (:zone %) [:hand]))}
    :msg (msg "trash " (join ", " (map :title targets)) " and gain " (* 2 (count targets)) " [Credits]")
    :effect (effect (trash-cards targets) (gain :credit (* 2 (count targets))))}

   "Reversed Accounts"
   {:advanceable :always
    :abilities [{:cost [:click 1]
                 :effect (effect (lose :runner :credit (* 4 (:advance-counter card))) (trash card))}]}

   "Rework"
   {:prompt "Choose a card to shuffle into R&D" :choices (req (:hand corp))
    :effect (effect (move target :deck) (shuffle! :deck))}

   "Rex Campaign"
   {:data [:counter 3]
    :events {:corp-turn-begins
             {:effect (req (add-prop state side card :counter -1)
                           (when (= (:counter card) 1)
                             (trash state side card)
                             (resolve-ability state side
                                              {:prompt "Remove 1 bad publicity or gain 5 [Credits]?"
                                               :choices ["Remove 1 bad publicity" "Gain 5 credits"]
                                               :msg (msg (.toLowerCase target))
                                               :effect (req (if (= target "Remove 1 bad publicity")
                                                              (lose state side :bad-publicity 1)
                                                              (gain state side :credit 5)))}
                                              card targets)))}}}

   "Ronin"
   {:advanceable :always
    :abilities [{:cost [:click 1] :req (req (>= (:advance-counter card) 4))
                 :msg "do 3 net damage" :effect (effect (trash card) (damage :net 3 {:card card}))}]}

   "Rook"
   {:abilities [{:label "Host Rook on a piece of ICE" :cost [:click 1]
                 :choices {:req #(and (= (:type %) "ICE")
                                      (= (last (:zone %)) :ices)
                                      (not (some (fn [c] (has? c :subtype "Caïssa")) (:hosted %))))}
                 :msg (msg "host it on " (if (:rezzed target) (:title target) "a piece of ICE")) 
                 :effect (effect (host target card))}]
    :events {:pre-rez {:req (req (= (:zone (:host card)) (:zone target)))
                       :effect (effect (rez-cost-bonus 2))}}}

   "Running Interference"
   {:prompt "Choose a server" :choices (req servers)
    :effect (effect (run target)
                    (register-events {:pre-rez
                                      {:req (req (= (:type target) "ICE"))
                                       :effect (effect (rez-cost-bonus (:cost target)))}
                                      :run-ends
                                      {:effect (effect (unregister-events card))}}
                                     (assoc card :zone '(:discard))))
    :events {:pre-rez nil :run-ends nil}}

   "Ryon Knight"
   {:abilities [{:msg "do 1 brain damage" :req (req (and this-server (zero? (:click runner))))
                 :effect (effect (trash card) (damage :brain 1 {:card card}))}]}

   "Sacrificial Clone"
   {:prevent {:damage [:meat :net :brain]}
    :abilities [{:effect (req (doseq [c (concat (get-in runner [:rig :hardware])
                                                (filter #(not (has? % :subtype "Virtual"))
                                                        (get-in runner [:rig :resource]))
                                                (:hand runner))]
                                (trash state side c {:cause :ability-cost}))
                              (lose state side :credit :all :tag :all)
                              (damage-prevent state side :net Integer/MAX_VALUE)
                              (damage-prevent state side :meat Integer/MAX_VALUE)
                              (damage-prevent state side :brain Integer/MAX_VALUE))}]}

   "Sacrificial Construct"
   {:prevent {:trash [:program :hardware]}
    :abilities [{:effect (effect (trash-prevent :program 1) (trash-prevent :hardware 1)
                                 (trash card {:cause :ability-cost}))}]}

   "Sahasrara"
   {:recurring 2}

   "Same Old Thing"
   {:abilities [{:cost [:click 2]
                 :prompt "Choose an event to play" :msg (msg "play " (:title target))
                 :choices (req (filter #(and (has? % :type "Event")
                                             (<= (:cost %) (:credit runner))) (:discard runner)))
                 :effect (effect (trash card {:cause :ability-cost}) (play-instant target))}]}

   "SanSan City Grid"
   {:effect (req (when-let [agenda (some #(when (= (:type %) "Agenda") %) (:content (card->server state card)))]
                   (update-advancement-cost state side agenda)))
    :events {:corp-install {:req (req (and (= (:type target) "Agenda") (= (:zone card) (:zone target))))
                            :effect (effect (update-advancement-cost target))}
             :pre-advancement-cost {:req (req (= (:zone card) (:zone target)))
                               :effect (effect (advancement-cost-bonus -1))}}}

   "Satellite Uplink"
   {:msg (msg "expose " (join ", " (map :title targets)))
    :choices {:max 2 :req #(= (first (:zone %)) :servers)}
    :effect (req (doseq [c targets] (expose state side c)))}

   "Savoir-faire"
   {:abilities [{:cost [:credit 2] :once :per-turn :msg (msg "install " (:title target))
                 :prompt "Choose a program to install"
                 :choices (req (filter #(= (:type %) "Program") (:hand runner)))
                 :effect (effect (runner-install target))}]}

   "Scavenge"
   {:choices {:req #(= (:type %) "Program")}
    :effect
    (req (let [trashed target]
           (trash state side trashed)
           (resolve-ability
            state side
            {:prompt "Install a card from Grip or Heap?" :choices ["Grip" "Heap"]
             :effect (req (let [fr target]
                            (system-msg state side (str "trashes " (:title trashed) " to install a card from " fr))
                            (resolve-ability
                             state side
                             {:prompt "Choose a program to install"
                              :choices (req (filter #(and (= (:type %) "Program")
                                                          (<= (:cost %) (+ (:credit runner) (:cost trashed))))
                                                    ((if (= fr "Grip") :hand :discard) runner)))
                              :effect (effect (register-events {:pre-install
                                                                {:effect
                                                                 (effect (install-cost-bonus (- (:cost trashed)))
                                                                         (unregister-events card))}}
                                                               (assoc card :zone '(:discard)))
                                              (runner-install target))} card nil)))} card nil)))
    :events {:pre-install nil}}

   "Scheherazade"
   {:abilities [{:cost [:click 1] :prompt "Choose a program to install on Scherazade"
                 :choices (req (filter #(and (has? % :type "Program")
                                             (<= (:cost %) (:credit runner))
                                             (<= (:memoryunits %) (:memory runner)))
                                       (:hand runner)))
                 :msg (msg "host " (:title target) " and gain 1 [Credits]")
                 :effect (effect (runner-install target {:host-card card}) (gain :credit 1))}]}

   "Scorched Earth"
   {:req (req tagged) :effect (effect (damage :meat 4 {:card card}))}

   "Scrubbed"
   {:events (let [sc {:effect (req (update! state side (dissoc card :scrubbed-target)))}]
                 {:encounter-ice {:once :per-turn
                                  :effect (effect (update! (assoc card :scrubbed-target target)))}
                  :pre-ice-strength {:req (req (= (:cid target) (:cid (:scrubbed-target card))))
                                     :effect (effect (ice-strength-bonus -2))}
                  :pass-ice sc :run-ends sc})}

   "Scrubber"
   {:recurring 2}

   "Sealed Vault"
   {:abilities [{:label "Store any number of [Credits] on Sealed Vault" :cost [:credit 1]
                 :prompt "How many [Credits]?" :choices :credit :msg (msg "store " target " [Credits]")
                 :effect (effect (add-prop card :counter target))}
                {:label "Spend [Click] to move any number of [Credits] to your credit pool"
                 :cost [:click 1] :prompt "How many [Credits]?"
                 :choices :counter :msg (msg "spend [Click] to gain " target " [Credits]")
                 :effect (effect (gain :credit target))}
                {:label "Trash Sealed Vault to move any number of [Credits] to your credit pool"
                 :prompt "How many [Credits]?" :choices :counter
                 :msg (msg "trash it and gain " target " [Credits]")
                 :effect (effect (gain :credit target) (trash card))}]}

   "SEA Source"
   {:req (req (:successful-run runner-reg))
    :trace {:base 3 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}

   "Security Subcontract"
   {:abilities [{:choices {:req #(and (= (:type %) "ICE") (:rezzed %))} :cost [:click 1]
                 :msg (msg "trash " (:title target) " to gain 4 [Credits]")
                 :label "Trash a rezzed ICE to gain 4 [Credits]"
                 :effect (effect (trash target) (gain :credit 4))}]}

   "Security Testing"
   {:events {:runner-turn-begins
             {:prompt "Choose a server for Security Testing" :choices (req servers)
              :msg (msg "target " target)
              :effect (effect (update! (assoc card :testing-target (vec (next (server->zone state target))))))}
             :successful-run
             {:req (req (= (get-in @state [:run :server]) (get (get-card state card) :testing-target)))
              :once :per-turn
              :effect (req (let [st card]
                             (swap! state assoc-in [:run :run-effect :replace-access]
                                    {:mandatory true
                                     :effect (effect (resolve-ability
                                                      {:msg "gain 2 [Credits] instead of accessing"
                                                       :effect (effect (gain :credit 2))} st nil))})))}}}

   "Self-destruct"
   {:abilities [{:req (req this-server)
                 :label "Trace X - Do 3 net damage"
                 :effect (req (let [serv (card->server state card)
                                    cards (concat (:ices serv) (:content serv))]
                                   (trash state side card)
                                   (doseq [c cards] (trash state side c))
                                   (resolve-ability
                                     state side
                                     {:trace {:base (req (dec (count cards)))
                                              :effect (effect (damage :net 3 {:card card}))
                                              :msg "do 3 net damage"}} card nil)))}]}

   "Self-Destruct Chips"
   {:effect (effect (lose :runner :max-hand-size 1))}

   "Self-modifying Code"
   {:abilities [{:prompt "Choose a program to install" :msg (msg "install " (:title target))
                 :priority true
                 :choices (req (filter #(has? % :type "Program") (:deck runner)))
                 :cost [:credit 2]
                 :effect (effect (trash card {:cause :ability-cost}) (runner-install target) (shuffle! :deck))}]}

   "Sentinel Defense Program"
   {:events {:damage {:req (req (= target :brain)) :msg "to do 1 net damage"
                      :effect (effect (damage :net 1 {:card card})) }}}

   "Server Diagnostics"
   {:events {:corp-turn-begins {:effect (effect (gain :credit 2)) :msg "gain 2 [Credits]"}
             :corp-install {:req (req (has? target :type "ICE"))
                            :effect (effect (trash card)
                                            (system-msg "trashes Server Diagnostics"))}}}

   "Shattered Remains"
   {:advanceable :always
    :access
    {:optional
     {:req (req installed) :prompt "Pay 1 [Credits] to use Shattered Remains ability?"
      :cost [:credit 1]
      :effect (req (let [shat card]
                     (resolve-ability
                       state side (assoc (assoc-in trash-hardware [:choices :max] (req (:advance-counter shat)))
                                    :effect (effect (trash-cards targets))) shat nil)))}}}

   "Shell Corporation"
   {:abilities
    [{:cost [:click 1] :msg "store 3 [Credits]" :once :per-turn
      :effect (effect (add-prop card :counter 3))}
     {:cost [:click 1] :msg (msg "gain " (:counter card) " [Credits]") :once :per-turn
      :label "Take all credits"
      :effect (effect (gain :credit (:counter card)) (set-prop card :counter 0))}]}

   "Shi.Kyū"
   {:access {:req (req (not= (first (:zone card)) :deck))
             :prompt "How many [Credits] for Shi.Kyū?" :choices :credit
             :msg (msg "attempt to do " target " net damage")
             :effect (effect (resolve-ability
                              {:player :runner :msg (msg target)
                               :prompt (str "Take " target " net damage or take Shi.Kyū as -1 agenda points?")
                               :choices [(str "take " target " net damage") "add Shi.Kyū to score area"]
                               :effect (let [dmg target]
                                         (req (if (= target "add Shi.Kyū to score area")
                                                (do (gain state :runner :agenda-point -1)
                                                    (move state :runner card :scored nil))
                                                (damage state :corp :net dmg {:card card}))))}
                              card targets))}}

   "Shipment from Kaguya"
   {:choices {:max 2 :req #(or (= (:advanceable %) "always")
                               (and (= (:advanceable %) "while-rezzed") (:rezzed %))
                               (= (:type %) "Agenda"))}
    :msg (msg "1 advancement tokens on " (count targets) " cards")
    :effect (req (doseq [t targets] (add-prop state :corp t :advance-counter 1)))}

   "Shipment from SanSan"
   {:choices ["0", "1", "2"] :prompt "How many advancement tokens?"
    :effect (req (let [c (Integer/parseInt target)]
                   (resolve-ability
                    state side
                    {:choices {:req #(or (= (:advanceable %) "always")
                                         (and (= (:advanceable %) "while-rezzed") (:rezzed %))
                                         (= (:type %) "Agenda"))}
                     :msg (msg "add " c " advancement tokens on a card")
                     :effect (effect (add-prop :corp target :advance-counter c))} card nil)))}

   "Shock!"
   {:access {:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}}

   "Shoot the Moon"
   {:choices {:req #(and (= (:type %) "ICE") (not (:rezzed %)))
              :max (req (min (:tag runner)
                             (reduce (fn [c server]
                                       (+ c (count (filter #(not (:rezzed %)) (:ices server)))))
                                     0 (flatten (seq (:servers corp))))))}
    :req (req tagged)
    :effect (req (doseq [t targets] (rez state side t {:no-cost true})))}

   "Silencer"
   {:recurring 1}

   "Silhouette: Stealth Operative"
   {:events {:successful-run
             {:req (req (= target :hq)) :once :per-turn
              :effect (effect (resolve-ability {:choices {:req #(= (first (:zone %)) :servers)}
                                                :effect (effect (expose target)) :msg "expose 1 card"}
                                               card nil))}}}

   "Simone Diego"
   {:recurring 2}

   "Singularity"
   {:prompt "Choose a server" :choices (req remotes)
    :effect (effect (run target
                      {:replace-access
                       {:msg "trash all cards in the server at no cost"
                        :effect (req (doseq [c (get-in (:servers corp) (conj (:server run) :content))]
                                       (trash state side c)))}} card))}

   "Skulljack"
   {:effect (effect (damage :brain 1 {:card card}))
    :events {:pre-trash {:effect (effect (trash-cost-bonus -1))}}}

   "Snatch and Grab"
   {:trace {:base 3 :choices {:req #(has? % :subtype "Connection")}
            :msg (msg "attempt to trash " (:title target))
            :effect (req (let [c target]
                           (resolve-ability
                            state side
                            {:prompt (msg "Take 1 tag to prevent " (:title c) " from being trashed?")
                             :choices ["Yes" "No"] :player :runner
                             :effect (effect (resolve-ability
                                              (if (= target "Yes")
                                                {:msg (msg "take 1 tag to prevent " (:title c)
                                                           " from being trashed")
                                                 :effect (effect (gain :runner :tag 1))}
                                                {:effect (trash state side c) :msg (msg "trash " (:title c))})
                                              card nil))}
                            card nil)))}}

   "Sneakdoor Beta"
   {:abilities [{:cost [:click 1] :msg "make a run on Archives"
                 :effect (effect (run :archives
                                   {:successful-run
                                    {:msg "make a successful run on HQ"
                                     :effect (req (swap! state assoc-in [:run :server] [:hq]))}} card))}]}

   "Snare!"
   {:access {:optional {:req (req (not= (first (:zone card)) :discard))
                        :prompt "Pay 4 [Credits] to use Snare! ability?" :cost [:credit 4]
                        :msg "do 3 net damage and give the Runner 1 tag"
                        :effect (effect (damage :net 3 {:card card}) (gain :runner :tag 1))}}}

   "Snitch"
   {:abilities [{:once :per-run :req (req current-ice) :msg (msg "expose " (:title current-ice))
                 :effect (effect (expose current-ice)
                                 (resolve-ability {:optional {:prompt "Jack out?" :msg "jack out"
                                                              :effect (effect (jack-out nil))}}
                                                  card nil))}]}

   "Space Camp"
   {:access {:msg (msg "place 1 advancement token on " (if (:rezzed target) (:title target) "a card"))
             :choices {:req #(or (= (:type %) "Agenda") (:advanceable %))}
             :effect (effect (add-prop target :advance-counter 1))}}

   "Special Order"
   {:prompt "Choose an Icebreaker"
    :effect (effect (system-msg (str "adds " (:title target) " to his Grip and shuffles his Stack"))
                    (move target :hand) (shuffle! :deck))
    :choices (req (filter #(has? % :subtype "Icebreaker") (:deck runner)))}

   "Spinal Modem"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1)) :recurring 2
    :events {:successful-trace {:req (req run) :effect (effect (damage :brain 1 {:card card}))}}}

   "Spooned"
   {:prompt "Choose a server" :choices (req servers) :effect (effect (run target))}

   "Starlight Crusade Funding"
   {:events {:runner-turn-begins {:msg "lose [Click]" :effect (effect (lose :click 1))}}}

   "Stim Dealer"
   {:events {:runner-turn-begins
             {:effect (req (if (>= (:counter card) 2)
                             (do (set-prop state side card :counter 0)
                                 (damage state side :brain 1 {:unpreventable true :card card})
                                 (system-msg state side "takes 1 brain damage from Stim Dealer"))
                             (do (add-prop state side card :counter 1)
                                 (gain state side :click 1)
                                 (system-msg state side "uses Stim Dealer to gain [Click]"))))}}}

   "Stimhack"
   {:prompt "Choose a server" :choices (req servers)
    :effect (effect (gain :credit 9)
                    (run target {:end-run
                                 {:msg " take 1 brain damage"
                                  :effect (effect (damage :brain 1 {:unpreventable true :card card}))}}
                      card))}

   "Sub Boost"
   {:choices {:req #(and (= (:type %) "ICE") (:rezzed %))}
    :effect (effect (host target (assoc card :zone [:discard] :seen true)))}

   "Subliminal Messaging"
   {:effect (effect (gain :credit 1)
                    (resolve-ability {:once :per-turn :once-key :subliminal-messaging
                                      :effect (effect (gain :corp :click 1))} card nil))}

   "Successful Demonstration"
   {:req (req (:unsuccessful-run runner-reg)) :effect (effect (gain :credit 7))}

   "Sundew"
   {:events {:runner-spent-click {:req (req (not (= (:server run) (:zone card)))) :once :per-turn
                                  :msg "gain 2 [Credits]" :effect (effect (gain :corp :credit 2))}}}

   "Superior Cyberwalls"
   {:msg (msg "gain " (reduce (fn [c server]
                                (+ c (count (filter (fn [ice] (and (has? ice :subtype "Barrier")
                                                                   (:rezzed ice))) (:ices server)))))
                              0 (flatten (seq (:servers corp))))
              " [Credits]")
    :effect (req (do (gain state :corp :credit
                          (reduce (fn [c server]
                                    (+ c (count (filter (fn [ice] (and (has? ice :subtype "Barrier")
                                                                       (:rezzed ice))) (:ices server)))))
                                  0 (flatten (seq (:servers corp)))))
                     (update-all-ice state side)))
    :events {:pre-ice-strength {:req (req (has? target :subtype "Barrier"))
                                :effect (effect (ice-strength-bonus 1))}}}

   "Sure Gamble"
   {:effect (effect (gain :credit 9))}

   "Surge"
   {:msg (msg "place 2 virus token on " (:title target))
    :choices {:req #(has? % :subtype "Virus")}
    :effect (effect (add-prop target :counter 2))}

   "Sweeps Week"
   {:effect (effect (gain :credit (count (:hand runner))))}

   "Symmetrical Visage"
   {:events {:runner-click-draw {:once :per-turn :msg "gain 1 [Credits]"
                                 :effect (effect (gain :credit 1))}}}

   "Synthetic Blood"
   {:events {:damage {:once :per-turn :msg "draw 1 card" :effect (effect (draw :runner))}}}

   "Tech Startup"
   {:abilities [{:label "Install an asset from R&D"
                 :prompt "Choose an asset to install" :msg (msg "install " (:title target))
                 :choices (req (filter #(has? % :type "Asset") (:deck corp)))
                 :effect (effect (trash card) (corp-install target nil) (shuffle! :deck))}]}

   "Tennin Institute: The Secrets Within"
   {:abilities [{:msg "add 1 advancement counter on a card" :choices {:req #(= (first (:zone %)) :servers)}
                 :req (req (not (:successful-run runner-reg))) :once :per-turn
                 :effect (effect (add-prop target :advance-counter 1))}]}

   "Test Ground"
   {:advanceable :always
    :abilities [{:label "Derez 1 card for each advancement token"
                 :msg (msg "derez " (:advance-counter card)) :effect (effect (trash card))}]}

   "Test Run"
   {:prompt "Install a card from Stack or Heap?" :choices ["Stack" "Heap"]
    :msg (msg "install a card from " target) :effect
    (effect (resolve-ability
             {:prompt "Choose a card to install"
              :choices (req (filter #(= (:type %) "Program")
                                    ((if (= target "Heap") :discard :deck) runner)))
              :effect (effect (runner-install (assoc-in target [:special :test-run] true) {:no-cost true}))
              :end-turn
              {:req (req (some #(when (and (= (:cid target) (:cid %)) (get-in % [:special :test-run])) %)
                               (get-in runner [:rig :program])))
               :msg (msg "move " (:title target) " on top of Stack")
               :effect (req (move state side (some #(when (= (:cid target) (:cid %)) %)
                                                   (get-in runner [:rig :program])) :deck true))}}
             card targets))}

   "TGTBT"
   {:access {:msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}

   "The Board"
   {:effect (effect (lose :runner :agenda-point
                          (count (filter #(> (:agendapoints %) 0) (:scored runner)))))
    :leave-play (effect (gain :runner :agenda-point
                              (count (filter #(> (:agendapoints %) 0) (:scored runner)))))
    :trash-effect {:req (req (:access @state))
                   :effect (effect (move :runner card :scored) (gain :runner :agenda-point 2))}
    :events {:agenda-stolen {:req (req (> (:agendapoints target) 0))
                             :effect (effect (lose :runner :agenda-point 1))}}}

   "The Cleaners"
   {:events {:pre-damage {:req (req (= target :meat)) :msg "to do 1 additional meat damage"
                          :effect (effect (damage-bonus :meat 1))}}}

   "The Foundry: Refining the Process"
   {:events
    {:rez {:req (req (and (= (:type target) "ICE") (some #(= (:title %) (:title target)) (:deck corp))))
           :optional
           {:prompt "Add another copy to HQ?" :once :per-turn
            :effect (effect (move (some #(when (= (:title %) (:title target)) %) (:deck corp)) :hand)
                            (shuffle! :deck))
            :msg (msg "add a copy of " (:title target) " from R&D to HQ")}}}}

   "The Future Perfect"
   {:steal-req (req installed)
    :access {:psi {:req (req (not installed)) :equal {:effect (effect (steal card))}}}}

   "The Helpful AI"
   {:effect (effect (gain :link 1)) :leave-play (effect (lose :link 1))
    :abilities [{:msg "give an icebreaker +2 strength" :effect (effect (trash card {:cause :ability-cost}))}]}

   "The Makers Eye"
   {:effect (effect (run :rd) (access-bonus 2))}

   "The Personal Touch"
   {:hosting {:req #(and (has? % :subtype "Icebreaker") (:installed %))}
    :effect (effect (update-breaker-strength (:host card)))
    :events {:pre-breaker-strength {:req (req (= (:cid target) (:cid (:host card))))
                                    :effect (effect (breaker-strength-bonus 1))}}}

   "The Supplier"
   {:abilities [{:label "Host a resource or piece of hardware" :cost [:click 1]
                 :prompt "Choose a card to host on The Supplier"
                 :choices (req (filter #(#{"Resource" "Hardware"} (:type %)) (:hand runner)))
                 :effect (effect (host card target)) :msg (msg "host " (:title target) "")}]
    :events {:runner-turn-begins
             {:prompt "Choose a card on The Supplier to install"
              :choices (req (conj (filter #(<= (- (or (:cost %) 0) 2) (:credit runner)) (:hosted card))
                                  "No install"))
              :req (req (not (string? target)))
              :msg (msg "install " (:title target) " lowering its install cost by 2")
              :effect (effect (gain :credit (min 2 (:cost target))) (runner-install target))}}}

   "Theophilius Bagbiter"
   {:effect (req (lose state :runner :credit :all)
                 (add-watch state :theophilius-bagbiter
                            (fn [k ref old new]
                              (let [credit (get-in new [:runner :credit])]
                                (when (not= (get-in old [:runner :credit]) credit)
                                  (swap! ref assoc-in [:runner :max-hand-size] credit))))))
    :leave-play (req (remove-watch state :theophilius-bagbiter))}

   "The Root"
   {:recurring 3}

   "The Toolbox"
   {:effect (effect (gain :link 2 :memory 2)) :leave-play (effect (lose :link 2 :memory 2))
    :recurring 2}

   "Three Steps Ahead"
   {:end-turn {:effect (effect (gain :credit (* 2 (count (:successful-run runner-reg)))))
               :msg (msg "gain " (* 2 (count (:successful-run runner-reg))) " [Credits]")}}

   "Thomas Haas"
   {:advanceable :always
    :abilities [{:label "Gain credits" :msg (msg "gain " (* 2 (:advance-counter card)) " [Credits]")
                 :effect (effect (gain :credit (* 2 (:advance-counter card))) (trash card))}]}

   "Titan Transnational: Investing In Your Future"
   {:events {:agenda-scored {:msg (msg "add 1 agenda counter to " (:title target))
                             :effect (effect (add-prop target :counter 1))}}}

   "Titanium Ribs"
   {:effect (effect (damage :meat 2 {:card card}))}

   "Toshiyuki Sakai"
   {:advanceable :always}

   "Trade-In"
   {:prompt "Choose a hardware to trash" :choices (req (get-in runner [:rig :hardware]))
    :msg (msg "trash " (:title target) " and gain " (quot (:cost target) 2) " [Credits]")
    :effect (effect (trash target) (gain [:credit (quot (:cost target) 2)])
                    (resolve-ability {:prompt "Choose a Hardware to add to Grip from Stack"
                                      :choices (req (filter #(= (:type %) "Hardware") (:deck runner)))
                                      :msg (msg "adds " (:title target) " to his Grip")
                                      :effect (effect (move target :hand))} card nil))}

   "Traffic Accident"
   {:req (req (>= (:tag runner) 2)) :effect (effect (damage :meat 2 {:card card}))}

   "Traffic Jam"
   {:effect (effect (update-all-advancement-costs))
    :leave-play (effect (update-all-advancement-costs))
    :events {:pre-advancement-cost
             {:effect (req (advancement-cost-bonus
                             state side (count (filter #(= (:title %) (:title target)) (:scored corp)))))}}}

   "Tri-maf Contact"
   {:abilities [{:cost [:click 1] :msg "gain 2 [Credits]" :once :per-turn
                 :effect (effect (gain :credit 2))}]
    :leave-play (effect (damage :meat 3 {:unboostable true :card card}))}

   "Trick of Light"
   {:choices {:req #(and (contains? % :advance-counter) (> (:advance-counter %) 0))}
    :effect
    (req (let [fr target tol card]
           (resolve-ability
            state side
            {:prompt "Move how many advancement tokens?"
             :choices (take (inc (:advance-counter fr)) ["0" "1" "2"])
             :effect (req (let [c (Integer/parseInt target)]
                            (resolve-ability
                             state side
                             {:prompt  "Move to where?"
                              :choices {:req #(and (not= (:cid fr) (:cid %))
                                                   (or (= (:advanceable %) "always")
                                                       (and (= (:advanceable %) "while-rezzed") (:rezzed %))
                                                       (= (:type %) "Agenda")))}
                              :effect  (effect (add-prop :corp target :advance-counter c)
                                               (add-prop :corp fr :advance-counter (- c))
                                               (system-msg (str "moves " c " advancement tokens from "
                                                                (if (:rezzed fr) (:title fr) "a card") " to "
                                                                (if (:rezzed target) (:title target) "a card"))))}
                             tol nil)))}
            card nil)))}

   "Turntable"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))}

   "Turtlebacks"
   {:events {:server-created {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Tyrs Hand"
   {:abilities [{:label "Prevent a subroutine on a Bioroid from being broken"
                 :req (req (and (= (butlast (:zone current-ice)) (butlast (:zone card)))
                                (has? current-ice :subtype "Bioroid"))) :effect (effect (trash card))
                 :msg (msg "prevent a subroutine on " (:title current-ice) " from being broken")}]}

   "Tyson Observatory"
   {:abilities [{:prompt "Choose a piece of Hardware" :msg (msg "adds " (:title target) " to his Grip")
                 :choices (req (filter #(has? % :type "Hardware") (:deck runner)))
                 :cost [:click 2] :effect (effect (move target :hand) (shuffle! :deck))}]}

   "Underway Renovation"
   {:install-state :face-up
    :events {:advance {:req (req (= (:cid card) (:cid target)))
                       :effect (effect (mill :runner
                                          (if (>= (:advance-counter (get-card state card)) 4) 2 1)))}}}

   "Underworld Contact"
   {:events {:runner-turn-begins {:msg "gain 1 [Credits]" :req (req (>= (:link runner) 2))
                                  :effect (effect (gain :credit 1))}}}

   "Uninstall"
   {:choices {:req #(and (= (first (:zone %)) :rig) (#{"Program" "Hardware"} (:type %)))}
    :msg (msg "move " (:title target) " to his or her grip")
    :effect (effect (move target :hand))}

   "Unorthodox Predictions"
   {:prompt "Choose an ICE type for Unorthodox Predictions" :choices ["Sentry", "Code Gate", "Barrier"]
    :msg (msg "prevent subroutines on " target " ICE from being broken until next turn.")}

   "Unregistered S&W 35"
   {:abilities
    [{:cost [:click 2] :req (req (some #{:hq} (:successful-run runner-reg)))
      :label "trash a Bioroid, Clone, Executive or Sysop" :prompt "Choose a card to trash"
      :choices (req (filter #(and (:rezzed %)
                                  (or (has? % :subtype "Bioroid") (has? % :subtype "Clone")
                                      (has? % :subtype "Executive") (has? % :subtype "Sysop")))
                            (mapcat :content (flatten (seq (:servers corp))))))
      :msg (msg "trash " (:title target)) :effect (effect (trash target))}]}

   "Utopia Shard"
   {:abilities [{:effect (effect (trash-cards :corp (take 2 (shuffle (:hand corp))))
                                 (trash card {:cause :ability-cost}))
                 :msg "force the Corp to discard 2 cards from HQ at random"}]}

   "Valencia Estevez: The Angel of Cayambe"
   {:effect (effect (gain :corp :bad-publicity 1))}

   "Vamp"
   {:effect (effect (run :hq {:replace-access
                              {:prompt "How many [Credits]?" :choices :credit
                               :msg (msg "take 1 tag and make the Corp lose " target " [Credits]")
                               :effect (effect (lose :corp :credit target) (gain :tag 1))}} card))}

   "Veterans Program"
   {:effect (effect (lose :bad-publicity 2))}

   "Vigil"
   {:effect (effect (gain :memory 1)) :leave-play (effect (lose :memory 1))
    :events {:runner-turn-begins {:req (req (= (count (:hand corp)) (:max-hand-size corp)))
                                  :msg "draw 1 card" :effect (effect (draw 1))}}}

   "Virus Breeding Ground"
   {:data {:counter-type "Virus"}
    :events {:runner-turn-begins {:effect (effect (add-prop card :counter 1))}}
    :abilities [{:cost [:click 1] :counter-cost 1 :msg (msg "move 1 virus counter to " (:title target))
                 :choices {:req #(and (has? % :subtype "Virus") (>= (:counter %) 1))}
                 :effect (effect (add-prop target :counter 1))}]}

   "Vulcan Coverup"
   {:msg "do 2 meat damage" :effect (effect (damage :meat 2 {:card card}))
    :stolen {:msg "force the Corp to take 1 bad publicity"
             :effect (effect (gain :corp :bad-publicity 1))}}

   "Wanton Destruction"
   {:effect (effect
             (run :hq {:replace-access
                       {:msg (msg "Wanton Destruction to force the Corp to discard " target
                                  " cards from HQ at random")
                        :prompt "How many [Click] do you want to spend?"
                        :choices (req (map str (range 1 (inc (:click runner)))))
                        :effect (req (let [n (Integer/parseInt target)]
                                       (when (pay state :runner card :click n)
                                         (trash-cards state :corp (take n (shuffle (:hand corp)))))))}} card))}

   "Weyland Consortium: Because We Built It"
   {:recurring 1}

   "Weyland Consortium: Building a Better World"
   {:events {:play-operation {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))
                              :req (req (has? target :subtype "Transaction"))}}}

   "Window"
   {:abilities [{:cost [:click 1] :msg "draw 1 card from the bottom of his Stack"
                 :effect (effect (move (last (:deck runner)) :hand))}]}

   "Whizzard: Master Gamer"
   {:recurring 3}

   "Will-o-the-Wisp"
   {:abilities [{:label "Add an icebreaker to the bottom of Stack"
                 :choices {:req #(has? % :subtype "Icebreaker")}
                 :msg (msg "add " (:title target) " to the bottom of Stack")
                 :effect (effect (trash card) (move :runner target :deck))}]}

   "Witness Tampering"
   {:effect (effect (lose :bad-publicity 2))}

   "Woman in the Red Dress"
   {:events {:runner-turn-begins
             {:msg (msg "reveal " (:title (first (:deck corp))) " on the top of R&D")
              :optional {:prompt (msg "Draw " (:title (first (:deck corp))) "?")
                         :msg (msg "draw " (:title (first (:deck corp))))
                         :no-msg "doesn't draw with Woman in the Red Dress"
                         :player :corp :effect (effect (draw))}}}}

   "Wyldside"
   {:events {:runner-turn-begins {:msg "draw 2 cards and lose [Click]"
                                  :effect (effect (lose :click 1) (draw 2))}}}

   "Xanadu"
   {:events {:pre-rez {:req (req (= (:type target) "ICE"))
                       :effect (effect (rez-cost-bonus 1))}}}
   ;; Icebreakers

   "Alpha"
   {:abilities [{:cost [:credit 1] :req (req (and run (zero? (:position run))))
                 :msg "break 1 subroutine on the outermost ICE protecting this server"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Alias"
   {:abilities [{:cost [:credit 1] :req (req (#{:hq :rd :archives} (first (:server run))))
                 :msg "break 1 sentry subroutine"}
                {:cost [:credit 2] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Atman"
   {:prompt "How many power counters?" :choices :credit :msg (msg "add " target " power counters")
    :effect (effect (add-prop card :counter target))
    :abilities [{:cost [:credit 1] :msg "break 1 subroutine"}]
    :strength-bonus (req (or (:counter card) 0))
    :events {:counter-added {:req (req (= :cid target) (:cid card))
                             :effect (effect (update-breaker-strength card))}}}

   "Aurora"
   {:abilities [{:cost [:credit 2] :msg "break 1 barrier subroutine"}
                {:cost [:credit 2] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Battering Ram"
   {:abilities [{:cost [:credit 2] :msg "break up to 2 barrier subroutines"}
                {:cost [:credit 1] :msg "add 1 strength for the remainder of this run"
                 :effect (effect (pump card 1 :all-run))}]}

   "BlacKat"
   {:abilities [{:cost [:credit 1] :msg "break 1 barrier subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Breach"
   {:abilities [{:cost [:credit 2] :req (req (#{:hq :rd :archives} (first (:server run))))
                 :msg "break 3 barrier subroutines"}
                {:cost [:credit 2] :msg "add 4 strength" :effect (effect (pump card 4))}]}

   "Cerberus \"Cuj.0\" H3"
   {:data {:counter 4}
    :abilities [{:counter-cost 1 :msg "break up to 2 sentry subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Cerberus \"Rex\" H2"
   {:data {:counter 4}
    :abilities [{:counter-cost 1 :msg "break up to 2 code gate subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Cerberus \"Lady\" H1"
   {:data {:counter 4}
    :abilities [{:counter-cost 1 :msg "break up to 2 barrier subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Chameleon"
   {:prompt "Choose one subtype" :choices ["Barrier" "Code Gate" "Sentry"]
    :msg (msg "choose " target) :effect (effect (set-prop card :ice-type target))
    :end-turn {:msg "add itself to Grip" :effect (effect (move card :hand))}
    :abilities [{:cost [:credit 1] :msg (msg "break 1 " (:ice-type card) " subroutine")}]}

   "Corroder"
   {:abilities [{:cost [:credit 1] :msg "break 1 barrier subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Creeper"
   {:abilities [{:cost [:credit 2] :msg "break 1 sentry subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Crowbar"
   {:abilities [{:msg "break up to 3 code gate subroutines" :effect (effect (trash card {:cause :ability-cost}))}]
    :events (let [cloud {:req (req (has? target :subtype "Icebreaker"))
                         :effect (effect (update-breaker-strength card))}]
              {:runner-install cloud :trash cloud :card-moved cloud})
    :strength-bonus (req (count (filter #(has? % :subtype "Icebreaker") (all-installed state :runner))))}

   "Crypsis"
   {:abilities [{:cost [:credit 1] :msg "break ICE subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}
                {:cost [:click 1] :msg "place 1 virus counter"
                 :effect (effect (add-prop card :counter 1))}
                {:counter-cost 1 :label "Remove 1 hosted virus counter" :msg "remove 1 virus counter"}]}

   "Cyber-Cypher"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Dagger"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 1] :msg "add 5 strength" :effect (effect (pump card 5))}]}

   "Darwin"
   {:events {:runner-turn-begins
             {:optional {:cost [:credit 1] :prompt "Place 1 virus counter on Darwin?"
                         :msg "place 1 virus counter" :effect (effect (add-prop card :counter 1))}}}
    :abilities [{:cost [:credit 2] :msg "break ICE subroutine"}]}

   "Deus X"
   {:prevent {:damage [:net]}
    :abilities [{:msg "break any number of AP subroutines" :effect (effect (trash card {:cause :ability-cost}))}
                {:msg "prevent any amount of net damage"
                 :effect (effect (trash card {:cause :ability-cost}) (damage-prevent :net Integer/MAX_VALUE))}]}

   "Eater"
   {:abilities [{:cost [:credit 1] :msg "break ICE subroutine and access 0 cards this run"
                 :effect (effect (max-access 0))}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Faerie"
   {:abilities [{:msg "break any number of sentry subroutines" :effect (effect (trash card))}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Faust"
   {:abilities [{:label "Trash 1 card from Grip to break 1 subroutine"
                 :prompt "Choose a card to trash for Faust" :choices (req (:hand runner))
                 :msg (msg "trash " (:title target) " and break 1 subroutine")
                 :effect (effect (trash target))}
                {:label "Trash 1 card from Grip to add 2 strength"
                 :prompt "Choose a card to trash for Faust" :choices (req (:hand runner))
                 :msg (msg "trash " (:title target) " and add 2 strength")
                 :effect (effect (trash target) (pump card 2))}]}

   "Femme Fatale"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 2] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Force of Nature"
   {:abilities [{:cost [:credit 2] :msg "break up to 2 code gate subroutines"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Garrote"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Gordian Blade"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 1 strength for the remainder of this run"
                 :effect (effect (pump card 1 :all-run))}]}

   "Gingerbread"
   {:abilities [{:cost [:credit 1] :msg "break 1 tracer subroutine"}
                {:cost [:credit 2] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Inti"
   {:abilities [{:cost [:credit 1] :msg "break 1 barrier subroutine"}
                {:cost [:credit 2] :msg "add 1 strength for the remainder of this run"
                 :effect (effect (pump card 1 true))}]}

   "Knight"
   {:abilities [{:label "Host Knight on a piece of ICE" :cost [:click 1]
                 :choices {:req #(and (= (:type %) "ICE")
                                      (= (last (:zone %)) :ices)
                                      (not (some (fn [c] (has? c :subtype "Caïssa")) (:hosted %))))}
                 :msg (msg "host it on " (if (:rezzed target) (:title target) "a piece of ICE"))
                 :effect (effect (host target card))}
                {:cost [:credit 2] :msg "break 1 subroutine on the host ICE"}]}

   "Leviathan"
   {:abilities [{:cost [:credit 3] :msg "break up to 3 code gate subroutines"}
                {:cost [:credit 3] :msg "add 5 strength" :effect (effect (pump card 5))}]}

   "Morning Star"
   {:abilities [{:cost [:credit 1] :msg "break any number of barrier subroutines"}]}

   "Mimic"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}]}

   "Ninja"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 3] :msg "add 5 strength" :effect (effect (pump card 5))}]}

   "Passport"
   {:abilities [{:cost [:credit 1] :req (req (#{:hq :rd :archives} (first (:server run))))
                 :msg "break 1 code gate subroutine"}
                {:cost [:credit 2] :msg "add 2 strength" :effect (effect (pump card 2))}]}

   "Omega"
   {:abilities [{:cost [:credit 1] :req (req (= (:position run) (dec (count (:ices run)))))
                 :msg "break 1 subroutine on the innermost ICE protecting this server"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Overmind"
   {:effect (effect (set-prop card :counter (:memory runner)))
    :abilities [{:counter-cost 1 :msg "break 1 subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Peacock"
   {:abilities [{:cost [:credit 2] :msg "break 1 code gate subroutine"}
                {:cost [:credit 2] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Pipeline"
   {:abilities [{:cost [:credit 1] :msg "break 1 sentry subroutine"}
                {:cost [:credit 2] :msg "add 1 strength for the remainder of this run"
                 :effect (effect (pump card 1 :all-run))}]}

   "Refractor"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 3 strength" :effect (effect (pump card 3))}]}

   "Sage"
   {:abilities [{:cost [:credit 2] :msg "break 1 code gate or barrier subroutine"}]
    :effect (req (add-watch state (keyword (str "sage" (:cid card)))
                            (fn [k ref old new]
                              (when (not= (get-in old [:runner :memory]) (get-in new [:runner :memory]))
                                (update-breaker-strength ref side card))))
                 (update-breaker-strength state side card))
    :leave-play (req (remove-watch state (keyword (str "sage" (:cid card)))))
    :strength-bonus (req (:memory runner))}

   "Snowball"
   {:abilities [{:cost [:credit 1] :msg "break 1 barrier subroutine"
                 :effect (effect (pump card 1 :all-run))}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Sharpshooter"
   {:abilities [{:msg "break any number of destroyer subroutines" :effect (effect (trash card {:cause :ability-cost}))}
                {:cost [:credit 1] :msg "add 2 strength" :effect (effect (pump card 2))}]}

   "Shiv"
   {:abilities [{:msg "break up to 3 sentry subroutines" :effect (effect (trash card {:cause :ability-cost}))}]
    :events (let [cloud {:req (req (has? target :subtype "Icebreaker"))
                         :effect (effect (update-breaker-strength card))}]
              {:runner-install cloud :trash cloud :card-moved cloud})
    :strength-bonus (req (count (filter #(has? % :subtype "Icebreaker") (all-installed state :runner))))}

   "Spike"
   {:abilities [{:msg "break up to 3 barrier subroutines" :effect (effect (trash card {:cause :ability-cost}))}]
    :events (let [cloud {:req (req (has? target :subtype "Icebreaker"))
                         :effect (effect (update-breaker-strength card))}]
              {:runner-install cloud :trash cloud :card-moved cloud})
    :strength-bonus (req (count (filter #(has? % :subtype "Icebreaker") (all-installed state :runner))))}

   "Study Guide"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 2] :msg "place 1 power counter" :effect (effect (add-prop card :counter 1)
                                                                                (update-breaker-strength card))}]
    :strength-bonus (req (or (:counter card) 0))}

   "Switchblade"
   {:abilities [{:cost [:credit 1] :msg "break any number of sentry subroutines"}
                {:cost [:credit 1] :msg "add 7 strength" :effect (effect (pump card 7))}]}

   "Torch"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   "Wyrm"
   {:abilities [{:cost [:credit 3] :msg "break 1 subroutine on ICE with 0 or less strength"}
                {:cost [:credit 1]
                 :label "give -1 strength to current ice" :msg (msg "give -1 strength to " (:title current-ice))
                 :req (req current-ice)
                 :effect (req (update! state side (update-in card [:wyrm-count] (fnil #(+ % 1) 0)))
                              (update-ice-strength state side current-ice))}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]
    :events (let [wy {:effect (req (update! state side (dissoc card :wyrm-count)))}]
                 {:pre-ice-strength {:req (req (and (= (:cid target) (:cid current-ice)) (:wyrm-count card)))
                                     :effect (req (let [c (:wyrm-count (get-card state card))]
                                                       (ice-strength-bonus state side (- c))))}
                  :pass-ice wy :run-ends wy})}

   "Yog.0"
   {:abilities [{:msg "break 1 code gate subroutine"}]}

   "ZU.13 Key Master"
   {:abilities [{:cost [:credit 1] :msg "break 1 code gate subroutine"}
                {:cost [:credit 1] :msg "add 1 strength" :effect (effect (pump card 1))}]}

   ;; ICE
   "Archer"
   {:additional-cost [:forfeit]
    :abilities [{:msg "gain 2 [Credits]" :effect (effect (gain :credit 2))}
                trash-program
                {:msg "end the run" :effect (effect (end-run))}]}

   "Architect"
   {:abilities [{:msg "look at the top 5 cards of R&D" :prompt "Choose a card to install"
                 :req (req (not (string? target))) :not-distinct true
                 :choices (req (conj (take 5 (:deck corp)) "No install"))
                 :effect (effect (corp-install target nil {:no-install-cost true}))}
                {:msg "install a card from Archives" :choices (req (:discard corp))
                 :prompt "Choose a card to install" :effect (effect (corp-install target nil))}
                {:msg "install a card from HQ" :choices (req (:hand corp))
                 :prompt "Choose a card to install" :effect (effect (corp-install target nil))}]}

   "Ashigaru"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Asteroid Belt"
   {:advanceable :always :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Bandwidth"
   {:abilities [{:msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}]}

   "Bastion"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Builder"
   {:abilities [{:label "Move Builder to the outermost position of any server"
                 :cost [:click 1] :prompt "Choose a server" :choices (req servers)
                 :msg (msg "move it to the outermost position of " target)
                 :effect (effect (move card (conj (server->zone state target) :ices)))}
                {:label "Place 1 advancement token on an ICE that can be advanced on this server"
                 :msg (msg "place 1 advancement token on " (if (:rezzed target) (:title target) "a card"))
                 :choices {:req #(or (= (:type %) "Agenda") (:advanceable %))}
                 :effect (effect (add-prop target :advance-counter 1))}]}

   "Bullfrog"
   {:abilities [{:msg "start a Psi game"
                 :psi {:not-equal
                       {:player :corp :prompt "Choose a server" :choices (req servers)
                        :msg (msg "move it to the outermost position of " target)
                        :effect (req (let [dest (server->zone state target)]
                                       (swap! state update-in [:run]
                                              #(assoc % :position (count (get-in corp (conj dest :ices)))
                                                      :server (rest dest))))
                                     (move state side card (conj (server->zone state target) :ices)))}}}]}

   "Burke Bugs"
   {:abilities [trash-program]}

   "Caduceus"
   {:abilities [{:label "Trace 3 - Gain 3 [Credits]"
                 :trace {:base 3 :msg "gain 3 [Credits]" :effect (effect (gain :credit 3))}}
                {:label "Trace 2 - End the run"
                 :trace {:base 2 :msg "end the run" :effect (effect (end-run))}}]}

   "Cell Portal"
   {:abilities [{:msg "make the Runner approach the outermost ICE"
                 :effect #(do (swap! %1 assoc-in [:run :position] 0) (derez %1 %2 %3))}]}

   "Changeling"
   {:advanceable :always :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Checkpoint"
   {:effect (effect (gain :bad-publicity 1) (system-msg "takes 1 bad publicity"))
    :abilities [{:label "Trace 5 - Do 3 meat damage when this run is successful"
                 :trace {:base 5
                         :effect #(do (swap! %1 assoc-in [:run :run-effect :end-run]
                                             {:req (req (:successful run)) :msg "do 3 meat damage"
                                              :effect (effect (damage :meat 3 {:card card}))})
                                      (swap! %1 assoc-in [:run :run-effect :card] %3))}}]}

   "Chimera"
   {:prompt "Choose one subtype" :choices ["Barrier" "Code Gate" "Sentry"]
    :msg (msg "change its subtype to " target) :end-turn {:effect (effect (derez card))}
    :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Clairvoyant Monitor"
   {:abilities [{:msg "start a Psi game"
                 :psi {:not-equal {:player :corp
                                   :prompt "Choose a target for Clairvoyant Monitor"
                                   :msg (msg "place 1 advancement token on "
                                             (if (:rezzed target) (:title target) "a card") " and end the run")
                                   :choices {:req #(or (= (:type %) "Agenda") (:advanceable %))}
                                   :effect (effect (add-prop target :advance-counter 1) (end-run))}}}]}

   "Chum"
   {:abilities [{:msg "do 3 net damage" :effect (effect (damage :net 3 {:card card}))}]}

   "Cortex Lock"
   {:abilities [{:label "Do 1 net damage for each unused memory units the Runner has"
                 :msg (msg "do " (:memory runner) " net damage")
                 :effect (effect (damage :net (:memory runner) {:card card}))}]}

   "Crick"
   {:abilities [{:msg "install a card from Archives" :choices (req (:discard corp))
                 :prompt "Choose a card to install" :effect (effect (corp-install target nil))}]
    :strength-bonus (req (if (= (second (:zone card)) :archives) 3 0))}

   "Curtain Wall"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (let [ices (:ices (card->server state card))]
                              (if (= (:cid card) (:cid (last ices))) 4 0)))
    :events (let [cw {:req (req (and (not= (:cid card) (:cid target))
                                     (= (card->server state card) (card->server state target))))
                      :effect (effect (update-ice-strength card))}]
                 {:corp-install cw :trash cw :card-moved cw})}

   "Data Hound"
   {:abilities [{:label "Trace 2 - Look at the top of Stack"
                 :trace {:base 2 :msg (msg "look at the top " (- target (second targets)) " cards of Stack")
                         :effect (req (doseq [c (take (- target (second targets)) (:deck runner))]
                                        (move state side c :play-area false true)))}}]}

   "Data Mine"
   {:abilities [{:msg "do 1 net damage" :effect (effect (trash card) (damage :net 1 {:card card}))}]}

   "Datapike"
   {:abilities [{:msg "force the Runner to pay 2 [Credits] if able"
                 :effect (effect (pay :runner card :credit 2))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Data Raven"
   {:abilities [{:msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}
                {:msg "give the Runner 1 tag using 1 power counter"
                 :counter-cost 1 :effect (effect (gain :runner :tag 1))}
                {:label "Trace 3 - Add 1 power counter"
                 :trace {:base 3 :msg "add 1 power counter" :effect (effect (add-prop card :counter 1))}}]}

   "Dracō"
   {:prompt "How many power counters?" :choices :credit :msg (msg "add " target " power counters")
    :effect (effect (set-prop card :counter target))
    :strength-bonus (req (or (:counter card) 0))
    :abilities [{:label "Trace 2"
                 :trace {:base 2 :msg "give the Runner 1 tag and end the run"
                         :effect (effect (gain :runner :tag 1) (end-run))}}]}

   "Eli 1.0"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Enigma"
   {:abilities [{:msg "force the Runner to lose 1 [Click] if able"
                 :effect (effect (lose :runner :click 1))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Errand Boy"
   {:abilities [{:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}
                {:msg "draw 1 card" :effect (effect (draw))}]}

   "Excalibur"
   {:abilities [{:label "The Runner cannot make another run this turn"
                 :msg "prevent the Runner from making another run" :effect (effect (prevent-run))}]}

   "Fenris"
   {:effect (effect (gain :bad-publicity 1) (system-msg "takes 1 bad publicity"))
    :abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1 {:card card}))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Fire Wall"
   {:advanceable :always :abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (or (:advance-counter card) 0))}

   "Flare"
   {:abilities [{:prompt "Choose a piece of hardware to trash"
                 :msg (msg "trash " (:title target)) :label "Trash a piece of hardware"
                 :choices (req (get-in runner [:rig :hardware])) :effect (effect (trash target))}
                {:msg "do 2 meat damage and end the run"
                 :effect (effect (damage :meat 2 {:unpreventable true :card card}) (end-run))}]}

   "Galahad"
   {:abilities [{:label "End the run" :msg "end the run" :effect (effect (end-run))}
                {:label "Reveal up to 2 Grail ICE from HQ"
                 :choices {:max 2 :req #(and (:side % "Corp") (= (:zone %) [:hand]) (has? % :subtype "Grail"))}
                 :msg (msg "reveal "
                           (join ", " (map #(str (:title %) " ("
                                                 (:label (first (:abilities (card-def %)))) ")") targets)))}
                {:label "Resolve a Grail ICE subroutine from HQ"
                 :choices {:req #(and (:side % "Corp") (= (:zone %) [:hand]) (has? % :subtype "Grail"))}
                 :effect (req (doseq [ice targets]
                                (resolve-ability state side (first (:abilities (card-def ice))) card nil)))}]}

   "Gemini"
   {:abilities [{:label "Trace 2"
                 :trace {:base 2 :msg "do 1 net damage" :effect (effect (damage :net 1) {:card card})
                         :kicker {:min 5 :msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}}}]}

   "Grim"
   {:effect (effect (gain :bad-publicity 1) (system-msg "takes 1 bad publicity"))
    :abilities [trash-program]}

   "Guard"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Gutenberg"
   {:abilities [{:label "Trace 7 - Give the Runner 1 tag"
                 :trace {:base 7 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}]
    :strength-bonus (req (if (= (second (:zone card)) :rd) 3 0))}

   "Hadrians Wall"
   {:advanceable :always
    :abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (or (:advance-counter card) 0))}

   "Himitsu-Bako"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}
                {:msg "add it to HQ" :cost [:credit 1] :effect (effect (move card :hand))}]}

   "Hive"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Heimdall 1.0"
   {:abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1 {:card card}))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Heimdall 2.0"
   {:abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1 {:card card}))}
                {:msg "do 1 brain damage and end the run" :effect (effect (damage :brain 1 {:card card}) (end-run))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Hourglass"
   {:abilities [{:msg "force the Runner to lose 1 [Click] if able"
                 :effect (effect (lose :runner :click 1))}]}

   "Hudson 1.0"
   {:abilities [{:msg "prevent the Runner from accessing more than 1 card during this run"
                 :effect (effect (max-access 1))}]}

   "Hunter"
   {:abilities [{:label "Trace 3 - Give the Runner 1 tag"
                 :trace {:base 3 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}]}

   "Ice Wall"
   {:advanceable :always :abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (or (:advance-counter card) 0))}

   "Ichi 1.0"
   {:abilities [trash-program
                {:label "Trace 1 - Give the Runner 1 tag and do 1 brain damage"
                 :trace {:base 1 :msg "give the Runner 1 tag and do 1 brain damage"
                         :effect (effect (damage :brain 1 {:card card}) (gain :runner :tag 1))}}]}

   "Ichi 2.0"
   {:abilities [trash-program
                {:label "Trace 3 - Give the Runner 1 tag and do 1 brain damage"
                 :trace {:base 3 :msg "give the Runner 1 tag and do 1 brain damage"
                         :effect (effect (damage :brain 1 {:card card}) (gain :runner :tag 1))}}]}

   "IQ"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (count (:hand corp)))}

   "Information Overload"
   {:abilities [{:label "Trace 1 - Give the Runner 1 tag"
                 :trace {:base 1 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}]}

   "Ireress"
   {:abilities [{:msg "make the Runner lose 1 [Credits]" :effect (effect (lose :runner :credit 1))}]}

   "Janus 1.0"
   {:abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1 {:card card}))}]}

   "Kitsune"
   {:abilities [{:prompt "Choose a card in HQ" :choices (req (:hand corp))
                 :label "Force the Runner to access a card in HQ"
                 :msg (msg "force the Runner to access " (:title target))
                 :effect (effect (handle-access targets) (trash card))}]}

   "Komainu"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}]}

   "Lab Dog"
   {:abilities [(assoc trash-hardware :label "Force the Runner to trash an installed piece of hardware"
                                      :player :runner
                                      :msg (msg "force the runner to trash " (:title target))
                                      :effect (effect (trash target) (trash card)))]}

   "Lancelot"
   {:abilities [trash-program
                {:label "Reveal up to 2 Grail ICE from HQ"
                 :choices {:max 2 :req #(and (:side % "Corp") (= (:zone %) [:hand]) (has? % :subtype "Grail"))}
                 :msg (msg "reveal "
                           (join ", " (map #(str (:title %) " ("
                                                 (:label (first (:abilities (card-def %)))) ")") targets)))}
                {:label "Resolve a Grail ICE subroutine from HQ"
                 :choices {:req #(and (:side % "Corp") (= (:zone %) [:hand]) (has? % :subtype "Grail"))}
                 :effect (req (doseq [ice targets]
                                (resolve-ability state side (first (:abilities (card-def ice))) card nil)))}]}

   "Little Engine"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}
                {:msg "make the Runner gain 5 [Credits]" :effect (effect (gain :runner :credit 5))}]}

   "Lotus Field"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Lycan"
   {:advanceable :always
    :abilities [trash-program]}

   "Mamba"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}
                {:msg "do 1 net damage using 1 power counter"
                 :counter-cost 1 :effect (effect (damage :net 1 {:card card}))}
                {:msg "start a Psi game"
                 :psi {:not-equal {:msg "add 1 power counter"
                                   :effect (effect (add-prop :runner card :counter 1))}}}]}

   "Markus 1.0"
   {:abilities [{:msg "force the Runner to trash a card"}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Matrix Analyzer"
   {:abilities [{:label "Place 1 advancement token on a card that can be advanced"
                 :msg (msg "place 1 advancement token on " (if (:rezzed target) (:title target) "a card"))
                 :choices {:req #(or (= (:type %) "Agenda") (:advanceable %))}
                 :cost [:credit 1] :effect (effect (add-prop target :advance-counter 1))}
                {:label "Trace 2 - Give the Runner 1 tag"
                 :trace {:base 2 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}]}

   "Merlin"
   {:abilities [{:label "Do 2 net damage" :msg "do 2 net damage" :effect (effect (damage :net 2 {:card card}))}
                {:label "Reveal up to 2 Grail ICE from HQ"
                 :choices {:max 2 :req #(and (:side % "Corp") (= (:zone %) [:hand]) (has? % :subtype "Grail"))}
                 :msg (msg "reveal "
                           (join ", " (map #(str (:title %) " ("
                                                 (:label (first (:abilities (card-def %)))) ")") targets)))}
                {:label "Resolve a Grail ICE subroutine from HQ"
                 :choices {:req #(and (:side % "Corp") (= (:zone %) [:hand]) (has? % :subtype "Grail"))}
                 :effect (req (doseq [ice targets]
                                (resolve-ability state side (first (:abilities (card-def ice))) card nil)))}]}

   "Meru Mati"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (if (= (second (:zone card)) :hq) 3 0))}

   "Minelayer"
   {:abilities [{:msg "install an ICE from HQ"
                 :choices (req (filter #(has? % :type "ICE") (:hand corp)))
                 :prompt "Choose an ICE to install"
                 :effect (req (corp-install state side target (:server run)))}]}

   "Mother Goddess"
   (let [ab {:req (req (= (:type target) "ICE"))
             :effect (effect (update! (assoc card :subtype
                                             (->> (mapcat :ices (flatten (seq (:servers corp))))
                                                  (filter #(and (:rezzed %) (not= (:cid card) (:cid %))))
                                                  (mapcat #(vec (.split (:subtype %) " - ")))
                                                  (cons "Mythic")
                                                  distinct
                                                  (join " - ")))))}]
     {:abilities [{:msg "end the run" :effect (effect (end-run))}]
      :events {:rez ab :trash ab :derez ab}})

   "Muckraker"
   {:effect (effect (gain :bad-publicity 1))
    :abilities [{:label "Trace 1 - Give the Runner 1 tag"
                 :trace {:base 1 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}
                {:label "Trace 2 - Give the Runner 1 tag"
                 :trace {:base 2 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}
                {:label "Trace 3 - Give the Runner 1 tag"
                 :trace {:base 3 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}
                {:msg "end the run if the Runner is tagged" :req (req tagged)
                 :effect (effect (end-run))}]}

   "Nebula"
   {:advanceable :always
    :abilities [trash-program]}

   "Negotiator"
   {:abilities [{:msg "gain 2 [Credits]" :effect (effect (gain :credit 2))}
                trash-program]}

   "Neural Katana"
   {:abilities [{:msg "do 3 net damage" :effect (effect (damage :net 3 {:card card}))}]}

   "NEXT Bronze"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (reduce (fn [c server]
                                         (+ c (count (filter (fn [ice] (and (:rezzed ice) (has? ice :subtype "NEXT")))
                                                             (:ices server)))))
                                     0 (flatten (seq (:servers corp)))))
    :events (let [nb {:req (req (and (not= (:cid target) (:cid card)) (has? target :subtype "NEXT")))
                   :effect (effect (update-ice-strength card))}]
                 {:rez nb :derez nb :trash nb :card-moved nb})}

   "NEXT Gold"
   {:abilities [{:label "Do 1 net damage for each rezzed NEXT ice"
                 :msg (msg "do "
                           (reduce (fn [c server]
                                     (+ c (count (filter (fn [ice]
                                                           (and (:rezzed ice) (has? ice :subtype "NEXT")))
                                                         (:ices server)))))
                                   0 (flatten (seq (:servers corp)))) " net damage")
                 :effect (effect (damage :net (reduce (fn [c server]
                                                        (+ c (count (filter (fn [ice]
                                                                              (and (:rezzed ice) (has? ice :subtype "NEXT")))
                                                                            (:ices server)))))
                                                      0 (flatten (seq (:servers corp)))) {:card card}))}
                trash-program]}

   "NEXT Silver"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Orion"
   {:advanceable :always
    :abilities [trash-program
                {:msg "end the run" :effect (effect (end-run))}]}

   "Pachinko"
   {:abilities [{:label "End the run if the Runner is tagged"
                 :req (req tagged) :msg "end the run" :effect (effect (end-run))}]}

   "Paper Wall"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Pop-up Window"
   {:abilities [{:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Pup"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}]}

   "Quandary"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Quicksand"
   {:events {:encounter-ice {:req (req (= (:cid target) (:cid card)))
                             :effect (effect (add-prop card :counter 1))}}
    :strength-bonus (req (or (:counter card) 0))
    :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Rainbow"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Rototurret"
   {:abilities [trash-program
                {:msg "end the run" :effect (effect (end-run))}]}

   "Sagittarius"
   {:abilities [{:label "Trace 2 - Trash a program"
                 :trace (assoc trash-program :base 2 :not-distinct true
                         :kicker (assoc trash-program :min 5))}]}

   "Salvage"
   {:advanceable :while-rezzed
    :abilities [{:label "Trace 2 - Give the Runner 1 tag"
                 :trace {:base 2 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}]}

   "Searchlight"
   {:advanceable :always
    :abilities [{:label "Trace X - Give the runner 1 tag"
                 :trace {:base (req (or (:advance-counter card) 0)) :effect (effect (gain :runner :tag 1))
                         :msg "give the Runner 1 tag"}}]}

   "Shadow"
   {:advanceable :always
    :abilities [{:msg "gain 2 [Credits]" :effect (effect (gain :credit 2))}
                {:label "Trace 3 - Give the Runner 1 tag"
                 :trace {:base 3 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))}}]
    :strength-bonus (req (or (:advance-counter card) 0))}

   "Sherlock 1.0"
   {:abilities [{:label "Trace 4 - Add an installed program to the top of Stack"
                 :trace {:base 4 :choices {:req #(= (:zone %) [:rig :program])}
                         :msg (msg "add " (:title target) " to the top of Stack")
                         :effect (effect (move :runner target :deck true))}}]}

   "Shinobi"
   {:effect (effect (gain :bad-publicity 1) (system-msg "takes 1 bad publicity"))
    :abilities [{:label "Trace 1 - Do 1 net damage"
                 :trace {:base 1 :msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}}
                {:label "Trace 2 - Do 2 net damage"
                 :trace {:base 2 :msg "do 2 net damage" :effect (effect (damage :net 2 {:card card}))}}
                {:label "Trace 3 - Do 3 net damage"
                 :trace {:base 3 :msg "do 3 net damage" :effect (effect (damage :net 3 {:card card}))}}]}

   "Snoop"
   {:abilities [{:msg "place 1 power counter on Snoop" :effect (effect (add-prop card :counter 1))}
                {:counter-cost 1 :label "Look at all cards in Grip and trash 1 card"
                 :msg (msg "Look at all cards in Grip and trashes " (:title target))
                 :choices (req (:hand runner)) :prompt "Choose a card to trash"
                 :effect (effect (trash target))}]}

   "Snowflake"
   {:abilities [{:msg "start a Psi game"
                 :psi {:not-equal {:msg "end the run" :effect (effect (end-run))}}}]}

   "Spiderweb"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Susanoo-No-Mikoto"
   {:abilities [{:req (req (not= (:server run) [:discard]))
                 :msg "make the Runner continue the run on Archives"
                 :effect (req (swap! state update-in [:run]
                                     #(assoc % :position (count (get-in corp [:servers :archives :ices]))
                                             :server [:archives])))}]}

   "Swarm"
   {:advanceable :always
    :abilities [trash-program]}

   "Swordsman"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}
                {:prompt "Choose an AI program to trash" :msg (msg "trashes " (:title target))
                 :label "Trash an AI program" :effect (effect (trash target))
                 :choices (req (filter #(has? % :subtype "AI") (get-in runner [:rig :program])))}]}

   "Taurus"
   {:abilities [{:label "Trace 2 - Trash a piece of hardware"
                 :trace (assoc trash-hardware :base 2 :not-distinct true
                                              :kicker (assoc trash-hardware :min 5))}]}

   "TMI"
   {:trace {:base 2 :unsuccessful {:effect (effect (derez card))}}
    :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Tollbooth"
   {:abilities [{:msg "force the Runner to lose 3 [Credits]"
                 :effect (effect (lose :runner :credit 3))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Troll"
   {:abilities [{:player :runner :prompt "Choose one" :choices ["Lose [Click]" "End the run"]
                 :label "Force the Runner to lose [Click] or end the run"
                 :effect (req (if-not (and (= target "Lose [Click]") (pay state side card :click 1))
                                (do (end-run state side) (system-msg state side "ends the run"))
                                (system-msg state side "loses [Click]")))}]}

   "Tsurugi"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}
                {:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}]}

   "Turing"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (if (= (second (:zone card)) :remote) 3 0))}

   "Tyrant"
   {:advanceable :while-rezzed
    :abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Universal Connectivity Fee"
   {:abilities [{:msg (msg "force the Runner to lose " (if (> (:tag runner) 0) "all credits" "1 [Credits]"))
                 :effect (req (if (> (get-in @state [:runner :tag]) 0)
                                (do (lose state :runner :credit :all) (trash state side card))
                                (lose state :runner :credit 1)))}]}

   "Uroboros"
   {:abilities [{:label "Trace 4 - Prevent the Runner from making another run"
                 :trace {:base 4 :msg "prevent the Runner from making another run"
                         :effect (effect (prevent-run))}}
                {:label "Trace 4 - End the run"
                 :trace {:base 4 :msg "end the run" :effect (effect (end-run))}}]}

   "Viktor 1.0"
   {:abilities [{:msg "do 1 brain damage" :effect (effect (damage :brain 1 {:card card}))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Viktor 2.0"
   {:abilities [{:msg "do 1 brain damage using 1 power counter" :counter-cost 1
                 :effect (effect (damage :brain 1 {:card card}))}
                {:label "Trace 2 - Add 1 power counter"
                 :trace {:base 2 :msg "add 1 power counter" :effect (effect (add-prop card :counter 1))}}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Viper"
   {:abilities [{:label "Trace 3 - The Runner loses 1 [Click] if able"
                 :trace {:base 3 :msg  "force the Runner to lose 1 [Click] if able"
                         :effect (effect (lose :runner :click 1))}}
                {:label "Trace 3 - End the run"
                 :trace {:base 3 :msg "end the run" :effect (effect (end-run))}}]}

   "Virgo"
   {:abilities [{:label "Trace 2"
                 :trace {:base 2 :msg "give the Runner 1 tag" :effect (effect (gain :runner :tag 1))
                         :kicker {:min 5 :msg "give the Runner 1 tag"
                                  :effect (effect (gain :runner :tag 1))}}}]}

   "Wall of Static"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Wall of Thorns"
   {:abilities [{:msg "do 2 net damage" :effect (effect (damage :net 2 {:card card}))}
                {:msg "end the run" :effect (effect (end-run))}]}

   "Wendigo"
   {:advanceable :always
    :abilities [{:msg "prevent the Runner from using a chosen program for the remainder of this run"}]}

   "Whirlpool"
   {:abilities [{:msg "prevent the Runner from jacking out"
                 :effect (effect (trash card) (prevent-jack-out))}]}

   "Woodcutter"
   {:advanceable :while-rezzed
    :abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}]}

   "Wormhole"
   {:advanceable :always}

   "Wotan"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]}

   "Wraparound"
   {:abilities [{:msg "end the run" :effect (effect (end-run))}]
    :strength-bonus (req (if (some #(has? % :subtype "Fracter") (get-in runner [:rig :program]))
                           0 7))
    :events (let [wr {:req (req (and (not= (:cid target) (:cid card)) (has? target :subtype "Fracter")))
                      :effect (effect (update-ice-strength card))}]
                 {:runner-install wr :trash wr :card-moved wr})}

   "Yagura"
   {:abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}
                {:msg "look at the top card of R&D"
                 :optional {:prompt (msg "Add " (:title (first (:deck corp))) " to bottom of R&D?")
                            :msg "add the top card of R&D to the bottom"
                            :effect (effect (move (first (:deck corp)) :deck))}}]}

   "Zona Sul Shipping"
   {:events {:runner-turn-begins {:effect (effect (add-prop card :counter 1))}}
    :abilities [{:cost [:click 1] :msg (msg "gain " (:counter card) " [Credits]")
                 :label "Take all credits"
                 :effect (effect (gain :credit (:counter card)) (set-prop card :counter 0))}]
    :effect (req (add-watch state (keyword (str "zona-sul-shipping" (:cid card)))
                            (fn [k ref old new]
                              (when (> (get-in new [:runner :tag]) 0)
                                (remove-watch state (keyword (str "zona-sul-shipping" (:cid card))))
                                (trash ref :runner card)
                                (system-msg ref side "trash Zona Sul Shipping for being tagged")))))}

   ;; partial implementation
   "Bad Times"
   {:req (req tagged)}

   "Exile: Streethawk"
   {:effect (effect (gain :link 1))}

   "Deep Red"
   {:effect (effect (gain :memory 3)) :leave-play (effect (lose :memory 3))}

   "Deep Thought"
   {:events {:successful-run {:effect (effect (add-prop card :counter 1)) :req (req (= target :rd))}
             :runner-turn-begins
             {:req (req (>= (get-virus-counters state side card) 3)) :msg "look at the top card of R&D"
              :effect (effect (prompt! card (str "The top card of your R&D is "
                                                 (:title (first (:deck corp)))) ["OK"] {}))}}}

   "Eden Shard"
   {:abilities [{:effect (effect (trash card {:cause :ability-cost}) (draw :corp 2))
                 :msg "force the Corp to draw 2 cards"}]}

   "Ghost Runner"
   {:data {:counter 3}
    :abilities [{:counter-cost 1 :msg "gain 1 [Credits]" :req (req (:run @state))
                 :effect (req (gain state side :credit 1)
                              (when (zero? (:counter card)) (trash state :runner card)))}]}

   "Nasir Meidan: Cyber Explorer"
   {:effect (effect (gain :link 1))}

   "Tallie Perrault"
   {:abilities [{:label "Draw 1 card for each Corp bad publicity"
                 :effect (effect (trash card {:cause :ability-cost}) (draw (:bad-publicity corp)))
                 :msg (msg "draw " (:bad-publicity corp) " cards")}]
    :events {:play-operation {:msg "give the Corp 1 bad publicity and take 1 tag"
                              :effect (effect (gain :bad-publicity 1) (gain :runner :tag 1))
                              :req (req (or (has? target :subtype "Black Ops")
                                            (has? target :subtype "Gray Ops")))}}}

   "The Source"
   {:effect (effect (update-all-advancement-costs))
    :leave-play (effect (update-all-advancement-costs))
    :events {:agenda-scored (effect (trash card)) :agenda-stolen (effect (trash card))
             :pre-advancement-cost {:effect (effect (advancement-cost-bonus 1))}}}})
