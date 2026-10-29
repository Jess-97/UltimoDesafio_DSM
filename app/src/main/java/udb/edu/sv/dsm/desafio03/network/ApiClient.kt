package udb.edu.sv.dsm.desafio03.network

import udb.edu.sv.dsm.desafio03.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Configura los tres clientes de Retrofit (uno por API).
 * Las claves salen de BuildConfig, que se llena desde local.properties:
 * nunca están escritas en el código ni se suben al repositorio.
 */
object ApiClient {

    // Tiempos máximos de espera. Si se pasan, Retrofit lanza SocketTimeoutException
    // y ErrorHandler lo convierte en "Tiempo de espera agotado".
    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    // REST Countries se autentica con Bearer token. El interceptor agrega el
    // encabezado Authorization a todas las solicitudes de este cliente.
    private val bearerClient: OkHttpClient = baseClient.newBuilder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.RESTCOUNTRIES_TOKEN}")
                .build()
            chain.proceed(req)
        }
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // REST Countries: usa el cliente con Bearer.
    val countries: RestCountriesService by lazy {
        retrofit("https://api.restcountries.com/", bearerClient)
            .create(RestCountriesService::class.java)
    }

    // GeoNames: se autentica con el parámetro username, no necesita encabezado.
    val geoNames: GeoNamesService by lazy {
        retrofit("https://secure.geonames.org/", baseClient)
            .create(GeoNamesService::class.java)
    }

    // WeatherAPI: se autentica con el parámetro key (API Key).
    val weather: WeatherService by lazy {
        retrofit("https://api.weatherapi.com/v1/", baseClient)
            .create(WeatherService::class.java)
    }
}