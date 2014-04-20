/*
 * C/C++ N-body benchmark.
 * Derived from Go N-body benchmark (http://benchmarksgame.alioth.debian.org/u32/program.php?test=nbody&lang=go&id=1)
 */ 

#include <math.h>
#include <stdio.h>
#include <stdlib.h>

int n = 5*1000*1000;

#define solarMass   (4 * M_PI * M_PI)
#define daysPerYear 365.24

struct Body {
	double x, y, z, vx, vy, vz, mass;

	void offsetMomentum(double px, double py, double pz) {
		vx = -px / solarMass;
		vy = -py / solarMass;
		vz = -pz / solarMass;
	}
};

struct System {
	int nBodies;
	Body *bodies;

	System(int nBodies, Body body[]) {
		this->nBodies = nBodies;
		bodies = new Body[nBodies];
		for(int i = 0; i < nBodies; i++) {
			bodies[i] = body[i];
		}
		double px=0, py=0, pz=0;
		for(int i = 0; i < nBodies; i++) {
			Body &body = bodies[i];
			px += body.vx * body.mass;
			py += body.vy * body.mass;
			pz += body.vz * body.mass;
		}
		bodies[0].offsetMomentum(px, py, pz);
	}

	~System() {
		delete[] bodies;
	}

	double energy() {
		double e = 0;
		for(int i=0; i<nBodies; i++ ) {
			Body &body = bodies[i];
			e += 0.5 * body.mass * (body.vx*body.vx + body.vy*body.vy + body.vz*body.vz);
			for(int j = i + 1; j < nBodies; j++) {
				Body &body2 = bodies[j];
				double dx = body.x - body2.x;
				double dy = body.y - body2.y;
				double dz = body.z - body2.z;
				double distance = sqrt(dx*dx + dy*dy + dz*dz);
				e -= (body.mass * body2.mass) / distance;
			}
		}
		return e;
	}

	void advance(double dt) {
		for(int i=0; i<nBodies; i++ ) {
			Body &body = bodies[i];
			for(int j = i + 1; j < nBodies; j++) {
				Body &body2 = bodies[j];
				double dx = body.x - body2.x;
				double dy = body.y - body2.y;
				double dz = body.z - body2.z;

				double dSquared = dx*dx + dy*dy + dz*dz;
				double distance = sqrt(dSquared);
				double mag = dt / (dSquared * distance);

				body.vx -= dx * body2.mass * mag;
				body.vy -= dy * body2.mass * mag;
				body.vz -= dz * body2.mass * mag;

				body2.vx += dx * body.mass * mag;
				body2.vy += dy * body.mass * mag;
				body2.vz += dz * body.mass * mag;
			}
		}
		for(int i=0; i<nBodies; i++ ) {
			Body &body = bodies[i];
			body.x += dt * body.vx;
			body.y += dt * body.vy;
			body.z += dt * body.vz;
		}
	}
};

Body jupiter = (Body){
	.x    = 4.84143144246472090e+00,
	.y    = -1.16032004402742839e+00,
	.z    = -1.03622044471123109e-01,
	.vx   = 1.66007664274403694e-03 * daysPerYear,
	.vy   = 7.69901118419740425e-03 * daysPerYear,
	.vz   = -6.90460016972063023e-05 * daysPerYear,
	.mass = 9.54791938424326609e-04 * solarMass,
};
Body saturn = (Body){
	.x    = 8.34336671824457987e+00,
	.y    = 4.12479856412430479e+00,
	.z    = -4.03523417114321381e-01,
	.vx   = -2.76742510726862411e-03 * daysPerYear,
	.vy   = 4.99852801234917238e-03 * daysPerYear,
	.vz   = 2.30417297573763929e-05 * daysPerYear,
	.mass = 2.85885980666130812e-04 * solarMass,
};
Body uranus = (Body){
	.x    = 1.28943695621391310e+01,
	.y    = -1.51111514016986312e+01,
	.z    = -2.23307578892655734e-01,
	.vx   = 2.96460137564761618e-03 * daysPerYear,
	.vy   = 2.37847173959480950e-03 * daysPerYear,
	.vz   = -2.96589568540237556e-05 * daysPerYear,
	.mass = 4.36624404335156298e-05 * solarMass,
};
Body neptune = (Body){
	.x    = 1.53796971148509165e+01,
	.y    = -2.59193146099879641e+01,
	.z    = 1.79258772950371181e-01,
	.vx   = 2.68067772490389322e-03 * daysPerYear,
	.vy   = 1.62824170038242295e-03 * daysPerYear,
	.vz   = -9.51592254519715870e-05 * daysPerYear,
	.mass = 5.15138902046611451e-05 * solarMass,
};
Body sun = (Body){
	.x    = 0,
	.y    = 0,
	.z    = 0,
	.vx   = 0,
	.vy   = 0,
	.vz   = 0,
	.mass = solarMass,
};

int main(int argc, char **argv) {
	if(argc > 1) {
		n = atoi(argv[1]);
	}

	Body bodies[] = {sun, jupiter, saturn, uranus, neptune};
	System system(sizeof(bodies)/sizeof(Body), bodies);
	printf("%.9f\n", system.energy());
	for(int i = 0; i < n; i++) {
		system.advance(0.01);
	}
	printf("%.9f\n", system.energy());
}
