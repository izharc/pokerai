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

    private val actedPlayers =
        mutableSetOf<Int>()


    /* ============================================================
       CREATE PLAYERS
       ============================================================ */

    init {

        require(numberOfPlayers in 2..8)

        for (i in 0 until numberOfPlayers) {

            players.add(
                Player(
                    id = i,
                    name =
                        if (i == 0)
                            "YOU"
                        else
                            "Computer $i",
                    chips = startingChips
                )
            )
        }
    }


    /* ============================================================
       START HAND
       ============================================================ */

    fun startNewHand() {

        deck = Deck()

        communityCards.clear()

        pot = 0

        lastResult = ""

        actedPlayers.clear()

        stage = GameStage.PRE_FLOP

        players.forEach {
            it.resetForNewHand()
        }


        /*
         * Deal two cards.
         */

        repeat(2) {

            players.forEach { player ->

                if (player.chips > 0) {

                    player.receiveCard(
                        deck.draw()
                    )
                }
            }
        }


        /*
         * Small blind.
         */

        val sb =
            minOf(
                smallBlind,
                players[0].chips
            )

        if (sb > 0) {

            pot += players[0].bet(sb)
        }


        /*
         * Big blind.
         */

        val bb =
            minOf(
                bigBlind,
                players[1].chips
            )

        if (bb > 0) {

            pot += players[1].bet(bb)
        }


        /*
         * Pre-flop turn.
         */

        currentPlayerIndex =
            if (players.size == 2) {

                0

            } else {

                findNextAvailablePlayer(1)
            }
    }


    /* ============================================================
       HUMAN FOLD
       ============================================================ */

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


    /* ============================================================
       HUMAN CHECK
       ============================================================ */

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


    /* ============================================================
       HUMAN CALL
       ============================================================ */

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


    /* ============================================================
       HUMAN RAISE
       ============================================================ */

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
         * Everybody must respond to the raise.
         */
        actedPlayers.clear()

        actedPlayers.add(playerId)

        afterAction()
    }


    /* ============================================================
       HUMAN ALL IN
       ============================================================ */

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

        val amount =
            player.chips

        val paid =
            player.bet(amount)

        pot += paid

        actedPlayers.clear()

        actedPlayers.add(id)

        afterAction()
    }


    /* ============================================================
       CAN CHECK
       ============================================================ */

    fun canCheck(
        id: Int
    ): Boolean {

        if (id !in players.indices) {
            return false
        }

        val player =
            players[id]

        return player.currentBet ==
                highestCurrentBet()
    }


    /* ============================================================
       AI TURN LOOP
       ============================================================ */

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


    /* ============================================================
       AI DECISION
       ============================================================ */

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

                    canCheck(id) -> {

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

                    canCheck(id) -> {

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

                    canCheck(id) -> {

                        aiCheck(id)
                    }

                    else -> {

                        aiCall(id)
                    }
                }
            }
        }
    }


    /* ============================================================
       AI CHECK
       ============================================================ */

    private fun aiCheck(
        id: Int
    ) {

        actedPlayers.add(id)

        afterAction()
    }


    /* ============================================================
       AI CALL
       ============================================================ */

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


    /* ============================================================
       AI RAISE
       ============================================================ */

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
         * New raise resets responses.
         */

        actedPlayers.clear()

        actedPlayers.add(id)

        afterAction()
    }


    /* ============================================================
       AI FOLD
       ============================================================ */

    private fun aiFold(
        id: Int
    ) {

        players[id].folded = true

        actedPlayers.add(id)

        afterAction()
    }


    /* ============================================================
       AFTER ACTION
       ============================================================ */

    private fun afterAction() {

        /*
         * One player remaining.
         */

        if (activePlayers().size <= 1) {

            finishByFold()

            return
        }


        /*
         * Everybody all-in.
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
         * Continue.
         */

        moveToNextPlayer()
    }


    /* ============================================================
       MOVE TO NEXT PLAYER
       ============================================================ */

    private fun moveToNextPlayer() {

        val next =
            findNextAvailablePlayerOrNull(
                currentPlayerIndex
            )

        if (next == null) {

            advanceStage()

            return
        }

        currentPlayerIndex = next
    }


    /* ============================================================
       NEXT PLAYER
       ============================================================ */

    private fun findNextAvailablePlayer(
        from: Int
    ): Int {

        for (offset in 1..players.size) {

            val index =
                (from + offset) %
                        players.size

            val player =
                players[index]

            if (
                !player.folded &&
                !player.allIn &&
                player.chips >= 0
            ) {

                return index
            }
        }

        return 0
    }


    private fun findNextAvailablePlayerOrNull(
        from: Int
    ): Int? {

        for (offset in 1..players.size) {

            val index =
                (from + offset) %
                        players.size

            val player =
                players[index]

            if (
                !player.folded &&
                !player.allIn &&
                player.chips >= 0
            ) {

                return index
            }
        }

        return null
    }


    /* ============================================================
       ROUND COMPLETE
       ============================================================ */

    private fun roundComplete(): Boolean {

        val active =
            activePlayers()

        val playersWhoCanAct =
            active.filter {
                !it.allIn &&
                        it.chips >= 0
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


    /* ============================================================
       ADVANCE STREET
       ============================================================ */

    private fun advanceStage() {

        actedPlayers.clear()

        /*
         * Reset betting for the new street.
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
         * Post-flop starts with first active player.
         */

        currentPlayerIndex =
            findFirstActivePlayer()

        /*
         * If everyone is all-in,
         * automatically go to showdown.
         */

        if (allActivePlayersAllIn()) {

            runToShowdown()
        }
    }


    /* ============================================================
       FIRST ACTIVE PLAYER
       ============================================================ */

    private fun findFirstActivePlayer(): Int {

        for (player in players) {

            if (
                !player.folded &&
                !player.allIn &&
                player.chips >= 0
            ) {

                return player.id
            }
        }

        return 0
    }


    /* ============================================================
       BURN CARD
       ============================================================ */

    private fun burnCard() {

        if (deck.cardsRemaining() > 0) {
            deck.draw()
        }
    }


    /* ============================================================
       AUTOMATIC SHOWDOWN
       ============================================================ */

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


    /* ============================================================
       SHOWDOWN
       ============================================================ */

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
         * Only one player.
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


        /*
         * Find best hand.
         */

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

                bestHand = hand
            }
        }


        /*
         * Find all winners.
         */

        val winners =
            active.filter { player ->

                HandEvaluator.compareHands(
                    evaluatePlayer(player),
                    bestHand
                ) == 0
            }


        val share =
            pot / winners.size

        val remainder =
            pot % winners.size


        /*
         * Distribute pot.
         */

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


    /* ============================================================
       EVALUATE
       ============================================================ */

    private fun evaluatePlayer(
        player: Player
    ): EvaluatedHand {

        return HandEvaluator.evaluate(
            player.hand +
                    communityCards
        )
    }


    /* ============================================================
       FOLD WINNER
       ============================================================ */

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


    /* ============================================================
       ACTIVE PLAYERS
       ============================================================ */

    private fun activePlayers(): List<Player> {

        return players.filter {
            !it.folded
        }
    }


    /* ============================================================
       ALL-IN CHECK
       ============================================================ */

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


    /* ============================================================
       HIGHEST BET
       ============================================================ */

    private fun highestCurrentBet(): Int {

        return players.maxOfOrNull {
            it.currentBet
        } ?: 0
    }


    /* ============================================================
       HUMAN TURN
       ============================================================ */

    private fun isHumanTurn(
        id: Int
    ): Boolean {

        if (id != 0) {
            return false
        }

        if (
            id !in players.indices
        ) {
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


    /* ============================================================
       AI STRENGTH
       ============================================================ */

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


    /* ============================================================
       PUBLIC METHODS
       ============================================================ */

    fun getCurrentPlayer(): Player {

        return players[
            currentPlayerIndex
        ]
    }

    fun getActivePlayers(): List<Player> {

        return activePlayers()
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