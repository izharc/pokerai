package com.example.pokerai.game

class Deck {

    private val cards = mutableListOf<Card>()

    init {
        reset()
    }

    fun reset() {
        cards.clear()

        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                cards.add(
                    Card(rank, suit)
                )
            }
        }

        shuffle()
    }

    fun shuffle() {
        cards.shuffle()
    }

    fun draw(): Card {
        if (cards.isEmpty()) {
            throw IllegalStateException("No cards left in deck")
        }

        return cards.removeAt(0)
    }

    fun draw(count: Int): List<Card> {

        require(count >= 0)

        require(cards.size >= count) {
            "Not enough cards"
        }

        return List(count) {
            draw()
        }
    }

    fun cardsRemaining(): Int {
        return cards.size
    }
}