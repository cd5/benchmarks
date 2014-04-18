/*
 * This file contains definitions for a simple raytracer.
 * Copyright Callum and Tony Garnock-Jones, 2008.
 * Copyright Jan Ziak <0xe2.0x9a.0x9b@gmail.com>, 2014.
 * This file may be freely redistributed under the MIT license,
 * http://www.opensource.org/licenses/mit-license.php
 *
 * Based on http://www.lshift.net/blog/2008/10/29/toy-raytracer-in-python
 */

package main

import (
	"fmt"
	"io"
	"log"
	"math"
	"os"
)

const EPSILON = 0.00001

type Vector struct {
	x, y, z float64
}

func (v Vector) magnitude() float64 {
	return math.Sqrt(v.dot(v))
}

func (v Vector) add(other Vector) Vector {
	return Vector{v.x + other.x, v.y + other.y, v.z + other.z}
}

func (v Vector) addPoint(other Point) Point {
	return Point{v.x + other.x, v.y + other.y, v.z + other.z}
}

func (v Vector) sub(other Vector) Vector {
	return Vector{v.x - other.x, v.y - other.y, v.z - other.z}
}

func (v Vector) subPoint(other Point) Vector {
	return Vector{v.x - other.x, v.y - other.y, v.z - other.z}
}

func (v Vector) scale(factor float64) Vector {
	return Vector{factor * v.x, factor * v.y, factor * v.z}
}

func (v Vector) dot(other Vector) float64 {
	return (v.x * other.x) + (v.y * other.y) + (v.z * other.z)
}

func (v Vector) cross(other Vector) Vector {
	return Vector{v.y*other.z - v.z*other.y, v.z*other.x - v.x*other.z, v.x*other.y - v.y*other.x}
}

func (v Vector) normalized() Vector {
	return v.scale(1.0 / v.magnitude())
}

func (v Vector) negated() Vector {
	return v.scale(-1)
}

func (v Vector) reflectThrough(normal Vector) Vector {
	d := normal.scale(v.dot(normal))
	return v.sub(d.scale(2))
}

type Point struct {
	x, y, z float64
}

func (p Point) toVector() Vector {
	return Vector{p.x, p.y, p.z}
}

func (p Point) addVector(other Vector) Point {
	return Point{p.x + other.x, p.y + other.y, p.z + other.z}
}

func (p Point) sub(other Point) Vector {
	return Vector{p.x - other.x, p.y - other.y, p.z - other.z}
}

func (p Point) subVector(other Vector) Point {
	return Point{p.x - other.x, p.y - other.y, p.z - other.z}
}

var (
	RIGHT Vector = Vector{1, 0, 0}
	UP    Vector = Vector{0, 1, 0}
	OUT   Vector = Vector{0, 0, 1}

	ZERO Point
)

type Ray struct {
	point  Point
	vector Vector
}

func newRay(point Point, vector Vector) Ray {
	return Ray{point, vector.normalized()}
}

func (r Ray) pointAtTime(t float64) Point {
	return r.point.addVector(r.vector.scale(t))
}

type SceneObject interface {
	intersectionTime(ray *Ray) float64
	normalAt(p Point) Vector
}

type Sphere struct {
	centre Point
	radius float64
}

func (s *Sphere) intersectionTime(ray *Ray) float64 {
	cp := s.centre.sub(ray.point)
	v := cp.dot(ray.vector)
	discriminant := (s.radius * s.radius) - (cp.dot(cp) - v*v)
	if discriminant < 0 {
		return math.NaN()
	} else {
		return v - math.Sqrt(discriminant)
	}
}

func (s *Sphere) normalAt(p Point) Vector {
	return p.sub(s.centre).normalized()
}

type HalfSpace struct {
	point  Point
	normal Vector
}

func newHalfSpace(point Point, normal Vector) *HalfSpace {
	return &HalfSpace{point, normal.normalized()}
}

func (s *HalfSpace) intersectionTime(ray *Ray) float64 {
	v := ray.vector.dot(s.normal)
	return 1 / -v
}

func (s *HalfSpace) normalAt(p Point) Vector {
	return s.normal
}

type Color struct {
	r, g, b float64
}

type PpmCanvas struct {
	width, height int
	filenameBase  string
	bytes         []byte
}

func newPpmCanvas(width, height int, filenameBase string) *PpmCanvas {
	c := &PpmCanvas{
		width,
		height,
		filenameBase,
		make([]byte, width*height*3),
	}
	for i := 0; i < width*height; i++ {
		c.bytes[i*3+2] = 255
	}
	return c
}

