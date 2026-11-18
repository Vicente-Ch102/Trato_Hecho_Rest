package cl.tratohecho.customers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["cl.tratohecho.common", "cl.tratohecho.customers"])
class CustomersApplication

fun main(args: Array<String>) {
    runApplication<CustomersApplication>(*args)
}
