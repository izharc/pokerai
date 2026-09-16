package com.example.pokerai.game

enum class HandRank(
    val title: String,
    val value: Int
) {
    HIGH_CARD("High Card", 1),
    ONE_PAIR("One Pair", 2),
    TWO_PAIR("Two Pair", 3),
    THREE_OF_A_KIND("Three of a Kind", 4),
    STRAIGHT("Straight", 5),
    FLUSH("Flush", 6),
    FULL_HOUSE("Full House", 7),
    FOUR_OF_A_KIND("Four of a Kind", 8),
    STRAIGHT_FLUSH("Straight Flush", 9)
}

data class EvaluatedHand(
    val rank: HandRank,
    val score: List<Int>
) {
    override fun toString(): String {
        return rank.title
    }
}

object HandEvaluator {

    fun evaluate(cards: List<Card>): EvaluatedHand {

        require(cards.size >= 5) {
            "At least 5 cards are required"
        }

        val combinations = combinationsOfFive(cards)

        if (combinations.isEmpty()) {
            return EvaluatedHand(
                HandRank.HIGH_CARD,
                emptyList()
            )
        }

        // Find the best 5-card hand manually.
        // This avoids Kotlin generic inference problems.
        var bestHand = evaluateFiveCards(combinations[0])

        for (i in 1 until combinations.size) {

            val candidate = evaluateFiveCards(combinations[i])

            if (compareHands(candidate, bestHand) > 0) {
                bestHand = candidate
            }
        }

        return bestHand
    }

    fun compareHands(
        first: EvaluatedHand,
        second: EvaluatedHand
    ): Int {

        // Compare hand type first
        if (first.rank.value != second.rank.value) {
            return first.rank.value.compareTo(second.rank.value)
        }

        // Same hand type - compare kickers
        val maxSize = maxOf(
            first.score.size,
            second.score.size
        )

        for (i in 0 until maxSize) {

            val a = first.score.getOrElse(i) { 0 }
            val b = second.score.getOrElse(i) { 0 }

            if (a != b) {
                return a.compareTo(b)
            }
        }

        return 0
    }

    private fun combinationsOfFive(
        cards: List<Card>
    ): List<List<Card>> {

        val result = mutableListOf<List<Card>>()

        for (a in 0 until cards.size - 4) {
            for (b in a + 1 until cards.size - 3) {
                for (c in b + 1 until cards.size - 2) {
                    for (d in c + 1 until cards.size - 1) {
                        for (e in d + 1 until cards.size) {

                            result.add(
                                listOf(
                                    cards[a],
                                    cards[b],
                                    cards[c],
                                    cards[d],
                                    cards[e]
                                )
                            )
                        }
                    }
                }
            }
        }

        return result
    }

    private fun evaluateFiveCards(
        cards: List<Card>
    ): EvaluatedHand {

        val ranks = cards
            .map { it.rank.value }
            .sortedDescending()

        val counts = ranks
            .groupingBy { it }
            .eachCount()

        val flush =
            cards.map { it.suit }.distinct().size == 1

        val straightHigh =
            findStraightHigh(ranks)

        // --------------------------------
        // STRAIGHT FLUSH
        // --------------------------------

        if (flush && straightHigh != null) {

            return EvaluatedHand(
                HandRank.STRAIGHT_FLUSH,
                listOf(straightHigh)
            )
        }

        // --------------------------------
        // FOUR OF A KIND
        // --------------------------------

        val four =
            counts
                .filter { it.value == 4 }
                .keys
                .maxOrNull()

        if (four != null) {

            val kicker =
                ranks
                    .filter { it != four }
                    .maxOrNull() ?: 0

            return EvaluatedHand(
                HandRank.FOUR_OF_A_KIND,
                listOf(
                    four,
                    kicker
                )
            )
        }

        // --------------------------------
        // FULL HOUSE
        // --------------------------------

        val triples =
            counts
                .filter { it.value == 3 }
                .keys
                .sortedDescending()

        val pairs =
            counts
                .filter { it.value == 2 }
                .keys
                .sortedDescending()

        if (triples.isNotEmpty()) {

            val triple = triples[0]

            val pairForFullHouse =
                if (triples.size >= 2) {
                    triples[1]
                } else {
                    pairs.firstOrNull()
                }

            if (pairForFullHouse != null) {

                return EvaluatedHand(
                    HandRank.FULL_HOUSE,
                    listOf(
                        triple,
                        pairForFullHouse
                    )
                )
            }
        }

        // --------------------------------
        // FLUSH
        // --------------------------------

        if (flush) {

            return EvaluatedHand(
                HandRank.FLUSH,
                ranks
            )
        }

        // --------------------------------
        // STRAIGHT
        // --------------------------------

        if (straightHigh != null) {

            return EvaluatedHand(
                HandRank.STRAIGHT,
                listOf(straightHigh)
            )
        }

        // --------------------------------
        // THREE OF A KIND
        // --------------------------------

        if (triples.isNotEmpty()) {

            val triple = triples[0]

            val kickers =
                ranks
                    .filter { it != triple }
                    .take(2)

            return EvaluatedHand(
                HandRank.THREE_OF_A_KIND,
                listOf(triple) + kickers
            )
        }

        // --------------------------------
        // TWO PAIR
        // --------------------------------

        if (pairs.size >= 2) {

            val highPair = pairs[0]
            val lowPair = pairs[1]

            val kicker =
                ranks
                    .filter {
                        it != highPair &&
                                it != lowPair
                    }
                    .firstOrNull() ?: 0

            return EvaluatedHand(
                HandRank.TWO_PAIR,
                listOf(
                    highPair,
                    lowPair,
                    kicker
                )
            )
        }

        // --------------------------------
        // ONE PAIR
        // --------------------------------

        if (pairs.size == 1) {

            val pair = pairs[0]

            val kickers =
                ranks
                    .filter { it != pair }
                    .take(3)

            return EvaluatedHand(
                HandRank.ONE_PAIR,
                listOf(pair) + kickers
            )
        }

        // --------------------------------
        // HIGH CARD
        // --------------------------------

        return EvaluatedHand(
            HandRank.HIGH_CARD,
            ranks
        )
    }

    private fun findStraightHigh(
        ranks: List<Int>
    ): Int? {

        val unique =
            ranks
                .distinct()
                .toMutableList()

        // Ace can also be used as 1
        if (unique.contains(14)) {
            unique.add(1)
        }

        unique.sortDescending()

        var consecutive = 1

        for (i in 0 until unique.size - 1) {

            if (unique[i] - unique[i + 1] == 1) {

                consecutive++

                if (consecutive >= 5) {

                    return unique[i - 3]
                }

            } else {

                consecutive = 1
            }
        }

        return null
    }
}