package com.thehecklers.coffeeclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;

@SpringBootApplication
public class CoffeeClientApplication {
    @Bean
    WebClient client() {
        return WebClient.create("http://localhost:8080");
    }

    @Bean
    RSocketRequester requester(RSocketRequester.Builder builder) {
        return builder.connectTcp("localhost", 8901).block();
    }

    public static void main(String[] args) {
        SpringApplication.run(CoffeeClientApplication.class, args);
    }

}

@Component
@AllArgsConstructor
class CoffeeClient {
    private final WebClient client;

    //    @PostConstruct
    void runIt() {
        client.get()
                .uri("/coffees")
                .retrieve()
                .bodyToFlux(Coffee.class)
                .filter(coffee -> coffee.getName().equalsIgnoreCase("don pablo"))
                .flatMap(coffee -> client.get()
                        .uri("/coffees/{id}/orders", coffee.getId())
                        .retrieve()
                        .bodyToFlux(CoffeeOrder.class))
                .subscribe(System.out::println);
    }
}

@RestController
@AllArgsConstructor
class ClientController {
    private final RSocketRequester requester;

    @GetMapping("/coffees")
    Flux<Coffee> allCoffeesFromService() {
        return requester.route("coffees").retrieveFlux(Coffee.class);
    }

    @GetMapping(value = "/orders/{coffeename}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<CoffeeOrder> ordersForCoffee(@PathVariable String coffeename) {
        return requester.route("orders.".concat(coffeename)).retrieveFlux(CoffeeOrder.class);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CoffeeOrder {
    private String coffeeId;
    private Instant whenOrdered;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Coffee {
    private String id, name;
}