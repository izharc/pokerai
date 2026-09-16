package com.example.pokerai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pokerai.game.Difficulty
import com.example.pokerai.game.GameStage
import com.example.pokerai.game.PokerGame

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF031B12)
                ) {
                    PokerApp()
                }
            }
        }
    }
}

@Composable
fun PokerApp() {

    var numberOfPlayers by remember {
        mutableIntStateOf(4)
    }

    var difficulty by remember {
        mutableStateOf(Difficulty.MEDIUM)
    }

    var game by remember {
        mutableStateOf<PokerGame?>(null)
    }

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    /*
     * SETUP
     */
    if (game == null) {

        SetupScreen(
            numberOfPlayers = numberOfPlayers,
            difficulty = difficulty,

            onPlayersChanged = {
                numberOfPlayers = it
            },

            onDifficultyChanged = {
                difficulty = it
            },

            onStart = {

                val newGame = PokerGame(
                    numberOfPlayers = numberOfPlayers,
                    startingChips = 1000,
                    smallBlind = 10,
                    bigBlind = 20,
                    difficulty = difficulty
                )

                newGame.startNewHand()

                /*
                 * Let computers play until our turn.
                 */
                newGame.processComputerTurns()

                game = newGame

                refreshKey++
            }
        )

    } else {

        /*
         * Force the table to redraw after every action.
         */
        key(refreshKey) {

            PokerTableScreen(
                game = game!!,

                onFold = {

                    game!!.playerFold(0)
                    game!!.processComputerTurns()

                    refreshKey++
                },

                onCheck = {

                    game!!.playerCheck(0)
                    game!!.processComputerTurns()

                    refreshKey++
                },

                onCall = {

                    game!!.playerCall(0)
                    game!!.processComputerTurns()

                    refreshKey++
                },

                onRaise = {

                    game!!.playerRaise(
                        playerId = 0,
                        raiseAmount = 40
                    )

                    game!!.processComputerTurns()

                    refreshKey++
                },

                onAllIn = {

                    game!!.playerAllIn(0)
                    game!!.processComputerTurns()

                    refreshKey++
                },

                onNewHand = {

                    game!!.startNewHand()
                    game!!.processComputerTurns()

                    refreshKey++
                },

                onNewGame = {

                    game = null

                    refreshKey++
                }
            )
        }
    }
}


/* ============================================================
   SETUP SCREEN
   ============================================================ */

