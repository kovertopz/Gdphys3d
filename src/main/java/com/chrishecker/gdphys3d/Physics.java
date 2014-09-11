package com.chrishecker.gdphys3d;

import java.nio.FloatBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

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
public class Physics {

    private float WorldSize = 8.0f;

    private void CreateOpenGLTransform(Matrix3f Orientation, Vector3f CMPosition, FloatBuffer fbout) {
        fbout.put(0, Orientation.getElement(0, 0));
        fbout.put(1, Orientation.getElement(1, 0));
        fbout.put(2, Orientation.getElement(2, 0));
        fbout.put(3, 0.0f);

        fbout.put(4, Orientation.getElement(0, 1));
        fbout.put(5, Orientation.getElement(1, 1));
        fbout.put(6, Orientation.getElement(2, 1));
        fbout.put(7, 0.0f);

        fbout.put(8, Orientation.getElement(0, 2));
        fbout.put(9, Orientation.getElement(1, 2));
        fbout.put(10, Orientation.getElement(2, 2));
        fbout.put(11, 0.0f);

        fbout.put(12, CMPosition.x);
        fbout.put(13, CMPosition.y);
        fbout.put(14, CMPosition.z);
        fbout.put(15, 1.0f);
    }
    public boolean integration = true;

    public void ToggleIntegration() {
        integration = !integration;
    }

    enum collision_state {

        Penetrating,
        Colliding,
        Clear
    };
    private int WorldSpringsActive = 1;
    private int BodySpringsActive = 1;
    private int GravityActive = 1;
    private int DampingActive = 0;
    private float Kws = 0.7f;        // Hooke's spring constant
    private float Kwd = 0.1f;        // damping constant
    private world_spring aWorldSprings[] = new world_spring[2];
    private int NumberOfWorldSprings = 2;
    private float Kbs = 0.6f;        // Hooke's spring constant
    private float Kbd = 0.1f;        // damping constant
    private body_spring aBodySprings[] = new body_spring[5];
    private int NumberOfBodySprings = 5;
    Vector3f Gravity = Vector3f.Set(0.0f, 0.0f, -10.0f);
    private float NoKdl = 0.002f;    // the no-damping linear damping factor
    private float NoKda = 0.001f;    // the no-damping angular damping factor
    private float Kdl = 0.04f;       // linear damping factor
    private float Kda = 0.01f;       // angular damping factor
    collision_state CollisionState;
    Vector3f CollisionNormal;
    int CollidingBodyIndex;
    int CollidingCornerIndex;
    int SourceConfigurationIndex = 0;
    int TargetConfigurationIndex = 1;
    int NumberOfWalls = 6;
    wall aWalls[] = new wall[NumberOfWalls];
    int NumberOfBodies = 6;
    rigid_body aBodies[] = new rigid_body[NumberOfBodies];

    public Physics() {
        aBodies[0] = new rigid_body();
        aBodies[1] = new rigid_body();
        aBodies[2] = new rigid_body();
        aBodies[3] = new rigid_body();
        aBodies[4] = new rigid_body();
        aBodies[5] = new rigid_body();

        aBodySprings[0] = new body_spring(0, 6, 1, 2);
        aBodySprings[1] = new body_spring(1, 6, 2, 2);
        aBodySprings[2] = new body_spring(2, 6, 3, 2);
        aBodySprings[3] = new body_spring(3, 6, 4, 2);
        aBodySprings[4] = new body_spring(4, 6, 5, 0);

        aWalls[0] = new wall();
        aWalls[1] = new wall();
        aWalls[2] = new wall();
        aWalls[3] = new wall();
        aWalls[4] = new wall();
        aWalls[5] = new wall();

        aWorldSprings[0] = new world_spring(0, 2, Vector3f.Set(-1.5f, 0.0f, 0.0f));
        aWorldSprings[1] = new world_spring(5, 2, Vector3f.Set(1.5f, 0.0f, 0.0f));

        InitializeBodies();

        // initialize walls
        aWalls[0].Normal = Vector3f.Set(0.0f, -1.0f, 0.0f);
        aWalls[0].d = WorldSize / 2f;
        aWalls[1].Normal = Vector3f.Set(0.0f, 1.0f, 0.0f);
        aWalls[1].d = WorldSize / 2f;

        aWalls[2].Normal = Vector3f.Set(-1.0f, 0.0f, 0.0f);
        aWalls[2].d = WorldSize / 2f;
        aWalls[3].Normal = Vector3f.Set(1.0f, 0.0f, 0.0f);
        aWalls[3].d = WorldSize / 2f;

        aWalls[4].Normal = Vector3f.Set(0.0f, 0.0f, -1.0f);
        aWalls[4].d = WorldSize / 2f;
        aWalls[5].Normal = Vector3f.Set(0.0f, 0.0f, 1.0f);
        aWalls[5].d = WorldSize / 2f;

        // calculate initial bounding volume positions
        CalculateVertices(0);
    }

