package pl.wsei.pam.lab03

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.CycleInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.wsei.pam.lab01.R
import java.util.Random

class Lab03Activity : AppCompatActivity() {
    private lateinit var mBoard: GridLayout
    private lateinit var mBoardModel: MemoryBoardView
    
    private var completionPlayer: MediaPlayer? = null
    private var negativePlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lab03)

        mBoard = findViewById(R.id.mBoard)
        val size = intent.getIntArrayExtra("size") ?: intArrayOf(3, 3)
        val rows = size[0]
        val cols = size[1]

        mBoard.rowCount = rows
        mBoard.columnCount = cols

        if (savedInstanceState != null) {
            val state = savedInstanceState.getIntArray("state")
            val icons = savedInstanceState.getIntArray("icons")?.toList()
            mBoardModel = MemoryBoardView(mBoard, cols, rows, icons)
            if (state != null) {
                mBoardModel.setState(state)
            }
        } else {
            mBoardModel = MemoryBoardView(mBoard, cols, rows)
        }

        mBoardModel.setOnGameChangeListener { e ->
            runOnUiThread {
                when (e.state) {
                    GameStates.Matching -> {
                        e.tiles.forEach { it.revealed = true }
                    }
                    GameStates.Match -> {
                        completionPlayer?.start()
                        e.tiles.forEach { tile ->
                            tile.revealed = true
                            animatePairedButton(tile.button) {
                                tile.removeOnClickListener()
                            }
                        }
                    }
                    GameStates.NoMatch -> {
                        negativePlayer?.start()
                        e.tiles.forEach { it.revealed = true }
                        
                        val firstTile = e.tiles[0]
                        val secondTile = e.tiles[1]
                        
                        // Małe opóźnienie przed animacją powrotu, żeby gracz widział co odkrył
                        mBoard.postDelayed({
                            animateWrongPair(firstTile.button) {
                                firstTile.revealed = false
                            }
                            animateWrongPair(secondTile.button) {
                                secondTile.revealed = false
                            }
                        }, 500)
                    }
                    GameStates.Finished -> {
                        completionPlayer?.start()
                        e.tiles.forEach { tile ->
                            tile.revealed = true
                            animatePairedButton(tile.button) {
                                tile.removeOnClickListener()
                            }
                        }
                        Toast.makeText(this, "Game finished", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun animatePairedButton(button: ImageButton, action: Runnable) {
        val set = AnimatorSet()
        val random = Random()
        button.pivotX = random.nextFloat() * 200f
        button.pivotY = random.nextFloat() * 200f

        val rotation = ObjectAnimator.ofFloat(button, "rotation", 1080f)
        val scallingX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 4f)
        val scallingY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 4f)
        val fade = ObjectAnimator.ofFloat(button, "alpha", 1f, 0f)
        
        set.duration = 2000
        set.interpolator = DecelerateInterpolator()
        set.playTogether(rotation, scallingX, scallingY, fade)
        set.addListener(object : AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}
            override fun onAnimationEnd(animator: Animator) {
                button.scaleX = 1f
                button.scaleY = 1f
                button.alpha = 0.0f
                action.run()
            }
            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
        })
        set.start()
    }

    private fun animateWrongPair(button: ImageButton, action: Runnable) {
        val rotation = ObjectAnimator.ofFloat(button, "rotation", 0f, 10f, -10f, 10f, -10f, 0f)
        rotation.duration = 500
        rotation.interpolator = CycleInterpolator(1f)
        
        rotation.addListener(object : AnimatorListener {
            override fun onAnimationStart(p0: Animator) {}
            override fun onAnimationEnd(p0: Animator) {
                action.run()
            }
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationRepeat(p0: Animator) {}
        })
        rotation.start()
    }

    override fun onResume() {
        super.onResume()
        completionPlayer = MediaPlayer.create(applicationContext, R.raw.completion)
        negativePlayer = MediaPlayer.create(applicationContext, R.raw.negative_guitar)
    }

    override fun onPause() {
        super.onPause()
        completionPlayer?.release()
        negativePlayer?.release()
        completionPlayer = null
        negativePlayer = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("state", mBoardModel.getState())
        outState.putIntArray("icons", mBoardModel.getIconsState().toIntArray())
    }
}
