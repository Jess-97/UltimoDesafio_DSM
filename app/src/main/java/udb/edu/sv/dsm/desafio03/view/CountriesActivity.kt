package udb.edu.sv.dsm.desafio03.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import udb.edu.sv.dsm.desafio03.controller.ApiResult
import udb.edu.sv.dsm.desafio03.controller.CountryController
import udb.edu.sv.dsm.desafio03.databinding.ActivityListBinding
import kotlinx.coroutines.launch

/**
 * Lista de países de la región elegida (nombre, bandera y capital).
 * Pide los datos al controlador y, al tocar un país, abre su pantalla de detalle.
 */
class CountriesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REGION = "region"
    }

    private lateinit var b: ActivityListBinding
    private val controller = CountryController()
    private lateinit var region: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityListBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.ajustarBarrasDelSistema()

        region = intent.getStringExtra(EXTRA_REGION) ?: ""
        title = "Países · $region"
        // Flecha de regreso en la barra superior.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        b.rvItems.layoutManager = LinearLayoutManager(this)
        // Si hubo un error, este botón vuelve a pedir los datos.
        b.btnRetry.setOnClickListener { load() }
        load()
    }

    private fun load() {
        // Mientras llega la respuesta se muestra el indicador de carga.
        b.progress.visibility = View.VISIBLE
        b.errorBox.visibility = View.GONE
        b.rvItems.visibility = View.GONE

        // lifecycleScope cancela la corrutina automáticamente si se cierra la pantalla.
        lifecycleScope.launch {
            when (val r = controller.getCountriesByRegion(region)) {
                is ApiResult.Success -> {
                    b.rvItems.adapter = CountryAdapter(r.data) { country ->
                        // Se envía el código ISO de 2 letras para pedir el detalle y las ciudades.
                        startActivity(
                            Intent(this@CountriesActivity, CountryDetailActivity::class.java)
                                .putExtra(CountryDetailActivity.EXTRA_CODE, country.codes?.alpha2)
                                .putExtra(CountryDetailActivity.EXTRA_NAME, country.names?.common)
                        )
                    }
                    b.rvItems.visibility = View.VISIBLE
                }
                // Mensaje de error amigable con opción de reintentar.
                is ApiResult.Failure -> {
                    b.txtError.text = r.message
                    b.errorBox.visibility = View.VISIBLE
                }
            }
            b.progress.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}