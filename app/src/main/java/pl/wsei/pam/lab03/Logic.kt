package pl.wsei.pam.lab03

import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
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
            if (_revealed) {
                button.setImageResource(tileResource)
            } else {
                button.setImageResource(deckResource)
            }
        }

    fun removeOnClickListener() {
        button.setOnClickListener(null)
    }
}

enum class GameStates {
    Matching, Match, NoMatch, Finished
}

class MemoryGameLogic(private val maxMatches: Int) {
    private var valueFunctions: MutableList<() -> Int> = mutableListOf()
    private var matches: Int = 0

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
            true -> if (matches == maxMatches) GameStates.Finished else GameStates.Match
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
        R.drawable.ic_launcher_background
    )
    private val deckResource: Int = R.drawable.ic_launcher_background
    private var onGameChangeStateListener: (MemoryGameEvent) -> Unit = { }
    private val matchedPair: Stack<Tile> = Stack()
    private val logic: MemoryGameLogic = MemoryGameLogic(cols * rows / 2)
    private val currentIcons: MutableList<Int> = mutableListOf()

    init {
        val shuffledIcons: MutableList<Int> = if (savedIcons != null) {
            savedIcons.toMutableList()
        } else {
            mutableListOf<Int>().also {
                val neededIconsCount = (cols * rows) / 2
                val subIcons = icons.take(neededIconsCount)
                it.addAll(subIcons)
                it.addAll(subIcons)
                it.shuffle()
            }
        }
        currentIcons.addAll(shuffledIcons)

        val tempIcons = shuffledIcons.toMutableList()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val btn = ImageButton(gridLayout.context).also {
                    it.tag = "${row}x${col}"
                    val layoutParams = GridLayout.LayoutParams()
                    layoutParams.width = 0
                    layoutParams.height = 0
                    layoutParams.setGravity(Gravity.CENTER)
                    layoutParams.columnSpec = GridLayout.spec(col, 1, 1f)
                    layoutParams.rowSpec = GridLayout.spec(row, 1, 1f)
                    it.layoutParams = layoutParams
                    gridLayout.addView(it)
                }
                val icon = tempIcons.removeAt(0)
                addTile(btn, icon)
            }
        }
    }

    private fun onClickTile(v: View) {
        val tile = tiles[v.tag]
        if (tile == null || tile.revealed) return
        
        matchedPair.push(tile)
        val matchResult = logic.process {
            tile.tileResource
        }
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
                val tile = tiles["${row}x${col}"]
                state[i++] = if (tile?.revealed == true) 1 else 0
            }
        }
        return state
    }

    fun setState(state: IntArray) {
        var i = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val revealed = state[i++] == 1
                val tile = tiles["${row}x${col}"]
                tile?.revealed = revealed
            }
        }
    }

    fun getIconsState(): List<Int> = currentIcons
}
