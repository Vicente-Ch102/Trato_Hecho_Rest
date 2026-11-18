package cl.tratohecho.identity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["cl.tratohecho.common", "cl.tratohecho.identity"])
class IdentityApplication

fun main(args: Array<String>) {
    runApplication<IdentityApplication>(*args)
}
