package udb.edu.sv.dsm.desafio03.view

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Agrega al contenido el espacio de la barra de estado, la barra de navegación y la cámara.
 * Android 15 en adelante dibuja las apps de borde a borde, y sin este ajuste
 * las banderas y los textos quedaban tapados por arriba.
 */
fun View.ajustarBarrasDelSistema() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val barras = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.updatePadding(barras.left, barras.top, barras.right, barras.bottom)
        insets
    }
}