func clamp(c int) int {
	if c < 0 {
		return 0
	}
	if c > 255 {
		return 255
	}
	return c
}

func (a *PpmCanvas) plot(x, y int, c Color) {
	i := ((a.height-y-1)*a.width + x) * 3
	a.bytes[i+0] = byte(clamp(int(c.r * 255)))
	a.bytes[i+1] = byte(clamp(int(c.g * 255)))
	a.bytes[i+2] = byte(clamp(int(c.b * 255)))
}

func (a *PpmCanvas) save() {
	name := a.filenameBase + ".ppm"
	os.Remove(name)
	f, err := os.Create(name)
	if err != nil {
		log.Fatal(err)
	}
	header := fmt.Sprintf("P6\n%d %d\n255\n", a.width, a.height)
	if _, err := io.WriteString(f, header); err != nil {
		log.Fatal(err)
	}
	if _, err := f.Write(a.bytes); err != nil {
		log.Fatal(err)
	}
	if err := f.Close(); err != nil {
		log.Fatal(err)
	}
}

type Intersection struct {
	o SceneObject
	t float64
	s Surface
}

func firstIntersection(intersections []Intersection) *Intersection {
	var result *Intersection
	for a := 0; a < len(intersections); a++ {
		i := &intersections[a]
		candidateT := i.t
		if !math.IsNaN(candidateT) && candidateT > -EPSILON {
			if result == nil || candidateT < result.t {
				result = i
			}
		}
	}
	return result
}

type Surface interface {
	colorAt(scene *Scene, ray *Ray, p Point, normal Vector) Color
}

type Obj struct {
	object  SceneObject
	surface Surface
}

type Scene struct {
	objects        []Obj
	lightPoints    []Point
	position       Point
	lookingAt      Point
	fieldOfView    float64
	recursionDepth int
}

func newScene() *Scene {
	s := &Scene{}
	s.position = Point{0, 1.8, 10}
	s.lookingAt = ZERO
	s.fieldOfView = 45
	s.recursionDepth = 0
	return s
}

func (s *Scene) moveTo(p Point) {
	s.position = p
}

func (s *Scene) lookAt(p Point) {
	s.lookingAt = p
}

func (s *Scene) addObject(object SceneObject, surface Surface) {
	s.objects = append(s.objects, Obj{object, surface})
}

func (s *Scene) addLight(p Point) {
	s.lightPoints = append(s.lightPoints, p)
}

func (s *Scene) render(canvas *PpmCanvas) {
	if false {
		fmt.Printf("Computing field of view\n")
	}
	fovRadians := math.Pi * (s.fieldOfView / 2.0) / 180.0
	halfWidth := math.Tan(fovRadians)
	halfHeight := 0.75 * halfWidth
	width := halfWidth * 2
	height := halfHeight * 2
	pixelWidth := width / float64(canvas.width-1)
	pixelHeight := height / float64(canvas.height-1)

	eye := newRay(s.position, s.lookingAt.sub(s.position))
	vpRight := eye.vector.cross(UP).normalized()
	vpUp := vpRight.cross(eye.vector).normalized()

	fmt.Printf("Looping over pixels\n")
	var previousfraction float32
	for y := 0; y < canvas.height; y++ {
		currentfraction := float32(y) / float32(canvas.height)
		if currentfraction-previousfraction > 0.05 {
			if false {
				canvas.save()
			}
			fmt.Printf("%d%% complete\n", int(currentfraction*100))
			previousfraction = currentfraction
		}
		for x := 0; x < canvas.width; x++ {
			xcomp := vpRight.scale(float64(x)*pixelWidth - halfWidth)
			ycomp := vpUp.scale(float64(y)*pixelHeight - halfHeight)
			ray := newRay(eye.point, eye.vector.add(xcomp).add(ycomp))
			color := s.rayColor(ray)
			canvas.plot(x, y, color)
		}
	}

	canvas.save()
	fmt.Printf("Complete.\n")
}

func (s *Scene) rayColor(ray Ray) Color {
	if s.recursionDepth > 3 {
		return Color{0, 0, 0}
	}
	s.recursionDepth++
	var intersections []Intersection
	for _, o := range s.objects {
		intersections = append(intersections, Intersection{o.object, o.object.intersectionTime(&ray), o.surface})
	}
	i := firstIntersection(intersections)
	if i == nil {
		s.recursionDepth--
		return Color{0, 0, 0} // the background color
	} else {
		p := ray.pointAtTime(i.t)
		s.recursionDepth--
		return i.s.colorAt(s, &ray, p, i.o.normalAt(p))
	}
}

