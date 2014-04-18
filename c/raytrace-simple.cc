/*
 * This file contains definitions for a simple raytracer.
 * Copyright Callum and Tony Garnock-Jones, 2008.
 * Copyright Jan Ziak <0xe2.0x9a.0x9b@gmail.com>, 2014.
 * This file may be freely redistributed under the MIT license,
 * http://www.opensource.org/licenses/mit-license.php
 *
 * Based on http://www.lshift.net/blog/2008/10/29/toy-raytracer-in-python
 */

#include <fcntl.h>
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <algorithm>
#include <vector>

const double EPSILON = 0.00001;

struct Point;
struct Ray;
struct Scene;
struct Surface;

struct Vector {
	double x, y, z;

	Vector() {
		x = y = z = 0;
	}

	Vector(double x, double y, double z) {
		this->x = x;
		this->y = y;
		this->z = z;
	}

	double magnitude() const {
		return sqrt(this->dot(*this));
	}

	Vector add(Point other) const;

	Vector add(Vector other) const {
		return Vector(x + other.x, y + other.y, z + other.z);
	}

	Vector sub(Vector other) const {
		return Vector(x - other.x, y - other.y, z - other.z);
	}

	Vector scale(double factor) const {
		return Vector(factor * x, factor * y, factor * z);
	}

	double dot(Vector other) const {
		return (x * other.x) + (y * other.y) + (z * other.z);
	}

	Vector cross(Vector other) const {
		return Vector(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
	}

	Vector normalized() const {
		return scale(1.0 / magnitude());
	}

	Vector negated() const {
		return scale(-1);
	}

	Vector reflectThrough(Vector normal) const {
		Vector d = normal.scale(this->dot(normal));
		return this->sub(d.scale(2));
	}

	static Vector ZERO;
	static Vector RIGHT;
	static Vector UP;
	static Vector OUT;
};

struct Point : Vector {
	Point() {
	}

	Point(double x, double y, double z) : Vector(x, y, z) {
	}

	Point add(Vector other) const {
		return Point(x + other.x, y + other.y, z + other.z);
	}

	Vector sub(Point other) const {
		return Vector(x - other.x, y - other.y, z - other.z);
	}

	Point sub(Vector other) const {
		return Point(x - other.x, y - other.y, z - other.z);
	}

	static Point ZERO;
};

Point Point::ZERO = Point(0,0,0);

Vector Vector::add(Point other) const {
	return Point(x + other.x, y + other.y, z + other.z);
}

Vector Vector::ZERO = Vector(0,0,0);
Vector Vector::RIGHT = Vector(1,0,0);
Vector Vector::UP = Vector(0,1,0);
Vector Vector::OUT = Vector(0,0,1);

struct Ray {
	Point point;
	Vector vector;

	Ray(Point _point, Vector _vector) : point(_point), vector(_vector.normalized()) {
	}

	Point pointAtTime(double t) {
		return point.add(vector.scale(t));
	}
};

struct SceneObject {
	virtual double intersectionTime(const Ray &ray) = 0;
	virtual Vector normalAt(Point p) = 0;
};

struct Sphere : SceneObject {
	Point centre;
	double radius;

	Sphere(Point _centre, double radius) : centre(_centre) {
		this->radius = radius;
	}

	double intersectionTime(const Ray &ray) {
		Vector cp = centre.sub(ray.point);
		double v = cp.dot(ray.vector);
		double discriminant = (radius * radius) - (cp.dot(cp) - v*v);
		if(discriminant < 0) {
			return NAN;
		} else {
			return v - sqrt(discriminant);
		}
	}

	Vector normalAt(Point p) {
		return p.sub(centre).normalized();
	}
};

struct Halfspace : SceneObject {
	Point point;
	Vector normal;

	Halfspace(Point _point, Vector _normal) : point(_point), normal(_normal.normalized()) {
	}

	double intersectionTime(const Ray &ray) {
		double v = ray.vector.dot(normal);
		return 1 / -v;
	}

	Vector normalAt(Point p) {
		return normal;
	}
};

Vector a(3,4,12);
Vector b(1,1,1);

struct Color {
	double r, g, b;

	Color() {
	}

	Color(double r, double g, double b) {
		this->r = r;
		this->g = g;
		this->b = b;
	}
};

struct PpmCanvas {
	int width, height;
	const char *filenameBase;
	uint8_t *bytes;

