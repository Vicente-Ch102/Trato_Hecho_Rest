package cl.tratohecho.identity.config

import cl.tratohecho.identity.domain.TenantEntity
import cl.tratohecho.identity.domain.UserAccountEntity
import cl.tratohecho.identity.domain.UserRole
import cl.tratohecho.identity.repository.TenantRepository
import cl.tratohecho.identity.repository.UserAccountRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class BootstrapConfiguration {
    @Bean
    fun bootstrapRunner(
        tenants: TenantRepository,
        users: UserAccountRepository,
        encoder: PasswordEncoder,
        @Value("\${app.bootstrap.enabled:false}") enabled: Boolean,
        @Value("\${app.bootstrap.tenant-name:Empresa Demo}") tenantName: String,
        @Value("\${app.bootstrap.admin-name:Administrador Demo}") adminName: String,
        @Value("\${app.bootstrap.admin-email:admin@tratohecho.cl}") adminEmail: String,
        @Value("\${app.bootstrap.admin-password:}") adminPassword: String
    ) = CommandLineRunner {
        if (!enabled || users.count() > 0) return@CommandLineRunner
        require(adminPassword.length >= 10) { "BOOTSTRAP_ADMIN_PASSWORD debe tener al menos 10 caracteres" }
        val tenant = tenants.save(TenantEntity(name = tenantName))
        users.save(UserAccountEntity(
            tenantId = tenant.id ?: error("Tenant sin ID"), name = adminName,
            email = adminEmail.trim().lowercase(), passwordHash = encoder.encode(adminPassword),
            role = UserRole.ADMIN, enabled = true
        ))
    }
}