func (s *Scene) lightIsVisible(l Vector, p Point) bool {
	for _, o := range s.objects {
		ray := newRay(p, l.subPoint(p))
		t := o.object.intersectionTime(&ray)
		if !math.IsNaN(t) && t > EPSILON {
			return false
		}
	}
	return true
}

func (s *Scene) visibleLights(p Point) []Point {
	var result []Point
	for _, l := range s.lightPoints {
		if s.lightIsVisible(l.toVector(), p) {
			result = append(result, l)
		}
	}
	return result
}

func addColors(a Color, scale float64, b Color) Color {
	return Color{a.r + scale*b.r, a.g + scale*b.g, a.b + scale*b.b}
}

type BaseColor interface {
	baseColorAt(p Point) Color
}

type UniformBaseColor struct {
	color Color
}

func (c *UniformBaseColor) baseColorAt(p Point) Color {
	return c.color
}

type SimpleSurface struct {
	baseColor           BaseColor
	specularCoefficient float64
	lambertCoefficient  float64
	ambientCoefficient  float64
}

func newSimpleSurface() *SimpleSurface {
	s := &SimpleSurface{}
	s.baseColor = &UniformBaseColor{Color{1, 1, 1}}
	s.specularCoefficient = 0.2
	s.lambertCoefficient = 0.6
	s.ambientCoefficient = 1.0 - s.specularCoefficient - s.lambertCoefficient
	return s
}

func (s *SimpleSurface) setBaseColor(c BaseColor) *SimpleSurface {
	s.baseColor = c
	return s
}

func (s *SimpleSurface) colorAt(scene *Scene, ray *Ray, p Point, normal Vector) Color {
	b := s.baseColor.baseColorAt(p)

	c := Color{0, 0, 0}
	if s.specularCoefficient > 0 {
		reflectedRay := newRay(p, ray.vector.reflectThrough(normal))
		reflectedColor := scene.rayColor(reflectedRay)
		c = addColors(c, s.specularCoefficient, reflectedColor)
	}

	if s.lambertCoefficient > 0 {
		lambertAmount := 0.0
		visibleLights := scene.visibleLights(p)
		for _, lightPoint := range visibleLights {
			contribution := lightPoint.sub(p).normalized().dot(normal)
			if contribution > 0 {
				lambertAmount += contribution
			}
		}
		if lambertAmount > 1.0 {
			lambertAmount = 1
		}
		c = addColors(c, s.lambertCoefficient*lambertAmount, b)
	}

	if s.ambientCoefficient > 0 {
		c = addColors(c, s.ambientCoefficient, b)
	}

	return c
}

type CheckerboardBaseColor struct {
	color      Color
	otherColor Color
	checkSize  float64
}

func (s *CheckerboardBaseColor) baseColorAt(p Point) Color {
	v := p.sub(ZERO)
	v = v.scale(1.0 / s.checkSize)
	if (int(math.Abs(v.x)+0.5)+int(math.Abs(v.y)+0.5)+int(math.Abs(v.z)+0.5))%2 == 1 {
		return s.otherColor
	} else {
		return s.color
	}
}

func newCheckerboardSurface() *SimpleSurface {
	s := newSimpleSurface()
	s.baseColor = &CheckerboardBaseColor{color: Color{1, 1, 1}, otherColor: Color{0, 0, 0}, checkSize: 1}
	return s
}

func main() {
	log.SetFlags(0)
	c := newPpmCanvas(320, 240, "raytrace")
	s := newScene()
	s.addLight(Point{30, 30, 10})
	s.addLight(Point{-10, 100, 30})
	s.lookAt(Point{0, 3, 0})
	s.addObject(&Sphere{Point{1, 3, -10}, 2}, newSimpleSurface().setBaseColor(&UniformBaseColor{Color{1, 1, 0}}))
	for y := float64(0); y < 6; y++ {
		s.addObject(
			&Sphere{Point{-3 - y*0.4, 2.3, -5}, 0.4},
			newSimpleSurface().setBaseColor(&UniformBaseColor{Color{y / 6.0, 1 - y/6.0, 0.5}}),
		)
	}
	s.addObject(newHalfSpace(Point{0, 0, 0}, UP), newCheckerboardSurface())
	s.render(c)
}