    public void Run() {
        if (integration == true) {
            Simulate(0.001f);
        }
        Render();
    }

    public void ToggleBodySprings() {
        BodySpringsActive = BodySpringsActive == 1 ? 0 : 1;
    }

    public void ToggleDamping() {
        DampingActive = DampingActive == 1 ? 0 : 1;
    }

    public void ToggleGravity() {
        GravityActive = GravityActive == 1 ? 0 : 1;
    }

    public void ToggleWorldSprings() {
        WorldSpringsActive = WorldSpringsActive == 1 ? 0 : 1;
    }

    public void InitializeBodies() {
        for (int BodyIndex = 0; BodyIndex < NumberOfBodies; BodyIndex++) {
            // initialize rigid bodies by randomly generating boxes

            rigid_body Body = aBodies[BodyIndex];

            // 1/2 the dimensions of the box
            float dX2 = GenerateReasonableRandomReal();
            float dY2 = GenerateReasonableRandomReal();
            float dZ2 = GenerateReasonableRandomReal();

            float Density = 0.4f;
            float Mass = 8.0f * Density * dX2 * dY2 * dZ2;

            Body.OneOverMass = 1.0f / Mass;
            Body.InverseBodyInertiaTensor.r0 = 3.0f / (Mass * (dY2 * dY2 + dZ2 * dZ2));
            Body.InverseBodyInertiaTensor.r4 = 3.0f / (Mass * (dX2 * dX2 + dZ2 * dZ2));
            Body.InverseBodyInertiaTensor.r8 = 3.0f / (Mass * (dX2 * dX2 + dY2 * dY2));

            Body.CoefficientOfRestitution = 1.0f;

            // Body.aConfigurations[0].CMPosition;
            // initialize geometric quantities
            // we'll use the body index+1 for the display list id
            GL11.glNewList(BodyIndex + 1, GL11.GL_COMPILE);

            GL11.glColor3f(GenerateUnitRandomReal(), GenerateUnitRandomReal(), GenerateUnitRandomReal());

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(-1.0F, 0.0F, 0.0F);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(-dX2, -dY2, -dZ2);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(-dX2, -dY2, dZ2);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(-dX2, dY2, dZ2);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(-dX2, dY2, -dZ2);

            GL11.glNormal3f(1.0F, 0.0F, 0.0F);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(dX2, dY2, dZ2);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(dX2, -dY2, dZ2);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(dX2, -dY2, -dZ2);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(dX2, dY2, -dZ2);

            GL11.glNormal3f(0.0F, -1.0F, 0.0F);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(-dX2, -dY2, -dZ2);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(dX2, -dY2, -dZ2);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(dX2, -dY2, dZ2);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(-dX2, -dY2, dZ2);

            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(dX2, dY2, dZ2);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(dX2, dY2, -dZ2);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(-dX2, dY2, -dZ2);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(-dX2, dY2, dZ2);

            GL11.glNormal3f(0.0F, 0.0F, -1.0F);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(-dX2, -dY2, -dZ2);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(-dX2, dY2, -dZ2);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(dX2, dY2, -dZ2);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(dX2, -dY2, -dZ2);

            GL11.glNormal3f(0.0F, 0.0F, 1.0F);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(dX2, dY2, dZ2);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(-dX2, dY2, dZ2);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(-dX2, -dY2, dZ2);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(dX2, -dY2, dZ2);
            GL11.glEnd();
            GL11.glEndList();

            // generate the body-space bounding volume vertices
            Body.NumberOfBoundingVertices = 8;
            Body.aBodyBoundingVertices[0] = Vector3f.Set(dX2, dY2, dZ2);
            Body.aBodyBoundingVertices[1] = Vector3f.Set(dX2, dY2, -dZ2);
            Body.aBodyBoundingVertices[2] = Vector3f.Set(dX2, -dY2, dZ2);
            Body.aBodyBoundingVertices[3] = Vector3f.Set(dX2, -dY2, -dZ2);
            Body.aBodyBoundingVertices[4] = Vector3f.Set(-dX2, dY2, dZ2);
            Body.aBodyBoundingVertices[5] = Vector3f.Set(-dX2, dY2, -dZ2);
            Body.aBodyBoundingVertices[6] = Vector3f.Set(-dX2, -dY2, dZ2);
            Body.aBodyBoundingVertices[7] = Vector3f.Set(-dX2, -dY2, -dZ2);
        }
    }

