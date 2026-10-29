package udb.edu.sv.dsm.desafio03.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import udb.edu.sv.dsm.desafio03.databinding.ItemCityBinding
import udb.edu.sv.dsm.desafio03.databinding.ItemCountryBinding
import udb.edu.sv.dsm.desafio03.databinding.ItemRegionBinding
import udb.edu.sv.dsm.desafio03.model.CityWeather
import udb.edu.sv.dsm.desafio03.model.Country

/*
 * Vista (la V de MVC).
 * Los adaptadores solo dibujan los datos que ya les llegan listos:
 * no hacen llamadas de red ni tienen lógica de negocio.
 */

// Lista de regiones de la pantalla inicial.
class RegionAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<RegionAdapter.VH>() {

    class VH(val b: ItemRegionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRegionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val region = items[position]
        holder.b.txtRegion.text = region
        holder.b.root.setOnClickListener { onClick(region) }
    }
}

// Lista de países de una región: nombre, bandera y capital.
class CountryAdapter(
    private val items: List<Country>,
    private val onClick: (Country) -> Unit
) : RecyclerView.Adapter<CountryAdapter.VH>() {

    class VH(val b: ItemCountryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCountryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.b.txtName.text = c.names?.common ?: "—"
        holder.b.txtCapital.text = "Capital: ${c.capitals?.firstOrNull()?.name ?: "—"}"
        // Coil descarga y muestra la bandera a partir de su URL.
        holder.b.imgFlag.load(c.flag?.urlPng) { crossfade(true) }
        holder.b.root.setOnClickListener { onClick(c) }
    }
}

// Lista de ciudades del país con su clima. La capital se destaca con una estrella.
class CityAdapter(
    private val items: List<CityWeather>,
    private val onClick: (CityWeather) -> Unit
) : RecyclerView.Adapter<CityAdapter.VH>() {

    class VH(val b: ItemCityBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cw = items[position]

        // La capital lleva estrella y la palabra "(capital)".
        holder.b.txtCityName.text =
            if (cw.city.isCapital) "★ ${cw.city.name} (capital)" else cw.city.name

        // Población y coordenadas (GeoNames) seguidas del clima (WeatherAPI).
        holder.b.txtCityWeather.text =
            "Población: ${"%,d".format(cw.city.population)} · " +
                    "${"%.2f".format(cw.city.lat)}, ${"%.2f".format(cw.city.lng)}\n" +
                    weatherText(cw)

        // El ícono del clima llega sin "https:", por eso se le agrega.
        val icon = cw.weather?.condition?.icon
        if (icon != null) holder.b.imgWeather.load("https:$icon")
        else holder.b.imgWeather.setImageDrawable(null)

        // Al tocar una ciudad, CountryDetailActivity centra el mapa en ella.
        holder.b.root.setOnClickListener { onClick(cw) }
    }
}

/**
 * Arma el texto del clima: temperatura en °C/°F, condición, viento, humedad
 * y última actualización. Se usa en la lista y en el globo de cada marcador del mapa.
 * Si no hay clima, muestra un aviso solo para esa ciudad.
 */
fun weatherText(cw: CityWeather): String {
    val w = cw.weather ?: return "Clima no disponible: ${cw.weatherError ?: "sin datos"}"
    return buildString {
        append("${w.tempC}°C / ${w.tempF}°F · ${w.condition?.text ?: ""}\n")
        append("Viento: ${w.windKph} kph / ${w.windMph} mph · Humedad: ${w.humidity}%\n")
        append("Actualizado: ${w.lastUpdated ?: "—"}")
    }
}