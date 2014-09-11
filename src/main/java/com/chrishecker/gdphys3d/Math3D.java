package com.chrishecker.gdphys3d;

/*
 * ----------------------------------------------------------------------------
 *
 * 3D Physics Test Program - a cheesy test harness for 3D physics
 *
 * by Chris Hecker for my Game Developer Magazine articles. See my homepage for
 * more information.
 *
 * NOTE: This is a hacked test program, not a nice example of Windows
 * programming. physics.cpp the only part of this you should look at!!!
 *
 * This material is Copyright 1997 Chris Hecker, All Rights Reserved. It's for
 * you to read and learn from, not to put in your own articles or books or on
 * your website, etc. Thank you.
 *
 * Chris Hecker checker@d6.com http://www.d6.com/users/checker
 *
 */
public class Math3D {
}

class Matrix3f {

    public float r0;
    public float r1;
    public float r2;
    public float r3;
    public float r4;
    public float r5;
    public float r6;
    public float r7;
    public float r8;

    public Matrix3f() {
        r0 = 1.0f;
        r1 = 0.0f;
        r2 = 0.0f;
        r3 = 0.0f;
        r4 = 1.0f;
        r5 = 0.0f;
        r6 = 0.0f;
        r7 = 0.0f;
        r8 = 1.0f;
    }

    public final float getElement(int row, int column) {
        if ((column < 0) || (column > 2) || (row < 0) || (row > 2)) {
            throw new IllegalArgumentException("Invalid Column/Row!");
        }

        if (row == 0) {
            if (column == 0) {
                return r0;
            } else if (column == 1) {
                return r1;
            } else {
                return r2;
            }
        } else if (row == 1) {
            if (column == 0) {
                return r3;
            } else if (column == 1) {
                return r4;
            } else {
                return r5;
            }
        } else {
            if (column == 0) {
                return r6;
            } else if (column == 1) {
                return r7;
            } else {
                return r8;
            }
        }
    }

    public final void setElement(int row, int column, float value) {
        if ((column < 0) || (column > 2) || (row < 0) || (row > 2)) {
            throw new IllegalArgumentException("Invalid Column/Row!");
        }

        if (row == 0) {
            if (column == 0) {
                r0 = value;
            } else if (column == 1) {
                r1 = value;
            } else {
                r2 = value;
            }
        } else if (row == 1) {
            if (column == 0) {
                r3 = value;
            } else if (column == 1) {
                r4 = value;
            } else {
                r5 = value;
            }
        } else {
            if (column == 0) {
                r6 = value;
            } else if (column == 1) {
                r7 = value;
            } else {
                r8 = value;
            }
        }
    }

    public static Matrix3f Add(Matrix3f Operand1, Matrix3f Operand2) {
        Matrix3f Return = Matrix3f.Identity();

        for (int Counter = 0; Counter < 3; Counter++) {
            Return.setElement(0, Counter, Operand1.getElement(0, Counter) + Operand2.getElement(0, Counter));
            Return.setElement(1, Counter, Operand1.getElement(1, Counter) + Operand2.getElement(1, Counter));
            Return.setElement(2, Counter, Operand1.getElement(2, Counter) + Operand2.getElement(2, Counter));
        }

        return Return;
    }

    public static Matrix3f Identity() {
        return new Matrix3f();
    }