	PpmCanvas(int width, int height, const char *filenameBase) {
		this->width = width;
		this->height = height;
		this->filenameBase = filenameBase;
		bytes = new uint8_t[width * height * 3];
		for(int i=0; i<width*height; i++) {
			bytes[i * 3 + 2] = 255;
		}
	}

	~PpmCanvas() {
		delete[] bytes;
	}

	int clamp(int c) {
		if(c < 0) return 0;
		if(c > 255) return 255;
		return c;
	}

	void plot(int x, int y, Color c) {
		int i = ((height - y - 1) * width + x) * 3;
		bytes[i+0] = (uint8_t)clamp(c.r * 255);
		bytes[i+1] = (uint8_t)clamp(c.g * 255);
		bytes[i+2] = (uint8_t)clamp(c.b * 255);
	}

	void save() {
		char name[100];
		strcpy(name, filenameBase);
		strcat(name, ".ppm");
		unlink(name);
		int fd = creat(name, 0444);
		if(fd == -1) { perror(NULL); exit(1); }
		char header[100];
		snprintf(header, sizeof(header), "P6\n%d %d\n255\n", width, height);
		if(write(fd, header, strlen(header)) != strlen(header)) { perror(NULL); exit(1); }
		if(write(fd, bytes, 3*width*height) != 3*width*height) { perror(NULL); exit(1); }
		if(close(fd) != 0) { perror(NULL); exit(1); }
	}
};

struct Intersection {
	SceneObject *o;
	double t;
	Surface *s;

	Intersection(SceneObject *o, double t, Surface *s) {
		this->o = o;
		this->t = t;
		this->s = s;
	}
};

Intersection* firstIntersection(std::vector<Intersection> &intersections) {
	Intersection *result = NULL;
	for(size_t a=0; a<intersections.size(); a++) {
		Intersection *i = &intersections[a];
		double candidateT = i->t;
   		if(candidateT != NAN && candidateT > -EPSILON) {
			if(result == NULL || candidateT < result->t) {
				result = i;
			}
		}
	}
	return result;
};

struct Surface {
	virtual Color colorAt(Scene *scene, const Ray &ray, Point p, Vector normal) = 0;
};

struct Scene {
	struct Obj {
		SceneObject *object;
		Surface *surface;
		Obj(SceneObject *object, Surface *surface) {
			this->object = object;
			this->surface = surface;
		}
	};

	std::vector<Obj> objects;
	std::vector<Point> lightPoints;
	Point position;
	Point lookingAt;
	double fieldOfView;
	int recursionDepth;

	Scene() {
		position = Point(0, 1.8, 10);
		lookingAt = Point::ZERO;
		fieldOfView = 45;
		recursionDepth = 0;
	}

	void moveTo(Point p) {
		position = p;
	}

	void lookAt(Point p) {
		lookingAt = p;
	}

	void addObject(SceneObject *object, Surface *surface) {
		objects.push_back(Obj(object, surface));
	}

	void addLight(Point p) {
		lightPoints.push_back(p);
	}

	void render(PpmCanvas *canvas) {
		if(false) {
			printf("Computing field of view\n");
		}
		double fovRadians = M_PI * (fieldOfView / 2.0) / 180.0;
		double halfWidth = tan(fovRadians);
		double halfHeight = 0.75 * halfWidth;
		double width = halfWidth * 2;
		double height = halfHeight * 2;
		double pixelWidth = width / (canvas->width - 1);
		double pixelHeight = height / (canvas->height - 1);

		Ray eye = Ray(position, lookingAt.sub(position));
		Vector vpRight = eye.vector.cross(Vector::UP).normalized();
		Vector vpUp = vpRight.cross(eye.vector).normalized();

		printf("Looping over pixels\n");
		float previousfraction = 0;
		for(int y=0; y<canvas->height; y++) {
			float currentfraction = (float)y / canvas->height;
			if(currentfraction - previousfraction > 0.05) {
				if(false) {
					canvas->save();
				}
				printf("%d%% complete\n", (int)(currentfraction * 100));
				previousfraction = currentfraction;
			}
			for(int x=0; x<canvas->width; x++) {
				Vector xcomp = vpRight.scale(x * pixelWidth - halfWidth);
				Vector ycomp = vpUp.scale(y * pixelHeight - halfHeight);
				Ray ray = Ray(eye.point, eye.vector.add(xcomp).add(ycomp));
				Color color = rayColor(ray);
				canvas->plot(x, y, color);
			}
		}

		canvas->save();
		printf("Complete.\n");
	}

