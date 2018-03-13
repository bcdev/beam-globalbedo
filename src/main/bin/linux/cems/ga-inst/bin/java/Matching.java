package wgs;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * This program create text file for a target tile that matches SinXY-1km pixels to  lat/lon grid pixels
 * for each tile (eg. h18v03), mosaic subdivision number d (eg. 02) and resolution res (005) it creates text file: tile.res.d.txt (eg h18v03.005.02.txt)
 * The line in resulted files is inserted like that: index_tilePixel:index_gridPixel1,centroidsDistance1,ratioCommonArea1;index_gridPixel2,centroidDistance2,ratioCommonArea2;...       
 * @author Said Kharbouche MSSL.UCL (2014)
 *
 */
public class Matching{
	//##################################################################################################################
	/**
	 * spatial resolution name (005deg, 05deg,..)
	 */
	private String resName;
	/**
	 * X-axis resolution (0.05, 0.5)
	 */
	private float resDegX;
	/**
	 * Y-axis resolution (0.05, 0.5)
	 */
	private float resDegY;

	/**
	 * height mosaic
	 */
	private int hG;
	/**
	 * width mosaic
	 */
	private int wG;

	/**
	 * if true the pixels in two extreme altitudes will be fused (required by TM mosaics)
	 */
	private boolean polarRegion = false;
	/**
	 * output directory 
	 */
	private String outFolder;;
	/**
	 * Number of mosaic subdivisions 
	 */
	private int div;
	/**
	 * target tile (h00v08, h00v09,...)
	 */
	String tile;

	/**
	 * if true coordinate in mosaic will represent pixel center otherwise upper left corner
	 */
	private boolean pixCentre;	
	/**
	 * tile height
	 */
	private int hTile = 1200;
	/**
	 * tile width
	 */
	private int wTile = 1200;
	/**
	 *  min latitude in mosaic
	 */
	private float latMin = -90.0f;
	/**
	 * max latitude in mosaic
	 */
	private float latMax = 90.0f;
	/**
	 * min longitude in mosaic
	 */
	private float lonMin = -180.0f;
	/**
	 * max longitude in mosaic
	 */
	private float lonMax = 180.0f;

	//##################################################################################################################
	
	
	
	
	
	
	
	
	
	private FileWriter fWriter = null;
	private Geometry geoOutUP;
	private Geometry geoOutDown;
	private Geometry geoOutLeft;
	private Geometry geoOutRight;
	private Geometry validArea;
	private Geometry polT;
	private ArrayList<Geometry> geoIdx = null;
	private String wkt="PROJCS[\"MODIS Sinusoidal\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]],PROJECTION[\"Sinusoidal\"],PARAMETER[\"false_easting\",0.0],PARAMETER[\"false_northing\",0.0],PARAMETER[\"central_meridian\",0.0],PARAMETER[\"semi_major\",6371007.181],PARAMETER[\"semi_minor\",6371007.181],UNIT[\"m\",1.0],AUTHORITY[\"SR-ORG\",\"6974\"]]";
 
	private CoordinateReferenceSystem stroPolCRS = null;
	private MathTransform transform = null;
	private double earthW = 2.001511E7;
	private double earthH = 1.0007555E7;
	private double tileW = (earthW * 2) / 36.0;
	private double tileH = (earthH * 2) / 18.0;
	private double res = 463.31271652777775 * 2;

	
	
	
	
