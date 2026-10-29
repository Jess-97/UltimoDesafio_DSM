package udb.edu.sv.dsm.desafio03.controller

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Resultado que el controlador entrega a las pantallas:
 * o trae los datos (Success) o trae un mensaje listo para mostrar (Failure).
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}

/**
 * Convierte las excepciones técnicas en mensajes entendibles para el usuario.
 * Casos que cubre:
 *  - Token o API Key inválidos: HTTP 401 / 403
 *  - Límite de solicitudes: HTTP 429 (y los códigos 18 a 20 de GeoNames)
 *  - Tiempo de espera agotado: SocketTimeoutException
 *  - Falta de conexión: UnknownHostException / IOException
 *  - Ubicación no encontrada: HTTP 400 (WeatherAPI)
 *  - País sin ciudades: se valida en CountryController
 */
object ErrorHandler {
    fun toMessage(e: Throwable): String = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "Token o API Key inválido. Revisa tus credenciales."
            403 -> "Acceso denegado o límite mensual de solicitudes alcanzado."
            400 -> "Ubicación no encontrada."
            404 -> "No se encontró la información solicitada."
            429 -> "Límite de solicitudes alcanzado. Intenta más tarde."
            in 500..599 -> "El servidor tuvo un problema (${e.code()})."
            else -> "Error HTTP ${e.code()}."
        }
        is SocketTimeoutException -> "Tiempo de espera agotado. Intenta de nuevo."
        is UnknownHostException -> "Sin conexión a internet."
        is IOException -> "Error de red. Verifica tu conexión."
        else -> e.message ?: "Ocurrió un error inesperado."
    }

    // GeoNames manda sus errores dentro de un JSON con HTTP 200, por eso tiene su propia función.
    fun geoNamesMessage(value: Int?, message: String?): String = when (value) {
        10 -> "Usuario de GeoNames inválido."
        18, 19, 20 -> "Límite de solicitudes de GeoNames alcanzado."
        else -> message ?: "Error en GeoNames."
    }
}