    public float GenerateReasonableRandomReal() {
        return 0.1f + 0.2f * GenerateUnitRandomReal();
    }

    public float GenerateUnitRandomReal() {
        Random r = new Random();

        return r.nextFloat();
    }

    public void Render() {
        int Counter;

        // draw bodies
        //GL11.glEnable(GL11.GL_LIGHTING);
        for (Counter = 0; Counter < NumberOfBodies; Counter++) {
            Matrix3f Orientation = aBodies[Counter].aConfigurations[SourceConfigurationIndex].Orientation;
            Vector3f CMPosition = aBodies[Counter].aConfigurations[SourceConfigurationIndex].CMPosition;
            FloatBuffer fb = BufferUtils.createFloatBuffer(16);

            CreateOpenGLTransform(Orientation, CMPosition, fb);

            GL11.glPushMatrix();
            GL11.glMultMatrix(fb);
            GL11.glCallList(Counter + 1);
            GL11.glPopMatrix();
        }
        GL11.glDisable(GL11.GL_LIGHTING);

        // draw springs
        if (WorldSpringsActive == 1) {
            GL11.glBegin(GL11.GL_LINES);
            for (int i = 0; i < NumberOfWorldSprings; i++) {
                world_spring Spring = aWorldSprings[i];
                rigid_body_configuration Configuration = aBodies[Spring.BodyIndex].aConfigurations[0];

                GL11.glColor3f(1.0f, 0.5f, 1.0f);

                Vector3f v = Configuration.aBoundingVertices[Spring.VertexIndex];
                GL11.glVertex3f(v.x, v.y, v.z);

                Vector3f u = Spring.Anchor;
                GL11.glVertex3f(u.x, u.y, u.z);
            }
            GL11.glEnd();
        }

        if (BodySpringsActive == 1) {
            GL11.glBegin(GL11.GL_LINES);
            for (int i = 0; i < NumberOfBodySprings; i++) {
                body_spring Spring = aBodySprings[i];

                rigid_body_configuration Configuration0 = aBodies[Spring.Body0Index].aConfigurations[SourceConfigurationIndex];
                rigid_body_configuration Configuration1 = aBodies[Spring.Body1Index].aConfigurations[SourceConfigurationIndex];

                GL11.glColor3f(1.0f, 1.0f, 1.0f);
                Vector3f v = Configuration0.aBoundingVertices[Spring.Body0VertexIndex];
                GL11.glVertex3f(v.x, v.y, v.z);

                Vector3f u = Configuration1.aBoundingVertices[Spring.Body1VertexIndex];
                GL11.glVertex3f(u.x, u.y, u.z);
            }
            GL11.glEnd();
        }

        // draw walls...I didn't bother implementing the cool clipper thing
        // like in the 2D sample
        GL11.glColor3f(1.0f, 1.0f, 1.0f);

        // do a big linestrip to get most of edges
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3f(-WorldSize / 2.0f, -WorldSize / 2.0f, -WorldSize / 2.0f);
        GL11.glVertex3f(-WorldSize / 2.0f, -WorldSize / 2.0f, WorldSize / 2.0f);
        GL11.glVertex3f(-WorldSize / 2.0f, WorldSize / 2.0f, WorldSize / 2.0f);
        GL11.glVertex3f(WorldSize / 2.0f, WorldSize / 2.0f, WorldSize / 2.0f);
        GL11.glVertex3f(WorldSize / 2.0f, -WorldSize / 2.0f, WorldSize / 2.0f);
        GL11.glVertex3f(WorldSize / 2.0f, -WorldSize / 2.0f, -WorldSize / 2.0f);
        GL11.glEnd();

        // fill in the stragglers
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3f(WorldSize / 2.0f, -WorldSize / 2.0f, WorldSize / 2.0f);
        GL11.glVertex3f(-WorldSize / 2.0f, -WorldSize / 2.0f, WorldSize / 2.0f);
        GL11.glVertex3f(WorldSize / 2.0f, WorldSize / 2.0f, WorldSize / 2.0f);
        GL11.glVertex3f(WorldSize / 2.0f, WorldSize / 2.0f, -WorldSize / 2.0f);
        GL11.glVertex3f(-WorldSize / 2.0f, WorldSize / 2.0f, WorldSize / 2.0f);
        GL11.glVertex3f(-WorldSize / 2.0f, WorldSize / 2.0f, -WorldSize / 2.0f);
        GL11.glEnd();

        // draw floor
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(0.5f, 0.5f, 0.5f);
        GL11.glVertex3f(WorldSize / 2.0f, WorldSize / 2.0f, -WorldSize / 2.0f);
        GL11.glVertex3f(-WorldSize / 2.0f, WorldSize / 2.0f, -WorldSize / 2.0f);
        GL11.glVertex3f(-WorldSize / 2.0f, -WorldSize / 2.0f, -WorldSize / 2.0f);
        GL11.glVertex3f(WorldSize / 2.0f, -WorldSize / 2.0f, -WorldSize / 2.0f);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    public void Simulate(float DeltaTime) {
        float CurrentTime = 0.0f;
        float TargetTime = DeltaTime;

        while (CurrentTime < DeltaTime) {
            ComputeForces(SourceConfigurationIndex);

            Integrate(TargetTime - CurrentTime);

            CalculateVertices(TargetConfigurationIndex);

            CheckForCollisions(TargetConfigurationIndex);

            if (CollisionState == collision_state.Penetrating) {
                // we simulated too far, so subdivide time and try again
                TargetTime = (CurrentTime + TargetTime) / 2.0f;

                // blow up if we aren't moving forward each step, which is
                // probably caused by interpenetration at the frame start
                // We never penetrate...it was causing the simulation to freeze.
            } else {
                // either colliding or clear

                if (CollisionState == collision_state.Colliding) {
                    // @todo handle multiple simultaneous collisions

                    int Counter = 0;
                    do {
                        ResolveCollisions(TargetConfigurationIndex);
                        Counter++;
                    } while ((CheckForCollisions(TargetConfigurationIndex) == collision_state.Colliding) && (Counter < 100));
                }

                // we made a successful step, so swap configurations
                // to "save" the data for the next step
                CurrentTime = TargetTime;
                TargetTime = DeltaTime;

                SourceConfigurationIndex = SourceConfigurationIndex == 1 ? 0 : 1;
                TargetConfigurationIndex = TargetConfigurationIndex == 1 ? 0 : 1;
            }
        }
    }

    private void ComputeForces(int ConfigurationIndex) {
        for (int Counter = 0; Counter < NumberOfBodies; Counter++) {
            rigid_body Body = aBodies[Counter];
            rigid_body_configuration Configuration = Body.aConfigurations[ConfigurationIndex];

            // clear forces
            Configuration.Torque = Vector3f.Zero();
            Configuration.CMForce = Vector3f.Zero();

            if (GravityActive == 1) {
                Configuration.CMForce.add(Vector3f.Divide(Gravity, Body.OneOverMass));
            }

            if (DampingActive == 1) {
                Configuration.CMForce.add(Vector3f.Multiply(Configuration.CMVelocity, -Kdl));
                Configuration.Torque.add(Vector3f.Multiply(Configuration.AngularVelocity, -Kda));
            } else {
                // there's always a little damping because our integrator sucks
                Configuration.CMForce.add(Vector3f.Multiply(Configuration.CMVelocity, -NoKdl));
                Configuration.Torque.add(Vector3f.Multiply(Configuration.AngularVelocity, -NoKda));
            }
        }

        if (BodySpringsActive == 1) {
            for (int i = 0; i < NumberOfBodySprings; i++) {
                body_spring SpringStructure = aBodySprings[i];

                rigid_body Body0 = aBodies[SpringStructure.Body0Index];
                rigid_body_configuration Configuration0 = Body0.aConfigurations[ConfigurationIndex];

                Vector3f Position0 = Configuration0.aBoundingVertices[SpringStructure.Body0VertexIndex];
                Vector3f U0 = Vector3f.Subtract(Position0, Configuration0.CMPosition);
                Vector3f VU0 = Vector3f.Add(Configuration0.CMVelocity, Vector3f.Cross(Configuration0.AngularVelocity, U0));

                rigid_body Body1 = aBodies[SpringStructure.Body1Index];
                rigid_body_configuration Configuration1 = Body1.aConfigurations[ConfigurationIndex];

                Vector3f Position1 = Configuration1.aBoundingVertices[SpringStructure.Body1VertexIndex];
                Vector3f U1 = Vector3f.Subtract(Position1, Configuration1.CMPosition);
                Vector3f VU1 = Vector3f.Add(Configuration1.CMVelocity, Vector3f.Cross(Configuration1.AngularVelocity, U1));

                // spring goes from 0 to 1
                Vector3f SpringVector = Vector3f.Subtract(Position1, Position0);
                Vector3f Spring = Vector3f.Multiply(SpringVector, -Kbs);

                Vector3f RelativeVelocity = Vector3f.Subtract(VU1, VU0);
                // project velocity onto spring to get damping vector
                // this is basically a Gram-Schmidt projection
                Vector3f DampingForce = Vector3f.Multiply(Vector3f.Multiply(SpringVector, Vector3f.Dot(RelativeVelocity, SpringVector) / Vector3f.Dot(SpringVector, SpringVector)), -Kbd);

                Spring.add(DampingForce);

                Configuration0.CMForce.subtract(Spring);
                Configuration0.Torque.subtract(Vector3f.Cross(U0, Spring));

                Configuration1.CMForce.add(Spring);
                Configuration1.Torque.add(Vector3f.Cross(U1, Spring));
            }
        }

        if (WorldSpringsActive == 1) {
            for (int i = 0; i < NumberOfWorldSprings; i++) {
                world_spring SpringStructure = aWorldSprings[i];

                rigid_body Body = aBodies[SpringStructure.BodyIndex];
                rigid_body_configuration Configuration = Body.aConfigurations[ConfigurationIndex];

                Vector3f Position = Configuration.aBoundingVertices[SpringStructure.VertexIndex];
                Vector3f U = Vector3f.Subtract(Position, Configuration.CMPosition);
                Vector3f VU = Vector3f.Add(Configuration.CMVelocity, Vector3f.Cross(Configuration.AngularVelocity, U));

                Vector3f Spring = Vector3f.Multiply(Vector3f.Subtract(Position, SpringStructure.Anchor), -Kws);
                // project velocity onto spring to get damping vector
                // this is basically a Gram-Schmidt projection
                Vector3f DampingForce = Vector3f.Multiply(Vector3f.Multiply(Spring, (Vector3f.Dot(VU, Spring) / Vector3f.Dot(Spring, Spring))), -Kwd);

                Spring.add(DampingForce);

                Configuration.CMForce.add(Spring);
                Configuration.Torque.add(Vector3f.Cross(U, Spring));
            }
        }
    }

    private void CalculateVertices(int ConfigurationIndex) {
        for (int Counter = 0; Counter < NumberOfBodies; Counter++) {
            rigid_body Body = aBodies[Counter];
            rigid_body_configuration Configuration = Body.aConfigurations[ConfigurationIndex];

            Matrix3f A = Configuration.Orientation;
            Vector3f R = Configuration.CMPosition;

            for (int i = 0; i < Body.NumberOfBoundingVertices; i++) {
                Configuration.aBoundingVertices[i] = Vector3f.Add(R, Vector3f.MultiplyByMatrix(A, Body.aBodyBoundingVertices[i]));
            }
        }
    }

    public void Integrate(float DeltaTime) {
        int Counter;

        for (Counter = 0; Counter < NumberOfBodies; Counter++) {
            rigid_body_configuration Source
                    = aBodies[Counter].aConfigurations[SourceConfigurationIndex];
            rigid_body_configuration Target
                    = aBodies[Counter].aConfigurations[TargetConfigurationIndex];

            // integrate primary quantities
            Target.CMPosition = Vector3f.Add(Source.CMPosition, Vector3f.Multiply(Source.CMVelocity, DeltaTime));

            Target.Orientation = Matrix3f.Add(Source.Orientation, Matrix3f.Multiply(Matrix3f.SkewSymmetric(Vector3f.Multiply(Source.AngularVelocity, DeltaTime)), Source.Orientation));

            Target.CMVelocity = Vector3f.Add(Source.CMVelocity, Vector3f.Multiply(Source.CMForce, DeltaTime * aBodies[Counter].OneOverMass));

            Target.AngularMomentum = Vector3f.Add(Source.AngularMomentum, Vector3f.Multiply(Source.Torque, DeltaTime));

            Matrix3f.OrthonormalizeOrientation(Target.Orientation);

            // compute auxiliary quantities
            Target.InverseWorldInertiaTensor = Matrix3f.Multiply(Matrix3f.Multiply(Target.Orientation, aBodies[Counter].InverseBodyInertiaTensor), Matrix3f.Transpose(Target.Orientation));

            Target.AngularVelocity = Vector3f.MultiplyByMatrix(Target.InverseWorldInertiaTensor, Target.AngularMomentum);
        }
    }

    private collision_state CheckForCollisions(int ConfigurationIndex) {
        // be optimistic!
        CollisionState = collision_state.Clear;
        float DepthEpsilon = 0.01f;

        for (int BodyIndex = 0; (BodyIndex < NumberOfBodies) && (CollisionState != collision_state.Penetrating); BodyIndex++) {
            rigid_body Body = aBodies[BodyIndex];
            rigid_body_configuration Configuration = Body.aConfigurations[ConfigurationIndex];

            for (int Counter = 0; (Counter < Body.NumberOfBoundingVertices) && (CollisionState != collision_state.Penetrating); Counter++) {
                Vector3f Position = Configuration.aBoundingVertices[Counter];
                Vector3f U = Vector3f.Subtract(Position, Configuration.CMPosition);

                Vector3f Velocity = Vector3f.Add(Configuration.CMVelocity, Vector3f.Cross(Configuration.AngularVelocity, U));

                for (int WallIndex = 0; (WallIndex < NumberOfWalls) && (CollisionState != collision_state.Penetrating); WallIndex++) {
                    wall Wall = aWalls[WallIndex];

                    float axbyczd = Vector3f.Dot(Position, Wall.Normal) + Wall.d;

                    if (axbyczd < 0.0f) {
                        float RelativeVelocity = Vector3f.Dot(Wall.Normal, Velocity);

                        if (RelativeVelocity < 0.0f) {
                            CollisionState = collision_state.Colliding;
                            CollisionNormal = Wall.Normal;
                            CollidingCornerIndex = Counter;
                            CollidingBodyIndex = BodyIndex;
                        }
                    }
                }
            }
        }

        return CollisionState;
    }

    public void ResolveCollisions(int ConfigurationIndex) {
        rigid_body Body = aBodies[CollidingBodyIndex];
        rigid_body_configuration Configuration = Body.aConfigurations[ConfigurationIndex];

        Vector3f Position = Configuration.aBoundingVertices[CollidingCornerIndex];

        Vector3f R = Vector3f.Subtract(Position, Configuration.CMPosition);

        Vector3f Velocity = Vector3f.Add(Configuration.CMVelocity, Vector3f.Cross(Configuration.AngularVelocity, R));

        float ImpulseNumerator = -(1.0f + Body.CoefficientOfRestitution) * Vector3f.Dot(Velocity, CollisionNormal);

        float ImpulseDenominator = Body.OneOverMass + Vector3f.Dot(Vector3f.Cross(Vector3f.MultiplyByMatrix(Configuration.InverseWorldInertiaTensor, Vector3f.Cross(R, CollisionNormal)), R), CollisionNormal);

        Vector3f Impulse = Vector3f.Multiply(CollisionNormal, ImpulseNumerator / ImpulseDenominator);

        // apply impulse to primary quantities
        Configuration.CMVelocity.add(Vector3f.Multiply(Impulse, Body.OneOverMass));
        Configuration.AngularMomentum.add(Vector3f.Cross(R, Impulse));

        // compute affected auxiliary quantities
        Configuration.AngularVelocity = Vector3f.MultiplyByMatrix(Configuration.InverseWorldInertiaTensor, Configuration.AngularMomentum);
    }

}

class body_spring {

