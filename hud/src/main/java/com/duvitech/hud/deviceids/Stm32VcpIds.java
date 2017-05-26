package com.duvitech.hud.deviceids;

/**
 * Created by gvige on 4/16/2017.
 */

public class Stm32VcpIds {

    /* Different products and vendors of XdcVcp family
    */
    private static final ConcreteDevice[] stm32devices = new ConcreteDevice[]
            {
                    new ConcreteDevice(0x5740, 0x0483), // VCP (Virtual Com Port)
            };

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        for(int i=0;i<=stm32devices.length-1;i++)
        {
            if(stm32devices[i].vendorId == vendorId && stm32devices[i].productId == productId )
            {
                return true;
            }
        }
        return false;
    }

    private static class ConcreteDevice
    {
        public int vendorId;
        public int productId;

        public ConcreteDevice(int vendorId, int productId)
        {
            this.vendorId = vendorId;
            this.productId = productId;
        }
    }

}
