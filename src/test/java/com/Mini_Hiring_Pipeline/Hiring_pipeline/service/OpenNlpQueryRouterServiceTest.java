package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.QueryRouteResponse;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.QueryRouteResponse.Intent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenNlpQueryRouterServiceTest {

    private OpenNlpQueryRouterService routerService;

    @BeforeEach
    void setUp() {
        routerService = new OpenNlpQueryRouterService();
        routerService.init();
    }

    @Test
    void testCandidateNameQueries() {
        QueryRouteResponse res1 = routerService.routeQuery("John Doe");
        assertEquals(Intent.NAME_SEARCH, res1.getIntent());

        QueryRouteResponse res2 = routerService.routeQuery("Ayush");
        assertEquals(Intent.NAME_SEARCH, res2.getIntent());

        QueryRouteResponse res3 = routerService.routeQuery("Ayusf");
        assertEquals(Intent.NAME_SEARCH, res3.getIntent());

        QueryRouteResponse res4 = routerService.routeQuery("will smith");
        assertEquals(Intent.NAME_SEARCH, res4.getIntent());

        QueryRouteResponse res5 = routerService.routeQuery("Will Smith");
        assertEquals(Intent.NAME_SEARCH, res5.getIntent());
    }

    @Test
    void testNaturalLanguageQueries() {
        QueryRouteResponse res1 = routerService.routeQuery("Who is in Interview right now?");
        assertEquals(Intent.NATURAL_LANGUAGE, res1.getIntent());

        QueryRouteResponse res2 = routerService.routeQuery("Show all rejected candidates");
        assertEquals(Intent.NATURAL_LANGUAGE, res2.getIntent());

        QueryRouteResponse res3 = routerService.routeQuery("Candidates > 5 years");
        assertEquals(Intent.NATURAL_LANGUAGE, res3.getIntent());
    }
}
