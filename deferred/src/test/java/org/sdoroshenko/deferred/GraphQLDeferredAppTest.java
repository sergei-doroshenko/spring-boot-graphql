package org.sdoroshenko.deferred;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraphQLDeferredAppTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void schemaTest() {
        String body = this.restTemplate.getForObject("/graphql/schema.json", String.class);
        assertNotNull(body);
    }
}
