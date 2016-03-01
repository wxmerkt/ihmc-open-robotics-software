package us.ihmc.robotics.geometry;

import us.ihmc.robotics.geometry.transformables.Transformable;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public abstract class AbstractFrameObject<T extends Transformable<T>> extends AbstractReferenceFrameHolder implements FrameObject<T>
{
   protected final T transformableDataObject;
   protected ReferenceFrame referenceFrame;

   public AbstractFrameObject(ReferenceFrame referenceFrame, T transformableDataObject)
   {
      this.transformableDataObject = transformableDataObject;
      this.referenceFrame = referenceFrame;
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return referenceFrame;
   }

   @Override
   public void changeFrame(ReferenceFrame desiredFrame)
   {
      if (desiredFrame != referenceFrame)
      {
         referenceFrame.verifySameRoots(desiredFrame);
         RigidBodyTransform referenceTf, desiredTf;

         if ((referenceTf = referenceFrame.getTransformToRoot()) != null)
         {
            transformableDataObject.applyTransform(referenceTf);
         }

         if ((desiredTf = desiredFrame.getInverseTransformToRoot()) != null)
         {
            transformableDataObject.applyTransform(desiredTf);
         }

         referenceFrame = desiredFrame;
      }

      // otherwise: in the right frame already, so do nothing
   }

   @Override
   public void changeFrameUsingTransform(ReferenceFrame desiredFrame, RigidBodyTransform transformToNewFrame)
   {
      transformableDataObject.applyTransform(transformToNewFrame);
      referenceFrame = desiredFrame;
   }

   @Override
   public void applyTransform(RigidBodyTransform transform)
   {
      transformableDataObject.applyTransform(transform);
   }
   
   @Override
   public void set(T other)
   {
      this.transformableDataObject.set(other);
   }

   @Override
   public void setToZero()
   {
      this.transformableDataObject.setToZero();
   }

   @Override
   public void setToNaN()
   {
      this.transformableDataObject.setToNaN();      
   }

   @Override
   public boolean containsNaN()
   {
      return this.transformableDataObject.containsNaN();
   }

   @Override
   public boolean epsilonEquals(T other, double epsilon)
   {
      return this.transformableDataObject.epsilonEquals(other, epsilon);
   }

   @Override
   public void setToZero(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
      setToZero();
   }

   @Override
   public void setToNaN(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
      setToNaN();
   }

}
