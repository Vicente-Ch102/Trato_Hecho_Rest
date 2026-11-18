package cl.tratohecho.commercial

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["cl.tratohecho.common", "cl.tratohecho.commercial"])
class CommercialApplication

fun main(args: Array<String>) {
    runApplication<CommercialApplication>(*args)
}
