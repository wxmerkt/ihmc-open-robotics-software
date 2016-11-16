package us.ihmc.footstepPlanning.polygonWiggling;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;

import org.junit.Test;

import us.ihmc.footstepPlanning.testTools.PlanningTestTools;
import us.ihmc.graphics3DDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.graphics3DDescription.yoGraphics.plotting.YoArtifactPolygon;
import us.ihmc.plotting.Plotter;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.geometry.ConvexPolygon2dCalculator;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.math.frames.YoFrameConvexPolygon2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.tools.continuousIntegration.IntegrationCategory;
import us.ihmc.tools.testing.MutationTestingTools;
import us.ihmc.tools.thread.ThreadTools;

@ContinuousIntegrationAnnotations.ContinuousIntegrationPlan(categories = IntegrationCategory.FAST)
public class PolygonWigglingTest
{
   private static final boolean visualize = false;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final ArtifactList artifacts = new ArtifactList(getClass().getSimpleName());

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testSimpleProjection()
   {
      ConvexPolygon2d plane = new ConvexPolygon2d();
      plane.addVertex(0.0, 0.0);
      plane.addVertex(0.5, 0.0);
      plane.addVertex(0.0, 0.5);
      plane.addVertex(0.5, 0.5);
      plane.update();

      ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
      RigidBodyTransform initialFootTransform = new RigidBodyTransform();
      initialFootTransform.setRotationYawAndZeroTranslation(Math.toRadians(-30.0));
      initialFootTransform.setTranslation(-0.1, -0.3, 0.0);
      initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

      ConvexPolygon2d foot = PolygonWiggler.wigglePolygon(initialFoot, plane, new WiggleParameters());

      if (visualize)
      {
         addPolygonToArtifacts("Plane", plane, Color.BLACK);
         addPolygonToArtifacts("InitialFoot", initialFoot, Color.RED);
         addPolygonToArtifacts("Foot", foot, Color.BLUE);
         showPlotterAndSleep(artifacts);
      }

      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(foot, 1.0e-5, plane));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testSimpleProjectionWithWiggleLimits()
   {
      ConvexPolygon2d plane = new ConvexPolygon2d();
      plane.addVertex(0.0, 0.0);
      plane.addVertex(0.5, 0.0);
      plane.addVertex(0.0, 0.5);
      plane.addVertex(0.5, 0.5);
      plane.update();

      ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
      RigidBodyTransform initialFootTransform = new RigidBodyTransform();
      initialFootTransform.setRotationYawAndZeroTranslation(Math.toRadians(-30.0));
      initialFootTransform.setTranslation(-0.1, -0.3, 0.0);
      initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

      WiggleParameters parameters = new WiggleParameters();
      parameters.minX = 0.02;
      parameters.maxX = 0.02;
      ConvexPolygon2d foot = PolygonWiggler.wigglePolygon(initialFoot, plane, parameters);

      if (visualize)
      {
         addPolygonToArtifacts("Plane", plane, Color.BLACK);
         addPolygonToArtifacts("InitialFoot", initialFoot, Color.RED);
         addPolygonToArtifacts("Foot", foot, Color.BLUE);
         showPlotterAndSleep(artifacts);
      }

      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(foot, 1.0e-5, plane));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testProjectionThatRequiredRotation()
   {
      ConvexPolygon2d plane = PlanningTestTools.createDefaultFootPolygon();
      plane.scale(1.1);

      ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
      RigidBodyTransform initialFootTransform = new RigidBodyTransform();
      initialFootTransform.setRotationYawAndZeroTranslation(Math.toRadians(-13.0));
      initialFootTransform.setTranslation(-0.1, -0.3, 0.0);
      initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

      ConvexPolygon2d foot = PolygonWiggler.wigglePolygon(initialFoot, plane, new WiggleParameters());

      if (visualize)
      {
         addPolygonToArtifacts("Plane", plane, Color.BLACK);
         addPolygonToArtifacts("InitialFoot", initialFoot, Color.RED);
         addPolygonToArtifacts("Foot", foot, Color.BLUE);
         showPlotterAndSleep(artifacts);
      }

      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(foot, 1.0e-5, plane));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testRotationLimit()
   {
      ConvexPolygon2d plane = PlanningTestTools.createDefaultFootPolygon();
      plane.scale(1.1);

      ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
      RigidBodyTransform initialFootTransform = new RigidBodyTransform();
      initialFootTransform.setRotationYawAndZeroTranslation(Math.toRadians(-13.0));
      initialFootTransform.setTranslation(-0.1, -0.3, 0.0);
      initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

      WiggleParameters parameters = new WiggleParameters();
      parameters.maxYaw = 0.0;
      parameters.minYaw = 0.0;
      ConvexPolygon2d foot = PolygonWiggler.wigglePolygon(initialFoot, plane, parameters);

      if (visualize)
      {
         addPolygonToArtifacts("Plane", plane, Color.BLACK);
         addPolygonToArtifacts("InitialFoot", initialFoot, Color.RED);
         addPolygonToArtifacts("Foot", foot, Color.BLUE);
         showPlotterAndSleep(artifacts);
      }

      // The rotation limit should make it impossible to reach goal since rotation is required in this case.
      assertFalse(ConvexPolygon2dCalculator.isPolygonInside(foot, 1.0e-5, plane));
   }

