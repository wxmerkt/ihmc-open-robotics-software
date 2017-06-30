package us.ihmc.robotDataLogger;

import java.nio.LongBuffer;
import java.util.List;

import us.ihmc.yoVariables.variable.YoVariable;

public class RegistryBuffer
{
   private final int variableOffset;
   
   private final long[] data;
   private final YoVariable<?>[] variables;

   private long timestamp;

   private long uid = 0;
   
   public RegistryBuffer(int variableOffset, List<YoVariable<?>> variables)
   {
      this.variableOffset = variableOffset;
      this.data = new long[variables.size()];
      this.variables = variables.toArray(new YoVariable[variables.size()]);
   }
   
   public void update(long timestamp, long uid)
   {
      this.uid = uid;
      this.timestamp = timestamp;
      for(int i = 0; i < variables.length; i++)
      {
         data[i] = variables[i].getValueAsLongBits();
      }
   }
   
   public void getIntoBuffer(LongBuffer buffer, int initialOffset)
   {
      buffer.position(initialOffset + variableOffset);
      buffer.put(data, 0, variables.length);
   }
   
   public void getJointStatesInBuffer(LongBuffer buffer, int offset)
   {
      // Nothing to do here
   }
   
   public static class Builder implements us.ihmc.concurrent.Builder<RegistryBuffer>
   {
      private final int variableOffset;
      private final List<YoVariable<?>> variables;

      public Builder(int variableOffset, List<YoVariable<?>> variables)
      {
         this.variableOffset = variableOffset;
         this.variables = variables;
      }

      @Override
      public RegistryBuffer newInstance()
      {
         return new RegistryBuffer(variableOffset, variables);
      }
      
   }
   

   public long getTimestamp()
   {
      return timestamp;
   }

   public long getUid()
   {
      return uid;
   }
}
