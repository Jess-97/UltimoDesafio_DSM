package udb.edu.sv.dsm.desafio03.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/*
 * Modelo (la M de MVC).
 * Solo clases de datos: no conocen las pantallas ni hacen llamadas de red.
 * Gson convierte el JSON de cada API en estas clases.
 */

// ---------- REST Countries v5 ----------
// La API envuelve la respuesta en data -> objects.
data class CountriesResponse(val data: CountriesData?)

data class CountriesData(val objects: List<Country>?)

// Datos de un país: nombres, capital, región, subregión, población, bandera,
// códigos ISO, monedas, idiomas y coordenadas.
data class Country(
    val names: Names?,
    val capitals: List<Capital>?,
    val region: String?,
    val subregion: String?,
    val population: Double?,
    val flag: Flag?,
    val codes: Codes?,
    val currencies: JsonElement?, // Estructura flexible, se convierte con toReadableText().
    val languages: JsonElement?,
    val coordinates: Coordinates?
)

data class Names(val common: String?, val official: String?)
data class Capital(val name: String?)

data class Flag(
    @SerializedName("url_png") val urlPng: String?,
    @SerializedName("url_svg") val urlSvg: String?
)

data class Codes(
    @SerializedName("alpha_2") val alpha2: String?,
    @SerializedName("alpha_3") val alpha3: String?
)

data class Coordinates(val lat: Double?, val lng: Double?)

/** Convierte monedas e idiomas (que vienen con estructura variable) en texto legible. */
fun JsonElement?.toReadableText(): String {
    if (this == null || isJsonNull) return "—"
    return when {
        isJsonPrimitive -> asString
        isJsonArray -> asJsonArray.joinToString(", ") { it.toReadableText() }
        else -> {
            val o = asJsonObject
            fun str(k: String) =
                if (o.has(k) && o[k].isJsonPrimitive) o[k].asString else null

            val name = str("name") ?: str("english_name")
            if (name != null) {
                val extra = listOfNotNull(str("code"), str("symbol")).joinToString(" ")
                val native = str("native_name")?.takeIf { it != name }
                buildString {
                    append(name)
                    if (native != null) append(" / $native")
                    if (extra.isNotBlank()) append(" ($extra)")
                }
            } else {
                o.entrySet().joinToString(", ") { it.value.toReadableText() }
            }
        }
    }
}

// ---------- GeoNames ----------
// Respuesta cruda del endpoint searchJSON.
data class GeoResponse(
    val geonames: List<GeoCity>?,
    val status: GeoStatus? // Solo viene cuando hay un error (usuario inválido, límite...).
)

data class GeoStatus(val message: String?, val value: Int?)

// Datos de cada ciudad: nombre, población, latitud, longitud y tipo (fcode).
data class GeoCity(
    val name: String?,
    val population: Long?,
    val lat: String?,
    val lng: String?,
    val fcode: String?
)

// Modelo propio de la app, ya limpio: coordenadas numéricas y capital como booleano.
data class City(
    val name: String,
    val population: Long,
    val lat: Double,
    val lng: Double,
    val isCapital: Boolean
)

// ---------- WeatherAPI ----------
data class WeatherResponse(val current: CurrentWeather?)

// Clima actual: temperatura en °C y °F, condición, viento en kph y mph,
// humedad y fecha de la última actualización.
data class CurrentWeather(
    @SerializedName("temp_c") val tempC: Double?,
    @SerializedName("temp_f") val tempF: Double?,
    val condition: Condition?,
    @SerializedName("wind_kph") val windKph: Double?,
    @SerializedName("wind_mph") val windMph: Double?,
    val humidity: Int?,
    @SerializedName("last_updated") val lastUpdated: String?
)

data class Condition(val text: String?, val icon: String?)

// Une cada ciudad con su clima.
// El clima es nullable a propósito: si falla, la ciudad se muestra igual.
data class CityWeather(
    val city: City,
    val weather: CurrentWeather?,
    val weatherError: String? = null
)