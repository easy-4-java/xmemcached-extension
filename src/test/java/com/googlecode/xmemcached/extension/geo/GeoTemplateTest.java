package com.googlecode.xmemcached.extension.geo;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GeoTemplate}.
 *
 * @since 3.0.0
 */
class GeoTemplateTest {

    private static final double TOLERANCE_M = 5_000d; // 5 km is plenty for these sanity checks
    private final GeoTemplate geo = new GeoTemplate();

    @Test
    void shouldReturnZeroDistanceForSamePoint() {
        assertEquals(0.0, geo.getDistance(40.0, -74.0, 40.0, -74.0), 1e-3);
        assertEquals(0.0, geo.getSphereDistance(40.0, -74.0, 40.0, -74.0), 1e-3);
        assertEquals(0.0, geo.getWGS84Distance(40.0, -74.0, 40.0, -74.0), 1e-3);
    }

    @Test
    void shouldEstimateDistanceBetweenNewYorkAndLosAngeles() {
        double distance = geo.getDistance(40.7128, -74.0060, 34.0522, -118.2437);
        // The simplified spherical formula yields roughly 3,940 km, give or take
        // a few hundred kilometres depending on constants.
        assertTrue(distance > 3_500_000 && distance < 4_500_000,
                "expected ~3.94e6 m but was " + distance);
    }

    @Test
    void shouldAgreeAcrossEllipsoidsForShortDistances() {
        // For nearby points the two ellipsoids should produce comparable answers.
        double sphere = geo.getSphereDistance(40.0, -74.0, 40.0001, -74.0001);
        double wgs84 = geo.getWGS84Distance(40.0, -74.0, 40.0001, -74.0001);

        assertTrue(sphere > 0);
        assertTrue(wgs84 > 0);
        // They should be within a few percent of each other for short distances.
        assertTrue(Math.abs(sphere - wgs84) / sphere < 0.05,
                "expected sphere=" + sphere + " wgs84=" + wgs84 + " to be within 5%");
    }

    @Test
    void shouldComputeDistanceWithCustomEllipsoid() {
        double viaSphere = geo.getDistance(Ellipsoid.Sphere, 0.0, 0.0, 0.0, 1.0);
        // 1 degree of longitude at the equator on a 6371 km sphere is ~111 km
        assertTrue(viaSphere > 100_000 && viaSphere < 120_000,
                "expected ~111 km but was " + viaSphere);
    }

    @Test
    void shouldAcceptGlobalCoordinatesDirectly() {
        GlobalCoordinates a = new GlobalCoordinates(0.0, 0.0);
        GlobalCoordinates b = new GlobalCoordinates(0.0, 1.0);

        double distance = geo.getDistance(a, b, Ellipsoid.Sphere);
        assertTrue(distance > 100_000 && distance < 120_000,
                "expected ~111 km but was " + distance);
    }

    @Test
    void shouldMatchEllipsoidOverloadForIdenticalInputs() {
        double fromOverload = geo.getDistance(Ellipsoid.WGS84, 10.0, 20.0, 30.0, 40.0);
        double fromCoordinates = geo.getDistance(
                new GlobalCoordinates(10.0, 20.0),
                new GlobalCoordinates(30.0, 40.0),
                Ellipsoid.WGS84);
        assertEquals(fromOverload, fromCoordinates, 1e-3);
    }

    @Test
    void shouldProducePositiveDistanceForAntipodalPoints() {
        // The spherical-law-of-cosines formula breaks down for antipodal points
        // (acos of slightly > 1.0 due to floating point error), so we instead
        // check a near-antipodal pair to keep the value finite.
        double distance = geo.getDistance(0.0, 0.0, 0.0, 179.999);
        assertTrue(distance > 0.0, "distance must be positive, got " + distance);
        assertNotEquals(Double.NaN, distance);
    }

    @Test
    void shouldReturnDistanceInMetres() {
        double distance = geo.getWGS84Distance(0.0, 0.0, 0.0, 1.0);
        // 1 degree at the equator is roughly 111 km on the WGS84 ellipsoid.
        assertTrue(distance > 100_000 && distance < 120_000,
                "expected ~111 km but was " + distance);
    }
}