	Color rayColor(Ray ray) {
		if(recursionDepth > 3) {
			return Color(0,0,0);
		}
		recursionDepth++;
		std::vector<Intersection> intersections;
		for(int i=0; i<objects.size(); i++) {
			Obj &o = objects[i];
			intersections.push_back(Intersection(o.object, o.object->intersectionTime(ray), o.surface));
		}
		Intersection *i = firstIntersection(intersections);
 		if(i == NULL) {
			recursionDepth--;
	  		return Color(0,0,0); // the background color
		} else {
	  		Point p = ray.pointAtTime(i->t);
			recursionDepth--;
			return i->s->colorAt(this, ray, p, i->o->normalAt(p));
		}
	}

	bool lightIsVisible(Vector l, Point p) {
		for(int i=0; i<objects.size(); i++) {
			Obj &o = objects[i];
			double t = o.object->intersectionTime(Ray(p, l.sub(p)));
			if(t != NAN && t > EPSILON) {
				return false;
			}
		}
		return true;
	}

	std::vector<Point> visibleLights(Point p) {
		std::vector<Point> result;
		for(int i=0; i<lightPoints.size(); i++) {
			Point &l = lightPoints[i];
			if(lightIsVisible(l, p)) {
				result.push_back(l);
			}
		}
		return result;
	}
};

Color addColors(Color a, double scale, Color b) {
	return Color(a.r + scale * b.r, a.g + scale * b.g, a.b + scale * b.b);
}

struct SimpleSurface : Surface {
	Color baseColor;
	double specularCoefficient;
	double lambertCoefficient;
	double ambientCoefficient;

	SimpleSurface() {
		baseColor = Color(1, 1, 1);
		specularCoefficient = 0.2;
		lambertCoefficient = 0.6;
		ambientCoefficient = 1.0 - specularCoefficient - lambertCoefficient;
	}

	SimpleSurface* setBaseColor(Color c) {
		baseColor = c;
		return this;
	}

	virtual Color baseColorAt(Point p) {
		return baseColor;
	}

	Color colorAt(Scene *scene, const Ray &ray, Point p, Vector normal) {
		Color b = baseColorAt(p);

		Color c = Color(0,0,0);
		if(specularCoefficient > 0) {
			Ray reflectedRay = Ray(p, ray.vector.reflectThrough(normal));
			Color reflectedColor = scene->rayColor(reflectedRay);
			c = addColors(c, specularCoefficient, reflectedColor);
		}

		if(lambertCoefficient > 0) {
			double lambertAmount = 0;
			std::vector<Point> visibleLights = scene->visibleLights(p);
			for(int i=0; i<visibleLights.size(); i++) {
				Point lightPoint = visibleLights[i];
				double contribution = lightPoint.sub(p).normalized().dot(normal);
				if(contribution > 0) {
					lambertAmount += contribution;
				}
			}
			lambertAmount = std::min(1.0, lambertAmount);
			c = addColors(c, lambertCoefficient * lambertAmount, b);
		}

		if(ambientCoefficient > 0) {
			c = addColors(c, ambientCoefficient, b);
		}

		return c;
	}
};

struct CheckerboardSurface : SimpleSurface {
	Color otherColor;
	double checkSize;

	CheckerboardSurface() {
		otherColor = Color(0,0,0);
		checkSize = 1;
	}

	Color baseColorAt(Point p) {
		Vector v = p.sub(Point::ZERO);
		v = v.scale(1.0 / checkSize);
		if((int(fabs(v.x) + 0.5) + int(fabs(v.y) + 0.5) + int(fabs(v.z) + 0.5)) % 2 == 1) {
			return otherColor;
		} else {
			return baseColor;
		}
	}
};

int main() {
	PpmCanvas *c = new PpmCanvas(320, 240, "raytrace");
	Scene *s = new Scene();
	s->addLight(Point(30, 30, 10));
	s->addLight(Point(-10, 100, 30));
	s->lookAt(Point(0, 3, 0));
	s->addObject(new Sphere(Point(1,3,-10), 2), (new SimpleSurface())->setBaseColor(Color(1,1,0)));
	for(int y=0; y<6; y++) {
		s->addObject(
			new Sphere(Point(-3 - y * 0.4, 2.3, -5), 0.4),
			(new SimpleSurface())->setBaseColor(Color(y / 6.0, 1 - y / 6.0, 0.5))
		);
	}
	s->addObject(new Halfspace(Point(0,0,0), Vector::UP), new CheckerboardSurface());
	s->render(c);
	return 0;
}
