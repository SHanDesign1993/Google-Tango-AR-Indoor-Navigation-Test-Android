package  trendmicro.com.tangoindoornavigation.data;

import android.util.Log;

import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

import trendmicro.com.tangoindoornavigation.util.TangoUtil;

/**
 * Created by hugo on 31/07/2017.
 */
public class PathFinder {

    private static final String TAG = PathFinder.class.getSimpleName();
    private double unit;
    private List<Node> openList;
    private List<Node> closedList;
    private ArrayList<Vector2> path;
    private Node goal;
    private QuadTree quadTree;

    public PathFinder(QuadTree quadTree) {
        this.quadTree = quadTree;
        this.unit = quadTree.getUnit();
    }

    private void resetSearchAlgorithm() {
        openList = new ArrayList<>();
        closedList = new ArrayList<>();
        path = new ArrayList<>();
        goal = null;
    }

    /**
     * finds the shortest path between a start and an end point using A*
     * https://en.wikipedia.org/wiki/A*_search_algorithm
     *
     * @param from start point
     * @param to   end point
     * @return list of way points (empty if not available)
     * @throws Exception when not path is found or the search space is not available
     */
    public List<Vector2> findPathBetween(Vector2 from, Vector2 to) throws Exception {
        resetSearchAlgorithm();
        if (!quadTree.isFilled(from) || !quadTree.isFilled(to)) {
            throw new Exception("fields are not visited in quadtree");
        }
        from = quadTree.rasterize(from);
        to = quadTree.rasterize(to);
        goal = new Node(to);
        openList.add(new Node(from));
        do {
            Node currentNode = getClosestNode();
            if (currentNode.equals(goal)) {
                while (currentNode.parent != null) {
                    path.add(new Vector2(currentNode.x, currentNode.y));
                    currentNode = currentNode.parent;
                }
                if (path.size() >= 1) {
                    path.remove(path.size() - 1);
                    path.add(from);
                }
                return path;
            }
            openList.remove(currentNode);
            closedList.add(currentNode);
            expandNode(currentNode);
        } while (!openList.isEmpty());
        throw new Exception("no path found");
    }

    /**
     * expand the openList by the surrounding fields in the quadtree
     *
     * @param currentNode center node for the expansion
     */
    private void expandNode(Node currentNode) {

        Node neighbours[] = new Node[8];
        neighbours[0] = new Node(currentNode.x, currentNode.y + unit);
        neighbours[1] = new Node(currentNode.x - unit, currentNode.y + unit);
        neighbours[2] = new Node(currentNode.x - unit, currentNode.y);
        neighbours[3] = new Node(currentNode.x - unit, currentNode.y - unit);
        neighbours[4] = new Node(currentNode.x, currentNode.y - unit);
        neighbours[5] = new Node(currentNode.x + unit, currentNode.y - unit);
        neighbours[6] = new Node(currentNode.x + unit, currentNode.y);
        neighbours[7] = new Node(currentNode.x + unit, currentNode.y + unit);
        for (Node neighbour : neighbours) {
            if (closedList.contains(neighbour) || !quadTree.isFilled(new Vector2(neighbour.x, neighbour.y))) {
                continue;
            }
            neighbour.parent = currentNode;
            neighbour.ancestor = currentNode.parent;

            if (!openList.contains(neighbour) || openList.contains(neighbour) && distance(openList.get(openList.indexOf(neighbour)), goal) > distance(neighbour, goal)) {
                openList.add(neighbour);
            }
        }
    }

    /**
     * finds the Node with the smallest distance in the openList
     *
     * @return a Node with the smallest distance
     */
    private Node getClosestNode() {
        Node min = openList.get(0);
        Node theSameDirection = null;

        List<Node> tempOpenList = new ArrayList<>();
        for (Node vector : openList) {

            int distanceVectorToGoal = distance(vector, goal);
            //int distanceMinToGoal = distance(min, goal);
            vector.distance = distanceVectorToGoal;
            if (vector.parent != null) {
                if (vector.ancestor != null) {
                    vector.direction = TangoUtil.getDirectionByVector3(new Vector3(vector.ancestor.x, 0, vector.ancestor.y),
                            new Vector3(vector.parent.x, 0, vector.parent.y),
                            new Vector3(vector.x, 0, vector.y));
                    //Log.i(TAG, "[getClosestNode]current vector = " + vector.toString() + ", distanceVectorToGoal = " + distanceVectorToGoal + ", vector.direction" + vector.direction  + ", current vector parent = " + vector.parent.toString() + ", current vector ancestor = " + vector.ancestor.toString());
                } else {
                    //Log.i(TAG, "[getClosestNode]current vector = " + vector.toString() + ", distanceVectorToGoal = " + distanceVectorToGoal + ", current vector parent = " + vector.parent.toString());
                }
            }

            tempOpenList.add(vector);

        }

        for (Node vector : tempOpenList) {
            int distanceMinToGoal = distance(min, goal);
            if (vector.direction == TangoUtil.VectorDirection.FORWARD) {
                if (theSameDirection == null) {
                    theSameDirection = vector;
                } else {
                    if (vector.distance < theSameDirection.distance) {
                        theSameDirection = vector;
                    }
                }
            } else {
                if (vector.distance < distanceMinToGoal) {
                    min = vector;
                } else if ((vector.distance == distanceMinToGoal) || ((vector.distance - 1) == distanceMinToGoal)) {
                    if (min.parent != null) {
                        if (min.parent.x == vector.x || min.parent.y == vector.y) {
                            min = vector;
                        }
                    }
                }
            }
        }
        if (theSameDirection != null) {
            min = theSameDirection;
        }
        return min;
    }

    /**
     * Computes the A* distance f(x) = g(x) + h(x)
     *
     * @param v1 field 1 for comparison
     * @param v2 field 1 for comparison
     * @return the distance
     */
    private int distance(Node v1, Node v2) {
        // g are the hops to the starting point
        int g = 0;
        Node v = v1;
        while (v.parent != null) {
            g++;
            v = v.parent;
        }
        // h is the rounded euclidean space to the end point
        int h = (int) (Math.sqrt((v1.x - v2.x) * (v1.x - v2.x) + (v1.y - v2.y) * (v1.y - v2.y)) / unit) + 1;
        return g + h;
    }

    public List<Vector2> findPathBetween(Vector3 a, Vector3 b) throws Exception {
        return findPathBetween(new Vector2(a.x, a.z), new Vector2(b.x, b.z));
    }

    /**
     * Node class for vector2d encapsulation with parent node and equals()
     */
    private class Node {
        final double x, y;
        Node parent;
        Node ancestor;
        int distance;
        TangoUtil.VectorDirection direction;

        public Node(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Node(Vector2 v) {
            this.x = v.getX();
            this.y = v.getY();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Node) {
                Node v = (Node) o;
                return v.x == x && v.y == y;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Node : <")
                    .append(this.x)
                    .append(", ")
                    .append(this.y)
                    .append(">");
            return sb.toString();
        }
    }
}