   /**
    * In this case the QP returns a solution that is not valid but close.
    */
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testImpossibleCase()
   {
      ConvexPolygon2d plane = PlanningTestTools.createDefaultFootPolygon();
      plane.scale(0.9);

      ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
      RigidBodyTransform initialFootTransform = new RigidBodyTransform();
      initialFootTransform.setRotationYawAndZeroTranslation(Math.toRadians(-13.0));
      initialFootTransform.setTranslation(-0.1, -0.3, 0.0);
      initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

      ConvexPolygon2d foot = PolygonWiggler.wigglePolygon(initialFoot, plane, new WiggleParameters());

      if (visualize)
      {
         addPolygonToArtifacts("Plane", plane, Color.BLACK);
         addPolygonToArtifacts("InitialFoot", initialFoot, Color.RED);
         addPolygonToArtifacts("Foot", foot, Color.BLUE);
         showPlotterAndSleep(artifacts);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testProjectionOnlyTranslation()
   {
      ConvexPolygon2d plane = new ConvexPolygon2d();
      plane.addVertex(0.0, 0.0);
      plane.addVertex(0.5, 0.0);
      plane.addVertex(0.0, 0.5);
      plane.addVertex(0.5, 0.5);
      plane.update();

      ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
      RigidBodyTransform initialFootTransform = new RigidBodyTransform();
      initialFootTransform.setTranslationAndIdentityRotation(-0.2, 0.25, 0.0);
      initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

      ConvexPolygon2d foot = PolygonWiggler.wigglePolygon(initialFoot, plane, new WiggleParameters());

      if (visualize)
      {
         addPolygonToArtifacts("Plane", plane, Color.BLACK);
         addPolygonToArtifacts("InitialFoot", initialFoot, Color.RED);
         addPolygonToArtifacts("Foot", foot, Color.BLUE);
         showPlotterAndSleep(artifacts);
      }

      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(foot, 1.0e-5, plane));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testProjectionTranslationLimits()
   {
      ConvexPolygon2d plane = new ConvexPolygon2d();
      plane.addVertex(0.0, 0.0);
      plane.addVertex(0.5, 0.0);
      plane.addVertex(0.0, 0.5);
      plane.addVertex(0.5, 0.5);
      plane.update();

      ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
      RigidBodyTransform initialFootTransform = new RigidBodyTransform();
      initialFootTransform.setTranslationAndIdentityRotation(-0.2, 0.25, 0.0);
      initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

      WiggleParameters parameters = new WiggleParameters();
      parameters.maxX = 0.1;
      ConvexPolygon2d foot = PolygonWiggler.wigglePolygon(initialFoot, plane, parameters);

      if (visualize)
      {
         addPolygonToArtifacts("Plane", plane, Color.BLACK);
         addPolygonToArtifacts("InitialFoot", initialFoot, Color.RED);
         addPolygonToArtifacts("Foot", foot, Color.BLUE);
         showPlotterAndSleep(artifacts);
      }

      assertFalse(ConvexPolygon2dCalculator.isPolygonInside(foot, 1.0e-5, plane));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testCompexProjectionArea()
   {
      ConvexPolygon2d plane = new ConvexPolygon2d();
      plane.addVertex(0.5, -0.5);
      plane.addVertex(-0.8, 0.5);
      plane.addVertex(0.5, 1.5);
      plane.addVertex(0.75, 0.5);
      plane.update();

      double yawLimit = Math.toRadians(15.0);
      WiggleParameters wiggleParameters = new WiggleParameters();
      wiggleParameters.maxYaw = yawLimit;
      wiggleParameters.minYaw = -yawLimit;
      wiggleParameters.maxX = 10.0;
      wiggleParameters.minX = -10.0;
      wiggleParameters.maxY = 10.0;
      wiggleParameters.minY = -10.0;
      Random random = new Random(482787427467L);

      for (int i = 0; i < 100; i++)
      {
         ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
         if (random.nextBoolean())
         {
            initialFoot.removeVertex(random.nextInt(4));
            initialFoot.update();
         }

         RigidBodyTransform initialFootTransform = new RigidBodyTransform();
         double x = 5.0 * (random.nextDouble() - 0.5);
         double y = 5.0 * (random.nextDouble() - 0.5);
         double theta = 2.0 * (random.nextDouble() - 0.5) * yawLimit;
         initialFootTransform.setRotationYawAndZeroTranslation(theta);
         initialFootTransform.setTranslation(x, y, 0.0);
         initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

         ConvexPolygon2d foot = PolygonWiggler.wigglePolygon(initialFoot, plane, wiggleParameters);
         assertTrue(ConvexPolygon2dCalculator.isPolygonInside(foot, 1.0e-5, plane));
         if (ConvexPolygon2dCalculator.isPolygonInside(initialFoot, 1.0e-5, plane))
            assertTrue(initialFoot.epsilonEquals(foot, 1.0e-10));

         if (visualize)
         {
            addPolygonToArtifacts("Plane" + i, plane, Color.BLACK);
            addPolygonToArtifacts("InitialFoot" + i, initialFoot, Color.RED);
            addPolygonToArtifacts("Foot" + i, foot, Color.BLUE);
         }
      }

      if (visualize)
      {
         showPlotterAndSleep(artifacts);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 300000)
   public void testProjectionIntoPlanarRegion()
   {
      ArrayList<ConvexPolygon2d> planes = new ArrayList<>();
      ConvexPolygon2d plane1 = new ConvexPolygon2d();
      plane1.addVertex(0.0, 0.0);
      plane1.addVertex(0.5, 0.0);
      plane1.addVertex(0.0, 0.5);
      plane1.addVertex(0.5, 0.5);
      plane1.update();
      planes.add(plane1);
      ConvexPolygon2d plane2 = new ConvexPolygon2d();
      plane2.addVertex(-0.6, 0.0);
      plane2.addVertex(-0.1, 0.0);
      plane2.addVertex(-0.6, 0.5);
      plane2.addVertex(-0.1, 0.5);
      plane2.update();
      planes.add(plane2);
      ConvexPolygon2d plane3 = new ConvexPolygon2d();
      plane3.addVertex(-0.25, 0.0);
      plane3.addVertex(0.25, 0.0);
      plane3.addVertex(-0.25, -0.5);
      plane3.addVertex(0.25, -0.5);
      plane3.update();
      planes.add(plane3);

      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      PlanarRegion region = new PlanarRegion(transformToWorld, planes);

      ConvexPolygon2d initialFoot = PlanningTestTools.createDefaultFootPolygon();
      RigidBodyTransform initialFootTransform = new RigidBodyTransform();
      initialFootTransform.setRotationYawAndZeroTranslation(Math.toRadians(-30.0));
      initialFootTransform.setTranslation(-0.05, 0.09, 0.0);
      initialFoot.applyTransformAndProjectToXYPlane(initialFootTransform);

      RigidBodyTransform wiggleTransfrom = PolygonWiggler.wigglePolygonIntoRegion(initialFoot, region, new WiggleParameters());
      assertFalse(wiggleTransfrom == null);

      ConvexPolygon2d foot = new ConvexPolygon2d(initialFoot);
      foot.applyTransformAndProjectToXYPlane(wiggleTransfrom);

      if (visualize)
      {
         for (int i = 0; i < region.getNumberOfConvexPolygons(); i++)
            addPolygonToArtifacts("Plane" + i, region.getConvexPolygon(i), Color.BLACK);
         addPolygonToArtifacts("InitialFoot", initialFoot, Color.RED);
         addPolygonToArtifacts("Foot", foot, Color.BLUE);
         showPlotterAndSleep(artifacts);
      }

      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(foot, 1.0e-5, plane2));
   }

   private void addPolygonToArtifacts(String name, ConvexPolygon2d polygon, Color color)
   {
      YoFrameConvexPolygon2d yoPlanePolygon = new YoFrameConvexPolygon2d(name + "Polygon", worldFrame, 10, registry);
      artifacts.add(new YoArtifactPolygon(name, yoPlanePolygon , color, false));
      yoPlanePolygon.setConvexPolygon2d(polygon);
   }

   private static void showPlotterAndSleep(ArtifactList artifacts)
   {
      Plotter plotter = new Plotter();
      plotter.setViewRange(2.0);
      artifacts.setVisible(true);
      JFrame frame = new JFrame("PolygonWigglingTest");
      Dimension preferredSize = new Dimension(600, 600);
      frame.setPreferredSize(preferredSize);
      frame.add(plotter.getJPanel(), BorderLayout.CENTER);
      frame.setSize(preferredSize);
      frame.setVisible(true);
      artifacts.addArtifactsToPlotter(plotter);
      ThreadTools.sleepForever();
   }

   public static void main(String[] args)
   {
      String targetTests = PolygonWigglingTest.class.getName();
      String targetClassesInSamePackage = PolygonWiggler.class.getName();
      MutationTestingTools.doPITMutationTestAndOpenResult(targetTests, targetClassesInSamePackage);
   }
}