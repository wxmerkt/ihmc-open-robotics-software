package us.ihmc.aware.controller.force;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.partNames.LegJointName;
import us.ihmc.aware.communication.QuadrupedControllerInputProvider;
import us.ihmc.aware.controller.common.DivergentComponentOfMotionController;
import us.ihmc.aware.controller.force.taskSpaceController.*;
import us.ihmc.aware.params.ParameterMap;
import us.ihmc.aware.params.ParameterMapRepository;
import us.ihmc.aware.util.ContactState;
import us.ihmc.aware.parameters.QuadrupedRuntimeEnvironment;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedRobotParameters;
import us.ihmc.quadrupedRobotics.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedJointNameMap;
import us.ihmc.quadrupedRobotics.supportPolygon.QuadrupedSupportPolygon;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.*;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class QuadrupedVirtualModelBasedStandController implements QuadrupedForceController
{
   private final SDFFullRobotModel fullRobotModel;
   private final DoubleYoVariable robotTimestamp;
   private final YoGraphicsListRegistry yoGraphicsListRegistry;
   private final QuadrupedJointNameMap jointNameMap;
   private final double controlDT;
   private final double gravity;
   private final double mass;
   private final QuadrupedControllerInputProvider inputProvider;

   // parameters
   private final ParameterMap params;
   private final static String BODY_ORIENTATION_PROPORTIONAL_GAINS = "bodyOrientationProportionalGains";
   private final static String BODY_ORIENTATION_DERIVATIVE_GAINS = "bodyOrientationDerivativeGains";
   private final static String BODY_ORIENTATION_INTEGRAL_GAINS = "bodyOrientationIntegralGains";
   private final static String BODY_ORIENTATION_MAX_INTEGRAL_ERROR = "bodyOrientationMaxIntegralError";
   private final static String DCM_POSITION_PROPORTIONAL_GAINS = "dcmPositionProportionalGains";
   private final static String DCM_POSITION_DERIVATIVE_GAINS = "dcmPositionDerivativeGains";
   private final static String DCM_POSITION_INTEGRAL_GAINS = "dcmPositionIntegralGains";
   private final static String DCM_POSITION_MAX_INTEGRAL_ERROR = "dcmPositionMaxIntegralError";
   private final static String COM_POSITION_PROPORTIONAL_GAINS = "comPositionProportionalGains";
   private final static String COM_POSITION_DERIVATIVE_GAINS = "comPositionDerivativeGains";
   private final static String COM_POSITION_INTEGRAL_GAINS = "comPositionIntegralGains";
   private final static String COM_POSITION_MAX_INTEGRAL_ERROR = "comPositionMaxIntegralError";

   // frames
   private final QuadrupedReferenceFrames referenceFrames;
   private final PoseReferenceFrame supportFrame;
   private final ReferenceFrame worldFrame;
   QuadrupedSupportPolygon supportPolygon;
   FramePoint supportCentroid;
   FrameOrientation supportOrientation;

   // dcm controller
   private final FramePoint dcmPositionEstimate;
   private final FramePoint dcmPositionSetpoint;
   private final FrameVector dcmVelocitySetpoint;
   private final DivergentComponentOfMotionController dcmPositionController;

   // task space controller
   private final QuadrupedTaskSpaceCommands taskSpaceCommands;
   private final QuadrupedTaskSpaceSetpoints taskSpaceSetpoints;
   private final QuadrupedTaskSpaceEstimates taskSpaceEstimates;
   private final QuadrupedTaskSpaceEstimator taskSpaceEstimator;
   private final QuadrupedTaskSpaceController taskSpaceController;
   private final QuadrupedTaskSpaceControllerSettings taskSpaceControllerSettings;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   public QuadrupedVirtualModelBasedStandController(QuadrupedRuntimeEnvironment runtimeEnvironment, QuadrupedRobotParameters robotParameters, ParameterMapRepository parameterMapRepository, QuadrupedControllerInputProvider inputProvider)
   {
      this.fullRobotModel = runtimeEnvironment.getFullRobotModel();
      this.robotTimestamp = runtimeEnvironment.getRobotTimestamp();
      this.yoGraphicsListRegistry = runtimeEnvironment.getGraphicsListRegistry();
      this.jointNameMap = robotParameters.getJointMap();
      this.controlDT = runtimeEnvironment.getControlDT();
      this.gravity = 9.81;
      this.mass = fullRobotModel.getTotalMass();
      this.inputProvider = inputProvider;

      // parameters
      this.params = parameterMapRepository.get(QuadrupedVirtualModelBasedStandController.class);
      params.setDefault(BODY_ORIENTATION_PROPORTIONAL_GAINS, 5000, 5000, 2500);
      params.setDefault(BODY_ORIENTATION_DERIVATIVE_GAINS, 750, 750, 500);
      params.setDefault(BODY_ORIENTATION_INTEGRAL_GAINS, 0, 0, 0);
      params.setDefault(BODY_ORIENTATION_MAX_INTEGRAL_ERROR, 0);
      params.setDefault(DCM_POSITION_PROPORTIONAL_GAINS, 2, 2, 0);
      params.setDefault(DCM_POSITION_DERIVATIVE_GAINS, 0, 0, 0);
      params.setDefault(DCM_POSITION_INTEGRAL_GAINS, 0, 0, 0);
      params.setDefault(DCM_POSITION_MAX_INTEGRAL_ERROR, 0);
      params.setDefault(COM_POSITION_PROPORTIONAL_GAINS, 0, 0, 5000);
      params.setDefault(COM_POSITION_DERIVATIVE_GAINS, 0, 0, 750);
      params.setDefault(COM_POSITION_INTEGRAL_GAINS, 0, 0, 0);
      params.setDefault(COM_POSITION_MAX_INTEGRAL_ERROR, 0);

      // frames
      referenceFrames = new QuadrupedReferenceFrames(fullRobotModel, jointNameMap, robotParameters.getPhysicalProperties());
      ReferenceFrame comFrame = referenceFrames.getCenterOfMassZUpFrame();
      supportFrame = new PoseReferenceFrame("SupportFrame", ReferenceFrame.getWorldFrame());
      worldFrame = ReferenceFrame.getWorldFrame();

      // support
      supportPolygon = new QuadrupedSupportPolygon();
      supportCentroid = new FramePoint();
      supportOrientation = new FrameOrientation();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         supportPolygon.setFootstep(robotQuadrant, new FramePoint());
      }

      // dcm controller
      dcmPositionEstimate = new FramePoint();
      dcmPositionSetpoint = new FramePoint();
      dcmVelocitySetpoint = new FrameVector();
      dcmPositionController = new DivergentComponentOfMotionController("dcmPosition", comFrame, controlDT, mass, gravity, inputProvider.getComPositionInput().getZ(), registry);

      // task space controllers
      taskSpaceCommands = new QuadrupedTaskSpaceCommands();
      taskSpaceSetpoints = new QuadrupedTaskSpaceSetpoints();
      taskSpaceEstimates = new QuadrupedTaskSpaceEstimates();
      taskSpaceEstimator = new QuadrupedTaskSpaceEstimator(fullRobotModel, referenceFrames, jointNameMap, registry, yoGraphicsListRegistry);
      taskSpaceController = new QuadrupedTaskSpaceController(fullRobotModel, referenceFrames, jointNameMap, robotParameters.getQuadrupedJointLimits(), controlDT, registry, yoGraphicsListRegistry);
      taskSpaceControllerSettings = new QuadrupedTaskSpaceControllerSettings();

      runtimeEnvironment.getParentRegistry().addChild(registry);
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   private void updateEstimates()
   {
      // update task space estimates
      taskSpaceEstimator.compute(taskSpaceEstimates);

      // update dcm estimate
      taskSpaceEstimates.getComPosition().changeFrame(worldFrame);
      taskSpaceEstimates.getComVelocity().changeFrame(worldFrame);
      dcmPositionEstimate.changeFrame(worldFrame);
      dcmPositionEstimate.set(taskSpaceEstimates.getComVelocity());
      dcmPositionEstimate.scale(1.0 / dcmPositionController.getNaturalFrequency());
      dcmPositionEstimate.add(taskSpaceEstimates.getComPosition());

      // compute support frame
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         taskSpaceEstimates.getSolePosition().get(robotQuadrant).changeFrame(supportPolygon.getReferenceFrame());
         supportPolygon.setFootstep(robotQuadrant, taskSpaceEstimates.getSolePosition().get(robotQuadrant));
         taskSpaceEstimates.getSolePosition().get(robotQuadrant).changeFrame(ReferenceFrame.getWorldFrame());
      }
      double minFrontFootHeight = Math.min(taskSpaceEstimates.getSolePosition().get(RobotQuadrant.FRONT_LEFT).getZ(), taskSpaceEstimates.getSolePosition().get(RobotQuadrant.FRONT_RIGHT).getZ());
      double minHindFootHeight = Math.min(taskSpaceEstimates.getSolePosition().get(RobotQuadrant.HIND_LEFT).getZ(), taskSpaceEstimates.getSolePosition().get(RobotQuadrant.HIND_RIGHT).getZ());

      // compute support frame (centroid and nominal orientation)
      supportCentroid.changeFrame(supportPolygon.getReferenceFrame());
      supportPolygon.getCentroid2d(supportCentroid);
      supportCentroid.changeFrame(ReferenceFrame.getWorldFrame());
      supportCentroid.setZ((minFrontFootHeight + minHindFootHeight) / 2.0);
      supportOrientation.changeFrame(supportPolygon.getReferenceFrame());
      supportOrientation.setYawPitchRoll(supportPolygon.getNominalYaw(), 0.0, 0.0);
      supportFrame.setPoseAndUpdate(supportCentroid, supportOrientation);
   }

   private void updateSetpoints()
   {
      // update desired dcm position
      dcmPositionSetpoint.changeFrame(supportFrame);
      dcmPositionSetpoint.set(inputProvider.getComVelocityInput());
      dcmPositionSetpoint.scale(1.0 / dcmPositionController.getNaturalFrequency());
      dcmPositionSetpoint.add(inputProvider.getComPositionInput());
      dcmVelocitySetpoint.setToZero(supportFrame);
      dcmPositionController.compute(taskSpaceSetpoints.getComForceFeedforward(), dcmPositionSetpoint, dcmVelocitySetpoint, dcmPositionEstimate);

      // update desired com position and velocity
      taskSpaceSetpoints.getComPosition().changeFrame(supportFrame);
      taskSpaceSetpoints.getComPosition().set(inputProvider.getComPositionInput());
      taskSpaceSetpoints.getComVelocity().changeFrame(supportFrame);
      taskSpaceSetpoints.getComVelocity().set(inputProvider.getComVelocityInput());
      taskSpaceSetpoints.getComForceFeedforward().changeFrame(supportFrame);
      taskSpaceSetpoints.getComForceFeedforward().setZ(mass * gravity);

      // update desired body orientation and angular rate
      taskSpaceSetpoints.getBodyOrientation().changeFrame(supportFrame);
      taskSpaceSetpoints.getBodyOrientation().set(inputProvider.getBodyOrientationInput());
      taskSpaceSetpoints.getBodyAngularVelocity().changeFrame(supportFrame);
      taskSpaceSetpoints.getBodyAngularVelocity().set(inputProvider.getBodyAngularRateInput());

      // update joint setpoints
      taskSpaceController.compute(taskSpaceControllerSettings, taskSpaceSetpoints, taskSpaceEstimates, taskSpaceCommands);

   }

   @Override public QuadrupedForceControllerEvent process()
   {
      dcmPositionController.setComHeight(inputProvider.getComPositionInput().getZ());
      updateEstimates();
      updateSetpoints();
      return null;
   }

   @Override public void onEntry()
   {
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         for (int i = 0; i < jointNameMap.getLegJointNames().length; i++)
         {
            // initialize leg joint mode to force control
            LegJointName legJointName = jointNameMap.getLegJointNames()[i];
            String jointName = jointNameMap.getLegJointName(robotQuadrant, legJointName);
            OneDoFJoint joint = fullRobotModel.getOneDoFJointByName(jointName);
            joint.setUnderPositionControl(false);
         }
      }

      // initialize dcm controller settings
      dcmPositionController.setGains(
            params.getVolatileArray(DCM_POSITION_PROPORTIONAL_GAINS),
            params.getVolatileArray(DCM_POSITION_DERIVATIVE_GAINS),
            params.getVolatileArray(DCM_POSITION_INTEGRAL_GAINS),
            params.get(DCM_POSITION_MAX_INTEGRAL_ERROR));
      dcmPositionController.reset();

      // initialize task space controller
      taskSpaceEstimator.compute(taskSpaceEstimates);
      taskSpaceSetpoints.initialize(taskSpaceEstimates);
      taskSpaceControllerSettings.initialize();
      taskSpaceControllerSettings.setComForceCommandWeights(1.0, 1.0, 1.0);
      taskSpaceControllerSettings.setComTorqueCommandWeights(1.0, 1.0, 1.0);
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         taskSpaceControllerSettings.setSoleForceCommandWeights(robotQuadrant, 0.0, 0.0, 0.0);
         taskSpaceControllerSettings.setContactState(robotQuadrant, ContactState.IN_CONTACT);
      }
      taskSpaceControllerSettings.setBodyOrientationFeedbackGains(
            params.getVolatileArray(BODY_ORIENTATION_PROPORTIONAL_GAINS),
            params.getVolatileArray(BODY_ORIENTATION_DERIVATIVE_GAINS),
            params.getVolatileArray(BODY_ORIENTATION_INTEGRAL_GAINS),
            params.get(BODY_ORIENTATION_MAX_INTEGRAL_ERROR)
      );
      taskSpaceControllerSettings.setComPositionFeedbackGains(
            params.getVolatileArray(COM_POSITION_PROPORTIONAL_GAINS),
            params.getVolatileArray(COM_POSITION_DERIVATIVE_GAINS),
            params.getVolatileArray(COM_POSITION_INTEGRAL_GAINS),
            params.get(COM_POSITION_MAX_INTEGRAL_ERROR)
      );
      taskSpaceController.reset();
   }

   @Override public void onExit()
   {
   }
}