@Composable
fun SetupScreen(
    numberOfPlayers: Int,
    difficulty: Difficulty,
    onPlayersChanged: (Int) -> Unit,
    onDifficultyChanged: (Difficulty) -> Unit,
    onStart: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF031B12))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "♠  POKER  ♥",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "TEXAS HOLD'EM",
            color = Color(0xFFFFD54F),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "NUMBER OF PLAYERS",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.Center
        ) {

            for (players in 2..8) {

                SelectionButton(
                    text = players.toString(),
                    selected = numberOfPlayers == players,
                    onClick = {
                        onPlayersChanged(players)
                    }
                )

                Spacer(modifier = Modifier.width(5.dp))
            }
        }

        Spacer(modifier = Modifier.height(35.dp))

        Text(
            text = "AI DIFFICULTY",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.Center
        ) {

            DifficultyButton(
                text = "EASY",
                selected = difficulty == Difficulty.EASY,
                onClick = {
                    onDifficultyChanged(Difficulty.EASY)
                }
            )

            Spacer(modifier = Modifier.width(7.dp))

            DifficultyButton(
                text = "MEDIUM",
                selected = difficulty == Difficulty.MEDIUM,
                onClick = {
                    onDifficultyChanged(Difficulty.MEDIUM)
                }
            )

            Spacer(modifier = Modifier.width(7.dp))

            DifficultyButton(
                text = "HARD",
                selected = difficulty == Difficulty.HARD,
                onClick = {
                    onDifficultyChanged(Difficulty.HARD)
                }
            )
        }

        Spacer(modifier = Modifier.height(42.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFC107),
                contentColor = Color.Black
            )
        ) {

            Text(
                text = "START GAME",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


/* ============================================================
   PLAYER BUTTON
   ============================================================ */

@Composable
fun SelectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (selected)
                    Color(0xFFFFC107)
                else
                    Color(0xFF123C2B),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                Color(0xFFFFC107),
                RoundedCornerShape(10.dp)
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            color = if (selected)
                Color.Black
            else
                Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}


/* ============================================================
   DIFFICULTY BUTTON
   ============================================================ */

@Composable
fun DifficultyButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .width(95.dp)
            .height(44.dp)
            .background(
                if (selected)
                    Color(0xFFFFC107)
                else
                    Color(0xFF123C2B),
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                Color(0xFFFFC107),
                RoundedCornerShape(10.dp)
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            color = if (selected)
                Color.Black
            else
                Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}


/* ============================================================
   POKER TABLE
   ============================================================ */

@Composable
fun PokerTableScreen(
    game: PokerGame,

    onFold: () -> Unit,
    onCheck: () -> Unit,
    onCall: () -> Unit,
    onRaise: () -> Unit,
    onAllIn: () -> Unit,

    onNewHand: () -> Unit,
    onNewGame: () -> Unit
) {

    val human = game.players[0]

    val humanTurn =
        game.currentPlayerIndex == 0 &&
                game.stage != GameStage.SHOWDOWN &&
                game.stage != GameStage.FINISHED &&
                !human.folded &&
                !human.allIn

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF031B12))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        /* HEADER */

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "♠ POKER",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = difficultyText(game.difficulty),
                color = Color(0xFFFFD54F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "PLAYERS: ${game.players.size}",
            color = Color.LightGray,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(5.dp))

        /*
         * TURN STATUS
         */

        Text(
            text = when {

                humanTurn ->
                    "YOUR TURN"

                game.stage == GameStage.SHOWDOWN ->
                    "SHOWDOWN"

                game.stage == GameStage.FINISHED ->
                    "HAND FINISHED"

                else ->
                    "COMPUTER THINKING..."
            },
            color = when {

                humanTurn ->
                    Color(0xFFFFD54F)

                else ->
                    Color.LightGray
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))


        /* COMPUTER PLAYERS */

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            game.players
                .filter { it.id != 0 }
                .forEach { player ->

                    PlayerMiniCard(
                        name = player.name,
                        chips = player.chips,
                        current =
                            game.currentPlayerIndex == player.id &&
                                    game.stage != GameStage.SHOWDOWN &&
                                    game.stage != GameStage.FINISHED
                    )
                }
        }

        Spacer(modifier = Modifier.height(7.dp))


        /* TABLE */

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF087A3E)
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "POT: ${game.pot}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.Center
                ) {

                    for (i in 0 until 5) {

                        if (i < game.communityCards.size) {

                            PokerCard(
                                text = game.communityCards[i].displayName()
                            )

                        } else {

                            PokerCard(
                                text = "?"
                            )
                        }

                        if (i < 4) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stageText(game.stage),
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                if (game.lastResult.isNotBlank()) {

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = game.lastResult,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))


        /* YOUR CARDS */

        Text(
            text = "YOUR CARDS",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.Center
        ) {

            human.hand.forEachIndexed { index, card ->

                PokerCard(
                    text = card.displayName()
                )

                if (index == 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "Your chips: ${human.chips}",
            color = Color.White,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(7.dp))


        /* ACTION BUTTONS */

        if (
            game.stage != GameStage.SHOWDOWN &&
            game.stage != GameStage.FINISHED
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                ActionButton(
                    text = "FOLD",
                    enabled = humanTurn,
                    onClick = onFold
                )

                ActionButton(
                    text = "CHECK",
                    enabled =
                        humanTurn &&
                                game.canCheck(0),
                    onClick = onCheck
                )

                ActionButton(
                    text = "CALL",
                    enabled =
                        humanTurn &&
                                !game.canCheck(0),
                    onClick = onCall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                ActionButton(
                    text = "RAISE +40",
                    enabled = humanTurn,
                    onClick = onRaise
                )

                Spacer(modifier = Modifier.width(12.dp))

                ActionButton(
                    text = "ALL IN",
                    enabled =
                        humanTurn &&
                                human.chips > 0,
                    onClick = onAllIn
                )
            }

        } else {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                Button(
                    onClick = onNewHand,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color.Black
                    )
                ) {

                    Text(
                        text = "NEW HAND",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onNewGame,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF444444),
                        contentColor = Color.White
                    )
                ) {

                    Text(
                        text = "NEW GAME",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}


/* ============================================================
   COMPUTER PLAYER
   ============================================================ */

@Composable
fun PlayerMiniCard(
    name: String,
    chips: Int,
    current: Boolean
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = name,
            color = if (current)
                Color(0xFFFFD54F)
            else
                Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        Row {

            PokerBackCard()

            Spacer(modifier = Modifier.width(2.dp))

            PokerBackCard()
        }

        Text(
            text = "$chips",
            color = Color.LightGray,
            fontSize = 8.sp
        )
    }
}


/* ============================================================
   CARD
   ============================================================ */

@Composable
fun PokerCard(
    text: String
) {

    val red =
        text.contains("♥") ||
                text.contains("♦")

    Box(
        modifier = Modifier
            .width(43.dp)
            .height(60.dp)
            .background(
                Color.White,
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                Color.DarkGray,
                RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            color = if (red)
                Color.Red
            else
                Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


/* ============================================================
   CARD BACK
   ============================================================ */

@Composable
fun PokerBackCard() {

    Box(
        modifier = Modifier
            .width(24.dp)
            .height(34.dp)
            .background(
                Color(0xFF1D2D50),
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                Color.White,
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "♠",
            color = Color.White,
            fontSize = 10.sp
        )
    }
}


/* ============================================================
   ACTION BUTTON
   ============================================================ */

@Composable
fun ActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {

                text == "FOLD" ->
                    Color(0xFFD32F2F)

                text == "ALL IN" ->
                    Color(0xFFFFC107)

                text.startsWith("RAISE") ->
                    Color(0xFF6A45B8)

                else ->
                    Color(0xFF303030)
            },

            contentColor =
                if (text == "ALL IN")
                    Color.Black
                else
                    Color.White,

            disabledContainerColor =
                Color(0xFF202020),

            disabledContentColor =
                Color(0xFF777777)
        )
    ) {

        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


/* ============================================================
   TEXT HELPERS
   ============================================================ */

fun stageText(
    stage: GameStage
): String {

    return when (stage) {

        GameStage.WAITING ->
            "WAITING"

        GameStage.PRE_FLOP ->
            "PRE-FLOP"

        GameStage.FLOP ->
            "FLOP"

        GameStage.TURN ->
            "TURN"

        GameStage.RIVER ->
            "RIVER"

        GameStage.SHOWDOWN ->
            "SHOWDOWN"

        GameStage.FINISHED ->
            "FINISHED"
    }
}


fun difficultyText(
    difficulty: Difficulty
): String {

    return when (difficulty) {

        Difficulty.EASY ->
            "EASY"

        Difficulty.MEDIUM ->
            "MEDIUM"

        Difficulty.HARD ->
            "HARD"
    }
}