package com.example.pokerai.game

enum class PlayerAction {
    FOLD,
    CHECK,
    CALL,
    RAISE,
    ALL_IN
}

data class Player(
    val id: Int,
    val name: String,
    var chips: Int = 1000,

    val hand: MutableList<Card> =
        mutableListOf(),

    var folded: Boolean = false,

    var allIn: Boolean = false,

    var currentBet: Int = 0
) {

    fun resetForNewHand() {

        hand.clear()

        folded = false

        allIn = false

        currentBet = 0
    }

    fun receiveCard(card: Card) {

        hand.add(card)
    }

    fun bet(amount: Int): Int {

        if (amount <= 0) {
            return 0
        }

        val actual =
            minOf(amount, chips)

        chips -= actual

        currentBet += actual

        if (chips == 0) {
            allIn = true
        }

        return actual
    }

    fun fold() {

        folded = true
    }
}