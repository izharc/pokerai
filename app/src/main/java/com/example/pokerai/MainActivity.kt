package com.example.pokerai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pokerai.game.Card
import com.example.pokerai.game.Difficulty
import com.example.pokerai.game.GameStage
import com.example.pokerai.game.Player
import com.example.pokerai.game.PokerGame

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PokerApp()
        }
    }
}

/* ============================================================
   COLORS
   ============================================================ */

private val DarkBackground = Color(0xFF041A13)
private val TableGreen = Color(0xFF08783F)
private val TableGreenDark = Color(0xFF04592F)
private val Gold = Color(0xFFFFC107)
private val GoldDark = Color(0xFFB8860B)
private val White = Color(0xFFFFFFFF)
private val CardRed = Color(0xFFD32F2F)
private val CardBlue = Color(0xFF172B4D)
private val FoldRed = Color(0xFFD32F2F)
private val CallGreen = Color(0xFF2E9B57)
private val RaisePurple = Color(0xFF7045B8)
private val AllInYellow = Color(0xFFFFC107)

/* ============================================================
   ROOT
   ============================================================ */

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

    MaterialTheme {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {

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

                        newGame.processComputerTurns()

                        game = newGame

                        refreshKey++
                    }
                )

            } else {

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
    }
}

/* ============================================================
   SETUP SCREEN
   ============================================================ */

