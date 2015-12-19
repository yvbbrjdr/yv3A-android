package tk.yvbb.yv3daudio;

public class Vec {
    double x,y,z;
    
    public Vec() {
        x=y=z=0;
    }
    
    public Vec(double _x,double _y,double _z) {
        x=_x;
        y=_y;
        z=_z;
    }
    
    public Vec Plus(Vec b) {
        Vec a=this;
        return new Vec(a.x+b.x,a.y+b.y,a.z+b.z);
    }
    
    public Vec Minus(Vec b) {
        Vec a=this;
        return new Vec(a.x-b.x,a.y-b.y,a.z-b.z);
    }
    
    public Vec Multiply(double b) {
        Vec a=this;
        return new Vec(a.x*b,a.y*b,a.z*b);
    }
    
    public Vec Devide(double b) {
        Vec a=this;
        return new Vec(a.x/b,a.y/b,a.z/b);
    }
    
    public double Dot(Vec b) {
        Vec a=this;
        return a.x*b.x+a.y*b.y+a.z*b.z;
    }
    
    public Vec Det(Vec b) {
        Vec a=this;
        return new Vec(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x);
    }
    
    public double Angle(Vec b) {
        Vec a=this;
        if (a.Len()==0||b.Len()==0)
            return 0;
        return Math.acos(a.Dot(b)/a.Len()/b.Len());
    }
    
    public double AngleDegree(Vec b) {
        return Angle(b)*180/Math.PI;
    }

    public double Len() {
        Vec a=this;
        return Math.sqrt(a.x*a.x+a.y*a.y+a.z*a.z);
    }

    public Vec toDirection() {
        Vec a=this;
        double l=Len();
        if (l==0)
            return new Vec();
        return new Vec(a.x/l,a.y/l,a.z/l);
    }
}
