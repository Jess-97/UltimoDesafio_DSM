package udb.edu.sv.dsm.desafio03.view

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import udb.edu.sv.dsm.desafio03.controller.ApiResult
import udb.edu.sv.dsm.desafio03.controller.CountryController
import udb.edu.sv.dsm.desafio03.databinding.ActivityCountryDetailBinding
import udb.edu.sv.dsm.desafio03.model.CityWeather
import udb.edu.sv.dsm.desafio03.model.Country
import udb.edu.sv.dsm.desafio03.model.toReadableText
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * Pantalla de detalle del país. Junta las tres APIs:
 *  1) REST Countries: datos del país (loadCountry y showCountry)
 *  2) GeoNames: hasta 10 ciudades principales (loadCities)
 *  3) WeatherAPI: el clima de cada ciudad (llega dentro de CityWeather)
 * También muestra el mapa con un marcador por ciudad.
 *
 * Los datos del país y las ciudades se piden en dos llamadas independientes:
 * si una falla, la otra se muestra igual.
 */
class CountryDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_NAME = "name"
    }

    private lateinit var b: ActivityCountryDetailBinding
    private val controller = CountryController()
    // Guarda cada marcador por nombre de ciudad para poder abrir su globo al seleccionarla.
    private val markers = mutableMapOf<String, Marker>()
    private lateinit var code: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid necesita configuración ANTES de inflar el MapView
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        b = ActivityCountryDetailBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.ajustarBarrasDelSistema()

        code = intent.getStringExtra(EXTRA_CODE) ?: ""
        title = intent.getStringExtra(EXTRA_NAME) ?: "Detalle"
        // Flecha de regreso en la barra superior.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupMap()
        b.rvCities.layoutManager = LinearLayoutManager(this)

        // Validación: sin código de país no se llama a ninguna API.
        if (code.isBlank()) {
            b.progress.visibility = View.GONE
            showCountryError("Código de país inválido.")
            return
        }
        loadCountry()
        loadCities()
    }

    // Configura el mapa de osmdroid (usa los mapas de OpenStreetMap, no requiere clave).
    private fun setupMap() {
        b.map.setTileSource(TileSourceFactory.MAPNIK)
        b.map.setMultiTouchControls(true) // Zoom con dos dedos y desplazamiento.
        b.map.controller.setZoom(4.0)
        // Permite mover el mapa dentro del ScrollView
        b.map.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    // ---------- País (REST Countries) ----------
    private fun loadCountry() {
        b.progress.visibility = View.VISIBLE // Indicador de carga.
        lifecycleScope.launch {
            when (val r = controller.getCountry(code)) {
                is ApiResult.Success -> showCountry(r.data)
                is ApiResult.Failure -> showCountryError(r.message)
            }
            b.progress.visibility = View.GONE
        }
    }

    // Muestra todos los datos del país: nombres, capital, región, subregión, población,
    // bandera, códigos ISO, monedas, idiomas y coordenadas.
    private fun showCountry(c: Country) {
        b.imgFlag.load(c.flag?.urlPng) { crossfade(true) }

        val lat = c.coordinates?.lat
        val lng = c.coordinates?.lng
        val coords = if (lat != null && lng != null) "$lat, $lng" else "—"
        val capitals = c.capitals?.mapNotNull { it.name }?.joinToString().orEmpty()
            .ifBlank { "—" }

        b.txtInfo.text = """
            Nombre común: ${c.names?.common ?: "—"}
            Nombre oficial: ${c.names?.official ?: "—"}
            Capital: $capitals
            Región: ${c.region ?: "—"}
            Subregión: ${c.subregion ?: "—"}
            Población: ${c.population?.let { "%,.0f".format(it) } ?: "—"}
            Códigos ISO: ${c.codes?.alpha2 ?: "—"} / ${c.codes?.alpha3 ?: "—"}
            Monedas: ${c.currencies.toReadableText()}
            Idiomas: ${c.languages.toReadableText()}
            Coordenadas: $coords
        """.trimIndent()

        // Solo centra en el país si las ciudades aún no llegaron
        if (lat != null && lng != null && markers.isEmpty()) {
            b.map.controller.setCenter(GeoPoint(lat, lng))
        }
    }

    // Un error en los datos del país no bloquea la lista de ciudades.
    private fun showCountryError(msg: String) {
        b.txtError.text = msg
        b.txtError.visibility = View.VISIBLE
    }

    // ---------- Ciudades (GeoNames) + clima (WeatherAPI) ----------
    private fun loadCities() {
        b.progressCities.visibility = View.VISIBLE // Indicador de carga propio de las ciudades.
        b.txtCitiesError.visibility = View.GONE
        lifecycleScope.launch {
            when (val r = controller.getCitiesWithWeather(code)) {
                is ApiResult.Success -> showCities(r.data)
                // Cubre: país sin ciudades, usuario de GeoNames inválido, sin conexión, timeout...
                is ApiResult.Failure -> {
                    b.txtCitiesError.text = r.message
                    b.txtCitiesError.visibility = View.VISIBLE
                }
            }
            b.progressCities.visibility = View.GONE
        }
    }

    // Llena la lista de ciudades y pone un marcador en el mapa por cada una.
    // El marcador muestra el nombre de la ciudad y, al tocarlo, su clima.
    private fun showCities(list: List<CityWeather>) {
        b.rvCities.adapter = CityAdapter(list) { focusCity(it) }

        markers.clear()
        b.map.overlays.clear()
        list.forEach { cw ->
            val m = Marker(b.map).apply {
                position = GeoPoint(cw.city.lat, cw.city.lng)
                title = if (cw.city.isCapital) "★ ${cw.city.name}" else cw.city.name
                snippet = weatherText(cw) // El clima aparece al tocar el marcador.
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            markers[cw.city.name] = m
            b.map.overlays.add(m)
        }

        // Centrar en la capital (o la primera ciudad)
        val first = list.firstOrNull { it.city.isCapital } ?: list.first()
        b.map.controller.setZoom(6.0)
        b.map.controller.setCenter(GeoPoint(first.city.lat, first.city.lng))
        b.map.invalidate()
    }

    // Se ejecuta al tocar una ciudad de la lista: sube la pantalla hasta el mapa,
    // mueve el mapa hacia esa ciudad con zoom y abre el globo de su marcador.
    private fun focusCity(cw: CityWeather) {
        b.scroll.smoothScrollTo(0, b.map.top)
        b.map.controller.animateTo(GeoPoint(cw.city.lat, cw.city.lng), 9.0, 800L)
        markers[cw.city.name]?.showInfoWindow()
    }

    // El mapa se pausa y se reanuda junto con la pantalla.
    override fun onResume() {
        super.onResume()
        b.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        b.map.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}