package udb.edu.sv.dsm.desafio03.controller

import udb.edu.sv.dsm.desafio03.BuildConfig
import udb.edu.sv.dsm.desafio03.model.City
import udb.edu.sv.dsm.desafio03.model.CityWeather
import udb.edu.sv.dsm.desafio03.model.Country
import udb.edu.sv.dsm.desafio03.network.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

/**
 * Controlador (la C de MVC).
 * Es el intermediario entre las pantallas y la red: las Activities nunca llaman
 * a Retrofit directamente, siempre pasan por esta clase. Aquí se ordenan los países,
 * se arman las ciudades y se une cada ciudad con su clima.
 */
class CountryController {

    // Regiones que se muestran en la pantalla inicial.
    fun getRegions(): List<String> =
        listOf("Africa", "Americas", "Asia", "Europe", "Oceania")

    // Trae los países de una región y los ordena por nombre.
    // Cualquier error se convierte en un Failure con mensaje amigable,
    // así la pantalla nunca recibe una excepción cruda.
    suspend fun getCountriesByRegion(region: String): ApiResult<List<Country>> = try {
        val list = ApiClient.countries.getByRegion(region)
            .data?.objects.orEmpty()
            .sortedBy { it.names?.common ?: "" }
        // Si la región no trae países, se avisa en vez de mostrar una lista vacía.
        if (list.isEmpty()) ApiResult.Failure("No hay países para esta región.")
        else ApiResult.Success(list)
    } catch (e: CancellationException) {
        throw e // Se relanza para no romper la cancelación de las corrutinas.
    } catch (e: Exception) {
        ApiResult.Failure(ErrorHandler.toMessage(e))
    }

    // Trae todos los datos de un país por su código ISO de 2 letras (para el detalle).
    suspend fun getCountry(code: String): ApiResult<Country> = try {
        val c = ApiClient.countries.getByCode(code).data?.objects?.firstOrNull()
        if (c == null) ApiResult.Failure("País no encontrado.") else ApiResult.Success(c)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResult.Failure(ErrorHandler.toMessage(e))
    }

    /**
     * Pide hasta 10 ciudades del país a GeoNames y, para cada una, consulta su clima
     * en WeatherAPI usando las coordenadas de esa ciudad. Devuelve ciudad + clima juntos.
     *
     * Las consultas de clima se hacen en paralelo (async + awaitAll) para que cargue rápido.
     * Cada consulta tiene su propio try/catch: si el clima de una ciudad falla, solo esa
     * ciudad muestra "Clima no disponible" y las demás siguen normales.
     */
    suspend fun getCitiesWithWeather(iso2: String): ApiResult<List<CityWeather>> = coroutineScope {
        try {
            // El usuario de GeoNames sale de local.properties, no está escrito en el código.
            val resp = ApiClient.geoNames.searchCities(
                iso2 = iso2,
                username = BuildConfig.GEONAMES_USER
            )

            // GeoNames reporta sus errores con HTTP 200 dentro del campo "status"
            // (usuario inválido, límite de créditos...), así que hay que revisarlo aparte.
            resp.status?.let {
                return@coroutineScope ApiResult.Failure(
                    ErrorHandler.geoNamesMessage(it.value, it.message)
                )
            }

            val cities = resp.geonames.orEmpty().mapNotNull { g ->
                // GeoNames envía latitud y longitud como texto, se convierten a número.
                val lat = g.lat?.toDoubleOrNull()
                val lng = g.lng?.toDoubleOrNull()
                // El código "PPLC" en GeoNames significa capital del país.
                if (g.name == null || lat == null || lng == null) null
                else City(g.name, g.population ?: 0L, lat, lng, g.fcode == "PPLC")
            }.sortedByDescending { it.isCapital } // La capital va primero.
            // Limitación: si la capital no está entre las 10 ciudades más pobladas
            // que devuelve GeoNames, no aparece destacada.

            // País sin ciudades disponibles.
            if (cities.isEmpty()) {
                return@coroutineScope ApiResult.Failure("Este país no tiene ciudades disponibles.")
            }

            val result = cities.map { city ->
                async {
                    try {
                        // La API Key del clima también sale de local.properties.
                        val w = ApiClient.weather.current(
                            key = BuildConfig.WEATHERAPI_KEY,
                            query = "${city.lat},${city.lng}" // formato: latitud,longitud
                        )
                        CityWeather(city, w.current)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // El error se guarda en esa ciudad y no afecta a las demás.
                        CityWeather(city, null, ErrorHandler.toMessage(e))
                    }
                }
            }.awaitAll()

            ApiResult.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Failure(ErrorHandler.toMessage(e))
        }
    }
}