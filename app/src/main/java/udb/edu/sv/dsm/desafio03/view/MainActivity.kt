package udb.edu.sv.dsm.desafio03.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import udb.edu.sv.dsm.desafio03.controller.CountryController
import udb.edu.sv.dsm.desafio03.databinding.ActivityListBinding

/**
 * Pantalla inicial: lista de regiones del mundo.
 * Pide las regiones al controlador y, al tocar una, abre la lista de sus países.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityListBinding
    private val controller = CountryController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityListBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.ajustarBarrasDelSistema()
        title = "Regiones del mundo"

        // Esta pantalla no consulta ninguna API, así que no necesita carga ni error.
        b.progress.visibility = View.GONE
        b.errorBox.visibility = View.GONE
        b.rvItems.layoutManager = LinearLayoutManager(this)
        b.rvItems.adapter = RegionAdapter(controller.getRegions()) { region ->
            startActivity(
                Intent(this, CountriesActivity::class.java)
                    .putExtra(CountriesActivity.EXTRA_REGION, region)
            )
        }
    }
}