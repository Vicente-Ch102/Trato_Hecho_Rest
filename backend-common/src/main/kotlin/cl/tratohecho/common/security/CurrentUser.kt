package cl.tratohecho.common.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

fun currentUser(): AppPrincipal {
    val principal = SecurityContextHolder.getContext().authentication?.principal
    return principal as? AppPrincipal
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida")
}
