package us.ihmc.exampleSimulations.fourBarLinkage;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.Axis;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.RotationalInertiaCalculator;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.robotController.RobotController;


public class FourBarLinkageRobot extends Robot
{	
   private static final boolean SHOW_CONTACT_GRAPHICS = true;
   
   private final ExternalForcePoint efpJoint1to4;
   private final ExternalForcePoint efpJoint1to2;
   
	public FourBarLinkageRobot(String name, FourBarLinkageParameters fourBarLinkageParameters, Vector3d offsetWorld, YoVariableRegistry registry) 
	{	
		super("fourBarLinkageRobot" + name);
		
		Vector3d offsetJoint12 = new Vector3d(0, 0, fourBarLinkageParameters.linkageLength_1);
		Vector3d offsetJoint23 = new Vector3d(0, 0, fourBarLinkageParameters.linkageLength_2);
		Vector3d offsetJoint34 = new Vector3d(0, 0, fourBarLinkageParameters.linkageLength_3);
		
		PinJoint rootPinJoint = new PinJoint("rootPinJoint", offsetWorld, this, Axis.Y);
		rootPinJoint.setInitialState(fourBarLinkageParameters.angle_1, 0.0);
		Link fourBarLink1 = fourBarLink("fourBarLink1" + name, fourBarLinkageParameters.linkageLength_1, fourBarLinkageParameters.radius_1, fourBarLinkageParameters.mass_1);
		rootPinJoint.setLink(fourBarLink1);
		rootPinJoint.setDamping(fourBarLinkageParameters.damping_1);
		this.addRootJoint(rootPinJoint);
		
		PinJoint pinJoint2 = new PinJoint("pinJoint2", offsetJoint12 , this, Axis.Y);
		pinJoint2.setInitialState(fourBarLinkageParameters.angle_2, 0.0);
		Link fourBarLink2 = fourBarLink("fourBarLink2" + name, fourBarLinkageParameters.linkageLength_2, fourBarLinkageParameters.radius_2, fourBarLinkageParameters.mass_2);
		pinJoint2.setLink(fourBarLink2);
		pinJoint2.setDamping(fourBarLinkageParameters.damping_2);
		rootPinJoint.addJoint(pinJoint2);
		
		PinJoint pinJoint3 = new PinJoint("pinJoint3", offsetJoint23 , this, Axis.Y);
		pinJoint3.setInitialState(fourBarLinkageParameters.angle_3, 0.0);
		Link fourBarLink3 = fourBarLink("fourBarLink3" + name, fourBarLinkageParameters.linkageLength_3, fourBarLinkageParameters.radius_3, fourBarLinkageParameters.mass_3);
		pinJoint3.setLink(fourBarLink3);
		pinJoint3.setDamping(fourBarLinkageParameters.damping_3);
		pinJoint2.addJoint(pinJoint3);
		
		PinJoint pinJoint4 = new PinJoint("pinJoint4", offsetJoint34, this, Axis.Y);
		pinJoint4.setInitialState(fourBarLinkageParameters.angle_3, 0.0);
      Link fourBarLink4 = fourBarLink("fourBarLink4" + name, fourBarLinkageParameters.linkageLength_3, fourBarLinkageParameters.radius_4, fourBarLinkageParameters.mass_4);
      pinJoint4.setLink(fourBarLink4);
      pinJoint4.setDamping(fourBarLinkageParameters.damping_4);
      pinJoint3.addJoint(pinJoint4);
      
      efpJoint1to4 = new ExternalForcePoint(name + "efp_1to4", new Vector3d(0.0, 0.0, fourBarLinkageParameters.linkageLength_4), this);
      efpJoint1to2 = new ExternalForcePoint(name + "efp_1to2", new Vector3d(fourBarLinkageParameters.linkageLength_1, 0.0, 0.0), this);
      
      pinJoint4.addExternalForcePoint(efpJoint1to4);
      pinJoint2.addExternalForcePoint(efpJoint1to2);
      
      if(SHOW_CONTACT_GRAPHICS)
      {
         double radius = 0.1;

         Graphics3DObject joint2Graphics = pinJoint2.getLink().getLinkGraphics();
         joint2Graphics.identity();
         joint2Graphics.translate(efpJoint1to2.getOffsetCopy());
         joint2Graphics.addSphere(radius, YoAppearance.LemonChiffon());

         Graphics3DObject joint4Graphics = pinJoint4.getLink().getLinkGraphics();
         joint4Graphics.identity();
         joint4Graphics.translate(efpJoint1to4.getOffsetCopy());
         joint4Graphics.addSphere(2*radius, YoAppearance.Glass());
      }
	}

   public Link fourBarLink(String linkName, double length, double radius, double mass) 
	{
		Link link = new Link(linkName);
		Matrix3d inertiaCylinder = createInertiaMatrixCylinder(linkName, length, radius, mass);
		link.setMomentOfInertia(inertiaCylinder);
		link.setMass(mass);
		link.setComOffset(new Vector3d(0.0, 0.0, length / 2.0));

		Graphics3DObject linkGraphics = new Graphics3DObject();
		linkGraphics.addCylinder(length, radius, YoAppearance.HotPink());
		link.setLinkGraphics(linkGraphics);
		link.addCoordinateSystemToCOM(0.15);
		return link;
	}
   
   public ExternalForcePoint getEfpJoint1to2()
   {
      return efpJoint1to2;
   }
   
   public ExternalForcePoint getEfpJoint1to4()
   {
      return efpJoint1to4;
   }   

	private Matrix3d createInertiaMatrixCylinder(String linkName, double length, double radius, double mass) 
	{
		Matrix3d inertiaCylinder = new Matrix3d();
		inertiaCylinder = RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidCylinder(mass, radius, length, Axis.Z);
		return inertiaCylinder;
	}
}