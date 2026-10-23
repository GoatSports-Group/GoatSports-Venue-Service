package iuh.fit.venueservice.domain.entity;

public record GeoPoint(double latitude, double longitude) {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public double metersTo(double otherLatitude, double otherLongitude) {
        double dLat = Math.toRadians(otherLatitude - latitude);
        double dLng = Math.toRadians(otherLongitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(otherLatitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
