package com.example.pokerai.game

enum class Suit(
    val symbol: String,
    val isRed: Boolean
) {
    HEARTS("♥", true),
    DIAMONDS("♦", true),
    CLUBS("♣", false),
    SPADES("♠", false)
}

enum class Rank(
    val value: Int,
    val symbol: String
) {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A")
}

data class Card(
    val rank: Rank,
    val suit: Suit
) {
    fun displayName(): String {
        return "${rank.symbol}${suit.symbol}"
    }
}