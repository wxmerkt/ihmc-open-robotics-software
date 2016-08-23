package us.ihmc.commonWalkingControlModules.controllerCore.command;

public enum ControllerCoreCommandType
{
   TASKSPACE, POINT, ORIENTATION, JOINTSPACE, MOMENTUM,
   PRIVILEGED_CONFIGURATION, LIMIT_REDUCTION, JOINT_LIMIT_ENFORCEMENT,
   EXTERNAL_WRENCH, PLANE_CONTACT_STATE, CENTER_OF_PRESSURE,
   JOINT_ACCELERATION_INTEGRATION,
   COMMAND_LIST,
   VIRTUAL_WRENCH, CONTROLLED_BODIES
}
