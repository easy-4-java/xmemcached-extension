package com.googlecode.xmemcached.extension.geo;

import lombok.extern.slf4j.Slf4j;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless geodetic distance helpers for the {@code xmemcached-extension}
 * toolkit.
 *
 * <p>Wraps Mike Gavaghan's {@code geodesy} library and exposes convenience
 * overloads for the two most common {@link Ellipsoid ellipsoids}
 * ({@link Ellipsoid#Sphere}, {@link Ellipsoid#WGS84}) in addition to a
 * fully customisable entry point that accepts any {@link Ellipsoid}. All
 * overloads return distances in metres.</p>
 *
 * <p>Coordinates are expressed in decimal degrees following the
 * latitude-first convention used by the underlying {@code geodesy}
 * library.</p>
 *
 * @author wandl
 * @since 3.0.0
 * @see GeodeticCalculator
 * @see Ellipsoid
 */
@Slf4j
public class GeoTemplate {



	/**
	 * Compute the great-circle distance between two points on a perfect
	 * sphere using the simplified spherical-law-of-cosines formula.
	 *
	 * <p>This overload assumes a constant earth radius of {@code 6371 km}
	 * and is therefore less accurate than the
	 * {@link #getDistance(Ellipsoid, double, double, double, double) general-purpose}
	 * variant, but cheaper to compute because it skips the
	 * {@link GeodeticCalculator} allocation.</p>
	 *
	 * @param latitude1  latitude of the first point in decimal degrees
	 * @param longitude1 longitude of the first point in decimal degrees
	 * @param latitude2  latitude of the second point in decimal degrees
	 * @param longitude2 longitude of the second point in decimal degrees
	 * @return the spherical distance in metres
	 * @see #getSphereDistance(double, double, double, double)
	 * @see #getWGS84Distance(double, double, double, double)
	 */
	public double getDistance(double latitude1, double longitude1, double latitude2, double longitude2) {

		double lat1 = (Math.PI / 180) * latitude1;
		double lat2 = (Math.PI / 180) * latitude2;

		double lon1 = (Math.PI / 180) * longitude1;
		double lon2 = (Math.PI / 180) * longitude2;

		// Mean earth radius, kilometres.
		double R = 6371;

		// Spherical-law-of-cosines distance, kilometres; converted to metres below.
		double d = Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1))
				* R;

		return d * 1000;
	}

	/**
	 * Compute the ellipsoidal distance using {@link Ellipsoid#Sphere}.
	 *
	 * @param latitude1  latitude of the first point in decimal degrees
	 * @param longitude1 longitude of the first point in decimal degrees
	 * @param latitude2  latitude of the second point in decimal degrees
	 * @param longitude2 longitude of the second point in decimal degrees
	 * @return the distance in metres
	 * @see #getDistance(Ellipsoid, double, double, double, double)
	 */
	public double getSphereDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
		return this.getDistance(Ellipsoid.Sphere, latitude1, longitude1, latitude2, longitude2);
	}

	/**
	 * Compute the ellipsoidal distance using {@link Ellipsoid#WGS84}, the
	 * ellipsoid used by the Global Positioning System.
	 *
	 * @param latitude1  latitude of the first point in decimal degrees
	 * @param longitude1 longitude of the first point in decimal degrees
	 * @param latitude2  latitude of the second point in decimal degrees
	 * @param longitude2 longitude of the second point in decimal degrees
	 * @return the distance in metres
	 * @see #getDistance(Ellipsoid, double, double, double, double)
	 */
	public double getWGS84Distance(double latitude1, double longitude1, double latitude2, double longitude2) {
	    return this.getDistance(Ellipsoid.WGS84, latitude1, longitude1, latitude2, longitude2);
	}

	/**
	 * Compute the ellipsoidal distance using the supplied {@link Ellipsoid}.
	 *
	 * @param ellipsoid  the ellipsoid model to use (e.g. {@link Ellipsoid#WGS84}
	 *                   or {@link Ellipsoid#Sphere})
	 * @param latitude1  latitude of the first point in decimal degrees
	 * @param longitude1 longitude of the first point in decimal degrees
	 * @param latitude2  latitude of the second point in decimal degrees
	 * @param longitude2 longitude of the second point in decimal degrees
	 * @return the ellipsoidal distance in metres
	 * @see GeodeticCalculator#calculateGeodeticCurve(Ellipsoid, GlobalCoordinates, GlobalCoordinates)
	 */
	public double getDistance(Ellipsoid ellipsoid, double latitude1, double longitude1, double latitude2, double longitude2) {

		// Starting point coordinates.
		GlobalCoordinates gpsFrom = new GlobalCoordinates(latitude1, longitude1);

		// Destination point coordinates.
		GlobalCoordinates gpsTo = new GlobalCoordinates(latitude2, longitude2);

	    // Delegate to the geodesy library and surface the resulting curve length.
	    return this.getDistance(gpsFrom, gpsTo, ellipsoid);

	}

	/**
	 * Low-level overload that accepts pre-built {@link GlobalCoordinates}
	 * instances, useful when the same origin point is reused across many
	 * distance computations.
	 *
	 * @param gpsFrom  starting point; must not be {@code null}
	 * @param gpsTo    destination point; must not be {@code null}
	 * @param ellipsoid ellipsoid model to use; must not be {@code null}
	 * @return the ellipsoidal distance in metres
	 * @see GeodeticCalculator#calculateGeodeticCurve(Ellipsoid, GlobalCoordinates, GlobalCoordinates)
	 */
	public double getDistance(GlobalCoordinates gpsFrom, GlobalCoordinates gpsTo, Ellipsoid ellipsoid){

        // Build a one-shot calculator; the geodesy library is stateless, so a
        // new instance per call is the canonical usage pattern.
        GeodeticCurve geoCurve = new GeodeticCalculator().calculateGeodeticCurve(ellipsoid, gpsFrom, gpsTo);

        // Surface only the ellipsoidal distance in metres.
        return geoCurve.getEllipsoidalDistance();
    }

}
