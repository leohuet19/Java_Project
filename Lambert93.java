package potager.geo;

/** Conversion Lambert 93 (EPSG:2154, RGF93 / GRS80)  WGS84. */
public final class Lambert93 {

  private static final double A = 6378137.0;
  private static final double E = 0.081819191042816;
  private static final double LAT_0 = Math.toRadians(46.5);
  private static final double LON_0 = Math.toRadians(3.0);
  private static final double LAT_1 = Math.toRadians(44.0);
  private static final double LAT_2 = Math.toRadians(49.0);
  private static final double X_0 = 700000.0;
  private static final double Y_0 = 6600000.0;

  private static final double N;
  private static final double A_F;
  private static final double RHO_0;

  static {
    double m1 = m(LAT_1);
    double m2 = m(LAT_2);
    double t1 = t(LAT_1);
    double t2 = t(LAT_2);
    N = (Math.log(m1) - Math.log(m2)) / (Math.log(t1) - Math.log(t2));
    A_F = A * m1 / (N * Math.pow(t1, N));
    RHO_0 = A_F * Math.pow(t(LAT_0), N);
  }

  private Lambert93() {
  }

  private static double m(double lat) {
    double s = Math.sin(lat);
    return Math.cos(lat) / Math.sqrt(1.0 - E * E * s * s);
  }

  private static double t(double lat) {
    double s = Math.sin(lat);
    return Math.tan(Math.PI / 4.0 - lat / 2.0) / Math.pow((1.0 - E * s) / (1.0 + E * s), E / 2.0);
  }

  /** @return {latitude, longitude} en degrés WGS84. */
  public static double[] toWgs84(double x, double y) {
    double dx = x - X_0;
    double dy = RHO_0 - (y - Y_0);
    double rho = Math.signum(N) * Math.hypot(dx, dy);
    double theta = Math.atan2(Math.signum(N) * dx, Math.signum(N) * dy);
    double tPrime = Math.pow(rho / A_F, 1.0 / N);

    double lat = Math.PI / 2.0 - 2.0 * Math.atan(tPrime);
    for (int iteration = 0; iteration < 12; iteration++) {
      double s = Math.sin(lat);
      double next = Math.PI / 2.0
          - 2.0 * Math.atan(tPrime * Math.pow((1.0 - E * s) / (1.0 + E * s), E / 2.0));
      if (Math.abs(next - lat) < 1e-12) {
        lat = next;
        break;
      }
      lat = next;
    }
    return new double[] { Math.toDegrees(lat), Math.toDegrees(theta / N + LON_0) };
  }

  /** @return {x, y} en mètres Lambert 93. */
  public static double[] fromWgs84(double latitudeDegrees, double longitudeDegrees) {
    double lat = Math.toRadians(latitudeDegrees);
    double lon = Math.toRadians(longitudeDegrees);
    double rho = A_F * Math.pow(t(lat), N);
    double theta = N * (lon - LON_0);
    return new double[] { X_0 + rho * Math.sin(theta), Y_0 + RHO_0 - rho * Math.cos(theta) };
  }
}
