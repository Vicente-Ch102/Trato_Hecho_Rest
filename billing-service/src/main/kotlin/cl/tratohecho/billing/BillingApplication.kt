package cl.tratohecho.billing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["cl.tratohecho.common", "cl.tratohecho.billing"])
class BillingApplication

fun main(args: Array<String>) {
    runApplication<BillingApplication>(*args)
}
