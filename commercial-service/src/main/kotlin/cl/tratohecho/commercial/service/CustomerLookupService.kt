package cl.tratohecho.commercial.service

import cl.tratohecho.commercial.web.CustomerSnapshot
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.util.UUID

@Service
class CustomerLookupService(private val customerRestClient: RestClient) {
    fun verify(customerId: UUID, authorization: String) {
        try {
            val customer = customerRestClient.get()
                .uri("/api/v1/clientes/{id}", customerId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(CustomerSnapshot::class.java)
                ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo validar el cliente")
            if (customer.status != "ACTIVE") {
                throw ResponseStatusException(HttpStatus.CONFLICT, "No se puede cotizar a un cliente inactivo")
            }
        } catch (ex: HttpClientErrorException.NotFound) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no existe para la empresa autenticada")
        } catch (ex: HttpClientErrorException) {
            throw ResponseStatusException(ex.statusCode, "El servicio de clientes rechazó la validación")
        } catch (ex: ResourceAccessException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de clientes no está disponible")
        }
    }
}
