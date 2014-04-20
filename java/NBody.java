/*
 * Java N-body benchmark.
 * Derived from Go N-body benchmark (http://benchmarksgame.alioth.debian.org/u32/program.php?test=nbody&lang=go&id=1)
 */

public class NBody {
	static int n = 5 * 1000 * 1000;

	static final double solarMass = 4 * Math.PI * Math.PI;
	static final double daysPerYear = 365.24;

	static class Body {
		double x, y, z, vx, vy, vz, mass;

		Body(double x, double y, double z, double vx, double vy, double vz, double mass) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.vx = vx;
			this.vy = vy;
			this.vz = vz;
			this.mass = mass;
		}

		Body(Body b) {
			this.x = b.x;
			this.y = b.y;
			this.z = b.z;
			this.vx = b.vx;
			this.vy = b.vy;
			this.vz = b.vz;
			this.mass = b.mass;
		}

		void offsetMomentum(double px, double py, double pz) {
			vx = -px / solarMass;
			vy = -py / solarMass;
			vz = -pz / solarMass;
		}

	}

	static class System {
		Body bodies[];

		System(Body body[]) {
			bodies = new Body[body.length];
			for (int i = 0; i < body.length; i++) {
				bodies[i] = new Body(body[i]);
			}
			double px = 0, py = 0, pz = 0;
			for (int i = 0; i < body.length; i++) {
				Body bodyi = bodies[i];
				px += bodyi.vx * bodyi.mass;
				py += bodyi.vy * bodyi.mass;
				pz += bodyi.vz * bodyi.mass;
			}
			bodies[0].offsetMomentum(px, py, pz);
		}

		double energy() {
			double e = 0;
			for (int i = 0; i < bodies.length; i++) {
				Body body = bodies[i];
				e += 0.5 * body.mass * (body.vx * body.vx + body.vy * body.vy + body.vz * body.vz);
				for (int j = i + 1; j < bodies.length; j++) {
					Body body2 = bodies[j];
					double dx = body.x - body2.x;
					double dy = body.y - body2.y;
					double dz = body.z - body2.z;
					double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
					e -= (body.mass * body2.mass) / distance;
				}
			}
			return e;
		}

		void advance(double dt) {
			for (int i = 0; i < bodies.length; i++) {
				Body body = bodies[i];
				for (int j = i + 1; j < bodies.length; j++) {
					Body body2 = bodies[j];
					double dx = body.x - body2.x;
					double dy = body.y - body2.y;
					double dz = body.z - body2.z;

					double dSquared = dx * dx + dy * dy + dz * dz;
					double distance = Math.sqrt(dSquared);
					double mag = dt / (dSquared * distance);

					body.vx -= dx * body2.mass * mag;
					body.vy -= dy * body2.mass * mag;
					body.vz -= dz * body2.mass * mag;

					body2.vx += dx * body.mass * mag;
					body2.vy += dy * body.mass * mag;
					body2.vz += dz * body.mass * mag;
				}
			}
			for (int i = 0; i < bodies.length; i++) {
				Body body = bodies[i];
				body.x += dt * body.vx;
				body.y += dt * body.vy;
				body.z += dt * body.vz;
			}
		}
	}

	static Body jupiter = new Body(
		4.84143144246472090e+00,
		-1.16032004402742839e+00,
		-1.03622044471123109e-01,
		1.66007664274403694e-03 * daysPerYear,
		7.69901118419740425e-03 * daysPerYear,
		-6.90460016972063023e-05 * daysPerYear,
		9.54791938424326609e-04 * solarMass
	);
	static Body saturn = new Body(
		8.34336671824457987e+00,
		4.12479856412430479e+00,
		-4.03523417114321381e-01,
		-2.76742510726862411e-03 * daysPerYear,
		4.99852801234917238e-03 * daysPerYear,
		2.30417297573763929e-05 * daysPerYear,
		2.85885980666130812e-04 * solarMass
	);
	static Body uranus = new Body(
		1.28943695621391310e+01,
		-1.51111514016986312e+01,
		-2.23307578892655734e-01,
		2.96460137564761618e-03 * daysPerYear,
		2.37847173959480950e-03 * daysPerYear,
		-2.96589568540237556e-05 * daysPerYear,
		4.36624404335156298e-05 * solarMass
	);
	static Body neptune = new Body(
		1.53796971148509165e+01,
		-2.59193146099879641e+01,
		1.79258772950371181e-01,
		2.68067772490389322e-03 * daysPerYear,
		1.62824170038242295e-03 * daysPerYear,
		-9.51592254519715870e-05 * daysPerYear,
		5.15138902046611451e-05 * solarMass
	);
	static Body sun = new Body(
		0,
		0,
		0,
		0,
		0,
		0,
		solarMass
	);

	public static void main(String[] args) {
		if (args.length > 0) {
			n = Integer.parseInt(args[0]);
		}

		Body bodies[] = {sun, jupiter, saturn, uranus, neptune};
		System system = new System(bodies);
		java.lang.System.out.printf("%.9f\n", system.energy());
		for (int i = 0; i < n; i++) {
			system.advance(0.01);
		}
		java.lang.System.out.printf("%.9f\n", system.energy());
	}
}
