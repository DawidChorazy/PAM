package pl.wsei.pam.lab03

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import pl.wsei.pam.lab01.R
import java.util.Stack

data class Tile(val button: ImageButton, val tileResource: Int, val deckResource: Int) {
    init {
        button.setImageResource(deckResource)
    }
    private var _revealed: Boolean = false
    var revealed: Boolean
        get() = _revealed
        set(value) {
            _revealed = value
            button.setImageResource(if (_revealed) tileResource else deckResource)
        }

    fun removeOnClickListener() {
        button.setOnClickListener(null)
    }
}

enum class GameStates {
    Matching, Match, NoMatch, Finished
}

class MemoryGameLogic(val maxMatches: Int) {
    private var valueFunctions: MutableList<() -> Int> = mutableListOf()
    var matches: Int = 0

    fun process(value: () -> Int): GameStates {
        if (valueFunctions.size < 1) {
            valueFunctions.add(value)
            return GameStates.Matching
        }
        valueFunctions.add(value)
        val result = valueFunctions[0]() == valueFunctions[1]()
        matches += if (result) 1 else 0
        valueFunctions.clear()
        return when (result) {
            true -> if (matches >= maxMatches) GameStates.Finished else GameStates.Match
            false -> GameStates.NoMatch
        }
    }
}

data class MemoryGameEvent(
    val tiles: List<Tile>,
    val state: GameStates
)

class MemoryBoardView(
    private val gridLayout: GridLayout,
    private val cols: Int,
    private val rows: Int,
    savedIcons: List<Int>? = null
) {
    private val tiles: MutableMap<String, Tile> = mutableMapOf()
    private val icons: List<Int> = listOf(
        R.drawable.baseline_rocket_launch_24,
        R.drawable.baseline_airline_seat_individual_suite_24,
        R.drawable.ic_launcher_foreground,
        R.drawable.gradient_1
    )
    // Używamy ic_launcher_background jako tyłu karty, ale dodamy tło dla widoczności
    private val deckResource: Int = R.drawable.ic_launcher_background
    private var onGameChangeStateListener: (MemoryGameEvent) -> Unit = { }
    private val matchedPair: Stack<Tile> = Stack()
    private val logic: MemoryGameLogic = MemoryGameLogic((cols * rows) / 2)
    private val currentIcons: MutableList<Int> = mutableListOf()

    init {
        val totalTiles = cols * rows
        val shuffledIcons: MutableList<Int> = if (savedIcons != null && savedIcons.size == totalTiles) {
            savedIcons.toMutableList()
        } else {
            val neededPairs = totalTiles / 2
            val list = mutableListOf<Int>()
            var available = icons
            while (available.size < neededPairs) available = available + icons
            
            val pairIcons = available.take(neededPairs)
            list.addAll(pairIcons)
            list.addAll(pairIcons)
            if (totalTiles % 2 != 0) list.add(icons[0])
            list.shuffle()
            list
        }
        currentIcons.addAll(shuffledIcons)

        val tempIcons = shuffledIcons.toMutableList()
        gridLayout.removeAllViews()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val btn = ImageButton(gridLayout.context).also {
                    it.tag = "${row}x${col}"
                    // Usuwamy domyślne tło i padding przycisku
                    it.setBackgroundColor(Color.LTGRAY) // Jasnoszare tło dla kafelka
                    it.setPadding(10, 10, 10, 10) // Mały odstęp wewnętrzny dla ikony
                    it.scaleType = ImageView.ScaleType.FIT_CENTER // Skalowanie ikony
                    
                    val lp = GridLayout.LayoutParams()
                    lp.width = 0
                    lp.height = 0
                    lp.setGravity(Gravity.FILL) // Wypełnienie komórki
                    // Dodajemy marginesy zewnętrzne między kafelkami
                    lp.setMargins(8, 8, 8, 8)
                    lp.columnSpec = GridLayout.spec(col, 1, 1f)
                    lp.rowSpec = GridLayout.spec(row, 1, 1f)
                    it.layoutParams = lp
                    gridLayout.addView(it)
                }
                if (tempIcons.isNotEmpty()) {
                    addTile(btn, tempIcons.removeAt(0))
                }
            }
        }
    }

    private fun onClickTile(v: View) {
        val tile = tiles[v.tag]
        if (tile == null || tile.revealed) return
        
        matchedPair.push(tile)
        val matchResult = logic.process { tile.tileResource }
        onGameChangeStateListener(MemoryGameEvent(matchedPair.toList(), matchResult))
        if (matchResult != GameStates.Matching) {
            matchedPair.clear()
        }
    }

    fun setOnGameChangeListener(listener: (event: MemoryGameEvent) -> Unit) {
        onGameChangeStateListener = listener
    }

    private fun addTile(button: ImageButton, resourceImage: Int) {
        button.setOnClickListener(::onClickTile)
        val tile = Tile(button, resourceImage, deckResource)
        tiles[button.tag.toString()] = tile
    }

    fun getState(): IntArray {
        val state = IntArray(rows * cols)
        var i = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                state[i++] = if (tiles["${row}x${col}"]?.revealed == true) 1 else 0
            }
        }
        return state
    }

    fun setState(state: IntArray) {
        var i = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (i < state.size) {
                    tiles["${row}x${col}"]?.revealed = (state[i++] == 1)
                }
            }
        }
    }

    fun getIconsState(): List<Int> = currentIcons
    fun getMatches(): Int = logic.matches
    fun setMatches(count: Int) { logic.matches = count }
}