	/**
	 * launche the process
	 */
	@SuppressWarnings("unchecked")
	public void compute() {

		int hi = Integer.parseInt(tile.substring(1, 3));
		int vi = Integer.parseInt(tile.substring(4, 6));

		System.out.println("start..");

		computeOutGeos();

		this.wG = Math.round(((lonMax - lonMin) / this.resDegX));
		this.hG = Math.round(((latMax - latMin) / this.resDegY));
		System.out.println("resDegX, resDegY: " + resDegX + ", " + resDegY);
		System.out.println("Mosaic (W, H): (" + wG + ", " + hG + ")");
		System.out.println("outfolder: " + this.outFolder);
		System.out.println(" --------------------------- ");
		int wg = wG / div;
		int hg = hG / div;
		int ng = wg * hg;

		this.polT = readtilebox(hi, vi);

		if (this.polT != null)
			for (int b = 0, di = 0; b < div; b++)
				for (int a = 0; a < div; a++, di++) {
					int x0 = a * wg;
					int y0 = b * hg;

					String diStr = (di < 10) ? ("0" + di) : ("" + di);

					diStr = "." + diStr;

					System.out.println("div:" + diStr + " --> (" + a + ", " + b
							+ ")");

					float lat0 = latMax - ((y0) * this.resDegY);
					float lon0 = lonMin + ((x0) * this.resDegX);
					float lat1 = latMax - ((y0 + hg) * this.resDegY);
					float lon1 = lonMin + ((x0 + wg) * this.resDegX);

					STRtree idxG = null;

					this.geoIdx = new ArrayList<Geometry>();
					idxG = idxGrid(lat0, lon0, this.resDegY, this.resDegX, hg,
							wg, this.pixCentre);

					float latMin = Math.min(lat0, lat1);
					float lonMin = Math.min(lon0, lon1);
					float latMax = Math.max(lat0, lat1);
					float lonMax = Math.max(lon0, lon1);

					Geometry polG = this.computeGeo(latMin, lonMin, latMax,
							lonMax);

					float dist[] = new float[ng];
					for (int i = 0; i < dist.length; i++)
						dist[i] = Float.MAX_VALUE;

					int tileNN[] = new int[ng];
					int pixNN[] = new int[ng];

					String hStr = (hi > 9) ? ("" + hi) : ("0" + hi);
					String vStr = (vi > 9) ? ("" + vi) : ("0" + vi);

					System.out.println("h" + hStr + "v" + vStr + " box: "
							+ this.polT);
					System.out.println(" box grid: " + polG);
					String outfile = this.outFolder + "/" + "h" + hStr + "v"
							+ vStr + "." + this.resName + diStr + ".txt";

					if (this.polT != null
							&& this.polT.distance(polG) <= 0.0000001) {
						appendString(outfile, "", false);
						System.out.println("writing " + outfile);
						int h = this.hTile;
						int w = this.wTile;

						List<Integer> resp = null;
						Geometry inter = null;

						String lineString = "";
						for (int y = 0, p = 0; y < h; y++)
							for (int x = 0; x < w; x++, p++) {

								Geometry polpixt = sin2latLonRect(hi, vi, x, y);

								String line = p + ":";

								if (polpixt.getArea() > 0) {

									resp = idxG.query(polpixt
											.getEnvelopeInternal());

									for (int k = 0; k < resp.size(); k++) {

										int yG = resp.get(k) / wg;
										int xG = resp.get(k) - (yG * wg);

										int pG = (xG * hg) + yG;

										if (xG >= 0 && xG < wg && yG >= 0
												&& yG < hg) {

											inter = polpixt
													.intersection(this.geoIdx
															.get(resp.get(k)));

											double area1 = this.geoIdx.get(
													resp.get(k)).getArea();
											double area2 = polpixt.getArea();
											double area3 = inter.getArea();

											double areaFra = 0.0f;

											if (area3 > 0.0)
												areaFra = Math.max(area3
														/ area1, area3 / area2);

											if (areaFra > 0.0) {
												double dis = this
														.geoDistance(
																this.geoIdx
																		.get(resp
																				.get(k))
																		.getCentroid()
																		.getCoordinate(),
																polpixt.getCentroid()
																		.getCoordinate());

												if (line.endsWith(":"))
													line += pG + "," + areaFra
															+ "," + dis;
												else
													line += ";" + pG + ","
															+ areaFra + ","
															+ dis;

												if (dis < dist[pG]) {
													dist[pG] = (float) dis;
													tileNN[pG] = (vi * 36) + hi;
													pixNN[pG] = p;
												}

											}

										}

									}

								}

								if (lineString.length() < 500
										|| (x == (w - 1) && y == (h - 1))) {
									if (x < w - 1 || y < h - 1)
										lineString += line + "\n";
									else
										lineString += line;
									appendString(outfile, lineString, true);
									lineString = "";
								} else
									lineString += line + "\n";
							}

					}

				}

	}

