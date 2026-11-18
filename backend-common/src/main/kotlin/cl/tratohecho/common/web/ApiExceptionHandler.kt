package cl.tratohecho.common.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException::class)
    fun handleStatus(ex: ResponseStatusException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val status = HttpStatus.valueOf(ex.statusCode.value())
        val code = when (status) {
            HttpStatus.UNAUTHORIZED -> "UNAUTHORIZED"
            HttpStatus.FORBIDDEN -> "FORBIDDEN"
            HttpStatus.NOT_FOUND -> "NOT_FOUND"
            HttpStatus.CONFLICT -> "CONFLICT"
            HttpStatus.BAD_REQUEST -> "BAD_REQUEST"
            else -> status.name
        }
        return ResponseEntity.status(status).body(ApiError(
            status = status.value(), code = code,
            message = ex.reason ?: status.reasonPhrase, path = request.requestURI
        ))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage ?: "valor inválido"}" }
        return ResponseEntity.badRequest().body(ApiError(
            status = 400, code = "VALIDATION_ERROR", message = "Revisa los datos ingresados",
            path = request.requestURI, details = details
        ))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(request: HttpServletRequest): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(ApiError(
            status = 400, code = "INVALID_JSON", message = "El cuerpo de la solicitud no tiene un formato válido",
            path = request.requestURI
        ))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflict(request: HttpServletRequest): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError(
            status = 409, code = "DATA_CONFLICT", message = "La operación produce un conflicto con datos existentes",
            path = request.requestURI
        ))

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(request: HttpServletRequest): ResponseEntity<ApiError> =
        ResponseEntity.internalServerError().body(ApiError(
            status = 500, code = "INTERNAL_ERROR", message = "Ocurrió un error interno al procesar la solicitud",
            path = request.requestURI
        ))
}
