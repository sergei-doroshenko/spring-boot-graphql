package org.sdoroshenko;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraphQLServerApplicationTest {

    /*@Test
    public void findAll() throws Exception {

    // RestAssured
        String json =
            "{\n" +
            "\t\"query\":\"{findAllCars{id vin make}}\"\n" +
            "}";

        String response =
            given()
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(json)
                .expect().statusCode(200)
                .when()
                .post("/graphql")
                .asString();

        System.out.println(response);
    }*/


    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void exampleTest() {
        String body = this.restTemplate.getForObject("/graphql/schema.json", String.class);
        System.out.println(body);
    }

    @Test
    public void findAllCars() {
        String json =
            "{\n" +
                "\t\"query\":\"{findAllCars{id vin make}}\"\n" +
                "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(json, headers);
        String body = this.restTemplate.postForObject("/graphql", entity, String.class);
        System.out.println(body);
    }
}
