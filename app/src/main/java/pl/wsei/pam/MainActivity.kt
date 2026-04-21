package pl.wsei.pam

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import pl.wsei.pam.lab01.Lab01Activity
import pl.wsei.pam.lab01.R
import pl.wsei.pam.lab02.Lab02Activity
import pl.wsei.pam.lab03.Lab03Activity
import pl.wsei.pam.lab06.Lab06Activity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClickMainBtnRunLab01(v: View) {
        val intent = Intent(this, Lab01Activity::class.java)
        startActivity(intent)
    }

    fun onClickMainBtnRunLab02(v: View) {
        val intent = Intent(this, Lab02Activity::class.java)
        startActivity(intent)
    }

    fun onClickMainBtnRunLab03(v: View) {
        val intent = Intent(this, Lab03Activity::class.java)
        // Przekazujemy domyślny rozmiar dla Lab 03
        intent.putExtra("size", intArrayOf(4, 4))
        startActivity(intent)
    }

    fun onClickMainBtnRunLab06(v: View) {
        val intent = Intent(this, Lab06Activity::class.java)
        startActivity(intent)
    }
}
