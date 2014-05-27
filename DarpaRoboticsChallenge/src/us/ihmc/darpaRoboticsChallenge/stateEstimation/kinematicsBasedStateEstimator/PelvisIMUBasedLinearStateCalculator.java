package us.ihmc.darpaRoboticsChallenge.stateEstimation.kinematicsBasedStateEstimator;

import java.util.List;

import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoVariable;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class PelvisIMUBasedLinearStateCalculator
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   
   private final BooleanYoVariable enableEstimationOfGravity = new BooleanYoVariable("enableEstimationOfGravity", registry);
   private final DoubleYoVariable alphaGravityEstimation = new DoubleYoVariable("alphaGravityEstimation", registry);
   private final AlphaFilteredYoVariable gravityEstimation = new AlphaFilteredYoVariable("gravityEstimation", registry, alphaGravityEstimation);
   private final FrameVector gravity = new FrameVector(worldFrame);
   
   private final YoFrameVector rootJointLinearVelocity = new YoFrameVector("imuRootJointLinearVelocity", worldFrame, registry);
   private final YoFrameVector rootJointPosition = new YoFrameVector("imuRootJointPosition", worldFrame, registry);

   private final YoFrameVector yoMeasurementFrameLinearVelocityInWorld;
   private final YoFrameVector yoLinearAccelerationMeasurementInWorld;
   private final YoFrameVector yoLinearAccelerationMeasurement;
   
   private final BooleanYoVariable imuBasedStateEstimationEnabled = new BooleanYoVariable("imuBasedStateEstimationEnabled", registry);

   private final ReferenceFrame measurementFrame;
   
   private final SixDoFJoint rootJoint;
   
   private final double estimatorDT;
   
   // Temporary variables
   private final FrameVector linearAccelerationMeasurement = new FrameVector();
   private final FrameVector measurementFrameLinearVelocityPrevValue = new FrameVector();
   private final FrameVector tempRootJointVelocityIntegrated = new FrameVector();

   private final IMUSensorReadOnly imuProcessedOutput;

   @Deprecated
   private final BooleanYoVariable useOldHackishAccelIntegrationWorkingForAtlas = new BooleanYoVariable("useOldHackishAccelIntegrationWorkingForAtlas", registry);

   public PelvisIMUBasedLinearStateCalculator(FullInverseDynamicsStructure inverseDynamicsStructure, List<? extends IMUSensorReadOnly> imuProcessedOutputs, double estimatorDT,
         double gravitationalAcceleration, YoVariableRegistry parentRegistry)
   {
      this.estimatorDT = estimatorDT;
      this.rootJoint = inverseDynamicsStructure.getRootJoint();

      enableEstimationOfGravity.set(false);
      gravityEstimation.reset();
      gravityEstimation.update(Math.abs(gravitationalAcceleration));
      gravity.setIncludingFrame(worldFrame, 0.0, 0.0, gravityEstimation.getDoubleValue());
      
      if (imuProcessedOutputs.size() == 0)
      {
         imuProcessedOutput = null;
         imuBasedStateEstimationEnabled.set(false);
      }
      else
      {
         if (imuProcessedOutputs.size() > 1)
            System.out.println(getClass().getSimpleName() + ": More than 1 IMU sensor, using only the first one: " + imuProcessedOutputs.get(0).getSensorName());
         imuProcessedOutput = imuProcessedOutputs.get(0);
         imuBasedStateEstimationEnabled.set(true);
      }
      
      if (imuBasedStateEstimationEnabled.getBooleanValue())
      {
         measurementFrame = imuProcessedOutput.getMeasurementFrame();
      }
      else
      {
         measurementFrame = null;
      }
      
      yoMeasurementFrameLinearVelocityInWorld = new YoFrameVector("imuLinearVelocityInWorld", worldFrame, registry);
      yoLinearAccelerationMeasurement = new YoFrameVector("imuLinearAcceleration", measurementFrame, registry);
      yoLinearAccelerationMeasurementInWorld = new YoFrameVector("imuLinearAccelerationInWorld", worldFrame, registry);
      
      parentRegistry.addChild(registry);
   }

   @Deprecated
   public void useHackishAccelerationIntegration(boolean val)
   {
      useOldHackishAccelIntegrationWorkingForAtlas.set(val);
   }
   
   public void enableGravityEstimation(boolean enable)
   {
      enableEstimationOfGravity.set(enable);
   }

   public void enableEsimationModule(boolean enable)
   {
      imuBasedStateEstimationEnabled.set(enable);
   }

   public boolean isEstimationEnabled()
   {
      return imuBasedStateEstimationEnabled.getBooleanValue();
   }
   
   public void setAlphaGravityEstimation(double alphaFilter)
   {
      alphaGravityEstimation.set(alphaFilter);
   }
   
   private void updateLinearAccelerationMeasurement(FrameVector measurementFrameLinearVelocityPrevValue)
   {
      if (!isEstimationEnabled())
         return;

      linearAccelerationMeasurement.setToZero(measurementFrame);
      imuProcessedOutput.getLinearAccelerationMeasurement(linearAccelerationMeasurement.getVector());
      if (enableEstimationOfGravity.getBooleanValue())
         gravityEstimation.update(linearAccelerationMeasurement.length());
      gravity.setIncludingFrame(worldFrame, 0.0, 0.0, gravityEstimation.getDoubleValue());

      // Update acceleration in world (minus gravity)
      linearAccelerationMeasurement.changeFrame(worldFrame);
      linearAccelerationMeasurement.sub(gravity);
      yoLinearAccelerationMeasurementInWorld.set(linearAccelerationMeasurement);

      // Update acceleration in local frame (minus gravity)
      gravity.changeFrame(measurementFrame);
      linearAccelerationMeasurement.setToZero(measurementFrame);
      imuProcessedOutput.getLinearAccelerationMeasurement(linearAccelerationMeasurement.getVector());
      linearAccelerationMeasurement.sub(gravity);
      yoLinearAccelerationMeasurement.set(linearAccelerationMeasurement);
   }

   private final FrameVector correctionVelocityForMeasurementFrameOffset = new FrameVector();

   public void updateIMUAndRootJointLinearVelocity(FrameVector rootJointVelocity, FrameVector imuLinearVelocityInWorldToUpdate)
   {
      measurementFrameLinearVelocityPrevValue.setToZero(measurementFrame);
      updateLinearAccelerationMeasurement(measurementFrameLinearVelocityPrevValue);
      
      yoLinearAccelerationMeasurementInWorld.getFrameTupleIncludingFrame(linearAccelerationMeasurement);
      linearAccelerationMeasurement.scale(estimatorDT);
      imuLinearVelocityInWorldToUpdate.add(linearAccelerationMeasurement);
      
      yoMeasurementFrameLinearVelocityInWorld.set(imuLinearVelocityInWorldToUpdate);
      
      rootJointVelocity.set(imuLinearVelocityInWorldToUpdate);

      getCorrectionVelocityForMeasurementFrameOffset(correctionVelocityForMeasurementFrameOffset);
      correctionVelocityForMeasurementFrameOffset.changeFrame(worldFrame);
      rootJointVelocity.sub(correctionVelocityForMeasurementFrameOffset);
   }
   
   public void correctIMULinearVelocity(FrameVector rootJointVelocity, FrameVector imuLinearVelocityInWorldToUpdate)
   {
      imuLinearVelocityInWorldToUpdate.set(rootJointVelocity);
      imuLinearVelocityInWorldToUpdate.add(correctionVelocityForMeasurementFrameOffset);
   }

   public void updatePelvisLinearVelocity(FrameVector rootJointLinearVelocityPrevValue, FrameVector rootJointLinearVelocityToPack)
   {
      if (!isEstimationEnabled())
         throw new RuntimeException("IMU estimation module for pelvis linear velocity is disabled.");

      measurementFrameLinearVelocityPrevValue.setToZero(measurementFrame);
      updateLinearAccelerationMeasurement(measurementFrameLinearVelocityPrevValue);

      yoLinearAccelerationMeasurementInWorld.getFrameTupleIncludingFrame(linearAccelerationMeasurement);
      linearAccelerationMeasurement.scale(estimatorDT);
      rootJointLinearVelocity.set(rootJointLinearVelocityPrevValue);
      rootJointLinearVelocity.add(linearAccelerationMeasurement);
      rootJointLinearVelocity.getFrameTupleIncludingFrame(rootJointLinearVelocityToPack);
   }

   public void updatePelvisPosition(FramePoint rootJointPositionPrevValue, FramePoint rootJointPositionToPack)
   {
      if (!isEstimationEnabled())
         throw new RuntimeException("IMU estimation module for pelvis linear velocity is disabled.");

      rootJointLinearVelocity.getFrameTupleIncludingFrame(tempRootJointVelocityIntegrated);
      tempRootJointVelocityIntegrated.scale(estimatorDT);

      rootJointPosition.set(rootJointPositionPrevValue);
      rootJointPosition.add(tempRootJointVelocityIntegrated);
      rootJointPosition.getFrameTupleIncludingFrame(rootJointPositionToPack);
   }

   private void getCorrectionVelocityForMeasurementFrameOffset(FrameVector correctionTermToPack)
   {
      Twist tempTwist = new Twist();
      rootJoint.packJointTwist(tempTwist);
      
      FrameVector angularPart = new FrameVector();
      tempTwist.packAngularPart(angularPart);
      
      FramePoint measurementOffset = new FramePoint(measurementFrame);
      measurementOffset.changeFrame(rootJoint.getFrameAfterJoint());
      
      correctionTermToPack.setToZero(angularPart.getReferenceFrame());
      correctionTermToPack.cross(angularPart, measurementOffset);
   }
}
