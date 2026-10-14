final public class RoadGraph{
    public static class RecordArc{
        static public record Arc(int targetIndex, double cost, int edgeIndex, boolean reversed){
            
        }
    }
}