    public static Matrix3f Multiply(Matrix3f Multiplicand, Matrix3f Multiplier) {
        Matrix3f ReturnMatrix = Matrix3f.Identity();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                float Value = 0;

                for (int k = 0; k < 3; k++) {
                    Value += Multiplicand.getElement(i, k)
                            * Multiplier.getElement(k, j);
                }

                ReturnMatrix.setElement(i, j, Value);
            }
        }

        return ReturnMatrix;
    }

    public static Matrix3f MultiplyByScalar(Matrix3f Matrix, float Value) {
        Matrix3f Return = Matrix3f.Identity();

        for (int Counter = 0; Counter < 3; Counter++) {
            Return.setElement(0, Counter, Value * Matrix.getElement(0, Counter));
            Return.setElement(1, Counter, Value * Matrix.getElement(1, Counter));
            Return.setElement(2, Counter, Value * Matrix.getElement(2, Counter));
        }

        return Return;
    }

    public static void OrthonormalizeOrientation(Matrix3f Orientation) {
        Vector3f X = Vector3f.Set(Orientation.getElement(0, 0), Orientation.getElement(1, 0), Orientation.getElement(2, 0));
        Vector3f Y = Vector3f.Set(Orientation.getElement(0, 1), Orientation.getElement(1, 1), Orientation.getElement(2, 1));
        Vector3f Z;

        X.normalize();
        Z = Vector3f.Cross(X, Y);
        Z.normalize();
        Y = Vector3f.Cross(Z, X);
        Y.normalize();

        Orientation.setElement(0, 0, X.x);
        Orientation.setElement(0, 1, Y.x);
        Orientation.setElement(0, 2, Z.x);
        Orientation.setElement(1, 0, X.y);
        Orientation.setElement(1, 1, Y.y);
        Orientation.setElement(1, 2, Z.y);
        Orientation.setElement(2, 0, X.z);
        Orientation.setElement(2, 1, Y.z);
        Orientation.setElement(2, 2, Z.z);
    }

    public static Matrix3f SkewSymmetric(Vector3f v) {
        Matrix3f m = Matrix3f.Identity();

        m.r0 = 0.0f;
        m.r1 = -v.z;
        m.r2 = v.y;

        m.r3 = v.z;
        m.r4 = 0.0f;
        m.r5 = -v.x;

        m.r6 = -v.y;
        m.r7 = v.x;
        m.r8 = 0.0f;

        return m;
    }

    public static Matrix3f Transpose(Matrix3f Matrix) {
        Matrix3f Return = Matrix3f.Identity();

        for (int Counter = 0; Counter < 3; Counter++) {
            Return.setElement(0, Counter, Matrix.getElement(Counter, 0));
            Return.setElement(1, Counter, Matrix.getElement(Counter, 1));
            Return.setElement(2, Counter, Matrix.getElement(Counter, 2));
        }

        return Return;
    }
}

class Vector3f {

    public float x;
    public float y;
    public float z;

    public Vector3f() {
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public final void add(Vector3f v) {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    public final float getElement(int index) {
        if (index == 0) {
            return x;
        } else if (index == 1) {
            return y;
        } else if (index == 2) {
            return z;
        } else {
            throw new IllegalArgumentException("Invalid Index!");
        }
    }

    public final float magnitude() {
        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }

    public final void normalize() {
        float mag = magnitude();

        if (mag != 0.0f) {
            mag = 1 / mag;

            x *= mag;
            y *= mag;
            z *= mag;
        }
    }

    public final void setElement(int index, float value) {
        if (index == 0) {
            x = value;
        } else if (index == 1) {
            y = value;
        } else if (index == 2) {
            z = value;
        } else {
            throw new IllegalArgumentException("Invalid Index!");
        }
    }

    public final void subtract(Vector3f v) {
        x -= v.x;
        y -= v.y;
        z -= v.z;
    }

    public static Vector3f Add(Vector3f v, Vector3f w) {
        return new Vector3f(
                v.x + w.x,
                v.y + w.y,
                v.z + w.z);
    }

    public static Vector3f Cross(Vector3f v, Vector3f w) {
        return new Vector3f(
                (v.y * w.z) - (v.z * w.y),
                (v.z * w.x) - (v.x * w.z),
                (v.x * w.y) - (v.y * w.x));
    }

    public static Vector3f Divide(Vector3f v, float s) {
        return new Vector3f(
                v.x / s,
                v.y / s,
                v.z / s);
    }

    public static float Dot(Vector3f v, Vector3f w) {
        return (v.x * w.x) + (v.y * w.y) + (v.z * w.z);
    }

    public static Vector3f Multiply(Vector3f v, float s) {
        return new Vector3f(
                v.x * s,
                v.y * s,
                v.z * s);
    }

    public static Vector3f MultiplyByMatrix(Matrix3f Multiplicand, Vector3f Multiplier) {
        Vector3f ReturnPoint = Vector3f.Zero();

        for (int i = 0; i < 3; i++) {
            float Value = 0;

            for (int k = 0; k < 3; k++) {
                Value += Multiplicand.getElement(i, k) * Multiplier.getElement(k);
            }

            ReturnPoint.setElement(i, Value);
        }

        return ReturnPoint;
    }

    public static Vector3f Set(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    public static Vector3f Subtract(Vector3f v, Vector3f w) {
        return new Vector3f(
                v.x - w.x,
                v.y - w.y,
                v.z - w.z);
    }

    public static Vector3f Zero() {
        return new Vector3f();
    }

}
