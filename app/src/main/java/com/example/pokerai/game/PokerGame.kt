package com.example.pokerai.game

import kotlin.math.abs
import kotlin.random.Random

class PokerGame(
    numberOfPlayers: Int,
    startingChips: Int = 1000,
    private val smallBlind: Int = 10,
    private val bigBlind: Int = 20,
    val difficulty: Difficulty = Difficulty.MEDIUM
) {

    val players = mutableListOf<Player>()
    val communityCards = mutableListOf<Card>()

    private var deck = Deck()

    var pot: Int = 0
        private set

    var stage: GameStage = GameStage.WAITING
        private set

    var currentPlayerIndex: Int = 0
        private set

    var lastResult: String = ""
        private set

    // ============================================================
    // DEALER / BLINDS
    // ============================================================

    var dealerIndex: Int = -1
        private set

    var smallBlindIndex: Int = -1
        private set

    var bigBlindIndex: Int = -1
        private set

    private val actedPlayers = mutableSetOf<Int>()

    // ============================================================
    // CREATE PLAYERS
    // ============================================================

    init {
        require(numberOfPlayers in 2..8)

        for (i in 0 until numberOfPlayers) {

            players.add(
                Player(
                    id = i,
                    name = if (i == 0) {
                        "YOU"
                    } else {
                        "Computer $i"
                    },
                    chips = startingChips
                )
            )
        }
    }

    // ============================================================
    // START NEW HAND
    // ============================================================

    fun startNewHand() {

        deck = Deck()

        communityCards.clear()

        pot = 0

        lastResult = ""

        actedPlayers.clear()

        stage = GameStage.PRE_FLOP

        // Reset all players.
        players.forEach {
            it.resetForNewHand()
        }

        // Players with no chips cannot participate.
        players.forEach {
            if (it.chips <= 0) {
                it.folded = true
            }
        }

        val eligiblePlayers =
            players.filter {
                it.chips > 0
            }

        // Game is over if fewer than two players remain.
        if (eligiblePlayers.size < 2) {

            stage = GameStage.FINISHED

            lastResult =
                eligiblePlayers.firstOrNull()?.let {
                    "${it.name} wins the game!"
                } ?: "No players remaining."

            currentPlayerIndex =
                eligiblePlayers.firstOrNull()?.id ?: 0

            return
        }

        // ========================================================
        // DEALER ROTATION
        // ========================================================

        /*
         * First hand:
         * Player 0 is the dealer.
         *
         * Every following hand:
         * Move dealer clockwise.
         */
        dealerIndex =
            if (dealerIndex < 0) {

                eligiblePlayers.first().id

            } else {

                findNextEligiblePlayer(
                    dealerIndex
                )
            }

        // ========================================================
        // BLINDS
        // ========================================================

        /*
         * Heads-up:
         *
         * Dealer = Small Blind
         * Other player = Big Blind
         */
        if (eligiblePlayers.size == 2) {

            smallBlindIndex =
                dealerIndex

            bigBlindIndex =
                findNextEligiblePlayer(
                    dealerIndex
                )

        } else {

            /*
             * Normal table:
             *
             * Dealer
             *   ↓
             * Small Blind
             *   ↓
             * Big Blind
             */

            smallBlindIndex =
                findNextEligiblePlayer(
                    dealerIndex
                )

            bigBlindIndex =
                findNextEligiblePlayer(
                    smallBlindIndex
                )
        }

        // ========================================================
        // DEAL TWO CARDS
        // ========================================================

        repeat(2) {

            players.forEach { player ->

                if (
                    !player.folded &&
                    player.chips > 0
                ) {

                    player.receiveCard(
                        deck.draw()
                    )
                }
            }
        }

        // ========================================================
        // POST SMALL BLIND
        // ========================================================

        postBlind(
            smallBlindIndex,
            smallBlind
        )

        // ========================================================
        // POST BIG BLIND
        // ========================================================

        postBlind(
            bigBlindIndex,
            bigBlind
        )

        // ========================================================
        // PRE-FLOP TURN
        // ========================================================

        currentPlayerIndex =
            if (eligiblePlayers.size == 2) {

                /*
                 * Heads-up:
                 * Big Blind acts first pre-flop.
                 */
                bigBlindIndex

            } else {

                /*
                 * Normal table:
                 * First player after Big Blind.
                 */
                findNextAvailablePlayer(
                    bigBlindIndex
                )
            }

        /*
         * If the player selected is already all-in
         * because of a short blind, skip them.
         */
        if (!canPlayerAct(currentPlayerIndex)) {

            currentPlayerIndex =
                findNextAvailablePlayer(
                    currentPlayerIndex
                )
        }
    }

    // ============================================================
    // POST BLIND
    // ============================================================

    private fun postBlind(
        playerIndex: Int,
        amount: Int
    ) {

        if (playerIndex !in players.indices) {
            return
        }

        val player =
            players[playerIndex]

        if (
            player.chips <= 0 ||
            player.folded
        ) {
            return
        }

        val paid =
            player.bet(
                minOf(
                    amount,
                    player.chips
                )
            )

        pot += paid
    }

    // ============================================================
    // HUMAN FOLD
    // ============================================================

    fun playerFold(
        id: Int
    ) {

        if (!isHumanTurn(id)) {
            return
        }

        players[id].folded = true

        actedPlayers.add(id)

        afterAction()
    }

    // ============================================================
    // HUMAN CHECK
    // ============================================================

    fun playerCheck(
        id: Int
    ) {

        if (!isHumanTurn(id)) {
            return
        }

        if (!canCheck(id)) {
            return
        }

        actedPlayers.add(id)

        afterAction()
    }

    // ============================================================
    // HUMAN CALL
    // ============================================================

    fun playerCall(
        id: Int
    ) {

        if (!isHumanTurn(id)) {
            return
        }

        val player =
            players[id]

        val highest =
            highestCurrentBet()

        val needed =
            maxOf(
                0,
                highest - player.currentBet
            )

        if (needed > 0) {

            val paid =
                player.bet(
                    minOf(
                        needed,
                        player.chips
                    )
                )

            pot += paid
        }

        actedPlayers.add(id)

        afterAction()
    }

    // ============================================================
    // HUMAN RAISE
    // ============================================================

    fun playerRaise(
        playerId: Int,
        raiseAmount: Int
    ) {

        if (!isHumanTurn(playerId)) {
            return
        }

        if (raiseAmount <= 0) {
            return
        }

        val player =
            players[playerId]

        val highest =
            highestCurrentBet()

        val target =
            highest + raiseAmount

        val needed =
            target - player.currentBet

        if (needed <= 0) {
            return
        }

        val paid =
            player.bet(
                minOf(
                    needed,
                    player.chips
                )
            )

        pot += paid

        /*
         * A new raise means all other players
         * have to respond again.
         */
        actedPlayers.clear()

        actedPlayers.add(playerId)

        afterAction()
    }

    // ============================================================
    // HUMAN ALL-IN
    // ============================================================

    fun playerAllIn(
        id: Int
    ) {

        if (!isHumanTurn(id)) {
            return
        }

        val player =
            players[id]

        if (player.chips <= 0) {
            return
        }

        val paid =
            player.bet(
                player.chips
            )

        pot += paid

        /*
         * Reset responses.
         */
        actedPlayers.clear()

        actedPlayers.add(id)

        afterAction()
    }

    // ============================================================
    // CAN CHECK
    // ============================================================

    fun canCheck(
        id: Int
    ): Boolean {

        if (id !in players.indices) {
            return false
        }

        if (!isHumanTurn(id)) {
            return false
        }

        val player =
            players[id]

        return player.currentBet ==
                highestCurrentBet()
    }

    // ============================================================
    // COMPUTER TURNS
    // ============================================================

    fun processComputerTurns() {

        var safety = 0

        while (
            currentPlayerIndex != 0 &&
            stage != GameStage.SHOWDOWN &&
            stage != GameStage.FINISHED &&
            safety < 100
        ) {

            safety++

            val player =
                players[currentPlayerIndex]

            /*
             * Skip players who cannot act.
             */
            if (
                player.folded ||
                player.allIn ||
                player.chips <= 0
            ) {

                actedPlayers.add(
                    player.id
                )

                moveToNextPlayer()

                continue
            }

            computerAction(
                player.id
            )
        }
    }

    // ============================================================
    // AI DECISION
    // ============================================================

    private fun computerAction(
        id: Int
    ) {

        val strength =
            calculateAiStrength(
                players[id]
            )

        val random =
            Random.nextInt(100)

        when (difficulty) {

            Difficulty.EASY -> {

                when {

                    strength < 25 &&
                            random < 40 -> {

                        aiFold(id)
                    }

                    canCheckForAi(id) -> {

                        aiCheck(id)
                    }

                    strength > 75 &&
                            random < 25 -> {

                        aiRaise(id)
                    }

                    else -> {

                        aiCall(id)
                    }
                }
            }

            Difficulty.MEDIUM -> {

                when {

                    strength < 22 &&
                            random < 55 -> {

                        aiFold(id)
                    }

                    strength > 72 &&
                            random < 35 -> {

                        aiRaise(id)
                    }

                    canCheckForAi(id) -> {

                        aiCheck(id)
                    }

                    else -> {

                        aiCall(id)
                    }
                }
            }

            Difficulty.HARD -> {

                when {

                    strength < 18 &&
                            random < 65 -> {

                        aiFold(id)
                    }

                    strength > 58 &&
                            random < 50 -> {

                        aiRaise(id)
                    }

                    canCheckForAi(id) -> {

                        aiCheck(id)
                    }

                    else -> {

                        aiCall(id)
                    }
                }
            }
        }
    }

    // ============================================================
    // AI CHECK
    // ============================================================

    private fun canCheckForAi(
        id: Int
    ): Boolean {

        if (id !in players.indices) {
            return false
        }

        return players[id].currentBet ==
                highestCurrentBet()
    }

    private fun aiCheck(
        id: Int
    ) {

        actedPlayers.add(id)

        afterAction()
    }

    // ============================================================
    // AI CALL
    // ============================================================

    private fun aiCall(
        id: Int
    ) {

        val player =
            players[id]

        val highest =
            highestCurrentBet()

        val needed =
            maxOf(
                0,
                highest - player.currentBet
            )

        if (needed > 0) {

            val paid =
                player.bet(
                    minOf(
                        needed,
                        player.chips
                    )
                )

            pot += paid
        }

        actedPlayers.add(id)

        afterAction()
    }

    // ============================================================
    // AI RAISE
    // ============================================================

    private fun aiRaise(
        id: Int
    ) {

        val player =
            players[id]

        if (player.chips <= 0) {

            actedPlayers.add(id)

            afterAction()

            return
        }

        val highest =
            highestCurrentBet()

        val raiseSize =
            when (difficulty) {

                Difficulty.EASY ->
                    30

                Difficulty.MEDIUM ->
                    40

                Difficulty.HARD ->
                    60
            }

        val target =
            highest + raiseSize

        val needed =
            target - player.currentBet

        val actual =
            minOf(
                needed,
                player.chips
            )

        if (actual > 0) {

            val paid =
                player.bet(actual)

            pot += paid
        }

        /*
         * New raise.
         */
        actedPlayers.clear()

        actedPlayers.add(id)

        afterAction()
    }

    // ============================================================
    // AI FOLD
    // ============================================================

    private fun aiFold(
        id: Int
    ) {

        players[id].folded = true

        actedPlayers.add(id)

        afterAction()
    }

    // ============================================================
    // AFTER ACTION
    // ============================================================

    private fun afterAction() {

        /*
         * Only one player remains.
         */
        if (activePlayers().size <= 1) {

            finishByFold()

            return
        }

        /*
         * Everyone is all-in.
         */
        if (allActivePlayersAllIn()) {

            runToShowdown()

            return
        }

        /*
         * Betting round finished.
         */
        if (roundComplete()) {

            advanceStage()

            return
        }

        /*
         * Continue to next player.
         */
        moveToNextPlayer()
    }

    // ============================================================
    // MOVE TO NEXT PLAYER
    // ============================================================

    private fun moveToNextPlayer() {

        val next =
            findNextAvailablePlayerOrNull(
                currentPlayerIndex
            )

        if (next == null) {

            advanceStage()

            return
        }

        currentPlayerIndex =
            next
    }

    // ============================================================
    // FIND NEXT AVAILABLE PLAYER
    // ============================================================

    private fun findNextAvailablePlayer(
        from: Int
    ): Int {

        return findNextAvailablePlayerOrNull(
            from
        ) ?: firstPlayerWhoCanAct()
    }

    private fun findNextAvailablePlayerOrNull(
        from: Int
    ): Int? {

        for (offset in 1..players.size) {

            val index =
                (from + offset) %
                        players.size

            if (canPlayerAct(index)) {

                return players[index].id
            }
        }

        return null
    }

    private fun canPlayerAct(
        index: Int
    ): Boolean {

        if (index !in players.indices) {
            return false
        }

        val player =
            players[index]

        return !player.folded &&
                !player.allIn &&
                player.chips > 0
    }

    private fun firstPlayerWhoCanAct(): Int {

        return players
            .firstOrNull {
                canPlayerAct(it.id)
            }
            ?.id ?: 0
    }

    // ============================================================
    // DEALER ROTATION
    // ============================================================

    private fun findNextEligiblePlayer(
        from: Int
    ): Int {

        for (offset in 1..players.size) {

            val index =
                (from + offset) %
                        players.size

            if (players[index].chips > 0) {

                return index
            }
        }

        return from
    }

    // ============================================================
    // ROUND COMPLETE
    // ============================================================

    private fun roundComplete(): Boolean {

        val active =
            activePlayers()

        val playersWhoCanAct =
            active.filter {
                !it.allIn &&
                        it.chips > 0
            }

        if (playersWhoCanAct.isEmpty()) {
            return true
        }

        val highest =
            highestCurrentBet()

        for (player in playersWhoCanAct) {

            if (
                player.id !in actedPlayers
            ) {

                return false
            }

            if (
                player.currentBet != highest
            ) {

                return false
            }
        }

        return true
    }

    // ============================================================
    // ADVANCE STREET
    // ============================================================

    private fun advanceStage() {

        actedPlayers.clear()

        /*
         * Reset street betting.
         */
        players.forEach {
            it.currentBet = 0
        }

        when (stage) {

            GameStage.PRE_FLOP -> {

                burnCard()

                communityCards.addAll(
                    deck.draw(3)
                )

                stage =
                    GameStage.FLOP
            }

            GameStage.FLOP -> {

                burnCard()

                communityCards.add(
                    deck.draw()
                )

                stage =
                    GameStage.TURN
            }

            GameStage.TURN -> {

                burnCard()

                communityCards.add(
                    deck.draw()
                )

                stage =
                    GameStage.RIVER
            }

            GameStage.RIVER -> {

                showdown()

                return
            }

            else -> {

                return
            }
        }

        /*
         * Post-flop:
         * First active player after dealer.
         */
        currentPlayerIndex =
            findFirstActivePlayerAfterDealer()

        /*
         * If everyone is all-in,
         * go directly to showdown.
         */
        if (allActivePlayersAllIn()) {

            runToShowdown()
        }
    }

    // ============================================================
    // FIRST PLAYER AFTER DEALER
    // ============================================================

    private fun findFirstActivePlayerAfterDealer(): Int {

        for (offset in 1..players.size) {

            val index =
                (dealerIndex + offset) %
                        players.size

            if (canPlayerAct(index)) {

                return index
            }
        }

        return dealerIndex.coerceIn(
            0,
            players.lastIndex
        )
    }

    // ============================================================
    // BURN CARD
    // ============================================================

    private fun burnCard() {

        if (deck.cardsRemaining() > 0) {

            deck.draw()
        }
    }

    // ============================================================
    // AUTOMATIC SHOWDOWN
    // ============================================================

    private fun runToShowdown() {

        while (
            communityCards.size < 5
        ) {

            when (
                communityCards.size
            ) {

                0 -> {

                    burnCard()

                    communityCards.addAll(
                        deck.draw(3)
                    )
                }

                3 -> {

                    burnCard()

                    communityCards.add(
                        deck.draw()
                    )
                }

                4 -> {

                    burnCard()

                    communityCards.add(
                        deck.draw()
                    )
                }

                else -> {

                    break
                }
            }
        }

        showdown()
    }

    // ============================================================
    // SHOWDOWN
    // ============================================================

    fun showdown() {

        if (
            stage == GameStage.SHOWDOWN ||
            stage == GameStage.FINISHED
        ) {

            return
        }

        stage =
            GameStage.SHOWDOWN

        val active =
            activePlayers()

        if (active.isEmpty()) {

            stage =
                GameStage.FINISHED

            return
        }

        /*
         * Only one player remains.
         */
        if (active.size == 1) {

            val winner =
                active[0]

            val amount =
                pot

            winner.chips += amount

            lastResult =
                "${winner.name} wins $amount chips!"

            pot = 0

            stage =
                GameStage.FINISHED

            return
        }

        // ========================================================
        // FIND BEST HAND
        // ========================================================

        var bestHand =
            evaluatePlayer(
                active[0]
            )

        for (i in 1 until active.size) {

            val hand =
                evaluatePlayer(
                    active[i]
                )

            if (
                HandEvaluator.compareHands(
                    hand,
                    bestHand
                ) > 0
            ) {

                bestHand =
                    hand
            }
        }

        // ========================================================
        // FIND WINNERS
        // ========================================================

        val winners =
            active.filter { player ->

                HandEvaluator.compareHands(
                    evaluatePlayer(player),
                    bestHand
                ) == 0
            }

        // ========================================================
        // SPLIT POT
        // ========================================================

        val share =
            pot / winners.size

        val remainder =
            pot % winners.size

        winners.forEachIndexed {
                index,
                player ->

            player.chips += share

            if (index == 0) {

                player.chips += remainder
            }
        }

        val names =
            winners.joinToString(", ") {
                it.name
            }

        lastResult =
            if (winners.size == 1) {

                "$names wins the pot with ${bestHand.rank.title}!"

            } else {

                "Tie! $names split the pot with ${bestHand.rank.title}."
            }

        pot = 0

        stage =
            GameStage.FINISHED
    }

    // ============================================================
    // EVALUATE PLAYER
    // ============================================================

    private fun evaluatePlayer(
        player: Player
    ): EvaluatedHand {

        return HandEvaluator.evaluate(
            player.hand +
                    communityCards
        )
    }

    // ============================================================
    // FOLD WINNER
    // ============================================================

    private fun finishByFold() {

        val winner =
            activePlayers().firstOrNull()

        if (winner == null) {

            stage =
                GameStage.FINISHED

            return
        }

        val amount =
            pot

        winner.chips += amount

        lastResult =
            "${winner.name} wins $amount chips!"

        pot = 0

        stage =
            GameStage.FINISHED
    }

    // ============================================================
    // ACTIVE PLAYERS
    // ============================================================

    private fun activePlayers(): List<Player> {

        return players.filter {
            !it.folded
        }
    }

    fun getActivePlayers(): List<Player> {

        return activePlayers()
    }

    // ============================================================
    // ALL-IN CHECK
    // ============================================================

    private fun allActivePlayersAllIn(): Boolean {

        val active =
            activePlayers()

        if (active.size <= 1) {

            return false
        }

        return active.all {
            it.allIn
        }
    }

    // ============================================================
    // HIGHEST BET
    // ============================================================

    private fun highestCurrentBet(): Int {

        return players.maxOfOrNull {
            it.currentBet
        } ?: 0
    }

    // ============================================================
    // HUMAN TURN
    // ============================================================

    private fun isHumanTurn(
        id: Int
    ): Boolean {

        if (id != 0) {
            return false
        }

        if (id !in players.indices) {
            return false
        }

        if (
            stage == GameStage.SHOWDOWN ||
            stage == GameStage.FINISHED
        ) {

            return false
        }

        if (
            currentPlayerIndex != 0
        ) {

            return false
        }

        val player =
            players[id]

        if (
            player.folded ||
            player.allIn ||
            player.chips <= 0
        ) {

            return false
        }

        return true
    }

    // ============================================================
    // AI STRENGTH
    // ============================================================

    private fun calculateAiStrength(
        player: Player
    ): Int {

        /*
         * PRE-FLOP
         */
        if (communityCards.size < 3) {

            if (player.hand.size < 2) {

                return 20
            }

            val first =
                player.hand[0].rank.value

            val second =
                player.hand[1].rank.value

            var strength =
                (first + second) * 2

            /*
             * Pair.
             */
            if (first == second) {

                strength += 35
            }

            /*
             * Same suit.
             */
            if (
                player.hand[0].suit ==
                player.hand[1].suit
            ) {

                strength += 8
            }

            /*
             * Connected cards.
             */
            if (
                abs(first - second) <= 2
            ) {

                strength += 8
            }

            return strength.coerceIn(
                1,
                100
            )
        }

        /*
         * POST-FLOP
         */
        return try {

            val hand =
                HandEvaluator.evaluate(
                    player.hand +
                            communityCards
                )

            when (hand.rank) {

                HandRank.HIGH_CARD ->
                    25

                HandRank.ONE_PAIR ->
                    45

                HandRank.TWO_PAIR ->
                    60

                HandRank.THREE_OF_A_KIND ->
                    72

                HandRank.STRAIGHT ->
                    82

                HandRank.FLUSH ->
                    86

                HandRank.FULL_HOUSE ->
                    94

                HandRank.FOUR_OF_A_KIND ->
                    98

                HandRank.STRAIGHT_FLUSH ->
                    100
            }

        } catch (_: Exception) {

            25
        }
    }

    // ============================================================
    // PUBLIC METHODS FOR UI
    // ============================================================

    fun getCurrentPlayer(): Player {

        return players[
            currentPlayerIndex
        ]
    }

    fun getPlayerHand(
        id: Int
    ): EvaluatedHand? {

        if (
            id !in players.indices
        ) {

            return null
        }

        if (
            communityCards.size < 3
        ) {

            return null
        }

        return try {

            evaluatePlayer(
                players[id]
            )

        } catch (_: Exception) {

            null
        }
    }
}