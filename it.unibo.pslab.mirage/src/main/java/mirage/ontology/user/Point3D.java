package mirage.ontology.user;

public class Point3D {

	private double x, y, z;
	
	public Point3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public double x() {
		return x;
	}
	
	public double y() {
		return y;
	}
	
	public double z() {
		return z;
	}
	
	@Override
	public boolean equals(Object obj) {
		Point3D p = (Point3D) obj;
		return this.x == p.x && this.y == p.y && this.z == p.z;
	}
}
