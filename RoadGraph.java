import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoadGraph {

    // --- Question 1 ---
    public record Arc(int targetIndex, double cost, int edgeIndex, boolean reversed) {}

    private final Map<Integer, Integer> indexByNodeId = new HashMap<>();
    private final List<Integer> nodeIds = new ArrayList<>();
    private final List<Double> latitudes = new ArrayList<>();
    private final List<Double> longitudes = new ArrayList<>();
    private final List<List<Arc>> adjacency = new ArrayList<>();

    private final List<double[]> edgeGeometries = new ArrayList<>();
    private final List<int[]> edgeEndpoints = new ArrayList<>();
    private final List<double[]> edgeCosts = new ArrayList<>();
    private int realEdgeCount;

    private static final double METERS_PER_DEGREE = 111_320.0;
    private static final double WALKING_SPEED = 1.2;

    private RoadGraph() {}

    public static RoadGraph load(Path verticesCsv, Path edgesCsv) {
        RoadGraph graph = new RoadGraph();
        graph.readVertices(verticesCsv);
        graph.readEdges(edgesCsv);
        return graph;
    }

    // --- Question 2 ---
    private static double[] reverse(double[] geometry) {
        double reverse[] = new double[geometry.length];
        for(int i = 0; i < geometry.length; i += 2){
            reverse[i] = geometry[geometry.length - 2 - i];
            reverse[i+1] = geometry[geometry.length - 1 - i];
        }
        return reverse;
    }

    // --- Question 3 ---
    private void readVertices(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split(";");
                int id = Integer.parseInt(columns[0].trim());
                double[] latLon = Lambert93.toWgs84(
                    Double.parseDouble(columns[1].trim()),
                    Double.parseDouble(columns[2].trim())
                );
                indexByNodeId.put(id, nodeIds.size());
                nodeIds.add(id);
                latitudes.add(latLon[0]);
                longitudes.add(latLon[1]);
                adjacency.add(new ArrayList<>());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
    }

    // --- Question 4 ---
    private static double parseCost(String raw) {
        String value = raw.trim();
        if (value.isEmpty())
            return -1;
        double cost = Double.parseDouble(value);
        return Double.isFinite(cost) && cost >= 0 ? cost : -1;
    }

    private boolean isCloserToEnd(double[] geometry, int sourceIndex) {
        double sourceLat = latitudes.get(sourceIndex);
        double sourceLon = longitudes.get(sourceIndex);
        double toStart = Math.hypot(geometry[0] - sourceLat, geometry[1] - sourceLon);
        // Math.hypot calcule la distance euclidienne de façon optimisée
        // en gérant les risques de débordement.
        double toEnd = Math.hypot(geometry[geometry.length - 2] - sourceLat,
                                  geometry[geometry.length - 1] - sourceLon);
        return toEnd < toStart;
    }

    /**
     * WKT : suite de couples (x y) en Lambert 93 ;
     * resultat : suite de couples (lat, lon) en WGS84, orientee source vers target
     */
    private double[] parseGeometry(String wkt, int sourceIndex, int targetIndex) {
        String cleaned = wkt.replace("\"", "");
        int start = cleaned.indexOf('(');
        if (start < 0) {
            return new double[] {
                latitudes.get(sourceIndex), longitudes.get(sourceIndex),
                latitudes.get(targetIndex), longitudes.get(targetIndex)
            };
        }
        String body = cleaned.substring(start).replace("(", " ").replace(")", " ");
        String[] pairs = body.split(",");
        List<Double> coordinates = new ArrayList<>(pairs.length * 2);
        for (String pair : pairs) {
            String[] parts = pair.trim().split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            double[] latLon = Lambert93.toWgs84(
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1])
            );
            coordinates.add(latLon[0]);
            coordinates.add(latLon[1]);
        }
        double[] geometry = new double[coordinates.size()];
        for (int i = 0; i < geometry.length; i++) {
            geometry[i] = coordinates.get(i);
        }
        if (geometry.length >= 4 && isCloserToEnd(geometry, sourceIndex)) {
            geometry = reverse(geometry);
        }
        return geometry;
    }

    // --- Question 5 ---
    private void readEdges(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split(";", 6);
                if (columns.length < 6) {
                    continue;
                }
                Integer source = indexByNodeId.get(Integer.parseInt(columns[1].trim()));
                Integer target = indexByNodeId.get(Integer.parseInt(columns[2].trim()));
                if (source == null || target == null) {
                    continue;
                }
                double forwardCost = parseCost(columns[3]);
                double reverseCost = parseCost(columns[4]);
                double[] geometry = parseGeometry(columns[5], source, target);
                int edgeIndex = edgeGeometries.size();
                edgeGeometries.add(geometry);
                edgeEndpoints.add(new int[] { source, target });
                edgeCosts.add(new double[] { forwardCost, reverseCost });
                if (forwardCost >= 0) {
                    adjacency.get(source).add(new Arc(target, forwardCost, edgeIndex, false));
                }
                if (reverseCost >= 0) {
                    adjacency.get(target).add(new Arc(source, reverseCost, edgeIndex, true));
                }
            }
            realEdgeCount = edgeGeometries.size();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible : " + file, e);
        }
    }
}
