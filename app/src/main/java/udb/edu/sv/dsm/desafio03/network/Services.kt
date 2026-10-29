package udb.edu.sv.dsm.desafio03.network

import udb.edu.sv.dsm.desafio03.model.CountriesResponse
import udb.edu.sv.dsm.desafio03.model.GeoResponse
import udb.edu.sv.dsm.desafio03.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints de REST Countries.
 * Son funciones "suspend" para ejecutarse con corrutinas sin bloquear la pantalla.
 */
interface RestCountriesService {
    // Lista de todos los países (la app no la usa en pantalla, pero está implementada).
    // La API devuelve 25 por defecto; 100 es el máximo del plan gratuito.
    @GET("countries/v5")
    suspend fun getAll(
        @Query("limit") limit: Int = 100,
        @Query("response_fields_omit") omit: String = "names.translations"
    ): CountriesResponse

    // Países de una región. Solo pide los 4 campos que muestra la lista
    // (nombre, código, capital y bandera) para que la respuesta sea ligera.
    @GET("countries/v5")
    suspend fun getByRegion(
        @Query("region") region: String,
        @Query("limit") limit: Int = 100,
        @Query("response_fields") fields: String =
            "names.common,codes.alpha_2,capitals,flag.url_png"
    ): CountriesResponse

    // Detalle completo de un país por su código ISO de 2 letras.
    // Se omiten las traducciones porque la app no las usa y pesan mucho.
    @GET("countries/v5/codes.alpha_2/{code}")
    suspend fun getByCode(
        @Path("code") code: String,
        @Query("response_fields_omit") omit: String = "names.translations"
    ): CountriesResponse
}

/**
 * Endpoint de GeoNames: busca ciudades de un país.
 * Los valores por defecto piden poblaciones de más de 15.000 habitantes,
 * ordenadas por población y limitadas a 10 resultados.
 */
interface GeoNamesService {
    @GET("searchJSON")
    suspend fun searchCities(
        @Query("country") iso2: String,
        @Query("username") username: String,
        @Query("featureClass") featureClass: String = "P",
        @Query("cities") cities: String = "cities15000",
        @Query("maxRows") maxRows: Int = 10,
        @Query("orderby") orderBy: String = "population"
    ): GeoResponse
}

/**
 * Endpoint de WeatherAPI: clima actual de un punto (latitud,longitud).
 */
interface WeatherService {
    @GET("current.json")
    suspend fun current(
        @Query("key") key: String,
        @Query("q") query: String,
        @Query("lang") lang: String = "es" // Condición del clima en español.
    ): WeatherResponse
}