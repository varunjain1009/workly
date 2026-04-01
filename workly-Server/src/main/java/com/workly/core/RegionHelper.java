package com.workly.core;

/**
 * Derives a region string from geographic coordinates using a 1°×1° grid
 * (approximately 111 km × 111 km cells).
 *
 * Example: lat=19.07, lon=72.87 → "19_72"
 *
 * The region string is used as the MongoDB shard key on the `jobs` and
 * `worker_profiles` collections, keeping geospatially-adjacent documents on
 * the same shard and bounding the size of each shard's 2dsphere index.
 */
public final class RegionHelper {

    private RegionHelper() {}

    /**
     * Returns the region key for the given latitude and longitude.
     *
     * @param latitude  WGS-84 latitude  [-90, 90]
     * @param longitude WGS-84 longitude [-180, 180]
     * @return region string in the form "{latDeg}_{lonDeg}", e.g. "19_72"
     */
    public static String fromCoordinates(double latitude, double longitude) {
        int latDeg = (int) Math.floor(latitude);
        int lonDeg = (int) Math.floor(longitude);
        return latDeg + "_" + lonDeg;
    }

    /**
     * Convenience overload for a [longitude, latitude] double array
     * (MongoDB / GeoJSON convention).
     *
     * @param location double[]{longitude, latitude}
     * @return region string, or null if location is null / too short
     */
    public static String fromLocation(double[] location) {
        if (location == null || location.length < 2) return null;
        return fromCoordinates(location[1], location[0]); // GeoJSON: [lon, lat]
    }
}
