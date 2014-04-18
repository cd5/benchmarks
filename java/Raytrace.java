/*
 * This file contains definitions for a simple raytracer.
 * Copyright Callum and Tony Garnock-Jones, 2008.
 * Copyright Jan Ziak <0xe2.0x9a.0x9b@gmail.com>, 2014.
 * This file may be freely redistributed under the MIT license,
 * http://www.opensource.org/licenses/mit-license.php
 *
 * Based on http://www.lshift.net/blog/2008/10/29/toy-raytracer-in-python
 */

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class Raytrace {
	static final double EPSILON = 0.00001;

	static class Vector {
		double x, y, z;

		Vector(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public String toString() {
			return "Vector(" + x + ", " + y + ", " + z + ")";
		}

		double magnitude() {
			return Math.sqrt(this.dot(this));
		}

		Vector add(Vector other) {
			if (other instanceof Point) {
				return new Point(x + other.x, y + other.y, z + other.z);
			} else {
				return new Vector(x + other.x, y + other.y, z + other.z);
			}
		}

		Vector sub(Vector other) {
			other.mustBeVector();
			return new Vector(x - other.x, y - other.y, z - other.z);
		}

		Vector scale(double factor) {
			return new Vector(factor * x, factor * y, factor * z);
		}

		double dot(Vector other) {
			other.mustBeVector();
			return (x * other.x) + (y * other.y) + (z * other.z);
		}

		Vector cross(Vector other) {
			other.mustBeVector();
			return new Vector(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
		}

		Vector normalized() {
			return scale(1.0 / magnitude());
		}

		Vector negated() {
			return scale(-1);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Vector) {
				Vector v = (Vector) o;
				return (x == v.x) && (y == v.y) && (z == v.z);
			}
			return false;
		}

		boolean isVector() {
			return true;
		}

		boolean isPoint() {
			return false;
		}

		void mustBeVector() {
		}

		void mustBePoint() {
			throw new Error("Vectors are not points!");
		}

		Vector reflectThrough(Vector normal) {
			Vector d = normal.scale(this.dot(normal));
			return this.sub(d.scale(2));
		}

		static Vector ZERO = new Vector(0, 0, 0);
		static Vector RIGHT = new Vector(1, 0, 0);
		static Vector UP = new Vector(0, 1, 0);
		static Vector OUT = new Vector(0, 0, 1);

		static {
			assert (RIGHT.reflectThrough(UP).equals(RIGHT));
			assert (new Vector(-1, -1, 0).reflectThrough(UP).equals(new Vector(-1, 1, 0)));
		}
	}

	static class Point extends Vector {
		Point(double x, double y, double z) {
			super(x, y, z);
		}

		@Override
		public String toString() {
			return "Point(" + x + ", " + y + ", " + z + ")";
		}

		@Override
		Point add(Vector other) {
			other.mustBeVector();
			return new Point(x + other.x, y + other.y, z + other.z);
		}

		@Override
		Vector sub(Vector other) {
			if (other instanceof Point) {
				return new Vector(x - other.x, y - other.y, z - other.z);
			} else {
				return new Point(x - other.x, y - other.y, z - other.z);
			}
		}

		@Override
		boolean isVector() {
			return false;
		}

		@Override
		boolean isPoint() {
			return true;
		}

		@Override
		void mustBeVector() {
			throw new Error("Points are not vectors!");
		}

		@Override
		void mustBePoint() {
		}

		static Point ZERO = new Point(0, 0, 0);
	}

	static abstract class SceneObject {
		abstract double intersectionTime(Ray ray);

		abstract Vector normalAt(Point p);
	}

	static class Sphere extends SceneObject {
		Point centre;
		double radius;

		Sphere(Point centre, double radius) {
			centre.mustBePoint();
			this.centre = centre;
			this.radius = radius;
		}

		@Override
		public String toString() {
			return "Sphere(" + centre + ", " + radius + ")";
		}

		@Override
		double intersectionTime(Ray ray) {
			Vector cp = centre.sub(ray.point);
			double v = cp.dot(ray.vector);
			double discriminant = (radius * radius) - (cp.dot(cp) - v * v);
			if (discriminant < 0) {
				return Double.NaN;
			} else {
				return v - Math.sqrt(discriminant);
			}
		}

		@Override
		Vector normalAt(Point p) {
			return p.sub(centre).normalized();
		}
	}

	static class Halfspace extends SceneObject {
		Point point;
		Vector normal;

		Halfspace(Point point, Vector normal) {
			this.point = point;
			this.normal = normal.normalized();
		}

		@Override
		public String toString() {
			return "Halfspace(" + point + ", " + normal + ")";
		}

		@Override
		double intersectionTime(Ray ray) {
			double v = ray.vector.dot(normal);
			return 1 / -v;
		}

		@Override
		Vector normalAt(Point p) {
			return normal;
		}
	}

	static class Ray {
		Point point;
		Vector vector;

		Ray(Point point, Vector vector) {
			this.point = point;
			this.vector = vector.normalized();
		}

		@Override
		public String toString() {
			return "Ray(" + point + ", " + vector + ")";
		}

		Point pointAtTime(double t) {
			return point.add(vector.scale(t));
		}
	}

	static Vector a = new Vector(3, 4, 12);
	static Vector b = new Vector(1, 1, 1);

	static class PpmCanvas {
		int width, height;
		String filenameBase;
		byte[] bytes;

		PpmCanvas(int width, int height, String filenameBase) {
			this.width = width;
			this.height = height;
			this.filenameBase = filenameBase;
			bytes = new byte[width * height * 3];
			for (int i = 0; i < width * height; i++) {
				bytes[i * 3 + 2] = (byte) 255;
			}
		}

		void plot(int x, int y, Color c) {
			int i = ((height - y - 1) * width + x) * 3;
			bytes[i + 0] = (byte) Math.max(0, Math.min(255, (int) (c.r * 255)));
			bytes[i + 1] = (byte) Math.max(0, Math.min(255, (int) (c.g * 255)));
			bytes[i + 2] = (byte) Math.max(0, Math.min(255, (int) (c.b * 255)));
		}

		void save() throws IOException {
			java.io.FileOutputStream w = new java.io.FileOutputStream(filenameBase + ".ppm");
			byte[] header = ("P6\n" + width + " " + height + "\n255\n").getBytes();
			w.write(header, 0, header.length);
			w.write(bytes, 0, bytes.length);
			w.close();
		}
	}

	static class Intersection {
		SceneObject o;
		double t;
		Surface s;

		Intersection(SceneObject o, double t, Surface s) {
			this.o = o;
			this.t = t;
			this.s = s;
		}
	}

	static Intersection firstIntersection(List<Intersection> intersections) {
		Intersection result = null;
		for (Intersection i : intersections) {
			double candidateT = i.t;
			if (candidateT != Double.NaN && candidateT > -EPSILON) {
				if (result == null || candidateT < result.t) {
					result = i;
				}
			}
		}
		return result;
	}

	static class Color {
		double r, g, b;

		Color(double r, double g, double b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		@Override
		public String toString() {
			return "Color(" + r + ", " + g + ", " + b + ")";
		}
	}

	static class Scene {
		static class Obj {
			SceneObject object;
			Surface surface;

			Obj(SceneObject object, Surface surface) {
				this.object = object;
				this.surface = surface;
			}
		}

		List<Obj> objects = new ArrayList<>();
		List<Point> lightPoints = new ArrayList<>();
		Point position = new Point(0, 1.8, 10);
		Point lookingAt = Point.ZERO;
		double fieldOfView = 45;
		int recursionDepth = 0;

		void moveTo(Point p) {
			position = p;
		}

		void lookAt(Point p) {
			lookingAt = p;
		}

		void addObject(SceneObject object, Surface surface) {
			objects.add(new Obj(object, surface));
		}

		void addLight(Point p) {
			lightPoints.add(p);
		}

		void render(PpmCanvas canvas) throws IOException {
			if (false) {
				System.out.println("Computing field of view");
			}
			double fovRadians = Math.PI * (fieldOfView / 2.0) / 180.0;
			double halfWidth = Math.tan(fovRadians);
			double halfHeight = 0.75 * halfWidth;
			double width = halfWidth * 2;
			double height = halfHeight * 2;
			double pixelWidth = width / (canvas.width - 1);
			double pixelHeight = height / (canvas.height - 1);

			Ray eye = new Ray(this.position, this.lookingAt.sub(this.position));
			Vector vpRight = eye.vector.cross(Vector.UP).normalized();
			Vector vpUp = vpRight.cross(eye.vector).normalized();

			System.out.println("Looping over pixels");
			float previousfraction = 0;
			for (int y = 0; y < canvas.height; y++) {
				float currentfraction = (float) y / canvas.height;
				if (currentfraction - previousfraction > 0.05) {
					if (false) {
						canvas.save();
					}
					System.out.println((int) (currentfraction * 100) + "% complete");
					previousfraction = currentfraction;
				}
				for (int x = 0; x < canvas.width; x++) {
					Vector xcomp = vpRight.scale(x * pixelWidth - halfWidth);
					Vector ycomp = vpUp.scale(y * pixelHeight - halfHeight);
					Ray ray = new Ray(eye.point, eye.vector.add(xcomp).add(ycomp));
					Color color = rayColor(ray);
					canvas.plot(x, y, color);
				}
			}

			canvas.save();
			System.out.println("Complete.");
		}

		Color rayColor(Ray ray) {
			if (recursionDepth > 3) {
				return new Color(0, 0, 0);
			}
			recursionDepth++;
			List<Intersection> intersections = new ArrayList<>();
			for (Obj o : objects) {
				intersections.add(new Intersection(o.object, o.object.intersectionTime(ray), o.surface));
			}
			Intersection i = firstIntersection(intersections);
			if (i == null) {
				recursionDepth--;
				return new Color(0, 0, 0); // the background color
			} else {
				Point p = ray.pointAtTime(i.t);
				recursionDepth--;
				return i.s.colorAt(this, ray, p, i.o.normalAt(p));
			}
		}

		boolean lightIsVisible(Vector l, Point p) {
			for (Obj o : objects) {
				double t = o.object.intersectionTime(new Ray(p, l.sub(p)));
				if (t != Double.NaN && t > EPSILON) {
					return false;
				}
			}
			return true;
		}

		List<Point> visibleLights(Point p) {
			List<Point> result = new ArrayList<>();
			for (Point l : lightPoints) {
				if (lightIsVisible(l, p)) {
					result.add(l);
				}
			}
			return result;
		}
	}

	static Color addColors(Color a, double scale, Color b) {
		return new Color(a.r + scale * b.r, a.g + scale * b.g, a.b + scale * b.b);
	}

	static abstract class Surface {
		abstract Color colorAt(Scene scene, Ray ray, Point p, Vector normal);
	}

	static class SimpleSurface extends Surface {
		Color baseColor = new Color(1, 1, 1);
		double specularCoefficient = 0.2;
		double lambertCoefficient = 0.6;
		double ambientCoefficient = 1.0 - specularCoefficient - lambertCoefficient;

		SimpleSurface setBaseColor(Color c) {
			baseColor = c;
			return this;
		}

		Color baseColorAt(Point p) {
			return baseColor;
		}

		@Override
		Color colorAt(Scene scene, Ray ray, Point p, Vector normal) {
			Color b = baseColorAt(p);

			Color c = new Color(0, 0, 0);
			if (specularCoefficient > 0) {
				Ray reflectedRay = new Ray(p, ray.vector.reflectThrough(normal));
				if (false) {
					System.out.println(p + " " + normal + " " + ray.vector + " " + reflectedRay.vector);
				}
				Color reflectedColor = scene.rayColor(reflectedRay);
				c = addColors(c, specularCoefficient, reflectedColor);
			}

			if (lambertCoefficient > 0) {
				double lambertAmount = 0;
				for (Point lightPoint : scene.visibleLights(p)) {
					double contribution = lightPoint.sub(p).normalized().dot(normal);
					if (contribution > 0) {
						lambertAmount += contribution;
					}
				}
				lambertAmount = Math.min(1, lambertAmount);
				c = addColors(c, lambertCoefficient * lambertAmount, b);
			}

			if (ambientCoefficient > 0) {
				c = addColors(c, ambientCoefficient, b);
			}

			return c;
		}
	}

	static class CheckerboardSurface extends SimpleSurface {
		Color otherColor = new Color(0, 0, 0);
		double checkSize = 1;

		@Override
		Color baseColorAt(Point p) {
			Vector v = p.sub(Point.ZERO);
			v = v.scale(1.0 / checkSize);
			if (((int) (Math.abs(v.x) + 0.5) + (int) (Math.abs(v.y) + 0.5) + (int) (Math.abs(v.z) + 0.5)) % 2 == 1) {
				return otherColor;
			} else {
				return baseColor;
			}
		}
	}

	public static void main(String[] args) throws IOException {
		PpmCanvas c = new PpmCanvas(320, 240, "raytrace");
		Scene s = new Scene();
		s.addLight(new Point(30, 30, 10));
		s.addLight(new Point(-10, 100, 30));
		s.lookAt(new Point(0, 3, 0));
		s.addObject(new Sphere(new Point(1, 3, -10), 2), new SimpleSurface().setBaseColor(new Color(1, 1, 0)));
		for (int y = 0; y < 6; y++) {
			s.addObject(
				new Sphere(new Point(-3 - y * 0.4, 2.3, -5), 0.4),
				new SimpleSurface().setBaseColor(new Color(y / 6.0, 1 - y / 6.0, 0.5))
			);
		}
		s.addObject(new Halfspace(new Point(0, 0, 0), Vector.UP), new CheckerboardSurface());
		s.render(c);
	}
}