@Composable
private fun SetupScreen(
    numberOfPlayers: Int,
    difficulty: Difficulty,
    onPlayersChanged: (Int) -> Unit,
    onDifficultyChanged: (Difficulty) -> Unit,
    onStart: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "♠ POKER ♥",
            color = Gold,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "TEXAS HOLD'EM",
            color = White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(35.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),

            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0A3024)
            ),

            shape = RoundedCornerShape(22.dp)
        ) {

            Column(
                modifier = Modifier.padding(22.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "NUMBER OF PLAYERS",
                    color = Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "$numberOfPlayers PLAYERS",
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Slider(
                    value = numberOfPlayers.toFloat(),
                    onValueChange = {

                        onPlayersChanged(
                            it.toInt().coerceIn(2, 8)
                        )
                    },

                    valueRange = 2f..8f,
                    steps = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "2",
                        color = White
                    )

                    Text(
                        text = "8",
                        color = White
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "AI DIFFICULTY",
            color = Gold,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            DifficultyButton(
                title = "EASY",
                selected =
                    difficulty == Difficulty.EASY
            ) {
                onDifficultyChanged(
                    Difficulty.EASY
                )
            }

            DifficultyButton(
                title = "MEDIUM",
                selected =
                    difficulty == Difficulty.MEDIUM
            ) {
                onDifficultyChanged(
                    Difficulty.MEDIUM
                )
            }

            DifficultyButton(
                title = "HARD",
                selected =
                    difficulty == Difficulty.HARD
            ) {
                onDifficultyChanged(
                    Difficulty.HARD
                )
            }
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Button(
            onClick = onStart,

            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = Color.Black
            ),

            shape = RoundedCornerShape(16.dp)
        ) {

            Text(
                text = "START GAME",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/* ============================================================
   DIFFICULTY BUTTON
   ============================================================ */

@Composable
private fun DifficultyButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    if (selected) {

        Button(
            onClick = onClick,

            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = Color.Black
            ),

            shape = RoundedCornerShape(12.dp)
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        }

    } else {

        OutlinedButton(
            onClick = onClick,

            border = BorderStroke(
                1.dp,
                GoldDark
            ),

            shape = RoundedCornerShape(12.dp)
        ) {

            Text(
                text = title,
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* ============================================================
   MAIN POKER TABLE
   ============================================================ */

@Composable
private fun PokerTableScreen(
    game: PokerGame,

    onFold: () -> Unit,
    onCheck: () -> Unit,
    onCall: () -> Unit,
    onRaise: () -> Unit,
    onAllIn: () -> Unit,

    onNewHand: () -> Unit,
    onNewGame: () -> Unit
) {

    val human = game.players.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 6.dp
            )
    ) {

        /* ====================================================
           HEADER
           ==================================================== */

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "♠ POKER",
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            StageBadge(
                stage = game.stage
            )
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        /* ====================================================
           TABLE
           ==================================================== */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(
                    RoundedCornerShape(34.dp)
                )
                .background(TableGreen)
                .border(
                    width = 4.dp,
                    color = GoldDark,
                    shape = RoundedCornerShape(34.dp)
                )
                .padding(10.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                /* ============================================
                   AI PLAYERS
                   ============================================ */

                AiPlayersRow(
                    players = game.players.drop(1),
                    currentPlayerIndex =
                        game.currentPlayerIndex
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                /* ============================================
                   POT
                   ============================================ */

                PotDisplay(
                    pot = game.pot
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                /* ============================================
                   COMMUNITY CARDS
                   ============================================ */

                CommunityCards(
                    cards = game.communityCards,
                    stage = game.stage
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                /* ============================================
                   RESULT
                   ============================================ */

                if (
                    game.stage == GameStage.SHOWDOWN ||
                    game.stage == GameStage.FINISHED
                ) {

                    ResultBox(
                        text = game.lastResult
                    )
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                /* ============================================
                   YOUR AREA
                   ============================================ */

                if (human != null) {

                    PlayerArea(
                        player = human,

                        isTurn =
                            game.currentPlayerIndex == 0 &&
                                    game.stage !=
                                    GameStage.SHOWDOWN &&
                                    game.stage !=
                                    GameStage.FINISHED
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        /* ====================================================
           ACTION AREA
           ==================================================== */

        if (
            game.stage != GameStage.SHOWDOWN &&
            game.stage != GameStage.FINISHED
        ) {

            ActionPanel(
                game = game,

                onFold = onFold,
                onCheck = onCheck,
                onCall = onCall,
                onRaise = onRaise,
                onAllIn = onAllIn
            )

        } else {

            EndGamePanel(
                onNewHand = onNewHand,
                onNewGame = onNewGame
            )
        }
    }
}

/* ============================================================
   STAGE BADGE
   ============================================================ */

@Composable
private fun StageBadge(
    stage: GameStage
) {

    val text = when (stage) {

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

    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(10.dp)
            )
            .background(
                Color.Black.copy(alpha = 0.45f)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text = text,
            color = Gold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ============================================================
   AI PLAYERS
   ============================================================ */

@Composable
private fun AiPlayersRow(
    players: List<Player>,
    currentPlayerIndex: Int
) {

    LazyRow(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(7.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        items(
            items = players,
            key = {
                it.id
            }
        ) { player ->

            SmallPlayerCard(
                player = player,

                isTurn =
                    currentPlayerIndex == player.id
            )
        }
    }
}

/* ============================================================
   SMALL PLAYER CARD
   ============================================================ */

@Composable
private fun SmallPlayerCard(
    player: Player,
    isTurn: Boolean
) {

    val borderColor =
        if (isTurn)
            Gold
        else
            Color.White.copy(alpha = 0.25f)

    val background =
        if (isTurn)
            Color(0xFF155F39)
        else
            Color.Black.copy(alpha = 0.28f)

    Card(
        modifier = Modifier
            .width(105.dp)
            .border(
                width = if (isTurn) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(13.dp)
            ),

        colors = CardDefaults.cardColors(
            containerColor = background
        ),

        shape = RoundedCornerShape(13.dp)
    ) {

        Column(
            modifier = Modifier.padding(6.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                if (isTurn) {

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Gold)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )
                }

                Text(
                    text = player.name,
                    color = White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Text(
                text = "${player.chips} chips",
                color = Gold,
                fontSize = 9.sp
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {

                CardBack(
                    small = true
                )

                CardBack(
                    small = true
                )
            }

            if (player.currentBet > 0) {

                Text(
                    text = "BET ${player.currentBet}",
                    color = White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (player.folded) {

                Text(
                    text = "FOLDED",
                    color = Color(0xFFFF7777),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (player.allIn) {

                Text(
                    text = "ALL IN",
                    color = Gold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ============================================================
   POT
   ============================================================ */

@Composable
private fun PotDisplay(
    pot: Int
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "POT",
            color = White.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$pot",
            color = Gold,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/* ============================================================
   COMMUNITY CARDS
   ============================================================ */

@Composable
private fun CommunityCards(
    cards: List<Card>,
    stage: GameStage
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {

            repeat(5) { index ->

                if (index < cards.size) {

                    PokerCard(
                        card = cards[index]
                    )

                } else {

                    EmptyCard()
                }
            }
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = when (stage) {

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

                else ->
                    ""
            },

            color = Gold,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/* ============================================================
   EMPTY CARD
   ============================================================ */

@Composable
private fun EmptyCard() {

    Box(
        modifier = Modifier
            .width(43.dp)
            .height(61.dp)
            .clip(
                RoundedCornerShape(5.dp)
            )
            .background(
                CardBlue
            )
            .border(
                1.dp,
                White.copy(alpha = 0.55f),
                RoundedCornerShape(5.dp)
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = "?",
            color = White.copy(alpha = 0.75f),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ============================================================
   POKER CARD
   ============================================================ */

@Composable
private fun PokerCard(
    card: Card
) {

    val textColor =
        if (card.suit.isRed)
            CardRed
        else
            Color.Black

    Box(
        modifier = Modifier
            .width(43.dp)
            .height(61.dp)
            .clip(
                RoundedCornerShape(5.dp)
            )
            .background(Color.White)
            .border(
                1.dp,
                Color.Black.copy(alpha = 0.25f),
                RoundedCornerShape(5.dp)
            )
            .padding(3.dp),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = card.displayName(),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/* ============================================================
   CARD BACK
   ============================================================ */

@Composable
private fun CardBack(
    small: Boolean = false
) {

    val width =
        if (small) 27.dp else 43.dp

    val height =
        if (small) 38.dp else 61.dp

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(
                RoundedCornerShape(4.dp)
            )
            .background(
                CardBlue
            )
            .border(
                1.dp,
                White.copy(alpha = 0.6f),
                RoundedCornerShape(4.dp)
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = "♠",
            color = White.copy(alpha = 0.7f),
            fontSize =
                if (small) 10.sp else 16.sp
        )
    }
}

/* ============================================================
   YOUR PLAYER AREA
   ============================================================ */

@Composable
private fun PlayerArea(
    player: Player,
    isTurn: Boolean
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (isTurn) {

                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Gold)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "YOUR TURN",
                    color = Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {

                Text(
                    text = "YOUR CARDS",
                    color = White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {

            player.hand.getOrNull(0)?.let {

                PokerCard(
                    card = it
                )
            }

            player.hand.getOrNull(1)?.let {

                PokerCard(
                    card = it
                )
            }
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "CHIPS ${player.chips}",
                color = White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            if (player.currentBet > 0) {

                Text(
                    text = "BET ${player.currentBet}",
                    color = Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ============================================================
   RESULT BOX
   ============================================================ */

@Composable
private fun ResultBox(
    text: String
) {

    if (text.isBlank()) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                Color.Black.copy(alpha = 0.55f)
            )
            .border(
                1.dp,
                Gold.copy(alpha = 0.65f),
                RoundedCornerShape(12.dp)
            )
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = text,
            color = White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/* ============================================================
   ACTION PANEL
   ============================================================ */

@Composable
private fun ActionPanel(
    game: PokerGame,

    onFold: () -> Unit,
    onCheck: () -> Unit,
    onCall: () -> Unit,
    onRaise: () -> Unit,
    onAllIn: () -> Unit
) {

    val humanTurn =
        game.currentPlayerIndex == 0

    Column(
        modifier = Modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            ActionButton(
                title = "FOLD",
                modifier = Modifier.weight(1f),
                background = FoldRed,
                enabled = humanTurn,
                onClick = onFold
            )

            if (game.canCheck(0)) {

                ActionButton(
                    title = "CHECK",
                    modifier = Modifier.weight(1f),
                    background = CallGreen,
                    enabled = humanTurn,
                    onClick = onCheck
                )

            } else {

                ActionButton(
                    title = "CALL",
                    modifier = Modifier.weight(1f),
                    background = CallGreen,
                    enabled = humanTurn,
                    onClick = onCall
                )
            }
        }

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            ActionButton(
                title = "RAISE +40",
                modifier = Modifier.weight(1f),
                background = RaisePurple,
                enabled = humanTurn,
                onClick = onRaise
            )

            ActionButton(
                title = "ALL IN",
                modifier = Modifier.weight(1f),
                background = AllInYellow,
                contentColor = Color.Black,
                enabled = humanTurn,
                onClick = onAllIn
            )
        }
    }
}

/* ============================================================
   ACTION BUTTON
   ============================================================ */

@Composable
private fun ActionButton(
    title: String,
    modifier: Modifier,
    background: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    contentColor: Color = White
) {

    Button(
        onClick = onClick,

        modifier = modifier
            .height(46.dp),

        enabled = enabled,

        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = contentColor,
            disabledContainerColor =
                Color.Gray.copy(alpha = 0.35f),
            disabledContentColor =
                White.copy(alpha = 0.4f)
        ),

        shape = RoundedCornerShape(12.dp)
    ) {

        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/* ============================================================
   END GAME PANEL
   ============================================================ */

@Composable
private fun EndGamePanel(
    onNewHand: () -> Unit,
    onNewGame: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = onNewHand,

            modifier = Modifier
                .weight(1f)
                .height(50.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = Color.Black
            ),

            shape = RoundedCornerShape(14.dp)
        ) {

            Text(
                text = "NEW HAND",
                fontWeight = FontWeight.ExtraBold
            )
        }

        OutlinedButton(
            onClick = onNewGame,

            modifier = Modifier
                .weight(1f)
                .height(50.dp),

            border = BorderStroke(
                1.dp,
                Gold
            ),

            shape = RoundedCornerShape(14.dp)
        ) {

            Text(
                text = "NEW GAME",
                color = White,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}