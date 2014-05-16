package us.ihmc.atlas.ObstacleCourseTests;

import javax.vecmath.Vector3d;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCourseFlatTest;

public class AtlasObstacleCourseFlatTest extends DRCObstacleCourseFlatTest
{

   private DRCRobotModel robotModel = new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, false, false);
   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }
   @Override
   protected Vector3d getFootSlipVector()
   {
      return new Vector3d(0.06, -0.06, 0.0);
   }
   @Override
   protected DoubleYoVariable getPelvisOrientationErrorVariableName(SimulationConstructionSet scs)
   {
      return (DoubleYoVariable) scs.getVariable("WalkingHighLevelHumanoidController.RootJointAngularAccelerationControlModule.pelvisAxisAngleOrientationController",
                                                "pelvisOrientationErrorMagnitude");
   }

}