    public int Body0Index;
    public int Body0VertexIndex;
    public int Body1Index;
    public int Body1VertexIndex;

    public body_spring(int b0i, int b0v, int b1i, int b1v) {
        Body0Index = b0i;
        Body0VertexIndex = b0v;
        Body1Index = b1i;
        Body1VertexIndex = b1v;
    }

}

class world_spring {

    public int BodyIndex;
    public int VertexIndex;
    public Vector3f Anchor;

    public world_spring(int B, int V, Vector3f A) {
        BodyIndex = B;
        VertexIndex = V;
        Anchor = A;
    }

}

class wall {

    public Vector3f Normal;        // inward pointing
    public float d;                // ax + by + cz + d = 0

}

class rigid_body {

    private int MaxNumberOfBoundingVertices = 20;
    private int NumberOfConfigurations = 2;
    public float OneOverMass;
    public Matrix3f InverseBodyInertiaTensor = Matrix3f.Identity();
    public float CoefficientOfRestitution;
    public int NumberOfBoundingVertices;
    public Vector3f aBodyBoundingVertices[] = new Vector3f[MaxNumberOfBoundingVertices];
    public rigid_body_configuration aConfigurations[] = new rigid_body_configuration[NumberOfConfigurations];

    public rigid_body() {
        aConfigurations[0] = new rigid_body_configuration();
        aConfigurations[1] = new rigid_body_configuration();
    }

}

class rigid_body_configuration {

    private int MaxNumberOfBoundingVertices = 20;
    // primary quantities
    public Vector3f CMPosition = Vector3f.Zero();
    public Matrix3f Orientation = Matrix3f.Identity();
    public Vector3f CMVelocity = Vector3f.Zero();
    public Vector3f AngularMomentum = Vector3f.Zero();
    public Vector3f CMForce = Vector3f.Zero();
    public Vector3f Torque = Vector3f.Zero();
    // auxiliary quantities
    public Matrix3f InverseWorldInertiaTensor = Matrix3f.Identity();
    public Vector3f AngularVelocity = Vector3f.Zero();
    public Vector3f aBoundingVertices[] = new Vector3f[MaxNumberOfBoundingVertices];

}
