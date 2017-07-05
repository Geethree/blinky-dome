package com.github.starcats.blinkydome.model;

import com.github.starcats.blinkydome.pattern.SCPattern;
import heronarts.lx.model.LXAbstractFixture;
import heronarts.lx.model.LXPoint;
import processing.core.PApplet;
import processing.data.Table;
import processing.data.TableRow;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Model of StarCats dome
 */
public class BlinkyDome extends StarcatsLxModel {
  /** Like LXModel.points, but up-cast to our LED class */
  public final List<LED> leds;

  public final List<TriangleFixture> allTriangles;

  private final Map<Integer, List<TriangleFixture>> trianglesByLayer;
  private final Map<Integer, List<TriangleFixture>> trianglesByIndex;

  /**
   * Factory to construct a new instance from CSV LED position map
   * @param p processing instance
   * @return new BlinkyDome instance
   */
  public static BlinkyDome makeModel(PApplet p) {

    Table ledTable = p.loadTable("led-locations.csv", "header,csv");
    if (ledTable == null) {
      throw new RuntimeException("Error: could not load LED position data");
    }

    // TODO: tony: is this used for anything?
//    Table vertexTable = p.loadTable("vertex-locations.csv", "header,csv");
//    if (vertexTable == null) {
//      throw new RuntimeException("Error: could not load vertex locations");
//    }

    List<LED> allLeds = new ArrayList<>();
    for (TableRow row : ledTable.rows()) {
      allLeds.add(new LED(row));
    }

    // Group LEDs into triangles
    Map<String, List<LED>> ledsByTriangleHash = allLeds.stream().collect(Collectors.groupingBy(
        led -> "" + led.triangleX + ", " + led.triangleY + ", " + led.triangleZ
    ));

    List<TriangleFixture> allTriangles = ledsByTriangleHash.values().stream()
        .map(TriangleFixture::new).collect(Collectors.toList());

    Map<Integer, List<TriangleFixture>> trianglesByLayer = allTriangles.stream()
        .collect(Collectors.groupingBy(triangle -> triangle.layer));

    Map<Integer, List<TriangleFixture>> trianglesByIndex = allTriangles.stream()
        .collect(Collectors.groupingBy(triangle -> triangle.index));

    return new BlinkyDome(allLeds, allTriangles, trianglesByLayer, trianglesByIndex, p);
  }

  /**
   * BlinkyDome LED definition
   */
  public static class LED extends LXPoint {
    final public int triangleIndex;
    final public int triangleSubindex;
    final public int ledIndex;
    final public int layer;

    final public float x, y, z;
    final public float triangleX, triangleY, triangleZ;

    final public float theta, phi;

    public LED(TableRow row) {
      super(row.getFloat("x"), row.getFloat("z"), -row.getFloat("y"));
      this.x = row.getFloat("x");
      this.y = row.getFloat("y");
      this.z = row.getFloat("z");

      this.triangleIndex = row.getInt("index");
      this.triangleSubindex = row.getInt("sub_index");
      this.ledIndex = row.getInt("led_num");
      this.layer = row.getInt("layer");

      this.triangleX = row.getFloat("triangle_center_x");
      this.triangleY = row.getFloat("triangle_center_y");
      this.triangleZ = row.getFloat("triangle_center_z");

      double r = Math.sqrt(x * x + y * y + z * z);

      this.theta = (float)Math.acos(z / r);
      this.phi = (float)Math.atan2(y, x);
    }
  }

  /**
   * BlinkyDome triangle definition
   */
  public static class TriangleFixture extends LXAbstractFixture {
    final public int index;
    final public int subindex;
    final public float x, y, z;
    final public int layer;

    private TriangleFixture(List<LED> leds) {
      if (leds == null || leds.size() == 0) {
        throw new RuntimeException("Configuration exception: no LED's passed to Triangle constructor!");
      }

      LED canonical = leds.get(0);
      this.index = canonical.triangleIndex;
      this.subindex = canonical.triangleSubindex;
      this.x = canonical.triangleX;
      this.y = canonical.triangleY;
      this.z = canonical.triangleZ;
      this.layer = canonical.layer;

      leds.forEach(this::addPoint);
    }
  }

  private BlinkyDome(List<LED> allLEDs,
                     List<TriangleFixture> allTriangles,
                     Map<Integer, List<TriangleFixture>> trianglesByLayer,
                     Map<Integer, List<TriangleFixture>> trianglesByIndex, PApplet p) {
    super(new ArrayList<>(allLEDs)); // dupe LXPoint-typed ArrayList needed for java generics type inference ಠ_ಠ

    this.leds = Collections.unmodifiableList(allLEDs);


    // Make fixture definitions immutable:

    this.allTriangles = Collections.unmodifiableList(allTriangles);

    Map<Integer, List<TriangleFixture>> immutableTrianglesByLayer = new HashMap<>();
    trianglesByLayer.forEach(
        (layer, triangles) -> immutableTrianglesByLayer.put(layer, Collections.unmodifiableList(triangles))
    );
    this.trianglesByLayer = Collections.unmodifiableMap(immutableTrianglesByLayer);

    Map<Integer, List<TriangleFixture>> immutableTrianglesByIndex = new HashMap<>();
    trianglesByIndex.forEach(
        (index, triangles) -> immutableTrianglesByIndex.put(index, Collections.unmodifiableList(triangles))
    );
    this.trianglesByIndex = Collections.unmodifiableMap(immutableTrianglesByIndex);
  }

  public List<TriangleFixture> getTrianglesByLayer(Integer layer) {
    return trianglesByLayer.get(layer);
  }

  public Set<Integer> getLayerKeys() {
    return trianglesByLayer.keySet();
  }

  public List<TriangleFixture> getTrianglesByIndex(Integer index) {
    return trianglesByIndex.get(index);
  }

  public Set<Integer> getTriangleIndexKeys() {
    return trianglesByIndex.keySet();
  }

  @Override
  public void visit(SCPattern pattern) {
    pattern.configureAgainst(this);
  }
}
