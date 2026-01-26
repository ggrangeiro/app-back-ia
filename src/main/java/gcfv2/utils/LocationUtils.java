package gcfv2.utils;

public class LocationUtils {

    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * Calculates the distance between two points in kilometers using the Haversine
     * formula.
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Checks if two locations are considered "same" within a small threshold (e.g.
     * 100 meters).
     * 100 meters is roughly 0.1km.
     */
    public static boolean isSameLocation(double lat1, double lon1, double lat2, double lon2, double thresholdKm) {
        return calculateDistanceKm(lat1, lon1, lat2, lon2) <= thresholdKm;
    }

    public static class Coordinate {
        public double lat;
        public double lon;

        public Coordinate(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}
