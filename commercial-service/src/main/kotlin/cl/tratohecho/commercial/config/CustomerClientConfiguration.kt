package cl.tratohecho.commercial.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class CustomerClientConfiguration {
    @Bean
    fun customerRestClient(
        builder: RestClient.Builder,
        @Value("\${services.customers.base-url:http://localhost:8082}") baseUrl: String
    ): RestClient = builder.baseUrl(baseUrl).build()
}
