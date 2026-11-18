package cl.tratohecho.identity.web

import cl.tratohecho.common.security.currentUser
import cl.tratohecho.identity.service.AuthenticationService
import cl.tratohecho.identity.service.UserManagementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController(private val authenticationService: AuthenticationService) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest) = authenticationService.login(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest) = authenticationService.refresh(request)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: LogoutRequest) = authenticationService.logout(request)

    @GetMapping("/me")
    fun me() = authenticationService.current(currentUser())
}

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
class UsersController(private val users: UserManagementService) {
    @GetMapping fun list() = users.list(currentUser().tenantId)
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = users.get(currentUser().tenantId, id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateUserRequest) = users.create(currentUser().tenantId, request)
    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: UpdateUserRequest) = users.update(currentUser().tenantId, id, request)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun disable(@PathVariable id: UUID) = users.disable(currentUser().tenantId, id)
}