	/**
	 * create a bounding box of a tile pixel
	 * @param hi tile horizontal index
	 * @param vi tile vertical index
	 * @param x sample  
	 * @param y line 
	 * @return geometry
	 */
	private Geometry sin2latLonRect(int hi, int vi, int x, int y) {
		double xy[] = SIN2XY(hi, vi, x+0.5, y+0.5);
        
		double latlon[][] = new double[4][];
		latlon[0] = xy2LatLon(xy[0], xy[1]);
		latlon[1] = xy2LatLon(xy[0]+res, xy[1]);
		latlon[2] = xy2LatLon(xy[0]+res, xy[1]-res);
		latlon[3] = xy2LatLon(xy[0], xy[1]-res);

		Geometry out = null;

		for (int i = 0; i < 4; i++) {
			Point point = new GeometryFactory().createPoint(new Coordinate(
					latlon[i][0], latlon[i][1]));
			// System.out.println("point"+i+": "+point);
			if (out == null)
				out = (Geometry) point.clone();
			else
				out = (Geometry) (out.union(point)).clone();

		}

		if (out != null)
			out = (Geometry) (out.getEnvelope()).clone();

		return out;
	}

	
	/**
	 * write txt file
	 * @param outfile
	 * @param line 
	 * @param mode true:append, false: write new file
	 */
	private void appendString(String outfile, String line, boolean mode) {

		// System.out.println("line: "+line);
		try {
			fWriter = new FileWriter(outfile, mode);

			fWriter.append(line);
			fWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * create boundinf box for valid area(-90<lat<90, -180<lon<180), lon<-180, lot>180, lat>90 and lot<-90 
	 */
	private void computeOutGeos() {

		this.geoOutUP = this.createPolygonNormal(90, -360, 180, 360);
		this.geoOutDown = this.createPolygonNormal(-180, -360, -90, 360);

		this.geoOutLeft = this.createPolygonNormal(-180, -360, 180, -180);
		this.geoOutRight = this.createPolygonNormal(-180, 180, 180, 360);

		this.validArea = this.createPolygonNormal(-90, -180, 90, 180);
	}

	/**
	 * index pixel of mosaic grid
	 * @param lat0 minLat
	 * @param lon0 minLon
	 * @param binLat
	 * @param binLon
	 * @param h height
	 * @param w width
	 * @param centre true:pixelcenter, false:upper_left_corner
	 * @return STRtree of moasic grid's pixels
	 */
	private STRtree idxGrid(float lat0, float lon0, float binLat, float binLon,
			int h, int w, boolean centre) {
		System.out.println("indexing...");
		// Draw draw =new Draw();
		STRtree idx = new STRtree();

		Geometry geoPN = null;
		Geometry geoPS = null;

		if (this.polarRegion)
			for (int i = 0; i < w; i++) {
				int j = 0;
				Geometry geos = null;
				if (centre)
					geos = this.computeGeo(lat0 - ((binLat * (j - 0.5f))), lon0
							+ (binLon * (i - 0.5f)), lat0
							- ((binLat * (j + 0.5f))), lon0
							+ (binLon * (i + 0.5f)));
				else
					geos = this.computeGeo(lat0 - ((binLat * (j))), lon0
							+ (binLon * (i)), lat0 - ((binLat * (j + 1.0f))),
							lon0 + (binLon * (i + 1.0f)));

				if (geoPN != null)
					geoPN = (Geometry) geos.union((Geometry) geoPN.clone())
							.clone();
				else
					geoPN = (Geometry) geos.clone();

			}

		if (this.polarRegion)
			for (int i = 0; i < w; i++) {
				int j = h - 1;
				Geometry geos = null;
				if (centre)
					geos = this.computeGeo(lat0 - ((binLat * (j - 0.5f))), lon0
							+ (binLon * (i - 0.5f)), lat0
							- ((binLat * (j + 0.5f))), lon0
							+ (binLon * (i + 0.5f)));
				else
					geos = this.computeGeo(lat0 - ((binLat * (j))), lon0
							+ (binLon * (i)), lat0 - ((binLat * (j + 1.0f))),
							lon0 + (binLon * (i + 1.0f)));

				if (geoPS != null)
					geoPS = (Geometry) geos.union((Geometry) geoPS.clone())
							.clone();
				else
					geoPS = (Geometry) geos.clone();

			}

		for (int j = 0, p = 0; j < h; j++)
			for (int i = 0; i < w; i++, p++) {

				if (j % 10 == 0 && i == 0)
					System.out.println("idx grid:: " + j + "/" + h);
				Geometry geos = null;
				if ((j != 0 && j != h - 1) || (!this.polarRegion)) {
					if (centre)
						geos = this.computeGeo(lat0 - ((binLat * (j - 0.5f))),
								lon0 + (binLon * (i - 0.5f)), lat0
										- ((binLat * (j + 0.5f))), lon0
										+ (binLon * (i + 0.5f)));
					else
						geos = this.computeGeo(lat0 - ((binLat * (j))), lon0
								+ (binLon * (i)), lat0
								- ((binLat * (j + 1.0f))), lon0
								+ (binLon * (i + 1.0f)));
				}
				if (j == 0 && this.polarRegion)
					geos = (Geometry) geoPN.clone();
				if (j == h - 1 && this.polarRegion)
					geos = (Geometry) geoPS.clone();

				if (geos.getArea() > 0)
					this.geoIdx.add(p, geos);
				for (int k = 0; k < geos.getNumGeometries(); k++)
					if (geos.getGeometryN(k).getArea() > 0) {
						idx.insert(geos.getGeometryN(k).getEnvelopeInternal(),
								p);

					}

			}

		System.out.println("there are " + idx.size() + " envelops");
		System.out.println("end indexing.");

		return idx;
	}

	/**
	 * create bounding box geometry from min/max of lat/lon 
	 * @param latMin
	 * @param lonMin
	 * @param latMax
	 * @param lonMax
	 * @return
	 */
	private Polygon createPolygonNormal(float latMin, float lonMin,
			float latMax, float lonMax) {
		Coordinate coords[] = new Coordinate[5];
		coords[0] = new Coordinate(latMin, lonMin);
		coords[1] = new Coordinate(latMax, lonMin);
		coords[2] = new Coordinate(latMax, lonMax);
		coords[3] = new Coordinate(latMin, lonMax);
		coords[4] = new Coordinate(latMin, lonMin);
		Polygon pol = new GeometryFactory().createPolygon(
				new GeometryFactory().createLinearRing(coords), null);
		return (Polygon) pol.clone();
	}

	/**
	 * create bonding box geometry of a tile
	 * @param h tile h-index
	 * @param v tile v-index
	 * @return
	 */
	private Geometry readtilebox(int h, int v) {

		Geometry out = null;
		double latMin = Double.NaN;
		double lonMin = Double.NaN;
		double latMax = Double.NaN;
		double lonMax = Double.NaN;

		boolean exist = false;
		for (int x = 0; x <= this.wTile; x += this.wTile)
			for (int y = 0; y <= this.hTile; y++) {
				double latlon[] = this.SIN2LatLon(h, v, x+0.5, y+0.5);
				if (Math.abs(latlon[0]) < 90.0 && Math.abs(latlon[1]) < 180.0) {

					if (latlon[0] <= latMin || Double.isNaN(latMin))
						latMin = latlon[0];
					if (latlon[1] <= lonMin || Double.isNaN(lonMin))
						lonMin = latlon[1];

					if (latlon[0] >= latMax || Double.isNaN(latMax))
						latMax = latlon[0];
					if (latlon[1] >= lonMax || Double.isNaN(lonMax))
						lonMax = latlon[1];
					exist = true;
				}
			}
		if (exist) {
			Coordinate[] coords = new Coordinate[4];
			coords[0] = new Coordinate(latMin, lonMin);
			coords[1] = new Coordinate(latMin, lonMax);
			coords[2] = new Coordinate(latMax, lonMax);
			coords[3] = new Coordinate(latMax, lonMin);

			Geometry multipoint = new GeometryFactory()
					.createMultiPoint(coords);

			out = multipoint.getEnvelope();
			return (Geometry) out.clone();
		} else
			return null;

	}

	/**
	 * Geographical distance between to coordinates (la/lon WGS84)
	 * @param coordA
	 * @param coordB
	 * @return
	 */
	private double geoDistance(Coordinate coordA, Coordinate coordB) {
		double dist = -1;
		try {

			dist = JTS.orthodromicDistance(new Coordinate(coordA.y, coordA.x),
					new Coordinate(coordB.y, coordB.x),
					DefaultGeographicCRS.WGS84);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return dist / 1000.0;
	}

	/**
	 * Recompute the correct bounding box that deals with lat>90, lon<-90, lon>180 and lon<-180 
	 * @param latmin
	 * @param lonmin
	 * @param latmax
	 * @param lonmax
	 * @return
	 */
	Geometry computeGeo(float latmin, float lonmin, float latmax, float lonmax) {
		Geometry inGeo = this.createPolygonNormal(latmin, lonmin, latmax,
				lonmax);

		Geometry geoUp = transGeo(inGeo, this.geoOutUP, 0, Float.NaN, 90);
		if (geoUp != null) {

			inGeo = (Geometry) inGeo.union(geoUp).clone();
		}

		Geometry geoDown = transGeo(inGeo, this.geoOutDown, 0, -90, Float.NaN);
		if (geoDown != null)
			inGeo = (Geometry) inGeo.union(geoDown).clone();

		Geometry geoLeft = transGeo(inGeo, this.geoOutLeft, 1, -180f, Float.NaN);
		if (geoLeft != null)
			inGeo = (Geometry) inGeo.union(geoLeft).clone();

		Geometry geoRight = transGeo(inGeo, this.geoOutRight, 1, Float.NaN,
				180f);
		if (geoRight != null)
			inGeo = (Geometry) inGeo.union(geoRight).clone();

		return (Geometry) inGeo.intersection(this.validArea).clone();
	}

	/**
	 * Recompute coordiantes of geometry that has lat>90, lon<-90, lon>180 or lon<-180 
	 * @param inGeo0 target geometry
	 * @param geoCompare0 refernce geometry
	 * @param axis 0:lat, 1:lon 
	 * @param min 
	 * @param max
	 * @return
	 */
	private Geometry transGeo(Geometry inGeo0, Geometry geoCompare0, int axis,
			float min, float max) {
		Geometry inGeo = (Geometry) inGeo0.clone();
		Geometry geoCompare = (Geometry) geoCompare0.clone();

		double e = 0.00000001;
		Coordinate coords[] = null;
		if (inGeo.distance(geoCompare) == 0) {
			Geometry inter = inGeo.intersection(geoCompare);
			if (inter != null && (!inter.isEmpty())
					&& inter.getArea() > 0.000000001) {
				// System.out.println("distance: "+inGeo.distance(geoCompare));
				coords = (inGeo.getBoundary()).intersection(geoCompare)
						.getCoordinates();
				for (int i = 0; i < coords.length; i++) {
					if (axis == 0) {
						if (coords[i].x > max - e)
							coords[i].setCoordinate(new Coordinate(
									180 - coords[i].x, coords[i].y));
						if (coords[i].x < min + e)
							coords[i].setCoordinate(new Coordinate(-180
									- coords[i].x, coords[i].y));
					}

					if (axis == 1) {
						if (coords[i].y <= min + e)
							coords[i].setCoordinate(new Coordinate(coords[i].x,
									360 + coords[i].y));
						if (coords[i].y >= max - e)
							coords[i].setCoordinate(new Coordinate(coords[i].x,
									coords[i].y - 360));
					}
				}
			}
		}

		Geometry outGeo = null;

		if (coords != null) {

			outGeo = new GeometryFactory().createMultiPoint(coords)
					.convexHull();

		}
		return outGeo;
	}

	/**
	 * convert tile, sample, line to sinXY
	 * @param h
	 * @param v
	 * @param s
	 * @param l
	 * @return [0]:x, [1]:y
	 */
	public double[] SIN2XY(double h, double v, double s, double l) {
		double x=((s*res)+(h*tileW)-earthW);
		double y= (((18-v)*tileH)-earthH-(l*res));
		
		return new double[] { x, y };

	}

	/**
	 * convert sin(x,y) to (lat, lon)
	 * @param x
	 * @param y
	 * @return [0]:lat, [1]:lon
	 */
	public double[] xy2LatLon(double x, double y) {

		double lat = Double.NaN;
		double lon = Double.NaN;

		try {

			if (stroPolCRS == null) {
				stroPolCRS = CRS.parseWKT(wkt);
				transform = CRS.findMathTransform(stroPolCRS,
						DefaultGeographicCRS.WGS84, true);
			}

			DirectPosition srcPos = new DirectPosition2D(x, y);
			DirectPosition distPos = new DirectPosition2D(Float.NaN, Float.NaN);
			transform.transform(srcPos, distPos);
			lat = distPos.getCoordinate()[1];
			lon = distPos.getCoordinate()[0];

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new double[] { lat, lon };
	}

	/**
	 * convert tile, sample, line to lat/lon
	 * @param h
	 * @param v
	 * @param s
	 * @param l
	 * @return [0]:lat, [1]:lon
	 */
	public double[] SIN2LatLon(double h, double v, double s, double l) {

		double x = ((s * res) + (h * tileW) - earthW) + (res / 2.0);
		double y = (((18 - v) * tileH) - earthH - (l * res)) - (res / 2.0);

		double lat = Double.NaN;
		double lon = Double.NaN;

		try {

			if (stroPolCRS == null) {
				stroPolCRS = CRS.parseWKT(wkt);
				transform = CRS.findMathTransform(stroPolCRS,
						DefaultGeographicCRS.WGS84, true);
			}

			DirectPosition srcPos = new DirectPosition2D(x, y);
			DirectPosition distPos = new DirectPosition2D(Float.NaN, Float.NaN);
			transform.transform(srcPos, distPos);
			lon = distPos.getCoordinate()[0];
			lat = distPos.getCoordinate()[1];

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new double[] { lat, lon };
	}

	public static void main(String[] args) {

		/*
		  float resDegX=0.05f;//Float.parseFloat(args[0]);//{36,72,192, 180};
		  float resDegY=0.05f;//Float.parseFloat(args[1]);//{24,48,96,90};
		  String resName="005";//args[2];//{"tm2", "tm3fg", "tm3vfg","hires"}; 
		  int div=4;
		  
		  boolean polRegion=false; 
		  String outfolder="/unsafe/said/tmp/testCodes/match/"+resName+"/"; 
		  String tile="h18v03"; 
		  boolean pixCentre=true;
		  
		  float latMin=-90;
		  float latMax=90; 
		  float lonMin=-180; 
		  float lonMax=180;
		  */
		
		float resDegX = Float.parseFloat(args[0]);
		float resDegY = Float.parseFloat(args[1]);
		String resName = args[2];
		int div = Integer.parseInt(args[3]);

		boolean polRegion = Boolean.parseBoolean(args[4]);
		String outfolder = args[5];
		String tile = args[6];
		boolean pixCentre = Boolean.parseBoolean(args[7]);

		float latMin = -90;
		float latMax = 90;
		float lonMin = 180;
		float lonMax = -180;

		if (args.length == 12) {
			latMin = Float.parseFloat(args[8]);
			latMax = Float.parseFloat(args[9]);
			lonMin = Float.parseFloat(args[10]);
			lonMax = Float.parseFloat(args[11]);
		}

		Matching col = new Matching();
		col.resDegX = resDegX;
		col.resDegY = resDegY;
		col.resName = resName;
		col.div = div;
		col.polarRegion = polRegion;

		col.outFolder = outfolder;

		col.tile = tile;

		col.pixCentre = pixCentre;
		col.latMin = latMin;
		col.latMax = latMax;
		col.lonMin = lonMin;
		col.lonMax = lonMax;
		col.compute();

	}
}
